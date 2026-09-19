package com.debloat.hyperos

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.debloat.hyperos.data.AppLanguage
import com.debloat.hyperos.data.SettingsRepository
import com.debloat.hyperos.ui.screens.MainScreen
import com.debloat.hyperos.ui.theme.AccentOrange
import com.debloat.hyperos.ui.theme.DebloatHyperOSTheme
import com.debloat.hyperos.ui.theme.ThemeOption
import com.debloat.hyperos.viewmodel.DebloatViewModelFactory
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DebloatApp
        val viewModelFactory = DebloatViewModelFactory(app.repository)

        setContent {
            val context = LocalContext.current
            val settingsRepo = remember { SettingsRepository(context) }
            val scope = rememberCoroutineScope()

            val rawTheme by settingsRepo.themeOption.collectAsState(initial = "SYSTEM")
            val currentLanguage by settingsRepo.language.collectAsState(initial = AppLanguage.SYSTEM)

            var showExitDialog by remember { mutableStateOf(false) }

            val currentTheme = when (rawTheme) {
                ThemeOption.LIGHT.name -> ThemeOption.LIGHT
                ThemeOption.DARK.name -> ThemeOption.DARK
                ThemeOption.AMOLED.name -> ThemeOption.AMOLED
                else -> ThemeOption.SYSTEM
            }

            // تطبيق لغة التطبيق واتجاه الواجهة ديناميكياً
            val (locale, layoutDirection) = when (currentLanguage) {
                AppLanguage.ARABIC -> Locale("ar") to LayoutDirection.Rtl
                AppLanguage.ENGLISH -> Locale("en") to LayoutDirection.Ltr
                AppLanguage.SYSTEM -> {
                    val systemLocale = java.util.Locale.getDefault()
                    val isRtl = android.text.TextUtils.getLayoutDirectionFromLocale(systemLocale) == android.view.View.LAYOUT_DIRECTION_RTL
                    systemLocale to (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
                }
            }

            val configuration = LocalConfiguration.current
            val localizedConfiguration = remember(configuration, locale) {
                Configuration(configuration).apply {
                    setLocale(locale)
                }
            }

            val localizedContext = remember(context, locale) {
                context.createConfigurationContext(localizedConfiguration)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration,
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection
            ) {
                DebloatHyperOSTheme(themeOption = currentTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        BackHandler {
                            showExitDialog = true
                        }

                        MainScreen(
                            viewModelFactory = viewModelFactory,
                            currentTheme = currentTheme,
                            currentLanguage = currentLanguage,
                            onThemeChange = { newTheme ->
                                scope.launch { settingsRepo.setThemeMode(newTheme.name) }
                            },
                            onLanguageChange = { newLang ->
                                scope.launch { settingsRepo.setLanguage(newLang) }
                            }
                        )

                        if (showExitDialog) {
                            AlertDialog(
                                onDismissRequest = { showExitDialog = false },
                                title = {
                                    Text(
                                        text = localizedContext.getString(R.string.exit_dialog_title),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                text = {
                                    Text(
                                        text = localizedContext.getString(R.string.exit_dialog_message),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showExitDialog = false
                                            finish()
                                        }
                                    ) {
                                        Text(
                                            text = localizedContext.getString(R.string.exit_dialog_confirm),
                                            color = AccentOrange
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showExitDialog = false }) {
                                        Text(
                                            text = localizedContext.getString(R.string.exit_dialog_cancel),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.debloat.hyperos.shizuku.ShizukuManager.refreshConnectionState()
    }
}