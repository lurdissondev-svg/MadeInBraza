package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.R
import com.madeinbraza.app.data.model.Member
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.ui.viewmodel.MembersViewModel

// Helper function to get role display name
@Composable
fun getRoleName(role: Role): String {
    return when (role) {
        Role.LEADER -> stringResource(R.string.leader)
        Role.COUNSELOR -> stringResource(R.string.counselor)
        Role.MEMBER -> stringResource(R.string.member)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToMemberProfile: (String) -> Unit,
    viewModel: MembersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var memberToBan by remember { mutableStateOf<Member?>(null) }
    var memberToUpdateRole by remember { mutableStateOf<Pair<Member, Role>?>(null) }

    // Show success message when ban is successful
    LaunchedEffect(uiState.banSuccess) {
        if (uiState.banSuccess) {
            viewModel.clearBanSuccess()
        }
    }

    // Show success message when role update is successful
    LaunchedEffect(uiState.updateRoleSuccess) {
        if (uiState.updateRoleSuccess) {
            viewModel.clearUpdateRoleSuccess()
        }
    }

    // Role change confirmation dialog
    memberToUpdateRole?.let { (member, newRole) ->
        val roleNameStr = getRoleName(newRole)
        AlertDialog(
            onDismissRequest = { memberToUpdateRole = null },
            title = { Text(stringResource(R.string.change_role_title, roleNameStr)) },
            text = { Text(stringResource(R.string.change_role_confirm, member.nick, roleNameStr)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMemberRole(member.id, newRole)
                        memberToUpdateRole = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToUpdateRole = null }) {
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
                                isUpdatingRole = uiState.isUpdatingRole == member.id,
                                onBanClick = { memberToBan = member },
                                onRoleChange = { newRole -> memberToUpdateRole = member to newRole },
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
    isUpdatingRole: Boolean,
    onBanClick: () -> Unit,
    onRoleChange: (Role) -> Unit,
    onMemberClick: () -> Unit
) {
    val isLeader = member.role == Role.LEADER
    val isCounselor = member.role == Role.COUNSELOR
    val isSelf = member.id == currentUserId
    val canChangeRole = isCurrentUserLeader && !isSelf
    val canBan = isCurrentUserLeader && !isLeader && !isSelf

    // Role dropdown state
    var showRoleMenu by remember { mutableStateOf(false) }

    // Role emoji and color
    val (roleEmoji, roleColor) = when (member.role) {
        Role.LEADER -> "👑" to MaterialTheme.colorScheme.primary
        Role.COUNSELOR -> "⭐" to Color(0xFFFFA000) // Amber
        Role.MEMBER -> "" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMemberClick() },
        colors = CardDefaults.cardColors()
    ) {
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
                        fontWeight = FontWeight.Bold,
                        color = roleColor
                    )
                    if (roleEmoji.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = roleEmoji,
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

            if (canChangeRole || canBan) {
                Row {
                    val banText = stringResource(R.string.ban)
                    val changeRoleText = stringResource(R.string.change_role)

                    // Role change button with dropdown
                    if (canChangeRole) {
                        Box {
                            IconButton(
                                onClick = { showRoleMenu = true },
                                enabled = !isUpdatingRole && !isBanning
                            ) {
                                if (isUpdatingRole) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = changeRoleText,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Role dropdown menu
                            DropdownMenu(
                                expanded = showRoleMenu,
                                onDismissRequest = { showRoleMenu = false }
                            ) {
                                // Leader option
                                if (!isLeader) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("👑 ", fontSize = 14.sp)
                                                Text(
                                                    stringResource(R.string.leader),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            showRoleMenu = false
                                            onRoleChange(Role.LEADER)
                                        }
                                    )
                                }
                                // Counselor option
                                if (!isCounselor) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("⭐ ", fontSize = 14.sp)
                                                Text(
                                                    stringResource(R.string.counselor),
                                                    color = Color(0xFFFFA000)
                                                )
                                            }
                                        },
                                        onClick = {
                                            showRoleMenu = false
                                            onRoleChange(Role.COUNSELOR)
                                        }
                                    )
                                }
                                // Member option
                                if (member.role != Role.MEMBER) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("👤 ", fontSize = 14.sp)
                                                Text(stringResource(R.string.member))
                                            }
                                        },
                                        onClick = {
                                            showRoleMenu = false
                                            onRoleChange(Role.MEMBER)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Ban button
                    if (canBan) {
                        IconButton(
                            onClick = onBanClick,
                            enabled = !isBanning && !isUpdatingRole
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
                // Show role badge for non-leaders
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = getRoleName(member.role),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}
