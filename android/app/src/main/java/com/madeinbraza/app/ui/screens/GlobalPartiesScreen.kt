package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.data.model.Party
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.ui.viewmodel.GlobalPartiesViewModel

private val CLASS_DISPLAY_NAMES = mapOf(
    PlayerClass.ASSASSIN to "Assassino",
    PlayerClass.BRAWLER to "Lutador",
    PlayerClass.ATALANTA to "Atalanta",
    PlayerClass.PIKEMAN to "Lanceiro",
    PlayerClass.FIGHTER to "Guerreiro",
    PlayerClass.MECHANIC to "Mecanico",
    PlayerClass.KNIGHT to "Cavaleiro",
    PlayerClass.PRIESTESS to "Sacerdotisa",
    PlayerClass.SHAMAN to "Xama",
    PlayerClass.MAGE to "Mago",
    PlayerClass.ARCHER to "Arqueiro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalPartiesScreen(
    viewModel: GlobalPartiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Create party dialog
    if (uiState.showCreateDialog) {
        CreateGlobalPartyDialog(
            isCreating = uiState.isCreating,
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, description, maxMembers -> viewModel.createParty(name, description, maxMembers) }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = "Criar Party")
            }
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
                uiState.parties.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nenhuma party",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Crie uma party para juntar a galera!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.parties) { party ->
                            GlobalPartyCard(
                                party = party,
                                currentUserId = uiState.currentUserId,
                                isLeader = uiState.isLeader,
                                isActionInProgress = uiState.actionInProgress == party.id,
                                onJoin = { viewModel.joinParty(party.id) },
                                onLeave = { viewModel.leaveParty(party.id) },
                                onDelete = { viewModel.deleteParty(party.id) }
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
fun CreateGlobalPartyDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String?, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var maxMembers by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Criar Party") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Party") },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descricao (opcional)") },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = maxMembers,
                    onValueChange = { maxMembers = it.filter { c -> c.isDigit() } },
                    label = { Text("Maximo de membros") },
                    singleLine = true,
                    enabled = !isCreating,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val max = maxMembers.toIntOrNull()
                    val desc = description.trim().ifEmpty { null }
                    onCreate(name, desc, max)
                },
                enabled = name.isNotBlank() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("CRIAR")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("CANCELAR")
            }
        }
    )
}

@Composable
fun GlobalPartyCard(
    party: Party,
    currentUserId: String?,
    isLeader: Boolean,
    isActionInProgress: Boolean,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit
) {
    val isMember = party.members.any { it.id == currentUserId }
    val isCreator = party.createdBy.id == currentUserId
    val canDelete = isCreator || isLeader

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (party.isClosed) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = party.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (party.isClosed) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Fechada",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Text(
                        text = "Criada por ${party.createdBy.nick}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    party.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        enabled = !isActionInProgress
                    ) {
                        if (isActionInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Deletar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Members count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))

                val membersText = "${party.members.size}/${party.maxMembers} membros"
                Text(
                    text = membersText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (party.isFull) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Members list with class chips
            if (party.members.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(party.members) { member ->
                        val isCurrentUser = member.id == currentUserId
                        val className = member.playerClass?.let { CLASS_DISPLAY_NAMES[it] ?: it.name } ?: "?"
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${member.nick} ($className)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isCurrentUser)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action button
            val buttonEnabled = !isActionInProgress && (isMember || !party.isClosed)
            val buttonText = when {
                isMember -> "Sair"
                party.isClosed -> "Fechada"
                party.isFull -> "Lotada"
                else -> "Entrar"
            }

            Button(
                onClick = { if (isMember) onLeave() else onJoin() },
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isMember) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(buttonText)
                }
            }
        }
    }
}
