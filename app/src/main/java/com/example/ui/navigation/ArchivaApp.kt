package com.example.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.FileItem
import com.example.data.model.FileSource
import com.example.data.model.ThemePreference
import com.example.ui.screens.collections.CollectionsScreen
import com.example.ui.screens.explorer.ExplorerScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.viewer.UniversalViewerScreen
import com.example.ui.theme.ArchivaTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object ExplorerRoute

@Serializable
data object CollectionsRoute

@Serializable
data class ViewerRoute(
    val uriString: String,
    val title: String,
    val mimeType: String,
    val categoryName: String,
    val sizeBytes: Long,
)

private data class TopLevelDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
    val testTag: String,
)

@Composable
fun ArchivaApp(
    initialExternalFile: FileItem? = null,
    viewModel: MainViewModel = viewModel(),
) {
    val themePref by viewModel.themePreference.collectAsStateWithLifecycle()
    val darkTheme = when (themePref) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    ArchivaTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            viewModel.snackbarMessages.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }

        LaunchedEffect(initialExternalFile) {
            if (initialExternalFile != null) {
                navController.navigate(
                    ViewerRoute(
                        uriString = initialExternalFile.uriString,
                        title = initialExternalFile.name,
                        mimeType = initialExternalFile.mimeType,
                        categoryName = initialExternalFile.category.name,
                        sizeBytes = initialExternalFile.sizeBytes,
                    )
                )
            }
        }

        // Zero-permission system pickers
        val mediaPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
        ) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                viewModel.importUrisToCurrentFolder(uris)
            }
        }

        val documentPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                viewModel.importUrisToCurrentFolder(uris)
            }
        }

        val folderLinkerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { treeUri: Uri? ->
            if (treeUri != null) {
                viewModel.linkSafFolder(treeUri, openImmediately = true)
                navController.navigate(ExplorerRoute) {
                    launchSingleTop = true
                }
            }
        }

        val openMediaPicker: () -> Unit = {
            mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        }
        val openDocPicker: () -> Unit = {
            documentPickerLauncher.launch(arrayOf("*/*"))
        }
        val openFolderLinker: () -> Unit = {
            folderLinkerLauncher.launch(null)
        }

        val openViewer: (FileItem) -> Unit = { item ->
            navController.navigate(
                ViewerRoute(
                    uriString = item.uriString,
                    title = item.name,
                    mimeType = item.mimeType,
                    categoryName = item.category.name,
                    sizeBytes = item.sizeBytes,
                )
            )
        }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val isViewerScreen = currentDestination?.hasRoute<ViewerRoute>() == true

        val destinations = remember {
            listOf(
                TopLevelDestination(
                    label = "Inicio",
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    route = HomeRoute,
                    testTag = "nav_home",
                ),
                TopLevelDestination(
                    label = "Explorar",
                    selectedIcon = Icons.Filled.FolderOpen,
                    unselectedIcon = Icons.Outlined.FolderOpen,
                    route = ExplorerRoute,
                    testTag = "nav_explorer",
                ),
                TopLevelDestination(
                    label = "Organizar",
                    selectedIcon = Icons.Filled.CollectionsBookmark,
                    unselectedIcon = Icons.Outlined.CollectionsBookmark,
                    route = CollectionsRoute,
                    testTag = "nav_collections",
                ),
            )
        }

        val navigateTopLevel: (Any) -> Unit = { target ->
            navController.navigate(target) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail && !isViewerScreen) {
                        NavigationBar(windowInsets = WindowInsets.navigationBars) {
                            destinations.forEach { dest ->
                                val selected = when (dest.route) {
                                    is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                    is ExplorerRoute -> currentDestination?.hasRoute<ExplorerRoute>() == true
                                    is CollectionsRoute -> currentDestination?.hasRoute<CollectionsRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag(dest.testTag),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useRail && !isViewerScreen) {
                        NavigationRail {
                            destinations.forEach { dest ->
                                val selected = when (dest.route) {
                                    is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                    is ExplorerRoute -> currentDestination?.hasRoute<ExplorerRoute>() == true
                                    is CollectionsRoute -> currentDestination?.hasRoute<CollectionsRoute>() == true
                                    else -> false
                                }
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag(dest.testTag),
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                viewModel = viewModel,
                                onOpenVault = { categoryFilter ->
                                    viewModel.openRootLocation(
                                        name = "Bóveda Local",
                                        uriString = viewModel.repository.vaultRootUri,
                                        source = FileSource.VAULT,
                                        categoryFilter = categoryFilter,
                                    )
                                    navigateTopLevel(ExplorerRoute)
                                },
                                onOpenLinkedFolder = { folder ->
                                    viewModel.openRootLocation(
                                        name = folder.displayName,
                                        uriString = folder.uriString,
                                        source = FileSource.SAF,
                                    )
                                    navigateTopLevel(ExplorerRoute)
                                },
                                onOpenFile = openViewer,
                                onPickMedia = openMediaPicker,
                                onPickDocuments = openDocPicker,
                                onLinkDeviceFolder = openFolderLinker,
                            )
                        }

                        composable<ExplorerRoute> {
                            ExplorerScreen(
                                viewModel = viewModel,
                                onOpenFile = openViewer,
                                onPickMedia = openMediaPicker,
                                onPickDocuments = openDocPicker,
                                onLinkDeviceFolder = openFolderLinker,
                            )
                        }

                        composable<CollectionsRoute> {
                            CollectionsScreen(
                                viewModel = viewModel,
                                onOpenFile = openViewer,
                            )
                        }

                        composable<ViewerRoute> { backStack ->
                            val args = backStack.toRoute<ViewerRoute>()
                            UniversalViewerScreen(
                                uriString = args.uriString,
                                title = args.title,
                                mimeType = args.mimeType,
                                categoryName = args.categoryName,
                                sizeBytes = args.sizeBytes,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
