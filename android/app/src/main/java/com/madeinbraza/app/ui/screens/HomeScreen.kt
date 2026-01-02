package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.R
import com.madeinbraza.app.data.model.Event
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.ui.viewmodel.HomeViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToPendingMembers: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBannedUsers: () -> Unit,
    onNavigateToSiegeWar: () -> Unit,
    onNavigateToChannels: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLeader = uiState.user?.role == Role.LEADER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BRAZA") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    TextButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Text("SAIR", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.user != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.welcome, uiState.user!!.nick),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.class_info, uiState.user!!.playerClass.name),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.role_info,
                                if (isLeader) stringResource(R.string.leader) else stringResource(R.string.member)
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Upcoming Events Widget
                if (uiState.upcomingEvents.isNotEmpty()) {
                    UpcomingEventsWidget(
                        events = uiState.upcomingEvents,
                        userId = uiState.user!!.id,
                        userClass = uiState.user!!.playerClass,
                        joiningEventId = uiState.joiningEventId,
                        onJoinEvent = { viewModel.joinEvent(it) },
                        onLeaveEvent = { viewModel.leaveEvent(it) },
                        onViewAllEvents = onNavigateToEvents
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Channels button - available to all approved members
                Button(
                    onClick = onNavigateToChannels,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CANAIS DE CHAT")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLeader) {
                    Button(
                        onClick = onNavigateToPendingMembers,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("APROVAR MEMBROS")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onNavigateToBannedUsers,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BANIDOS")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Events button - available to all approved members
                Button(
                    onClick = onNavigateToEvents,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EVENTOS")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Siege War button - available to all approved members
                Button(
                    onClick = onNavigateToSiegeWar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SIEGE WAR")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Members button - only available to leaders
                if (isLeader) {
                    OutlinedButton(
                        onClick = onNavigateToMembers,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MEMBROS")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Profile button
                OutlinedButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MEU PERFIL")
                }
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun UpcomingEventsWidget(
    events: List<Event>,
    userId: String,
    userClass: PlayerClass,
    joiningEventId: String?,
    onJoinEvent: (String) -> Unit,
    onLeaveEvent: (String) -> Unit,
    onViewAllEvents: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.events_title).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onViewAllEvents) {
                    Text(
                        text = stringResource(R.string.view_all),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Events list
            events.forEach { event ->
                EventCard(
                    event = event,
                    userId = userId,
                    userClass = userClass,
                    isJoining = joiningEventId == event.id,
                    onJoinEvent = onJoinEvent,
                    onLeaveEvent = onLeaveEvent
                )
                if (event != events.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    userId: String,
    userClass: PlayerClass,
    isJoining: Boolean,
    onJoinEvent: (String) -> Unit,
    onLeaveEvent: (String) -> Unit
) {
    val isParticipating = event.participants.any { it.id == userId }
    val canJoin = !event.isFull &&
            (event.requiredClasses.isEmpty() || event.requiredClasses.contains(userClass))

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")
    val eventDate = try {
        ZonedDateTime.parse(event.eventDate).format(dateFormatter)
    } catch (e: Exception) {
        event.eventDate
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = eventDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (event.maxParticipants != null) {
                            "${event.participants.size}/${event.maxParticipants}"
                        } else {
                            "${event.participants.size} ${stringResource(R.string.participants).lowercase()}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (event.isFull) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isJoining) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (isParticipating) {
                FilledTonalButton(
                    onClick = { onLeaveEvent(event.id) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.leave_event),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else if (canJoin) {
                Button(
                    onClick = { onJoinEvent(event.id) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.join_event),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Text(
                    text = if (event.isFull) {
                        stringResource(R.string.event_full)
                    } else {
                        stringResource(R.string.class_not_allowed)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
