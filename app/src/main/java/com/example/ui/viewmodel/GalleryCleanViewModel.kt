package com.example.ui.viewmodel

import android.app.PendingIntent
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaRepository
import com.example.model.MediaFolder
import com.example.model.MediaItem
import com.example.model.MediaTypeFilter
import com.example.model.NavigationTab
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.SwipeAction
import com.example.model.SwipeDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryCleanUiState(
    val allMedia: List<MediaItem> = emptyList(),
    val filteredQueue: List<MediaItem> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val selectedFolderId: String? = null,
    val selectedFolderName: String = "Toda la Galería",
    val sortField: SortField = SortField.DATE,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val typeFilter: MediaTypeFilter = MediaTypeFilter.ALL,
    val trashItems: List<MediaItem> = emptyList(),
    val keptItems: List<MediaItem> = emptyList(),
    val history: List<SwipeAction> = emptyList(),
    val activeTab: NavigationTab = NavigationTab.SWIPE,
    val selectedDetailItem: MediaItem? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val message: String? = null,
    val totalDeletedSpaceSession: Long = 0L,
    val totalDeletedFilesSession: Int = 0
) {
    val currentCardItem: MediaItem?
        get() = filteredQueue.firstOrNull()

    val pendingTrashSizeBytes: Long
        get() = trashItems.sumOf { it.sizeBytes }

    val formattedPendingTrashSize: String
        get() {
            val kb = pendingTrashSizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }

    val formattedSavedSessionSize: String
        get() {
            val kb = totalDeletedSpaceSession / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }

    val totalStorageAnalyzedBytes: Long
        get() = allMedia.sumOf { it.sizeBytes }

    val formattedTotalAnalyzedSize: String
        get() {
            val kb = totalStorageAnalyzedBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}

class GalleryCleanViewModel(
    private val repository: MediaRepository = MediaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryCleanUiState())
    val uiState: StateFlow<GalleryCleanUiState> = _uiState.asStateFlow()

    fun loadGallery(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val media = repository.queryDeviceMedia(context)
            val folders = repository.extractFolders(media)

            _uiState.update { state ->
                val queue = repository.filterAndSort(
                    items = media.filter { item ->
                        !state.trashItems.any { it.id == item.id } &&
                        !state.keptItems.any { it.id == item.id }
                    },
                    folderId = state.selectedFolderId,
                    sortField = state.sortField,
                    sortDirection = state.sortDirection,
                    typeFilter = state.typeFilter
                )
                state.copy(
                    allMedia = media,
                    folders = folders,
                    filteredQueue = queue,
                    isLoading = false
                )
            }
        }
    }

    fun onSwipeLeft(item: MediaItem) {
        _uiState.update { state ->
            val updatedTrash = state.trashItems + item
            val updatedQueue = state.filteredQueue.filterNot { it.id == item.id }
            val updatedHistory = state.history + SwipeAction(item, SwipeDecision.Delete)
            state.copy(
                trashItems = updatedTrash,
                filteredQueue = updatedQueue,
                history = updatedHistory
            )
        }
    }

    fun onSwipeRight(item: MediaItem) {
        _uiState.update { state ->
            val updatedKept = state.keptItems + item
            val updatedQueue = state.filteredQueue.filterNot { it.id == item.id }
            val updatedHistory = state.history + SwipeAction(item, SwipeDecision.Keep)
            state.copy(
                keptItems = updatedKept,
                filteredQueue = updatedQueue,
                history = updatedHistory
            )
        }
    }

    fun undo() {
        _uiState.update { state ->
            if (state.history.isEmpty()) return@update state
            val lastAction = state.history.last()
            val remainingHistory = state.history.dropLast(1)

            val updatedTrash = if (lastAction.decision is SwipeDecision.Delete) {
                state.trashItems.filterNot { it.id == lastAction.item.id }
            } else {
                state.trashItems
            }

            val updatedKept = if (lastAction.decision is SwipeDecision.Keep) {
                state.keptItems.filterNot { it.id == lastAction.item.id }
            } else {
                state.keptItems
            }

            val updatedQueue = listOf(lastAction.item) + state.filteredQueue

            state.copy(
                trashItems = updatedTrash,
                keptItems = updatedKept,
                filteredQueue = updatedQueue,
                history = remainingHistory
            )
        }
    }

    fun selectFolder(folder: MediaFolder?) {
        _uiState.update { state ->
            val folderId = folder?.id
            val folderName = folder?.name ?: "Toda la Galería"
            val queue = repository.filterAndSort(
                items = state.allMedia.filter { item ->
                    !state.trashItems.any { it.id == item.id } &&
                    !state.keptItems.any { it.id == item.id }
                },
                folderId = folderId,
                sortField = state.sortField,
                sortDirection = state.sortDirection,
                typeFilter = state.typeFilter
            )
            state.copy(
                selectedFolderId = folderId,
                selectedFolderName = folderName,
                filteredQueue = queue,
                activeTab = NavigationTab.SWIPE
            )
        }
    }

    fun setSort(field: SortField, direction: SortDirection) {
        _uiState.update { state ->
            val queue = repository.filterAndSort(
                items = state.allMedia.filter { item ->
                    !state.trashItems.any { it.id == item.id } &&
                    !state.keptItems.any { it.id == item.id }
                },
                folderId = state.selectedFolderId,
                sortField = field,
                sortDirection = direction,
                typeFilter = state.typeFilter
            )
            state.copy(
                sortField = field,
                sortDirection = direction,
                filteredQueue = queue
            )
        }
    }

    fun setFilter(filter: MediaTypeFilter) {
        _uiState.update { state ->
            val queue = repository.filterAndSort(
                items = state.allMedia.filter { item ->
                    !state.trashItems.any { it.id == item.id } &&
                    !state.keptItems.any { it.id == item.id }
                },
                folderId = state.selectedFolderId,
                sortField = state.sortField,
                sortDirection = state.sortDirection,
                typeFilter = filter
            )
            state.copy(
                typeFilter = filter,
                filteredQueue = queue
            )
        }
    }

    fun removeFromTrash(item: MediaItem) {
        _uiState.update { state ->
            val updatedTrash = state.trashItems.filterNot { it.id == item.id }
            val updatedQueue = listOf(item) + state.filteredQueue
            state.copy(
                trashItems = updatedTrash,
                filteredQueue = updatedQueue
            )
        }
    }

    fun restoreAllTrash() {
        _uiState.update { state ->
            val restored = state.trashItems
            val queue = repository.filterAndSort(
                items = (state.filteredQueue + restored).distinctBy { it.id },
                folderId = state.selectedFolderId,
                sortField = state.sortField,
                sortDirection = state.sortDirection,
                typeFilter = state.typeFilter
            )
            state.copy(
                trashItems = emptyList(),
                filteredQueue = queue,
                message = "Se restauraron ${restored.size} elementos"
            )
        }
    }

    fun setActiveTab(tab: NavigationTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun showItemDetail(item: MediaItem?) {
        _uiState.update { it.copy(selectedDetailItem = item) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun executeDelete(
        context: Context,
        onRequireIntent: (PendingIntent) -> Unit,
        onSuccess: (deletedCount: Int, deletedBytes: Long) -> Unit
    ) {
        val itemsToDelete = _uiState.value.trashItems
        if (itemsToDelete.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val pendingIntent = repository.createDeletePendingIntent(context, itemsToDelete)
            if (pendingIntent != null) {
                _uiState.update { it.copy(isDeleting = false) }
                onRequireIntent(pendingIntent)
            } else {
                // Direct delete (API < 30 or mock/file)
                val directSuccess = repository.deleteDirectly(context, itemsToDelete)
                val freedBytes = itemsToDelete.sumOf { it.sizeBytes }
                val count = itemsToDelete.size
                if (directSuccess) {
                    onDeletionCompleted(freedBytes, count)
                    onSuccess(count, freedBytes)
                } else {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            message = "No se pudieron eliminar todos los archivos"
                        )
                    }
                }
            }
        }
    }

    fun onDeletionCompleted(freedBytes: Long, count: Int) {
        _uiState.update { state ->
            val deletedIds = state.trashItems.map { it.id }.toSet()
            val remainingAllMedia = state.allMedia.filterNot { deletedIds.contains(it.id) }
            val remainingFolders = repository.extractFolders(remainingAllMedia)

            state.copy(
                allMedia = remainingAllMedia,
                folders = remainingFolders,
                trashItems = emptyList(),
                isDeleting = false,
                totalDeletedSpaceSession = state.totalDeletedSpaceSession + freedBytes,
                totalDeletedFilesSession = state.totalDeletedFilesSession + count,
                message = "¡Se liberaron $count archivos con éxito!"
            )
        }
    }
}
