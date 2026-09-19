package com.debloat.hyperos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debloat.hyperos.data.entity.DebloatAppEntity
import com.debloat.hyperos.repository.DebloatRepository
import com.debloat.hyperos.shizuku.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FilterTab { INSTALLED, REMOVED, ALL }

enum class BatchMode { UNINSTALL, RESTORE }

/**
 * تحديد نوع العملية بذكاء لمنع التعارض:
 * - إذا وجد خليط بين مثبت ومحذوف -> يعيد null (غير مسموح)
 * - إذا كان التبويب REMOVED أو العناصر المحددة كلها محذوفة -> RESTORE
 * - إذا كانت العناصر المحددة مثبتة -> UNINSTALL
 */
fun resolveBatchMode(
    filterTab: FilterTab,
    installedSelectedCount: Int,
    removedSelectedCount: Int
): BatchMode? = when {
    installedSelectedCount > 0 && removedSelectedCount > 0 -> null
    filterTab == FilterTab.REMOVED || (removedSelectedCount > 0 && installedSelectedCount == 0) -> BatchMode.RESTORE
    installedSelectedCount > 0 -> BatchMode.UNINSTALL
    else -> null
}

data class CategoryGroup(
    val category: String,
    val apps: List<DebloatAppEntity>,
    val removedCount: Int,
    val totalCount: Int
)

data class DebloatUiState(
    val isLoading: Boolean = true,
    val groups: List<CategoryGroup> = emptyList(),
    val filterTab: FilterTab = FilterTab.INSTALLED,
    val selectedCount: Int = 0,
    val installedSelectedCount: Int = 0,
    val removedSelectedCount: Int = 0,
    val shizukuConnected: Boolean = false,
    val shizukuPermissionGranted: Boolean = false,
    val batchInProgress: Boolean = false,
    val batchProgressLabel: String = "",
    val batchProgressCompleted: Int = 0,
    val batchProgressTotal: Int = 0,
    val lastResultMessage: String? = null
)

