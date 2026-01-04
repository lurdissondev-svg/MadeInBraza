package com.madeinbraza.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.madeinbraza.app.ui.components.DownloadProgressDialog
import com.madeinbraza.app.ui.components.UpdateDialog
import com.madeinbraza.app.ui.theme.BrazaTheme
import com.madeinbraza.app.util.AppUpdate
import com.madeinbraza.app.util.AppUpdateManager
import com.madeinbraza.app.util.LanguageManager
import com.madeinbraza.app.util.UpdateDownloadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    private var pendingNotificationNavigation by mutableStateOf<NotificationNavigation?>(null)
    private var triggerUpdateCheck by mutableStateOf(false)
    private var pendingUpdate by mutableStateOf<AppUpdate?>(null)

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
            val downloadState by appUpdateManager.downloadState.collectAsState()

            // Notification permission launcher
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Permission result handled - mark version as seen
                CoroutineScope(Dispatchers.IO).launch {
                    appUpdateManager.markCurrentVersionAsSeen(this@MainActivity)
                }
            }

            // Check for first run after install/update and request notification permission
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val isFirstRun = appUpdateManager.isFirstRunAfterInstallOrUpdate(this@MainActivity)
                    if (isFirstRun) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    // For older Android versions, just mark as seen
                    appUpdateManager.markCurrentVersionAsSeen(this@MainActivity)
                }
            }

            // Check for updates on app start or when triggered by notification
            LaunchedEffect(Unit, triggerUpdateCheck) {
                if (triggerUpdateCheck || availableUpdate == null) {
                    val update = appUpdateManager.checkForUpdate()
                    if (update != null && appUpdateManager.isUpdateAvailable(update)) {
                        // Check if this version was already downloaded
                        val alreadyDownloaded = appUpdateManager.wasVersionDownloaded(
                            this@MainActivity,
                            update.versionName
                        )
                        if (!alreadyDownloaded) {
                            availableUpdate = update
                            showUpdateDialog = true
                        }
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
                        },
                        pendingUpdate = pendingUpdate,
                        onUpdateClick = {
                            showUpdateDialog = true
                        }
                    )

                    // Update dialog
                    if (showUpdateDialog && availableUpdate != null) {
                        UpdateDialog(
                            update = availableUpdate!!,
                            onUpdate = {
                                // Mark version as downloaded to prevent re-showing dialog
                                CoroutineScope(Dispatchers.IO).launch {
                                    appUpdateManager.markVersionAsDownloaded(
                                        this@MainActivity,
                                        availableUpdate!!.versionName
                                    )
                                }
                                appUpdateManager.downloadAndInstall(this@MainActivity, availableUpdate!!)
                                showUpdateDialog = false
                                pendingUpdate = null
                            },
                            onDismiss = {
                                showUpdateDialog = false
                                // Keep the update available for the banner
                                pendingUpdate = availableUpdate
                            }
                        )
                    }

                    // Download progress dialog
                    if (downloadState !is UpdateDownloadState.Idle) {
                        DownloadProgressDialog(
                            downloadState = downloadState,
                            onDismiss = {
                                appUpdateManager.resetDownloadState()
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
