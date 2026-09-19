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

    fun selectAllInstalled(select: Boolean) {
        val currentTabApps = uiState.value.groups.flatMap { it.apps }
        currentTabApps.forEach { app ->
            toggleAppSelection(app.packageName, select)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
    }

    fun setFilterTab(tab: FilterTab) {
        _filterTab.value = tab
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

    /** Runs uninstall on selected installed apps, or restore on selected removed apps */
    fun runBatchAction() {
        if (!uiState.value.shizukuPermissionGranted || !uiState.value.shizukuConnected) {
            _lastResultMessage.value = "Action failed: Shizuku permission required. Tap 'Grant access' above."
            return
        }

        val state = uiState.value
        // تحديد نوع العملية بذكاء:
        // 1. إذا كان في تبويب REMOVED -> دائماً Restore
        // 2. إذا كان في تبويب ALL وكل العناصر المحددة محذوفة -> Restore
        // 3. عدا ذلك -> Uninstall
        val isRestoring = state.filterTab == FilterTab.REMOVED ||
                (state.filterTab == FilterTab.ALL && state.removedSelectedCount > 0 && state.installedSelectedCount == 0)

        viewModelScope.launch {
            _batchState.value = BatchUiState(inProgress = true, total = 0, completed = 0)
            val onProgress: suspend (DebloatRepository.BatchOpResult.Progress) -> Unit = { progress ->
                _batchState.value = BatchUiState(
                    inProgress = true,
                    label = progress.currentLabel,
                    completed = progress.completed,
                    total = progress.total
                )
            }

            val finished = if (isRestoring) {
                repository.restoreSelected(onProgress)
            } else {
                repository.uninstallSelected(onProgress)
            }

            _batchState.value = BatchUiState()

            val succeeded = finished.successCount
            val failed = finished.failureCount

            _lastResultMessage.value = when {
                failed == 0 -> "$succeeded succeeded"
                succeeded == 0 -> {
                    if (isRestoring) {
                        "Restore failed: APK not found in system ROM (user app)"
                    } else {
                        "Uninstall failed: Package is system-protected"
                    }
                }
                else -> {
                    if (isRestoring) {
                        "$succeeded restored, $failed failed (some APKs missing from ROM)"
                    } else {
                        "$succeeded uninstalled, $failed failed"
                    }
                }
            }
        }
    }

    fun consumeResultMessage() {
        _lastResultMessage.value = null
    }
}