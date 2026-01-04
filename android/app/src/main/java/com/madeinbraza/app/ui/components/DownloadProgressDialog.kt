package com.madeinbraza.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.madeinbraza.app.R
import com.madeinbraza.app.util.UpdateDownloadState

@Composable
fun DownloadProgressDialog(
    downloadState: UpdateDownloadState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            // Only allow dismiss if failed
            if (downloadState is UpdateDownloadState.Failed) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = downloadState is UpdateDownloadState.Failed,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        Text(
                            text = stringResource(R.string.downloading_update),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        LinearProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.download_progress, downloadState.progress),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdateDownloadState.Installing -> {
                        Text(
                            text = stringResource(R.string.installing_update),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.download_complete),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdateDownloadState.Failed -> {
                        Text(
                            text = stringResource(R.string.update_failed),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.ok))
                        }
                    }

                    else -> {
                        // Idle state - should not be shown
                    }
                }
            }
        }
    }
}
