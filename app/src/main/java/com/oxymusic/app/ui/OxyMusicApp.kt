package com.oxymusic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oxymusic.app.ui.components.MiniPlayer
import com.oxymusic.app.ui.screens.EqualizerScreen
import com.oxymusic.app.ui.screens.HomeScreen
import com.oxymusic.app.ui.screens.PlayerScreen
import com.oxymusic.app.ui.screens.SearchScreen
import com.oxymusic.app.ui.viewmodel.PlayerViewModel

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Início", Icons.Default.Home),
    SEARCH("search", "Buscar", Icons.Default.Search),
    EQUALIZER("equalizer", "EQ", Icons.Default.Equalizer),
}

@Composable
fun OxyMusicApp(playerVm: PlayerViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { playerVm.connect() }

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(playerVm = playerVm, onClick = { nav.navigate("player") })
                NavigationBar(containerColor = colors.background.copy(alpha = 0.98f)) {
                    val backStack by nav.currentBackStackEntryAsState()
                    val currentRoute = backStack?.destination?.route
                    Dest.entries.forEach { d ->
                        NavigationBarItem(
                            selected = currentRoute == d.route,
                            onClick = {
                                nav.navigate(d.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(d.icon, contentDescription = d.label) },
                            label = { Text(d.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.onPrimary,
                                selectedTextColor = colors.primary,
                                unselectedIconColor = colors.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = colors.onSurface.copy(alpha = 0.6f),
                                indicatorColor = colors.primary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Dest.HOME.route,
            modifier = Modifier.background(colors.background).padding(padding)
        ) {
            composable(Dest.HOME.route) { HomeScreen(onTrackClick = { nav.navigate("player") }, playerVm = playerVm) }
            composable(Dest.SEARCH.route) { SearchScreen(onTrackClick = { nav.navigate("player") }, playerVm = playerVm) }
            composable(Dest.EQUALIZER.route) { EqualizerScreen(playerVm = playerVm) }
            composable("player") { PlayerScreen(onBack = { nav.popBackStack() }, playerVm = playerVm) }
        }
    }
}
