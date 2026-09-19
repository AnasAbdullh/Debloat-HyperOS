package com.debloat.hyperos.data

import android.content.Context
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

    private const val UPDATE_URL = "https://raw.githubusercontent.com/AnasAbdullh/Debloat-HyperOS/main/version.json"

    suspend fun checkForUpdates(context: Context): Pair<Boolean, AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }

            val url = URL(UPDATE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = reader.readText()
                reader.close()

                val json = JSONObject(jsonString)
                val latestCode = json.getInt("latestVersionCode")
                val updateInfo = AppUpdateInfo(
                    latestVersionCode = latestCode,
                    latestVersionName = json.getString("latestVersionName"),
                    releaseNotes = json.getString("releaseNotes"),
                    downloadUrl = json.getString("downloadUrl")
                )

                return@withContext (latestCode > currentVersionCode) to updateInfo
            }
        } catch (_: Exception) {
            // تجاهل أخطاء الاتصال
        }
        false to null
    }
}