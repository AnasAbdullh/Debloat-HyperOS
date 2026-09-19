package com.debloat.hyperos.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String,
    val downloadUrl: String
)

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val BASE_UPDATE_URL = "https://raw.githubusercontent.com/AnasAbdullh/Debloat-HyperOS/main/version.json"

    suspend fun checkForUpdates(context: Context): Pair<Boolean, AppUpdateInfo?> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // جلب رقم الإصدار الحالي بأمان حسب إصدار الأندرويد
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }

            // منع التخزين المؤقت
            val url = URL("$BASE_UPDATE_URL?nocache=${System.currentTimeMillis()}")
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Update check response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)

                val latestCode = if (json.has("latestVersionCode")) json.getInt("latestVersionCode") else json.optInt("latest_version_code", currentVersionCode)
                val latestName = if (json.has("latestVersionName")) json.getString("latestVersionName") else json.optString("latest_version_name", "1.0.0")
                val notes = if (json.has("releaseNotes")) json.getString("releaseNotes") else json.optString("changelog", "تحسينات عامة وإصلاحات للأخطاء.")
                val download = if (json.has("downloadUrl")) json.getString("downloadUrl") else json.optString("download_url", "https://github.com/AnasAbdullh/Debloat-HyperOS/releases")

                val updateInfo = AppUpdateInfo(
                    latestVersionCode = latestCode,
                    latestVersionName = latestName,
                    releaseNotes = notes,
                    downloadUrl = download
                )

                return@withContext (latestCode > currentVersionCode) to updateInfo
            } else {
                Log.w(TAG, "Failed to fetch version.json, HTTP response: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
        } finally {
            connection?.disconnect()
        }
        false to null
    }
}