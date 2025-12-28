package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
            title = { Text("Promover a Líder") },
            text = { Text("Tem certeza que deseja promover ${member.nick} a líder da guilda?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.promoteMember(member.id)
                        memberToPromote = null
                    }
                ) {
                    Text("PROMOVER")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToPromote = null }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    // Ban confirmation dialog
    memberToBan?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToBan = null },
            title = { Text("Banir Membro") },
            text = { Text("Tem certeza que deseja banir ${member.nick}? Esta ação não pode ser desfeita.") },
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
                    Text("BANIR")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToBan = null }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    // Demote confirmation dialog
    memberToDemote?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDemote = null },
            title = { Text("Rebaixar Líder") },
            text = { Text("Tem certeza que deseja rebaixar ${member.nick} de líder para membro?") },
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
                    Text("REBAIXAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDemote = null }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Membros da Guilda") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
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
                            text = "Nenhum membro",
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
                            Text("OK")
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
        colors = if (isLeader) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
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
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Lider",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isSelf) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(você)",
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
                                    Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Promover",
                                    tint = MaterialTheme.colorScheme.primary
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
                                    contentDescription = "Rebaixar",
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
                                    contentDescription = "Banir",
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
                            text = if (isLeader) "Líder" else "Membro",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}
