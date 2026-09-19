package com.debloat.hyperos.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.debloat.hyperos.R
import com.debloat.hyperos.data.AppLanguage
import com.debloat.hyperos.ui.components.AppCard
import com.debloat.hyperos.ui.components.BottomActionBar
import com.debloat.hyperos.ui.components.CategorySectionHeader
import com.debloat.hyperos.ui.components.HeaderCard
import com.debloat.hyperos.ui.components.SettingsBottomSheet
import com.debloat.hyperos.ui.theme.AccentOrange
import com.debloat.hyperos.ui.theme.ThemeOption
import com.debloat.hyperos.viewmodel.DebloatViewModel
import com.debloat.hyperos.viewmodel.DebloatViewModelFactory
import com.debloat.hyperos.viewmodel.FilterTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModelFactory: DebloatViewModelFactory,
    currentTheme: ThemeOption = ThemeOption.SYSTEM,
    currentLanguage: AppLanguage = AppLanguage.SYSTEM,
    onThemeChange: (ThemeOption) -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val viewModel: DebloatViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.lastResultMessage) {
        uiState.lastResultMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeResultMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                HeaderCard(
                    shizukuConnected = uiState.shizukuConnected,
                    shizukuPermissionGranted = uiState.shizukuPermissionGranted,
                    onRequestPermission = viewModel::requestShizukuPermission
                )

                Spacer(modifier = Modifier.padding(top = 8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        cursorColor = AccentOrange
                    )
                )

                Spacer(modifier = Modifier.padding(top = 4.dp))

                FilterTabRow(
                    current = uiState.filterTab,
                    onSelect = {
                        focusManager.clearFocus()
                        viewModel.setFilterTab(it)
                    },
                    onRefresh = {
                        focusManager.clearFocus()
                        viewModel.refreshFromDevice()
                    },
                    onOpenSettings = {
                        focusManager.clearFocus()
                        showSettingsSheet = true
                    }
                )
            }
        },
        bottomBar = {
            BottomActionBar(
                filterTab = uiState.filterTab,
                selectedCount = uiState.selectedCount,
                batchInProgress = uiState.batchInProgress,
                batchLabel = uiState.batchProgressLabel,
                batchCompleted = uiState.batchProgressCompleted,
                batchTotal = uiState.batchProgressTotal,
                onActionClick = {
                    focusManager.clearFocus()
                    viewModel.runBatchAction()
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentOrange)
            }
            return@Scaffold
        }

        val filteredGroups = remember(uiState.groups, searchQuery) {
            if (searchQuery.isBlank()) {
                uiState.groups
            } else {
                uiState.groups.map { group ->
                    group.copy(
                        apps = group.apps.filter { app ->
                            app.name.contains(searchQuery, ignoreCase = true) ||
                                    app.packageName.contains(searchQuery, ignoreCase = true)
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.filterTab == FilterTab.INSTALLED) {
                val allInstalledApps = filteredGroups.flatMap { it.apps }
                val isAllSelected = allInstalledApps.isNotEmpty() && allInstalledApps.all { it.isSelected }

                if (allInstalledApps.isNotEmpty()) {
                    item(key = "select_all_installed_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.select_all,
                                    allInstalledApps.count { it.isSelected },
                                    allInstalledApps.size
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Checkbox(
                                checked = isAllSelected,
                                onCheckedChange = { checked: Boolean ->
                                    allInstalledApps.forEach { app ->
                                        viewModel.toggleAppSelection(app.packageName, checked)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentOrange,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    checkmarkColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }

                items(allInstalledApps, key = { it.packageName }) { app ->
                    AppCard(
                        app = app,
                        onSelectedChange = { selected ->
                            viewModel.toggleAppSelection(app.packageName, selected)
                        }
                    )
                }
            } else {
                filteredGroups.forEach { group ->
                    if (group.apps.isNotEmpty()) {
                        val allSelected = group.apps.all { it.isSelected }

                        item(key = "header_${group.category}") {
                            CategorySectionHeader(
                                group = group,
                                allSelected = allSelected,
                                onSelectAllToggle = { selected ->
                                    viewModel.toggleCategorySelection(group.category, selected)
                                }
                            )
                        }

                        items(group.apps, key = { it.packageName }) { app ->
                            AppCard(
                                app = app,
                                onSelectedChange = { selected ->
                                    viewModel.toggleAppSelection(app.packageName, selected)
                                }
                            )
                        }
                    }
                }
            }

            if (filteredGroups.all { it.apps.isEmpty() }) {
                item {
                    EmptyState(tab = uiState.filterTab)
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            currentTheme = currentTheme,
            currentLanguage = currentLanguage,
            onThemeChange = {
                onThemeChange(it)
                showSettingsSheet = false
            },
            onLanguageChange = {
                onLanguageChange(it)
                showSettingsSheet = false
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
private fun FilterTabRow(
    current: FilterTab,
    onSelect: (FilterTab) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val tabs = listOf(
        FilterTab.INSTALLED to stringResource(R.string.tab_installed),
        FilterTab.REMOVED to stringResource(R.string.tab_removed),
        FilterTab.ALL to stringResource(R.string.tab_all)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == current },
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = AccentOrange,
            divider = {}, // إزالة الخط الرمادي الطويل لتوحيد مظهر الشريط
            modifier = Modifier.weight(1f)
        ) {
            tabs.forEach { (tab, label) ->
                val isSelected = current == tab
                Tab(
                    selected = isSelected,
                    onClick = { onSelect(tab) },
                    selectedContentColor = AccentOrange,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = label,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelMedium, // استخدام labelMedium بدلاً من titleSmall لتوفير المساحة
                            color = if (isSelected) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.cd_refresh),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.action_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(tab: FilterTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        val message = when (tab) {
            FilterTab.INSTALLED -> stringResource(R.string.empty_installed)
            FilterTab.REMOVED -> stringResource(R.string.empty_removed)
            FilterTab.ALL -> stringResource(R.string.empty_all)
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}