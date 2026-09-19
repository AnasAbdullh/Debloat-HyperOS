package com.debloat.hyperos.ui.components

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.debloat.hyperos.R
import com.debloat.hyperos.data.AppLanguage
import com.debloat.hyperos.data.AppUpdateInfo
import com.debloat.hyperos.data.UpdateChecker
import com.debloat.hyperos.ui.theme.AccentOrange
import com.debloat.hyperos.ui.theme.ThemeOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    currentTheme: ThemeOption,
    currentLanguage: AppLanguage,
    onThemeChange: (ThemeOption) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateInfoDialog by remember { mutableStateOf<AppUpdateInfo?>(null) }

     fun isNetworkAvailable(ctx: Context): Boolean {
        return try {
            val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // قسم المظهر
            Text(
                text = stringResource(R.string.theme_section),
                style = MaterialTheme.typography.labelLarge,
                color = AccentOrange
            )

            ThemeOptionItem(stringResource(R.string.theme_system), currentTheme == ThemeOption.SYSTEM) {
                onThemeChange(ThemeOption.SYSTEM)
            }
            ThemeOptionItem(stringResource(R.string.theme_light), currentTheme == ThemeOption.LIGHT) {
                onThemeChange(ThemeOption.LIGHT)
            }
            ThemeOptionItem(stringResource(R.string.theme_dark), currentTheme == ThemeOption.DARK) {
                onThemeChange(ThemeOption.DARK)
            }
            ThemeOptionItem(stringResource(R.string.theme_amoled), currentTheme == ThemeOption.AMOLED) {
                onThemeChange(ThemeOption.AMOLED)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // قسم اللغة
            Text(
                text = stringResource(R.string.lang_section),
                style = MaterialTheme.typography.labelLarge,
                color = AccentOrange
            )

            ThemeOptionItem(stringResource(R.string.lang_system), currentLanguage == AppLanguage.SYSTEM) {
                onLanguageChange(AppLanguage.SYSTEM)
            }
            ThemeOptionItem(stringResource(R.string.lang_ar), currentLanguage == AppLanguage.ARABIC) {
                onLanguageChange(AppLanguage.ARABIC)
            }
            ThemeOptionItem(stringResource(R.string.lang_en), currentLanguage == AppLanguage.ENGLISH) {
                onLanguageChange(AppLanguage.ENGLISH)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // قسم حول التطبيق (About)
            Text(
                text = "About",
                style = MaterialTheme.typography.labelLarge,
                color = AccentOrange
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Debloat HyperOS v1.0.0",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Developed by Anas Abdullah",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // زر المستودع على GitHub
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AnasAbdullh/Debloat-HyperOS")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "GitHub",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // زر فحص التحديثات
                // زر فحص التحديثات
                Button(
                    onClick = {
                        try {
                            if (!isNetworkAvailable(context)) {
                                Toast.makeText(context, "No internet connection. Please connect and try again", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                isCheckingUpdates = true
                                try {
                                    val (hasUpdate, info) = UpdateChecker.checkForUpdates(context)
                                    if (hasUpdate && info != null) {
                                        updateInfoDialog = info
                                    } else {
                                        Toast.makeText(context, "You are using the latest version!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("UpdateChecker", "Error checking update", e)
                                    Toast.makeText(context, "Failed to check update: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isCheckingUpdates = false
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("UpdateChecker", "Network check error", e)
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    enabled = !isCheckingUpdates
                ) {
                    if (isCheckingUpdates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Updates",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // نافذة التحديث في حال توفر إصدار جديد
    updateInfoDialog?.let { updateInfo ->
        AlertDialog(
            onDismissRequest = { updateInfoDialog = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AccentOrange
                )
            },
            title = {
                Text(
                    text = "New Update: ${updateInfo.latestVersionName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = updateInfo.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        updateInfoDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateInfoDialog = null }) {
                    Text(
                        text = "Later",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AccentOrange)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}