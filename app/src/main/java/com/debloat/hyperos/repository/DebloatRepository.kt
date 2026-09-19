package com.debloat.hyperos.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import com.debloat.hyperos.data.PresetRoot
import com.debloat.hyperos.data.dao.DebloatDao
import com.debloat.hyperos.data.entity.DebloatAppEntity
import com.debloat.hyperos.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Single source of truth for debloat app data: seeds Room from the bundled
 * preset JSON on first run, exposes reactive queries, and drives Shizuku
 * shell calls for install-state refresh and batch uninstall/restore.
 */
class DebloatRepository(
    private val context: Context,
    private val dao: DebloatDao
) {
    /**
     * فحص هل التطبيق هو تطبيق نظام أم تطبيق مستخدم (User app).
     */
    private fun isSystemAppPackage(packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (t: Throwable) {
            // إذا كان التطبيق محذوفاً مسبقاً أو غير موجود حالياً، نعتبره نظام افتراضياً
            true
        }
    }

    suspend fun refreshInstallStates() = withContext(Dispatchers.IO) {
        val installedPackages: Set<String> = if (ShizukuManager.hasPermission()) {
            ShizukuManager.getInstalledPackageNames()
        } else {
            // فحص عبر نظام أندرويد مباشرة بدون شيزوكو
            try {
                val pm = context.packageManager
                pm.getInstalledPackages(0).map { it.packageName }.toSet()
            } catch (t: Throwable) {
                emptySet()
            }
        }

        if (installedPackages.isEmpty()) return@withContext

        val current = dao.getAllOnce()
        for (app in current) {
            val installed = installedPackages.contains(app.packageName)
            val removedByApp = app.isRemovedByApp && !installed
            dao.updateInstallState(app.packageName, installed, removedByApp)
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun observeAll(): Flow<List<DebloatAppEntity>> = dao.observeAll()

    fun observeCategories(): Flow<List<String>> = dao.observeCategories()

    /**
     * Seeds the Room database from assets/debloat_presets.json the first
     * time the app runs (no-op on subsequent launches).
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        if (dao.count() > 0) return@withContext

        val raw = context.assets.open("debloat_presets.json").bufferedReader().use { it.readText() }
        val preset = json.decodeFromString<PresetRoot>(raw)

        val entities = preset.categories.flatMap { cat ->
            cat.apps.map { app ->
                DebloatAppEntity(
                    packageName = app.packageName,
                    name = app.name,
                    category = cat.category,
                    isInstalled = true, // optimistic default; corrected by refreshInstallStates()
                    isRemovedByApp = false,
                    isSelected = false,
                    isSystemApp = isSystemAppPackage(app.packageName) // فحص وتحديد نوع التطبيق بدقة
                )
            }
        }
        dao.insertAll(entities)
    }

    // ---- Selection ----

    suspend fun toggleSelection(packageName: String, selected: Boolean) =
        dao.setSelected(packageName, selected)

    suspend fun setCategorySelection(category: String, selected: Boolean) =
        dao.setSelectedForCategory(category, selected)

    suspend fun clearSelection() = dao.clearAllSelection()

    suspend fun getSelectedApps(): List<DebloatAppEntity> = dao.getSelected()

    // ---- Batch operations ----

    sealed class BatchOpResult {
        data class Progress(val completed: Int, val total: Int, val currentLabel: String) : BatchOpResult()
        data class Finished(val successCount: Int, val failureCount: Int, val failures: List<String>) : BatchOpResult()
    }

    /**
     * Uninstalls every currently-selected, currently-installed app
     * sequentially via `pm uninstall --user 0`, syncing Room after each
     * step so the UI reflects progress live.
     */
    suspend fun uninstallSelected(onProgress: suspend (BatchOpResult.Progress) -> Unit): BatchOpResult.Finished =
        withContext(Dispatchers.IO) {
            val targets = dao.getSelected().filter { it.isInstalled }
            var success = 0
            val failures = mutableListOf<String>()

            targets.forEachIndexed { index, app ->
                onProgress(BatchOpResult.Progress(index, targets.size, app.name))
                val result = ShizukuManager.uninstallPackage(app.packageName)
                if (result.isSuccess) {
                    dao.updateInstallState(app.packageName, isInstalled = false, isRemovedByApp = true)
                    success++
                } else {
                    dao.setSelected(app.packageName, false)
                    failures += "${app.name}:${result.stderr.ifBlank { "unknown error" }}"
                }
            }
            BatchOpResult.Finished(success, failures.size, failures)
        }

    /**
     * Restores every currently-selected, currently-removed app via
     * `cmd package install-existing`.
     */
    suspend fun restoreSelected(onProgress: suspend (BatchOpResult.Progress) -> Unit): BatchOpResult.Finished =
        withContext(Dispatchers.IO) {
            val targets = dao.getSelected().filter { !it.isInstalled }
            var success = 0
            val failures = mutableListOf<String>()

            targets.forEachIndexed { index, app ->
                onProgress(BatchOpResult.Progress(index, targets.size, app.name))
                val result = ShizukuManager.restorePackage(app.packageName)
                if (result.isSuccess) {
                    dao.updateInstallState(app.packageName, isInstalled = true, isRemovedByApp = false)
                    success++
                } else {
                    dao.setSelected(app.packageName, false)
                    failures += "${app.name}:${result.stderr.ifBlank { "unknown error" }}"
                }
            }
            BatchOpResult.Finished(success, failures.size, failures)
        }

    /**
     * تجلب أحدث قائمة حزم من GitHub وتدمج الجديد منها في Room تلقائياً.
     */
    /**
     * تجلب أحدث قائمة حزم من GitHub وتقوم بالمزامنة الكاملة:
     * - إضافة الحزم الجديدة
     * - تحديث الأسماء والفئات المعدلة
     * - حذف الحزم التي أُزيلت من السيرفر
     */
    suspend fun syncWithRemote(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // كاسر الكاش لضمان وصول التعديل فوراً
            val remoteUrl = "https://raw.githubusercontent.com/AnasAbdullh/Debloat-HyperOS/main/app/src/main/assets/debloat_presets.json?nocache=${System.currentTimeMillis()}"
            val connection = (java.net.URL(remoteUrl).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                useCaches = false
                defaultUseCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }

            if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                val remotePresets = json.decodeFromString<PresetRoot>(rawJson)

                // تحويل القادمة من السيرفر إلى خريطة Map ليسهل التعامل معها
                val remoteAppsMap = mutableMapOf<String, Pair<String, String>>() // packageName -> Pair(name, category)
                remotePresets.categories.forEach { cat ->
                    cat.apps.forEach { app ->
                        remoteAppsMap[app.packageName] = Pair(app.name, cat.category)
                    }
                }

                val currentLocalApps = dao.getAllOnce()
                val currentLocalMap = currentLocalApps.associateBy { it.packageName }

                val toInsertOrUpdate = mutableListOf<DebloatAppEntity>()
                val toDelete = mutableListOf<DebloatAppEntity>()

                // 1. فحص الإضافة والتحديث
                remoteAppsMap.forEach { (pkg, pair) ->
                    val (newName, newCat) = pair
                    val existing = currentLocalMap[pkg]

                    if (existing != null) {
                        // إذا تم تعديل الاسم أو التصنيف أونلاين
                        if (existing.name != newName || existing.category != newCat) {
                            toInsertOrUpdate.add(
                                existing.copy(name = newName, category = newCat)
                            )
                        }
                    } else {
                        // حزمة جديدة كلياً
                        toInsertOrUpdate.add(
                            DebloatAppEntity(
                                packageName = pkg,
                                name = newName,
                                category = newCat,
                                isInstalled = true,
                                isRemovedByApp = false,
                                isSelected = false,
                                isSystemApp = isSystemAppPackage(pkg)
                            )
                        )
                    }
                }

                // 2. فحص الحذف (أي حزمة كانت موجودة محلياً ولم تعد في السيرفر)
                currentLocalApps.forEach { localApp ->
                    if (!remoteAppsMap.containsKey(localApp.packageName)) {
                        toDelete.add(localApp)
                    }
                }

                // تطبيق التغييرات على قاعدة البيانات Room
                if (toDelete.isNotEmpty()) {
                    dao.deleteAll(toDelete)
                }
                if (toInsertOrUpdate.isNotEmpty()) {
                    dao.insertAll(toInsertOrUpdate)
                }

                refreshInstallStates()

                val totalChanges = toInsertOrUpdate.size + toDelete.size
                Result.success(totalChanges)
            } else {
                Result.failure(Exception("HTTP error ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}