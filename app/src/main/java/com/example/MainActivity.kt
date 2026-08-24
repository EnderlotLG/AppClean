package com.example

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.NavigationTab
import com.example.ui.components.MediaDetailSheet
import com.example.ui.screens.FolderScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.SwipeScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GalleryCleanViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryCleanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                // Intent Sender Launcher for Android 10+ MediaStore Deletion confirmation
                val deleteIntentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val freed = uiState.pendingTrashSizeBytes
                        val count = uiState.trashItems.size
                        viewModel.onDeletionCompleted(freed, count)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("¡Se liberaron $count archivos ($freed bytes) con éxito!")
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Operación de eliminación cancelada")
                        }
                    }
                }

                // Storage Permissions Launcher
                val permissionsToRequest = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                        )
                    } else {
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // Load gallery regardless (will fallback to sample media if permission denied)
                    viewModel.loadGallery(context)
                }

                LaunchedEffect(Unit) {
                    val hasPermission = permissionsToRequest.any {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (!hasPermission) {
                        permissionLauncher.launch(permissionsToRequest)
                    } else {
                        viewModel.loadGallery(context)
                    }
                }

                LaunchedEffect(uiState.message) {
                    uiState.message?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearMessage()
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("bottom_nav_bar")
                        ) {
                            // 1. Swipe Tab
                            NavigationBarItem(
                                selected = uiState.activeTab == NavigationTab.SWIPE,
                                onClick = { viewModel.setActiveTab(NavigationTab.SWIPE) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == NavigationTab.SWIPE) Icons.Filled.Swipe else Icons.Outlined.Swipe,
                                        contentDescription = "Limpiar"
                                    )
                                },
                                label = { Text("Limpiar", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("tab_swipe")
                            )

                            // 2. Folders Tab
                            NavigationBarItem(
                                selected = uiState.activeTab == NavigationTab.FOLDERS,
                                onClick = { viewModel.setActiveTab(NavigationTab.FOLDERS) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == NavigationTab.FOLDERS) Icons.Filled.Folder else Icons.Outlined.Folder,
                                        contentDescription = "Carpetas"
                                    )
                                },
                                label = { Text("Carpetas", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("tab_folders")
                            )

                            // 3. Trash Tab
                            NavigationBarItem(
                                selected = uiState.activeTab == NavigationTab.TRASH,
                                onClick = { viewModel.setActiveTab(NavigationTab.TRASH) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.trashItems.isNotEmpty()) {
                                                Badge {
                                                    Text("${uiState.trashItems.size}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.activeTab == NavigationTab.TRASH) Icons.Filled.Delete else Icons.Outlined.Delete,
                                            contentDescription = "Papelera"
                                        )
                                    }
                                },
                                label = { Text("Papelera", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("tab_trash")
                            )

                            // 4. Stats Tab
                            NavigationBarItem(
                                selected = uiState.activeTab == NavigationTab.STATS,
                                onClick = { viewModel.setActiveTab(NavigationTab.STATS) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == NavigationTab.STATS) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                        contentDescription = "Resumen"
                                    )
                                },
                                label = { Text("Resumen", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("tab_stats")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.activeTab) {
                            NavigationTab.SWIPE -> {
                                SwipeScreen(
                                    state = uiState,
                                    viewModel = viewModel,
                                    onNavigateTab = { viewModel.setActiveTab(it) }
                                )
                            }
                            NavigationTab.FOLDERS -> {
                                FolderScreen(
                                    state = uiState,
                                    viewModel = viewModel
                                )
                            }
                            NavigationTab.TRASH -> {
                                TrashScreen(
                                    state = uiState,
                                    viewModel = viewModel,
                                    onRequestDeleteIntent = { pendingIntent ->
                                        try {
                                            val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                            deleteIntentLauncher.launch(intentSenderRequest)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Error al solicitar eliminación", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onNavigateTab = { viewModel.setActiveTab(it) }
                                )
                            }
                            NavigationTab.STATS -> {
                                StatsScreen(
                                    state = uiState
                                )
                            }
                        }
                    }

                    // Media Detail Sheet
                    if (uiState.selectedDetailItem != null) {
                        MediaDetailSheet(
                            item = uiState.selectedDetailItem,
                            onDismiss = { viewModel.showItemDetail(null) },
                            onKeep = { viewModel.onSwipeRight(it) },
                            onDelete = { viewModel.onSwipeLeft(it) }
                        )
                    }
                }
            }
        }
    }
}
