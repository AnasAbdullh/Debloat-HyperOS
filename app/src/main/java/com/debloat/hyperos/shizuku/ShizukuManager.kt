package com.debloat.hyperos.shizuku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.BufferedReader

/**
 * Central point for all Shizuku interaction: binder lifecycle, permission
 * handshake, and shell execution under the ADB/root-elevated uid (2000)
 * that Shizuku grants — no on-device root required.
 */
object ShizukuManager {

    private const val REQUEST_CODE = 5162
    private const val EXEC_TIMEOUT_MS = 15_000L

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connected : ConnectionState()
        data object PermissionDenied : ConnectionState()
        data object PermissionGranted : ConnectionState()
    }

    private val _connectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var permissionResultListener: Shizuku.OnRequestPermissionResultListener? = null
    private var binderReceivedListener: Shizuku.OnBinderReceivedListener? = null
    private var binderDeadListener: Shizuku.OnBinderDeadListener? = null

    /** Call once, e.g. from Application.onCreate(), to start observing binder state. */
    fun initialize() {
        binderReceivedListener = Shizuku.OnBinderReceivedListener {
            refreshConnectionState()
        }
        binderDeadListener = Shizuku.OnBinderDeadListener {
            _connectionState.value = ConnectionState.Disconnected
        }
        permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                _connectionState.value = if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    ConnectionState.PermissionGranted
                } else {
                    ConnectionState.PermissionDenied
                }
            }
        }

        binderReceivedListener?.let { Shizuku.addBinderReceivedListenerSticky(it) }
        binderDeadListener?.let { Shizuku.addBinderDeadListener(it) }
        permissionResultListener?.let { Shizuku.addRequestPermissionResultListener(it) }

        refreshConnectionState()
    }

    fun teardown() {
        binderReceivedListener?.let { Shizuku.removeBinderReceivedListener(it) }
        binderDeadListener?.let { Shizuku.removeBinderDeadListener(it) }
        permissionResultListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
    }

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        isAvailable() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    /**
     * Requests the Shizuku permission if needed. Result arrives asynchronously
     * via [connectionState] (PermissionGranted / PermissionDenied).
     */
    fun requestPermission() {
        if (!isAvailable()) {
            _connectionState.value = ConnectionState.Disconnected
            return
        }
        if (hasPermission()) {
            _connectionState.value = ConnectionState.PermissionGranted
            return
        }
        Shizuku.requestPermission(REQUEST_CODE)
    }

    fun refreshConnectionState() {
        _connectionState.value = when {
            !isAvailable() -> ConnectionState.Disconnected
            hasPermission() -> ConnectionState.PermissionGranted
            else -> ConnectionState.Connected
        }
    }

    data class ShellResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        val isSuccess: Boolean get() = exitCode == 0
    }

    /**
     * Executes a shell command through Shizuku.newProcess with a 15-second timeout.
     */
    suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            return@withContext ShellResult(-1, "", "Shizuku permission not granted")
        }
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

            val stdoutDeferred = async(Dispatchers.IO) {
                process.inputStream.bufferedReader().use(BufferedReader::readText)
            }
            val stderrDeferred = async(Dispatchers.IO) {
                process.errorStream.bufferedReader().use(BufferedReader::readText)
            }

            // انتظار انتهاء العملية بحد أقصى 15 ثانية لمنع تعليق التطبيق
            val exitCode = withTimeoutOrNull(EXEC_TIMEOUT_MS) {
                process.waitFor()
            } ?: run {
                process.destroyForcibly()
                return@withContext ShellResult(-1, "", "Command timed out after 15s")
            }

            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()

            ShellResult(exitCode, stdout.trim(), stderr.trim())
        } catch (t: Throwable) {
            ShellResult(-1, "", t.message ?: "Unknown Shizuku exec error")
        }
    }

    suspend fun getInstalledPackageNames(): Set<String> {
        val result = exec("pm list packages --user 0")
        if (!result.isSuccess) return emptySet()
        return result.stdout.lineSequence()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("package:")) trimmed.removePrefix("package:") else null
            }
            .toSet()
    }

    /** فحص سريع لسلامة تنفيذ أوامر Shizuku بالـ Reflection */
    suspend fun testShellExecution(): Boolean {
        val res = exec("echo ok")
        return res.isSuccess && res.stdout == "ok"
    }

    /** `pm uninstall --user 0 <package>` — user-scoped removal */
    suspend fun uninstallPackage(packageName: String): ShellResult =
        exec("pm uninstall --user 0 $packageName")

    /** `cmd package install-existing <package>` — restores uninstalled-for-user app */
    suspend fun restorePackage(packageName: String): ShellResult =
        exec("cmd package install-existing $packageName")
}