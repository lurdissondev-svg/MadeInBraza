package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.madeinbraza.app.ui.NotificationNavigation

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "main_home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    object Channels : BottomNavItem(
        route = "main_channels",
        title = "Chat",
        selectedIcon = Icons.Filled.List,
        unselectedIcon = Icons.Outlined.List
    )
    object Events : BottomNavItem(
        route = "main_events",
        title = "Eventos",
        selectedIcon = Icons.Filled.DateRange,
        unselectedIcon = Icons.Outlined.DateRange
    )
    object Parties : BottomNavItem(
        route = "main_parties",
        title = "Parties",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.Star
    )
    object Members : BottomNavItem(
        route = "main_members",
        title = "Membros",
        selectedIcon = Icons.Filled.Face,
        unselectedIcon = Icons.Outlined.Face
    )
    object Profile : BottomNavItem(
        route = "main_profile",
        title = "Perfil",
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle
    )
}

@Composable
fun MainScreen(
    notificationNavigation: NotificationNavigation? = null,
    onNotificationHandled: () -> Unit = {},
    onLogout: () -> Unit,
    onNavigateToPendingMembers: () -> Unit,
    onNavigateToBannedUsers: () -> Unit,
    onNavigateToSiegeWar: () -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToMemberProfile: (String) -> Unit,
    onLanguageChanged: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Channels,
        BottomNavItem.Events,
        BottomNavItem.Parties,
        BottomNavItem.Members,
        BottomNavItem.Profile
    )

    // Handle notification navigation
    LaunchedEffect(notificationNavigation) {
        notificationNavigation?.let { nav ->
            val targetRoute = when (nav.target) {
                "channel", "channels" -> BottomNavItem.Channels.route
                "events" -> BottomNavItem.Events.route
                "parties" -> BottomNavItem.Parties.route
                "home" -> BottomNavItem.Home.route
                "siege_war" -> {
                    // Navigate to Siege War screen (external navigation)
                    onNavigateToSiegeWar()
                    onNotificationHandled()
                    return@LaunchedEffect
                }
                else -> BottomNavItem.Home.route
            }

            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onNotificationHandled()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                MainHomeContent(
                    onLogout = onLogout,
                    onNavigateToPendingMembers = onNavigateToPendingMembers,
                    onNavigateToBannedUsers = onNavigateToBannedUsers,
                    onNavigateToSiegeWar = onNavigateToSiegeWar
                )
            }

            composable(BottomNavItem.Channels.route) {
                ChannelsScreen()
            }

            composable(BottomNavItem.Events.route) {
                EventsScreen(
                    onNavigateToCreateEvent = onNavigateToCreateEvent
                )
            }

            composable(BottomNavItem.Parties.route) {
                GlobalPartiesScreen()
            }

            composable(BottomNavItem.Members.route) {
                MembersScreen(
                    onNavigateToMemberProfile = onNavigateToMemberProfile
                )
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onLanguageChanged = onLanguageChanged
                )
            }
        }
    }
}
