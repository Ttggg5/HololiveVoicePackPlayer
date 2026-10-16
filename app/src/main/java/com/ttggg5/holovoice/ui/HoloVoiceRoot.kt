package com.ttggg5.holovoice.ui

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.layout.onSizeChanged
import com.ttggg5.holovoice.ui.nowplaying.PlayerSheet
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.ui.components.LocalIsWideLayout
import com.ttggg5.holovoice.ui.components.WIDE_LAYOUT_MIN_DP
import com.ttggg5.holovoice.ui.components.MiniPlayerCorner
import com.ttggg5.holovoice.ui.components.GlassNavBar
import com.ttggg5.holovoice.ui.components.GlassNavRail
import com.ttggg5.holovoice.ui.components.NavEntry
import com.ttggg5.holovoice.ui.components.NoHaptics
import com.ttggg5.holovoice.ui.alarms.AlarmsScreen
import com.ttggg5.holovoice.ui.alarms.AlarmsViewModel
import com.ttggg5.holovoice.ui.settings.SettingsScreen
import com.ttggg5.holovoice.ui.stats.StatsScreen
import com.ttggg5.holovoice.ui.stats.StatsViewModel
import com.ttggg5.holovoice.ui.settings.SettingsViewModel
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
private const val STATS = "stats"

/*
 * Motion between pages: a page slides in over the current one, which drifts back and dims a
 * little (back reverses it). Tabs are a pager that follows the finger, and Now Playing is an
 * overlay sheet on top (see HoloVoiceRoot), so neither needs a transition here.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, Motion.enter(Motion.LONG))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, Motion.enter(Motion.LONG)) { it / 4 } +
        fadeOut(Motion.enter(Motion.LONG), targetAlpha = 0.6f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, Motion.move(Motion.MEDIUM)) { it / 4 } +
        fadeIn(Motion.move(Motion.MEDIUM), initialAlpha = 0.6f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, Motion.move(Motion.MEDIUM))

/** The five tabs live in one destination as pages of a pager. */
private const val TABS = "tabs"
private const val QUEUE = "queue"

