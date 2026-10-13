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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.flow.first
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.ui.components.LocalIsWideLayout
import com.ttggg5.holovoice.ui.components.NoHaptics
import com.ttggg5.holovoice.ui.alarms.AlarmsScreen
import com.ttggg5.holovoice.ui.alarms.AlarmsViewModel
import com.ttggg5.holovoice.ui.settings.SettingsScreen
import com.ttggg5.holovoice.ui.settings.SettingsViewModel
import com.ttggg5.holovoice.ui.components.swipeBetweenTabs
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
private const val SETTINGS = "settings"
private const val FAVORITES = "favorites"
private const val ALARMS = "alarms"

/** +1 when moving to a tab further right, -1 further left, 0 when not a tab-to-tab change. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val from = Tab.entries.indexOfFirst { it.route == initialState.destination.route }
    val to = Tab.entries.indexOfFirst { it.route == targetState.destination.route }
    return if (from < 0 || to < 0) 0 else (to - from).coerceIn(-1, 1)
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter(): EnterTransition = when (tabDirection()) {
    1 -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn(tween(280))
    -1 -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeIn(tween(280))
    else -> fadeIn(tween(220))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit(): ExitTransition = when (tabDirection()) {
    1 -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeOut(tween(200))
    -1 -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeOut(tween(200))
    else -> fadeOut(tween(180))
}

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
    // "Set as alarm…" on a clip, or the status-bar alarm icon, opens the Alarms screen.
    val alarmClip by container.alarms.pendingClip.collectAsStateWithLifecycle()
    val openAlarms by container.alarms.openRequested.collectAsStateWithLifecycle()
    LaunchedEffect(alarmClip, openAlarms) {
        if (alarmClip != null || openAlarms) {
            container.alarms.openRequested.value = false
            nav.currentBackStackEntryFlow.first() // wait until the nav graph is ready
            nav.navigate(ALARMS) { launchSingleTop = true }
        }
    }
    // Folder sync results (automatic, refresh button or pull to refresh), on any screen.
    LaunchedEffect(repository) {
        repository.syncMessages.collect { snackbar.showSnackbar(it) }
    }
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

    val tabSwipes by container.uiPrefs.tabSwipes.state.collectAsStateWithLifecycle()
    val hapticsOn by container.uiPrefs.haptics.state.collectAsStateWithLifecycle()
    val systemHaptics = LocalHapticFeedback.current
    val openSettings = { nav.navigate(SETTINGS) { launchSingleTop = true } }

    // Tablets and unfolded foldables (600dp+): navigation rail on the side instead of the bottom bar.
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val wide = maxWidth >= 600.dp
    CompositionLocalProvider(
        LocalUndo provides undo,
        LocalIsWideLayout provides wide,
        LocalHapticFeedback provides if (hapticsOn) systemHaptics else NoHaptics,
    ) {
    Row(Modifier.fillMaxSize()) {
    if (wide) {
        NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Spacer(Modifier.height(8.dp))
            Tab.entries.forEach { tab ->
                NavigationRailItem(
                    selected = route == tab.route,
                    onClick = { goToTab(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label) },
                )
            }
            Spacer(Modifier.weight(1f))
            NavigationRailItem(
                selected = route == SETTINGS,
                onClick = openSettings,
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                label = { Text("Settings") },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
    Scaffold(
        modifier = Modifier.weight(1f),
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
                    visible = topLevel && !wide,
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
                if (!topLevel || wide) {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = Tab.Home.route,
            modifier = Modifier
                .padding(padding)
                // Fling left/right on a tab to move to the neighbouring tab.
                .swipeBetweenTabs(enabled = topLevel && tabSwipes) { direction ->
                    val index = Tab.entries.indexOfFirst { it.route == route }
                    Tab.entries.getOrNull(index + direction)?.let(::goToTab)
                },
            // Tabs slide in the direction of travel; drill-down screens override these below.
            enterTransition = { tabEnter() },
            exitTransition = { tabExit() },
            popEnterTransition = { tabEnter() },
            popExitTransition = { tabExit() },
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
                    onOpenSettings = openSettings,
                    onOpenAlarms = { nav.navigate(ALARMS) { launchSingleTop = true } },
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
                    onOpenTalent = { slug -> nav.navigate("talent/$slug") },
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
            composable(
                SETTINGS,
                enterTransition = { pushIn() },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { pushOut() },
            ) {
                SettingsScreen(
                    viewModel = viewModel { SettingsViewModel(repository) },
                    onBack = { nav.popBackStack() },
                    onShowHelp = { showHelp = true },
                    onOpenAlarms = { nav.navigate(ALARMS) { launchSingleTop = true } },
                )
            }
            composable(
                ALARMS,
                enterTransition = { pushIn() },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { pushOut() },
            ) {
                AlarmsScreen(viewModel { AlarmsViewModel(container.alarms, repository, container.player) }, onBack = { nav.popBackStack() })
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
    }
}
