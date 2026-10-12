package com.ttggg5.holovoice.ui

import androidx.compose.foundation.layout.Column
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
    Library("library", "Library", Icons.Filled.LibraryMusic),
    Playlists("playlists", "Playlists", Icons.AutoMirrored.Filled.QueueMusic),
    Favorites("favorites", "Favorites", Icons.Filled.Star),
    Search("search", "Search", Icons.Filled.Search),
}

private const val NOW_PLAYING = "now_playing"
private const val QUEUE = "queue"

@Composable
fun HoloVoiceRoot(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val playerState by container.player.state.collectAsStateWithLifecycle()
    val repository = container.repository

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                if (playerState.clipId != null && route != NOW_PLAYING) {
                    MiniPlayer(playerState, container.player, onOpen = { nav.navigate(NOW_PLAYING) { launchSingleTop = true } })
                }
                if (Tab.entries.any { it.route == route }) {
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
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
        NavHost(nav, startDestination = Tab.Library.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Library.route) {
                LibraryScreen(
                    viewModel = viewModel { LibraryViewModel(repository) },
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
            composable("playlist/{playlistId}", arguments = listOf(navArgument("playlistId") { type = NavType.LongType })) { entry ->
                val playlistId = entry.arguments!!.getLong("playlistId")
                PlaylistScreen(
                    viewModel = viewModel { PlaylistViewModel(playlistId, repository) },
                    player = container.player,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(NOW_PLAYING) {
                NowPlayingScreen(
                    viewModel = viewModel { NowPlayingViewModel(repository, container.player) },
                    player = container.player,
                    onBack = { nav.popBackStack() },
                    onOpenQueue = { nav.navigate(QUEUE) { launchSingleTop = true } },
                )
            }
            composable(QUEUE) {
                QueueScreen(player = container.player, onBack = { nav.popBackStack() })
            }
            composable(Tab.Favorites.route) {
                FavoritesScreen(viewModel { FavoritesViewModel(repository) }, container.player)
            }
            composable(Tab.Search.route) {
                SearchScreen(viewModel { SearchViewModel(repository) }, container.player)
            }
            composable("pack/{packId}", arguments = listOf(navArgument("packId") { type = NavType.LongType })) { entry ->
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