@Composable
fun HoloVoiceRoot(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val playerState by container.player.state.collectAsStateWithLifecycle()
    val repository = container.repository

    val onTabs = route == TABS

    // Now Playing is an overlay sheet above the pages; drags from the mini player and the player move it.
    val overlayScope = rememberCoroutineScope()
    var savedPlayerOpen by rememberSaveable { mutableStateOf(false) }
    val playerSheet = remember { PlayerSheet(overlayScope, savedPlayerOpen) }
    LaunchedEffect(playerSheet) { snapshotFlow { playerSheet.isOpen }.collect { savedPlayerOpen = it } }
    val backProgress = remember { Animatable(0f) }
    LaunchedEffect(playerSheet.isOpen) { if (playerSheet.isOpen) backProgress.snapTo(0f) }
    // Nothing to show once the queue is cleared.
    LaunchedEffect(playerState.clipId) { if (playerState.clipId == null && playerSheet.isOpen) playerSheet.close() }
    // Tapping a clip opens the player once the controller reports that clip, so the sheet never opens empty.
    LaunchedEffect(Unit) {
        container.player.openRequests.collect { id ->
            withTimeoutOrNull(2_000) { snapshotFlow { playerState.clipId }.first { it == id } }?.let { playerSheet.open() }
        }
    }
    // Tapping the media notification opens the player.
    val openPlayerRequest by container.openPlayerRequested.collectAsStateWithLifecycle()
    LaunchedEffect(openPlayerRequest) {
        if (openPlayerRequest) {
            container.openPlayerRequested.value = false
            playerSheet.open()
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
    // Tabs are pages of a pager, so swiping between them follows the finger. A sub-page keeps the
    // tab it was opened from highlighted; picking a tab from a sub-page goes back to the tabs first.
    val tabPager = rememberPagerState { Tab.entries.size }
    val currentTab = Tab.entries[tabPager.currentPage]
    fun goToTab(tab: Tab) {
        if (!onTabs) nav.popBackStack(TABS, inclusive = false)
        scope.launch { tabPager.animateScrollToPage(tab.ordinal, animationSpec = Motion.move(Motion.LONG)) }
    }
    fun selectTab(tab: Tab) = goToTab(tab)
    // Back on another tab returns to Home first, as before.
    BackHandler(enabled = onTabs && tabPager.currentPage != Tab.Home.ordinal && !playerSheet.isOpen) {
        goToTab(Tab.Home)
    }
    // Back closes the player, previewing it on Android 14+ (it shrinks and drops with the swipe).
    PredictiveBackHandler(enabled = playerSheet.isOpen) { events ->
        try {
            events.collect { backProgress.snapTo(it.progress) }
            playerSheet.close()
        } catch (e: CancellationException) {
            overlayScope.launch { backProgress.animateTo(0f, Motion.move()) }
            throw e
        }
    }

    val tabSwipes by container.uiPrefs.tabSwipes.state.collectAsStateWithLifecycle()
    val hapticsOn by container.uiPrefs.haptics.state.collectAsStateWithLifecycle()
    val systemHaptics = LocalHapticFeedback.current
    val openSettings = { nav.navigate(SETTINGS) { launchSingleTop = true } }

    // Wide windows (840dp+, e.g. tablets in landscape): navigation rail on the side instead of the bottom bar.
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val wide = maxWidth >= WIDE_LAYOUT_MIN_DP.dp
    CompositionLocalProvider(
        LocalUndo provides undo,
        LocalIsWideLayout provides wide,
        LocalHapticFeedback provides if (hapticsOn) systemHaptics else NoHaptics,
    ) {
    Row(Modifier.fillMaxSize()) {
    if (wide) {
        GlassNavRail(
            entries = Tab.entries.map { tab -> NavEntry(tab.label, tab.icon, currentTab == tab) { selectTab(tab) } },
            footer = NavEntry("Settings", Icons.Filled.Settings, route == SETTINGS, openSettings),
        )
    }
    // Where this area sits on screen, so the mini player's position can be expressed inside it.
    var areaOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .weight(1f)
            .onSizeChanged { playerSheet.height = it.height.toFloat() }
            .onGloballyPositioned { areaOrigin = it.boundsInRoot().topLeft },
    ) {
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
                    DisposableEffect(Unit) { onDispose { playerSheet.miniBounds = null } }
                    MiniPlayer(
                        playerState,
                        container.player,
                        modifier = Modifier
                            .onGloballyPositioned { playerSheet.miniBounds = it.boundsInRoot().translate(-areaOrigin) }
                            // Hands over to Now Playing as it grows out of the bar.
                            .graphicsLayer { alpha = 1f - ((1f - playerSheet.offset.value) / HANDOVER).coerceIn(0f, 1f) },
                        onOpen = playerSheet::open,
                        onSheetDrag = playerSheet::drag,
                        onSheetRelease = playerSheet::release,
                        onDismiss = { container.player.dismiss { restore -> undo.show("Player closed", restore) } },
                    )
                }
                if (!wide) {
                    GlassNavBar(Tab.entries.map { tab -> NavEntry(tab.label, tab.icon, currentTab == tab) { selectTab(tab) } })
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
            startDestination = TABS,
            enterTransition = { enter() },
            exitTransition = { exit() },
            popEnterTransition = { popEnter() },
            popExitTransition = { popExit() },
        ) {
            composable(TABS) {
                HorizontalPager(
                    state = tabPager,
                    userScrollEnabled = tabSwipes,
                    beyondViewportPageCount = 1,
                    key = { Tab.entries[it].route },
                ) { page ->
                    Page {
                        when (Tab.entries[page]) {
                            Tab.Home -> {
                                HomeScreen(
                                    viewModel = viewModel { HomeViewModel(repository) },
                                    player = container.player,
                                    onOpenTopic = { id -> nav.navigate("pack/$id") },
                                    onOpenTalent = { slug -> nav.navigate("talent/$slug") },
                                    onOpenFavorites = { nav.navigate(FAVORITES) },
                                    onOpenNowPlaying = playerSheet::open,
                                    onBrowseTopics = { goToTab(Tab.Library) },
                                    onShowHelp = { showHelp = true },
                                    onOpenSettings = openSettings,
                                    onOpenAlarms = { nav.navigate(ALARMS) { launchSingleTop = true } },
                                    onOpenStats = { nav.navigate(STATS) { launchSingleTop = true } },
                                )
                            }
                            Tab.Library -> {
                                LibraryScreen(
                                    viewModel = viewModel { LibraryViewModel(repository) },
                                    playerState = playerState,
                                    onOpenPack = { id -> nav.navigate("pack/$id") },
                                )
                            }
                            Tab.Talents -> {
                                TalentsScreen(
                                    viewModel = viewModel { TalentsViewModel(repository) },
                                    playerState = playerState,
                                    onOpen = { slug -> nav.navigate("talent/$slug") },
                                )
                            }
                            Tab.Playlists -> {
                                PlaylistsScreen(
                                    viewModel = viewModel { PlaylistsViewModel(repository) },
                                    player = container.player,
                                    onOpen = { id -> nav.navigate("playlist/$id") },
                                    onOpenFavorites = { nav.navigate(FAVORITES) },
                                )
                            }
                            Tab.Search -> {
                                SearchScreen(
                                    viewModel { SearchViewModel(repository, container.uiPrefs) },
                                    container.player,
                                    onOpenTalent = { slug -> nav.navigate("talent/$slug") },
                                    onOpenTopic = { id -> nav.navigate("pack/$id") },
                                )
                            }
                        }
                    }
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
                        viewModel = viewModel { SettingsViewModel(repository, container.backup) },
                        onBack = { nav.popBackStack() },
                        onShowHelp = { showHelp = true },
                        onOpenAlarms = { nav.navigate(ALARMS) { launchSingleTop = true } },
                        onOpenStats = { nav.navigate(STATS) { launchSingleTop = true } },
                    )
                }
            }
            composable(STATS) {
                Page {
                    StatsScreen(
                        viewModel = viewModel { StatsViewModel(repository) },
                        player = container.player,
                        onBack = { nav.popBackStack() },
                        onOpenTalent = { slug -> nav.navigate("talent/$slug") },
                        onOpenTopic = { id -> nav.navigate("pack/$id") },
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
            composable("pack/{packId}", arguments = listOf(navArgument("packId") { type = NavType.LongType }),
            ) { entry ->
                Page {
                    val packId = entry.arguments!!.getLong("packId")
                    PackScreen(
                        viewModel = viewModel { PackViewModel(packId, repository) },
                        player = container.player,
                        onBack = { nav.popBackStack() },
                        onOpenTalent = { slug -> nav.navigate("talent/$slug") },
                    )
                }
            }
        }
    }

    // Now Playing opens over the current page, so that page shows underneath while it slides or is dragged.
    if (playerSheet.isVisible) {
        val open = 1f - playerSheet.offset.value
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.32f * open }.background(Color.Black))
        val mini = playerSheet.miniBounds
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val drag = playerSheet.offset.value.coerceIn(0f, 1f)
                val p = backProgress.value
                if (mini != null && p == 0f) {
                    // Grows out of the mini player: the visible box goes from the bar's rectangle to the
                    // whole area (following the finger), the content isn't scaled, and the player fades in
                    // while the bar fades out.
                    // The open/close animation is already eased, so the box follows the offset linearly.
                    val t = 1f - drag
                    val grow = t
                    val full = Rect(Offset.Zero, size)
                    val box = lerp(mini, full, grow)
                    translationX = box.left
                    translationY = box.top
                    shape = ClipToBox(box.width, box.height, MiniPlayerCorner.toPx() * (1f - grow))
                    this.clip = t < 1f
                    alpha = (t / HANDOVER).coerceIn(0f, 1f)
                } else {
                    // Fallback, and the predictive back preview: drops, shrinks a little and rounds its corners.
                    val shrink = (drag * 0.06f + p * 0.08f).coerceAtMost(0.12f)
                    scaleX = 1f - shrink
                    scaleY = 1f - shrink
                    translationY = drag * size.height + p * size.height * 0.08f
                    shape = RoundedCornerShape(((drag * 120) + p * 40).coerceIn(0f, 32f).dp)
                    this.clip = drag > 0f || p > 0f
                }
            },
        ) {
            NowPlayingScreen(
                viewModel = viewModel { NowPlayingViewModel(repository, container.player) },
                player = container.player,
                playerSheet = playerSheet,
                onBack = playerSheet::close,
                onOpenQueue = {
                    playerSheet.close()
                    nav.navigate(QUEUE) { launchSingleTop = true }
                },
                onOpenTopic = { id ->
                    playerSheet.close()
                    nav.navigate("pack/$id")
                },
                onOpenTalent = { slug ->
                    playerSheet.close()
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

/** How much of the open/close movement the mini player and Now Playing take to swap places. */
private const val HANDOVER = 0.35f

/** Clips a layer to its top-left [width] × [height] with rounded corners (Now Playing growing out of the bar). */
private class ClipToBox(private val width: Float, private val height: Float, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(0f, 0f, width, height, CornerRadius(radius)))
}
