package com.ttggg5.holovoice.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer
import com.ttggg5.holovoice.ui.theme.Motion
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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

private const val SETTINGS = "settings"
private const val FAVORITES = "favorites"
private const val ALARMS = "alarms"

private fun isTab(route: String?) = Tab.entries.any { it.route == route }

/** +1 when moving to a tab further right, -1 further left, 0 when not a tab-to-tab change. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val from = Tab.entries.indexOfFirst { it.route == initialState.destination.route }
    val to = Tab.entries.indexOfFirst { it.route == targetState.destination.route }
    return if (from < 0 || to < 0) 0 else (to - from).coerceIn(-1, 1)
}

private val AnimatedContentTransitionScope<NavBackStackEntry>.toTab get() = isTab(targetState.destination.route)

/*
 * Motion between destinations:
 * - tab to tab: shared axis X (a short slide in the direction of travel with a fade);
 * - opening a page: it slides in over the current one, which drifts back and dims a little (and back reverses it).
 * Now Playing is not a destination but an overlay on top (see HoloVoiceRoot).
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition = when {
    tabDirection() != 0 -> sharedAxisIn(tabDirection())
    toTab -> fadeIn(Motion.enter(Motion.MEDIUM, delay = 60)) + scaleIn(Motion.enter(Motion.MEDIUM, delay = 60), initialScale = 0.96f)
    else -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, Motion.enter(Motion.LONG))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition = when {
    tabDirection() != 0 -> sharedAxisOut(tabDirection())
    toTab -> fadeOut(Motion.exit(90))
    else -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, Motion.enter(Motion.LONG)) { it / 4 } +
        fadeOut(Motion.enter(Motion.LONG), targetAlpha = 0.6f)
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition = when {
    tabDirection() != 0 -> sharedAxisIn(tabDirection())
    else -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, Motion.move(Motion.MEDIUM)) { it / 4 } +
        fadeIn(Motion.move(Motion.MEDIUM), initialAlpha = 0.6f)
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition = when {
    tabDirection() != 0 -> sharedAxisOut(tabDirection())
    else -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, Motion.move(Motion.MEDIUM))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedAxisIn(direction: Int): EnterTransition =
    slideInHorizontally(Motion.enter(Motion.MEDIUM)) { width -> direction * width / 7 } + fadeIn(Motion.enter(Motion.MEDIUM, delay = 50))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedAxisOut(direction: Int): ExitTransition =
    slideOutHorizontally(Motion.exit(Motion.MEDIUM)) { width -> -direction * width / 7 } + fadeOut(Motion.exit(120))

private const val QUEUE = "queue"

@Composable
fun HoloVoiceRoot(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val playerState by container.player.state.collectAsStateWithLifecycle()
    val repository = container.repository

    val topLevel = Tab.entries.any { it.route == route }

    // Now Playing is an overlay above the pages rather than a destination.
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    val backProgress = remember { Animatable(0f) }
    val overlayScope = rememberCoroutineScope()
    LaunchedEffect(playerOpen) { if (playerOpen) backProgress.snapTo(0f) }
    // Nothing to show once the queue is cleared.
    LaunchedEffect(playerState.clipId) { if (playerState.clipId == null) playerOpen = false }
    // Tapping the media notification opens the player.
    val openPlayerRequest by container.openPlayerRequested.collectAsStateWithLifecycle()
    LaunchedEffect(openPlayerRequest) {
        if (openPlayerRequest) {
            container.openPlayerRequested.value = false
            playerOpen = true
        }
    }
    PredictiveBackHandler(enabled = playerOpen) { events ->
        try {
            events.collect { backProgress.snapTo(it.progress) }
            playerOpen = false
        } catch (e: CancellationException) {
            overlayScope.launch { backProgress.animateTo(0f, Motion.move()) }
            throw e
        }
    }

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

    // The tab a sub-page was opened from stays highlighted; tapping it again goes back to its top page.
    var currentTab by rememberSaveable { mutableStateOf(Tab.Home.route) }
    LaunchedEffect(route) { if (isTab(route)) currentTab = route!! }
    fun selectTab(tab: Tab) {
        if (tab.route == currentTab && route != tab.route) {
            if (!nav.popBackStack(tab.route, inclusive = false)) goToTab(tab)
        } else {
            goToTab(tab)
        }
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
                    selected = currentTab == tab.route,
                    onClick = { selectTab(tab) },
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
    Box(Modifier.weight(1f)) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // Always laid out, so pages never change size mid-transition (Now Playing simply covers them).
            Column {
                AnimatedVisibility(
                    visible = playerState.clipId != null,
                    enter = expandVertically(Motion.enter(Motion.MEDIUM), expandFrom = Alignment.Top) + fadeIn(Motion.enter(Motion.MEDIUM)),
                    exit = shrinkVertically(Motion.exit(Motion.MEDIUM), shrinkTowards = Alignment.Top) + fadeOut(Motion.exit()),
                ) {
                    MiniPlayer(playerState, container.player, onOpen = { playerOpen = true })
                }
                if (!wide) {
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab.route,
                                onClick = { selectTab(tab) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        },
    ) { padding ->
        val bottomPad = padding.calculateBottomPadding()
        val pageBackground = MaterialTheme.colorScheme.background

        // Every page except Now Playing keeps clear of the mini player and nav bar. The bars are always
        // laid out (only slid away for Now Playing), so pages never change size mid-transition.
        @Composable
        fun Page(content: @Composable () -> Unit) {
            Box(Modifier.fillMaxSize().background(pageBackground).padding(bottom = bottomPad)) { content() }
        }

        NavHost(
            nav,
            startDestination = Tab.Home.route,
            modifier = Modifier
                // Fling left/right on a tab to move to the neighbouring tab.
                .swipeBetweenTabs(enabled = topLevel && tabSwipes) { direction ->
                    val index = Tab.entries.indexOfFirst { it.route == route }
                    Tab.entries.getOrNull(index + direction)?.let(::goToTab)
                },
            enterTransition = { enter() },
            exitTransition = { exit() },
            popEnterTransition = { popEnter() },
            popExitTransition = { popExit() },
        ) {
            composable(Tab.Home.route) {
                Page {
                    HomeScreen(
                        viewModel = viewModel { HomeViewModel(repository) },
                        player = container.player,
                        onOpenTopic = { id -> nav.navigate("pack/$id") },
                        onOpenTalent = { slug -> nav.navigate("talent/$slug") },
                        onOpenFavorites = { nav.navigate(FAVORITES) },
                        onOpenNowPlaying = { playerOpen = true },
                        onBrowseTopics = { goToTab(Tab.Library) },
                        onShowHelp = { showHelp = true },
                        onOpenSettings = openSettings,
                        onOpenAlarms = { nav.navigate(ALARMS) { launchSingleTop = true } },
                    )
                }
            }
            composable(Tab.Talents.route) {
                Page {
                    TalentsScreen(
                        viewModel = viewModel { TalentsViewModel(repository) },
                        playerState = playerState,
                        onOpen = { slug -> nav.navigate("talent/$slug") },
                    )
                }
            }
            composable(
                "talent/{slug}",
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            ) { entry ->
                Page {
                    val slug = entry.arguments?.getString("slug") ?: UNASSIGNED_SLUG
                    TalentScreen(
                        viewModel = viewModel { TalentViewModel(slug, repository) },
                        player = container.player,
                        onBack = { nav.popBackStack() },
                    )
                }
            }
            composable(Tab.Library.route) {
                Page {
                    LibraryScreen(
                        viewModel = viewModel { LibraryViewModel(repository) },
                        playerState = playerState,
                        onOpenPack = { id -> nav.navigate("pack/$id") },
                    )
                }
            }
            composable(Tab.Playlists.route) {
                Page {
                    PlaylistsScreen(
                        viewModel = viewModel { PlaylistsViewModel(repository) },
                        player = container.player,
                        onOpen = { id -> nav.navigate("playlist/$id") },
                    )
                }
            }
            composable("playlist/{playlistId}", arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            ) { entry ->
                Page {
                    val playlistId = entry.arguments!!.getLong("playlistId")
                    PlaylistScreen(
                        viewModel = viewModel { PlaylistViewModel(playlistId, repository) },
                        player = container.player,
                        onBack = { nav.popBackStack() },
                    )
                }
            }
            composable(QUEUE,
            ) {
                Page {
                    QueueScreen(player = container.player, onBack = { nav.popBackStack() })
                }
            }
            composable(
                FAVORITES,
            ) {
                Page {
                    FavoritesScreen(viewModel { FavoritesViewModel(repository) }, container.player, onBack = { nav.popBackStack() })
                }
            }
            composable(
                SETTINGS,
            ) {
                Page {
                    SettingsScreen(
                        viewModel = viewModel { SettingsViewModel(repository) },
                        onBack = { nav.popBackStack() },
                        onShowHelp = { showHelp = true },
                        onOpenAlarms = { nav.navigate(ALARMS) { launchSingleTop = true } },
                    )
                }
            }
            composable(
                ALARMS,
            ) {
                Page {
                    AlarmsScreen(viewModel { AlarmsViewModel(container.alarms, repository, container.player) }, onBack = { nav.popBackStack() })
                }
            }
            composable(Tab.Search.route) {
                Page {
                    SearchScreen(viewModel { SearchViewModel(repository) }, container.player)
                }
            }
            composable("pack/{packId}", arguments = listOf(navArgument("packId") { type = NavType.LongType }),
            ) { entry ->
                Page {
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

    // Now Playing opens over the current page, so that page shows underneath while it slides or is dragged.
    val scrim by animateFloatAsState(
        if (playerOpen) 0.32f else 0f,
        if (playerOpen) Motion.enter(Motion.LONG) else Motion.exit(Motion.MEDIUM),
        label = "playerScrim",
    )
    if (scrim > 0f) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = scrim }.background(Color.Black))
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = playerOpen,
        enter = slideInVertically(Motion.enter(Motion.LONG)) { it },
        exit = slideOutVertically(Motion.exit(Motion.MEDIUM)) { it },
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                // Follows the predictive back swipe: shrinks, drops a little and rounds its corners.
                val p = backProgress.value
                scaleX = 1f - p * 0.08f
                scaleY = 1f - p * 0.08f
                translationY = p * size.height * 0.08f
                shape = RoundedCornerShape((p * 40).dp)
                this.clip = p > 0f
            },
        ) {
            NowPlayingScreen(
                viewModel = viewModel { NowPlayingViewModel(repository, container.player) },
                player = container.player,
                onBack = { playerOpen = false },
                onOpenQueue = {
                    playerOpen = false
                    nav.navigate(QUEUE) { launchSingleTop = true }
                },
                onOpenTopic = { id ->
                    playerOpen = false
                    nav.navigate("pack/$id")
                },
                onOpenTalent = { slug ->
                    playerOpen = false
                    nav.navigate("talent/$slug")
                },
            )
        }
    }
    }
    }
    }
    }
}
