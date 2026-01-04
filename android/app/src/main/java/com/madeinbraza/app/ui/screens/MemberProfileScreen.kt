package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.R
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.ui.viewmodel.MemberProfileViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemberProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.member_profile)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.unknown_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfile() }) {
                            Text(stringResource(R.string.try_again))
                        }
                    }
                }
                uiState.profile != null -> {
                    val profile = uiState.profile!!
                    val isLeader = profile.role == Role.LEADER
                    val isCounselor = profile.role == Role.COUNSELOR
                    val leaderText = stringResource(R.string.leader)
                    val counselorText = stringResource(R.string.counselor)
                    val memberText = stringResource(R.string.member)

                    val roleText = when (profile.role) {
                        Role.LEADER -> leaderText
                        Role.COUNSELOR -> counselorText
                        else -> memberText
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = profile.nick,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (isLeader) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = leaderText,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isCounselor) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Outlined.Star,
                                            contentDescription = counselorText,
                                            modifier = Modifier.size(24.dp),
                                            tint = androidx.compose.ui.graphics.Color(0xFFFFA000)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = roleText,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val classLabel = stringResource(R.string.class_label)
                            val statusLabel = stringResource(R.string.status)
                            val approvedStatus = stringResource(R.string.approved)
                            val pendingStatus = stringResource(R.string.pending)
                            val bannedStatus = stringResource(R.string.banned)
                            val memberSinceLabel = stringResource(R.string.member_since)
                            val approvedOnLabel = stringResource(R.string.approved_on)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                ProfileInfoRow(
                                    label = classLabel,
                                    value = profile.playerClass.name
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                ProfileInfoRow(
                                    label = statusLabel,
                                    value = when (profile.status.name) {
                                        "APPROVED" -> approvedStatus
                                        "PENDING" -> pendingStatus
                                        "BANNED" -> bannedStatus
                                        else -> profile.status.name
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                ProfileInfoRow(
                                    label = memberSinceLabel,
                                    value = formatDate(profile.createdAt)
                                )

                                profile.approvedAt?.let { approvedAt ->
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                    ProfileInfoRow(
                                        label = approvedOnLabel,
                                        value = formatDate(approvedAt)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val parsed = ZonedDateTime.parse(isoDate)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
        parsed.format(formatter)
    } catch (e: Exception) {
        isoDate.take(10)
    }
}
