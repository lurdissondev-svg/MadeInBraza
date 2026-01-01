package com.madeinbraza.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.madeinbraza.app.ui.BrazaNavHost
import com.madeinbraza.app.ui.NotificationNavigation
import com.madeinbraza.app.ui.components.UpdateDialog
import com.madeinbraza.app.ui.theme.BrazaTheme
import com.madeinbraza.app.util.AppUpdate
import com.madeinbraza.app.util.AppUpdateManager
import com.madeinbraza.app.util.LanguageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    private var pendingNotificationNavigation by mutableStateOf<NotificationNavigation?>(null)
    private var triggerUpdateCheck by mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        val languageCode = runBlocking {
            LanguageManager.getLanguageFlow(newBase).first()
        }
        val context = LanguageManager.applyLanguage(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle notification intent on cold start
        handleNotificationIntent(intent)

        setContent {
            val languageCode by LanguageManager.getLanguageFlow(this).collectAsState(initial = "pt")
            var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
            var showUpdateDialog by remember { mutableStateOf(false) }

            // Check for updates on app start or when triggered by notification
            LaunchedEffect(Unit, triggerUpdateCheck) {
                if (triggerUpdateCheck || availableUpdate == null) {
                    val update = appUpdateManager.checkForUpdate()
                    if (update != null && appUpdateManager.isUpdateAvailable(update)) {
                        availableUpdate = update
                        showUpdateDialog = true
                    }
                    triggerUpdateCheck = false
                }
            }

            BrazaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrazaNavHost(
                        notificationNavigation = pendingNotificationNavigation,
                        onNotificationHandled = { pendingNotificationNavigation = null },
                        onLanguageChanged = {
                            recreate()
                        }
                    )

                    // Update dialog
                    if (showUpdateDialog && availableUpdate != null) {
                        UpdateDialog(
                            update = availableUpdate!!,
                            onUpdate = {
                                appUpdateManager.downloadAndInstall(this@MainActivity, availableUpdate!!)
                                showUpdateDialog = false
                            },
                            onDismiss = {
                                showUpdateDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle notification intent when app is in foreground/background
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val navigateTo = it.getStringExtra("navigateTo")
            if (navigateTo != null) {
                // Handle update notification - trigger update check
                if (navigateTo == "update") {
                    triggerUpdateCheck = true
                    it.removeExtra("navigateTo")
                    return
                }

                pendingNotificationNavigation = NotificationNavigation(
                    target = navigateTo,
                    channelId = it.getStringExtra("channelId"),
                    channelName = it.getStringExtra("channelName"),
                    eventId = it.getStringExtra("eventId")
                )
                // Clear the intent extras to prevent re-handling
                it.removeExtra("navigateTo")
            }
        }
    }
}
