package com.ttggg5.holovoice.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.navigation.NavBackStackEntry
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ttggg5.holovoice.AppContainer
import com.ttggg5.holovoice.ui.components.MiniPlayer
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ttggg5.holovoice.ui.components.LocalUndo
import com.ttggg5.holovoice.ui.components.UndoController
import com.ttggg5.holovoice.ui.home.HomeScreen
import com.ttggg5.holovoice.ui.home.HomeViewModel
import com.ttggg5.holovoice.ui.onboarding.OnboardingDialog
import com.ttggg5.holovoice.ui.talents.TalentScreen
import com.ttggg5.holovoice.ui.talents.TalentViewModel
import com.ttggg5.holovoice.ui.talents.TalentsScreen
import com.ttggg5.holovoice.ui.talents.TalentsViewModel
import com.ttggg5.holovoice.ui.talents.UNASSIGNED_SLUG
import com.ttggg5.holovoice.ui.favorites.FavoritesScreen
import com.ttggg5.holovoice.ui.favorites.FavoritesViewModel
import com.ttggg5.holovoice.ui.nowplaying.NowPlayingScreen
import com.ttggg5.holovoice.ui.nowplaying.NowPlayingViewModel
import com.ttggg5.holovoice.ui.playlists.PlaylistScreen
import com.ttggg5.holovoice.ui.playlists.PlaylistViewModel
import com.ttggg5.holovoice.ui.playlists.PlaylistsScreen
import com.ttggg5.holovoice.ui.playlists.PlaylistsViewModel
import com.ttggg5.holovoice.ui.queue.QueueScreen
import com.ttggg5.holovoice.ui.library.LibraryScreen
import com.ttggg5.holovoice.ui.library.LibraryViewModel
import com.ttggg5.holovoice.ui.pack.PackScreen
import com.ttggg5.holovoice.ui.pack.PackViewModel
import com.ttggg5.holovoice.ui.search.SearchScreen
import com.ttggg5.holovoice.ui.search.SearchViewModel

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Library("library", "Topics", Icons.Filled.LibraryMusic),
    Talents("talents", "Talents", Icons.Filled.Face),
    Playlists("playlists", "Playlists", Icons.AutoMirrored.Filled.QueueMusic),
    Search("search", "Search", Icons.Filled.Search),
}

private const val NOW_PLAYING = "now_playing"
private const val FAVORITES = "favorites"

/** Drill-down screens slide in from the end and back out to it. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushIn(): EnterTransition =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) + fadeIn(tween(320))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushOut(): ExitTransition =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeOut(tween(280))
private const val QUEUE = "queue"

@Composable
fun HoloVoiceRoot(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val playerState by container.player.state.collectAsStateWithLifecycle()
    val repository = container.repository

    val topLevel = Tab.entries.any { it.route == route }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undo = remember(snackbar, scope) { UndoController(snackbar, scope) }
    val onboardingDone by container.uiPrefs.onboardingDone.collectAsStateWithLifecycle()
    var showHelp by remember { mutableStateOf(false) }
    if (!onboardingDone || showHelp) {
        OnboardingDialog(onDone = {
            container.uiPrefs.setOnboardingDone(true)
            showHelp = false
        })
    }
    fun goToTab(tab: Tab) = nav.navigate(tab.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    CompositionLocalProvider(LocalUndo provides undo) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = playerState.clipId != null && route != NOW_PLAYING,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    MiniPlayer(playerState, container.player, onOpen = { nav.navigate(NOW_PLAYING) { launchSingleTop = true } })
                }
                AnimatedVisibility(
                    visible = topLevel,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = { goToTab(tab) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
                if (!topLevel) {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding),
            // Tabs crossfade; drill-down screens override these with slides below.
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Tab.Home.route) {
                HomeScreen(
                    viewModel = viewModel { HomeViewModel(repository) },
                    player = container.player,
                    onOpenTopic = { id -> nav.navigate("pack/$id") },
                    onOpenTalent = { slug -> nav.navigate("talent/$slug") },
                    onOpenFavorites = { nav.navigate(FAVORITES) },
                    onOpenNowPlaying = { nav.navigate(NOW_PLAYING) { launchSingleTop = true } },
                    onBrowseTopics = { goToTab(Tab.Library) },
                    onShowHelp = { showHelp = true },
                )
            }
            composable(Tab.Talents.route) {
                TalentsScreen(
                    viewModel = viewModel { TalentsViewModel(repository) },
                    playerState = playerState,
                    onOpen = { slug -> nav.navigate("talent/$slug") },
                )
            }
            composable(
                "talent/{slug}",
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                enterTransition = { pushIn() },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { pushOut() },
            ) { entry ->
                val slug = entry.arguments?.getString("slug") ?: UNASSIGNED_SLUG
                TalentScreen(
                    viewModel = viewModel { TalentViewModel(slug, repository) },
                    player = container.player,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Tab.Library.route) {
                LibraryScreen(
                    viewModel = viewModel { LibraryViewModel(repository) },
                    playerState = playerState,
                    onOpenPack = { id -> nav.navigate("pack/$id") },
                )
            }
            composable(Tab.Playlists.route) {
                PlaylistsScreen(
                    viewModel = viewModel { PlaylistsViewModel(repository) },
                    player = container.player,
                    onOpen = { id -> nav.navigate("playlist/$id") },
                )
            }
            composable("playlist/{playlistId}", arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                enterTransition = { pushIn() },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { pushOut() },
            ) { entry ->
                val playlistId = entry.arguments!!.getLong("playlistId")
                PlaylistScreen(
                    viewModel = viewModel { PlaylistViewModel(playlistId, repository) },
                    player = container.player,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                NOW_PLAYING,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(380)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(320)) },
            ) {
                NowPlayingScreen(
                    viewModel = viewModel { NowPlayingViewModel(repository, container.player) },
                    player = container.player,
                    onBack = { nav.popBackStack() },
                    onOpenQueue = { nav.navigate(QUEUE) { launchSingleTop = true } },
                    onOpenTopic = { id -> nav.navigate("pack/$id") },
                )
            }
            composable(QUEUE,
                enterTransition = { pushIn() },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { pushOut() },
            ) {
                QueueScreen(player = container.player, onBack = { nav.popBackStack() })
            }
            composable(
                FAVORITES,
                enterTransition = { pushIn() },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { pushOut() },
            ) {
                FavoritesScreen(viewModel { FavoritesViewModel(repository) }, container.player, onBack = { nav.popBackStack() })
            }
            composable(Tab.Search.route) {
                SearchScreen(viewModel { SearchViewModel(repository) }, container.player)
            }
            composable("pack/{packId}", arguments = listOf(navArgument("packId") { type = NavType.LongType }),
                enterTransition = { pushIn() },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { pushOut() },
            ) { entry ->
                val packId = entry.arguments!!.getLong("packId")
                PackScreen(
                    viewModel = viewModel { PackViewModel(packId, repository) },
                    player = container.player,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
    }
}
