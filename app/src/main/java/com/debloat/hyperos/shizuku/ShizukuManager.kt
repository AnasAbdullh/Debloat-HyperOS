package com.debloat.hyperos.shizuku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.async

/**
 * Central point for all Shizuku interaction: binder lifecycle, permission
 * handshake, and shell execution under the ADB/root-elevated uid (2000)
 * that Shizuku grants — no on-device root required.
 */
object ShizukuManager {

    private const val REQUEST_CODE = 5162

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
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            // Caller (UI layer) is responsible for surfacing an explanation
            // before calling this again — we still issue the request.
        }
        Shizuku.requestPermission(REQUEST_CODE)
    }

    fun refreshConnectionState() {
        _connectionState.value = when {
            !isAvailable() -> ConnectionState.Disconnected
            hasPermission() -> ConnectionState.PermissionGranted
            else -> ConnectionState.Connected // binder alive, permission not yet granted
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
     * Executes a shell command through Shizuku.newProcess, running under the
     * ADB shell uid (2000) — sufficient for `pm uninstall --user 0` /
     * `cmd package install-existing` without root.
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

            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()
            val exitCode = process.waitFor()

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

    /** `pm list packages --user 0 <package>` → true if `package:<name>` is present. */
    suspend fun getInstalledPackages(): Set<String> {
        val result = exec("pm list packages --user 0")
        if (!result.isSuccess) return emptySet()

        return result.stdout.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .toSet()
    }

    suspend fun isPackageInstalled(packageName: String): Boolean {
        val result = exec("pm path $packageName")
        return result.isSuccess && result.stdout.contains("package:")
    }

    /** `pm uninstall --user 0 <package>` — user-scoped removal, restorable without a flash. */
    suspend fun uninstallPackage(packageName: String): ShellResult =
        exec("pm uninstall --user 0 $packageName")

    /** `cmd package install-existing <package>` — restores a previously uninstalled-for-user app. */
    suspend fun restorePackage(packageName: String): ShellResult =
        exec("cmd package install-existing $packageName")
}
