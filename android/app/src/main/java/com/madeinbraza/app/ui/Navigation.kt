package com.madeinbraza.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.madeinbraza.app.ui.screens.BannedUsersScreen
import com.madeinbraza.app.ui.screens.CreateEventScreen
import com.madeinbraza.app.ui.screens.LoginScreen
import com.madeinbraza.app.ui.screens.MainScreen
import com.madeinbraza.app.ui.screens.MemberProfileScreen
import com.madeinbraza.app.ui.screens.PartiesScreen
import com.madeinbraza.app.ui.screens.PendingMembersScreen
import com.madeinbraza.app.ui.screens.RegisterScreen
import com.madeinbraza.app.ui.screens.SiegeWarScreen
import com.madeinbraza.app.ui.screens.SplashScreen
import com.madeinbraza.app.ui.screens.WaitingScreen
import com.madeinbraza.app.util.AppUpdate

// Data class for notification navigation
data class NotificationNavigation(
    val target: String,
    val channelId: String? = null,
    val channelName: String? = null,
    val eventId: String? = null
)

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Waiting : Screen("waiting")
    object Main : Screen("main")
    object PendingMembers : Screen("pending_members")
    object CreateEvent : Screen("create_event")
    object BannedUsers : Screen("banned_users")
    object MemberProfile : Screen("member_profile/{memberId}") {
        fun createRoute(memberId: String) = "member_profile/$memberId"
    }
    object Parties : Screen("parties/{eventId}?eventTitle={eventTitle}") {
        fun createRoute(eventId: String, eventTitle: String) =
            "parties/$eventId?eventTitle=${java.net.URLEncoder.encode(eventTitle, "UTF-8")}"
    }
    object SiegeWar : Screen("siege_war")
}

@Composable
fun BrazaNavHost(
    notificationNavigation: NotificationNavigation? = null,
    onNotificationHandled: () -> Unit = {},
    onLanguageChanged: () -> Unit = {},
    pendingUpdate: AppUpdate? = null,
    onUpdateClick: () -> Unit = {}
) {
    val navController = rememberNavController()

    // Track pending notification navigation to pass to MainScreen
    var pendingNotification by remember { mutableStateOf(notificationNavigation) }

    // Update pending notification when new one arrives
    LaunchedEffect(notificationNavigation) {
        if (notificationNavigation != null) {
            pendingNotification = notificationNavigation
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToWaiting = {
                    navController.navigate(Screen.Waiting.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { status ->
                    when (status) {
                        "APPROVED" -> navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                        else -> navController.navigate(Screen.Waiting.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Waiting.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Waiting.route) {
            WaitingScreen(
                onApproved = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Waiting.route) { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                notificationNavigation = pendingNotification,
                onNotificationHandled = {
                    pendingNotification = null
                    onNotificationHandled()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToPendingMembers = {
                    navController.navigate(Screen.PendingMembers.route)
                },
                onNavigateToBannedUsers = {
                    navController.navigate(Screen.BannedUsers.route)
                },
                onNavigateToSiegeWar = {
                    navController.navigate(Screen.SiegeWar.route)
                },
                onNavigateToMemberProfile = { memberId ->
                    navController.navigate(Screen.MemberProfile.createRoute(memberId))
                },
                onLanguageChanged = onLanguageChanged,
                pendingUpdate = pendingUpdate,
                onUpdateClick = onUpdateClick
            )
        }

        composable(Screen.PendingMembers.route) {
            PendingMembersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateEvent.route) {
            CreateEventScreen(
                onNavigateBack = { navController.popBackStack() },
                onEventCreated = { navController.popBackStack() }
            )
        }

        composable(Screen.BannedUsers.route) {
            BannedUsersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MemberProfile.route,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) {
            MemberProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Parties.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("eventTitle") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            PartiesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SiegeWar.route) {
            SiegeWarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