class DebloatViewModel(
    private val repository: DebloatRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _filterTab = MutableStateFlow(FilterTab.INSTALLED)
    private val _batchState = MutableStateFlow(BatchUiState())
    private val _lastResultMessage = MutableStateFlow<String?>(null)

    private data class BatchUiState(
        val inProgress: Boolean = false,
        val label: String = "",
        val completed: Int = 0,
        val total: Int = 0
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<DebloatUiState> = combine(
        combine(repository.observeAll(), _filterTab, ::Pair),
        combine(_batchState, ShizukuManager.connectionState, ::Pair),
        combine(_lastResultMessage, _isLoading, ::Pair)
    ) { (apps, tab), (batch, shizukuState), (resultMsg, loading) ->
        val groups = apps.groupBy { it.category }.map { (category, categoryApps) ->
            val visibleApps = categoryApps.filter { app ->
                when (tab) {
                    FilterTab.INSTALLED -> app.isInstalled
                    FilterTab.REMOVED -> !app.isInstalled
                    FilterTab.ALL -> true
                }
            }
            CategoryGroup(
                category = category,
                apps = visibleApps,
                removedCount = categoryApps.count { !it.isInstalled },
                totalCount = categoryApps.size
            )
        }.filter { it.apps.isNotEmpty() || tab == FilterTab.ALL }
            .sortedBy { it.category }

        val selectedApps = apps.filter { it.isSelected }
        val installedSelected = selectedApps.count { it.isInstalled }
        val removedSelected = selectedApps.count { !it.isInstalled }

        DebloatUiState(
            isLoading = loading,
            groups = groups,
            filterTab = tab,
            selectedCount = selectedApps.size,
            installedSelectedCount = installedSelected,
            removedSelectedCount = removedSelected,
            shizukuConnected = shizukuState !is ShizukuManager.ConnectionState.Disconnected,
            shizukuPermissionGranted = shizukuState is ShizukuManager.ConnectionState.PermissionGranted,
            batchInProgress = batch.inProgress,
            batchProgressLabel = batch.label,
            batchProgressCompleted = batch.completed,
            batchProgressTotal = batch.total,
            lastResultMessage = resultMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebloatUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
            if (ShizukuManager.hasPermission() && !ShizukuManager.testShellExecution()) {
                _lastResultMessage.value = "Shizuku shell execution isn't working on this build — try updating Shizuku."
            }
            refreshFromDevice()
        }
    }

    fun refreshFromDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val syncResult = repository.syncWithRemote()
                syncResult.onSuccess { count ->
                    android.util.Log.d("RemoteSync", "Sync successful, updated/inserted: $count")
                }.onFailure { error ->
                    android.util.Log.e("RemoteSync", "Sync failed: ${error.message}", error)
                }

                repository.refreshInstallStates()
            } catch (e: Exception) {
                android.util.Log.e("RemoteSync", "Unexpected error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
    }

    fun setFilterTab(tab: FilterTab) {
        if (_filterTab.value == tab) return // لا تمسح التحديد إذا كان نفس التبويب
        _filterTab.value = tab
        viewModelScope.launch { repository.clearSelection() }
    }

    fun toggleAppSelection(packageName: String, selected: Boolean) {
        viewModelScope.launch {
            repository.toggleSelection(packageName, selected)
        }
    }

    fun toggleCategorySelection(category: String, selected: Boolean) {
        viewModelScope.launch {
            repository.setCategorySelection(category, selected)
        }
    }

    fun clearSelection() {
        viewModelScope.launch {
            repository.clearSelection()
        }
    }

    /** تشغيل الحذف أو الاسترجاع مع حماية شاملة من الأخطاء والتعليق */
    fun runBatchAction() {
        // حماية من النقر المكرر أثناء سير العملية
        if (_batchState.value.inProgress) return

        if (!uiState.value.shizukuPermissionGranted || !uiState.value.shizukuConnected) {
            _lastResultMessage.value = "Action failed: Shizuku permission required. Tap 'Grant access' above."
            return
        }

        val state = uiState.value
        val mode = resolveBatchMode(state.filterTab, state.installedSelectedCount, state.removedSelectedCount)

        if (mode == null) {
            _lastResultMessage.value = "Can't mix installed and removed apps. Select only one type at a time."
            return
        }

        viewModelScope.launch {
            _batchState.value = BatchUiState(inProgress = true, total = 0, completed = 0)
            try {
                val onProgress: suspend (DebloatRepository.BatchOpResult.Progress) -> Unit = { progress ->
                    _batchState.value = BatchUiState(
                        inProgress = true,
                        label = progress.currentLabel,
                        completed = progress.completed,
                        total = progress.total
                    )
                }

                val finished = if (mode == BatchMode.RESTORE) {
                    repository.restoreSelected(onProgress)
                } else {
                    repository.uninstallSelected(onProgress)
                }

                val succeeded = finished.successCount
                val failed = finished.failureCount

                _lastResultMessage.value = when {
                    failed == 0 -> "$succeeded succeeded"
                    succeeded == 0 -> {
                        if (mode == BatchMode.RESTORE) {
                            "Restore failed: APK not found in system ROM (user app)"
                        } else {
                            "Uninstall failed: Package is system-protected"
                        }
                    }
                    else -> {
                        if (mode == BatchMode.RESTORE) {
                            "$succeeded restored, $failed failed"
                        } else {
                            "$succeeded uninstalled, $failed failed"
                        }
                    }
                }
            } catch (e: Exception) {
                _lastResultMessage.value = "Operation failed unexpectedly: ${e.message}"
            } finally {
                // يضمن إعادة تعيين الحالة حتى لا يعلق الزر ومؤشر التحميل مطلقاً
                _batchState.value = BatchUiState()
            }
        }
    }

    fun consumeResultMessage() {
        _lastResultMessage.value = null
    }
}