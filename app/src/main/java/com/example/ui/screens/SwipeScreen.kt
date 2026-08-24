package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItem
import com.example.model.MediaTypeFilter
import com.example.model.NavigationTab
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.ui.components.SwipeCardStack
import com.example.ui.viewmodel.GalleryCleanUiState
import com.example.ui.viewmodel.GalleryCleanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    state: GalleryCleanUiState,
    viewModel: GalleryCleanViewModel,
    onNavigateTab: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar: Folder indicator & Space to free chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Folder Selector Pill
            Surface(
                onClick = { onNavigateTab(NavigationTab.FOLDERS) },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.testTag("folder_selector_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = state.selectedFolderName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Pending Trash indicator / Quick clean pill
            if (state.trashItems.isNotEmpty()) {
                Surface(
                    onClick = { onNavigateTab(NavigationTab.TRASH) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFDC2626),
                    modifier = Modifier.testTag("pending_trash_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${state.trashItems.size} (${state.formattedPendingTrashSize})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Sort & Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort Button with dropdown
            Box {
                FilterChip(
                    selected = true,
                    onClick = { showSortMenu = true },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${state.sortField.displayName} (${if (state.sortDirection == SortDirection.DESCENDING) "↓" else "↑"})",
                                fontSize = 12.sp
                            )
                        }
                    },
                    modifier = Modifier.testTag("sort_chip")
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Fecha: Más recientes primero") },
                        onClick = {
                            viewModel.setSort(SortField.DATE, SortDirection.DESCENDING)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Fecha: Más antiguos primero") },
                        onClick = {
                            viewModel.setSort(SortField.DATE, SortDirection.ASCENDING)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tamaño: Más pesados primero (libera más)") },
                        onClick = {
                            viewModel.setSort(SortField.SIZE, SortDirection.DESCENDING)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tamaño: Más ligeros primero") },
                        onClick = {
                            viewModel.setSort(SortField.SIZE, SortDirection.ASCENDING)
                            showSortMenu = false
                        }
                    )
                }
            }

            // Filter Chips
            FilterChip(
                selected = state.typeFilter == MediaTypeFilter.ALL,
                onClick = { viewModel.setFilter(MediaTypeFilter.ALL) },
                label = { Text("Todos", fontSize = 12.sp) }
            )
            FilterChip(
                selected = state.typeFilter == MediaTypeFilter.LARGE_ONLY,
                onClick = { viewModel.setFilter(MediaTypeFilter.LARGE_ONLY) },
                label = { Text("🔥 >15 MB", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
            FilterChip(
                selected = state.typeFilter == MediaTypeFilter.PHOTOS,
                onClick = { viewModel.setFilter(MediaTypeFilter.PHOTOS) },
                label = { Text("Solo Fotos", fontSize = 12.sp) }
            )
            FilterChip(
                selected = state.typeFilter == MediaTypeFilter.VIDEOS,
                onClick = { viewModel.setFilter(MediaTypeFilter.VIDEOS) },
                label = { Text("Solo Videos", fontSize = 12.sp) }
            )
        }

        // Swipe Instructions Banner (Subtle hint)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "👈 Desliza para Eliminar", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "${state.filteredQueue.size} restantes",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "Conservar 👉", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
            }
        }

        // Main Card Area or Empty State
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Cargando galería...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (state.filteredQueue.isEmpty()) {
                // Completed / Empty State
                CompletedQueueCard(
                    state = state,
                    onNavigateToTrash = { onNavigateTab(NavigationTab.TRASH) },
                    onNavigateToFolders = { onNavigateTab(NavigationTab.FOLDERS) },
                    onResetFilter = { viewModel.setFilter(MediaTypeFilter.ALL) }
                )
            } else {
                SwipeCardStack(
                    items = state.filteredQueue,
                    canUndo = state.history.isNotEmpty(),
                    onSwipeLeft = { viewModel.onSwipeLeft(it) },
                    onSwipeRight = { viewModel.onSwipeRight(it) },
                    onUndo = { viewModel.undo() },
                    onShowDetail = { viewModel.showItemDetail(it) }
                )
            }
        }
    }
}

@Composable
fun CompletedQueueCard(
    state: GalleryCleanUiState,
    onNavigateToTrash: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onResetFilter: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("completed_queue_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                )
            }

            Text(
                text = "¡Revisión Completada!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Has revisado todos los archivos de ${state.selectedFolderName}.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.trashItems.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Listos para eliminar:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${state.trashItems.size} archivos · ${state.formattedPendingTrashSize}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Button(
                    onClick = onNavigateToTrash,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("go_to_trash_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ir a Papelera y Liberar Espacio", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onNavigateToFolders,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Limpiar Otra Carpeta", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
