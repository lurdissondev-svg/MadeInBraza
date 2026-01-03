package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.R
import com.madeinbraza.app.data.model.Member
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.ui.viewmodel.MembersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToMemberProfile: (String) -> Unit,
    viewModel: MembersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var memberToBan by remember { mutableStateOf<Member?>(null) }
    var memberToPromote by remember { mutableStateOf<Member?>(null) }
    var memberToDemote by remember { mutableStateOf<Member?>(null) }

    // Show success message when ban is successful
    LaunchedEffect(uiState.banSuccess) {
        if (uiState.banSuccess) {
            viewModel.clearBanSuccess()
        }
    }

    // Show success message when promote is successful
    LaunchedEffect(uiState.promoteSuccess) {
        if (uiState.promoteSuccess) {
            viewModel.clearPromoteSuccess()
        }
    }

    // Show success message when demote is successful
    LaunchedEffect(uiState.demoteSuccess) {
        if (uiState.demoteSuccess) {
            viewModel.clearDemoteSuccess()
        }
    }

    // Promote confirmation dialog
    memberToPromote?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToPromote = null },
            title = { Text(stringResource(R.string.promote_leader)) },
            text = { Text(stringResource(R.string.promote_confirm, member.nick)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.promoteMember(member.id)
                        memberToPromote = null
                    }
                ) {
                    Text(stringResource(R.string.promote))
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToPromote = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Ban confirmation dialog
    memberToBan?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToBan = null },
            title = { Text(stringResource(R.string.ban_member)) },
            text = { Text(stringResource(R.string.ban_confirm, member.nick)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.banMember(member.id)
                        memberToBan = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.ban))
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToBan = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Demote confirmation dialog
    memberToDemote?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDemote = null },
            title = { Text(stringResource(R.string.demote_leader)) },
            text = { Text(stringResource(R.string.demote_confirm, member.nick)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.demoteMember(member.id)
                        memberToDemote = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.demote))
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDemote = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guild_members)) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                uiState.members.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = stringResource(R.string.no_members),
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.members) { member ->
                            MemberCard(
                                member = member,
                                isCurrentUserLeader = uiState.isLeader,
                                currentUserId = uiState.currentUser?.id,
                                isBanning = uiState.isBanning == member.id,
                                isPromoting = uiState.isPromoting == member.id,
                                isDemoting = uiState.isDemoting == member.id,
                                onBanClick = { memberToBan = member },
                                onPromoteClick = { memberToPromote = member },
                                onDemoteClick = { memberToDemote = member },
                                onMemberClick = { onNavigateToMemberProfile(member.id) }
                            )
                        }
                    }
                }
            }

            // Error snackbar
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
fun MemberCard(
    member: Member,
    isCurrentUserLeader: Boolean,
    currentUserId: String?,
    isBanning: Boolean,
    isPromoting: Boolean,
    isDemoting: Boolean,
    onBanClick: () -> Unit,
    onPromoteClick: () -> Unit,
    onDemoteClick: () -> Unit,
    onMemberClick: () -> Unit
) {
    val isLeader = member.role == Role.LEADER
    val isSelf = member.id == currentUserId
    val canBan = isCurrentUserLeader && !isLeader && !isSelf
    val canPromote = isCurrentUserLeader && !isLeader && !isSelf
    val canDemote = isCurrentUserLeader && isLeader && !isSelf

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMemberClick() },
        colors = CardDefaults.cardColors()
    ) {
        val leaderText = stringResource(R.string.leader)
        val youText = "(${stringResource(R.string.you)})"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.nick,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isLeader) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "👑",
                            fontSize = 16.sp
                        )
                    }
                    if (isSelf) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = youText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = member.playerClass.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (canPromote || canBan || canDemote) {
                Row {
                    val promoteText = stringResource(R.string.promote)
                    val demoteText = stringResource(R.string.demote)
                    val banText = stringResource(R.string.ban)

                    if (canPromote) {
                        IconButton(
                            onClick = onPromoteClick,
                            enabled = !isPromoting
                        ) {
                            if (isPromoting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Star,
                                    contentDescription = promoteText,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (canDemote) {
                        IconButton(
                            onClick = onDemoteClick,
                            enabled = !isDemoting
                        ) {
                            if (isDemoting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = demoteText,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    if (canBan) {
                        IconButton(
                            onClick = onBanClick,
                            enabled = !isBanning
                        ) {
                            if (isBanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = banText,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            } else {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (isLeader) leaderText else stringResource(R.string.member),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}
