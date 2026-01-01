package com.madeinbraza.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.data.model.AvailableShare
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.model.SWResponseItem
import com.madeinbraza.app.data.model.SWResponseType
import com.madeinbraza.app.data.model.SWResponseUser
import com.madeinbraza.app.data.model.SWTag
import com.madeinbraza.app.data.model.SiegeWar
import com.madeinbraza.app.data.model.SWUserResponse
import com.madeinbraza.app.data.model.SiegeWarHistoryItem
import com.madeinbraza.app.data.model.SWHistoryResponseItem
import com.madeinbraza.app.ui.viewmodel.SiegeWarViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CLASS_DISPLAY_NAMES = mapOf(
    PlayerClass.ASSASSIN to "ASS",
    PlayerClass.BRAWLER to "BS",
    PlayerClass.ATALANTA to "ATA",
    PlayerClass.PIKEMAN to "PS",
    PlayerClass.FIGHTER to "FS",
    PlayerClass.MECHANIC to "MS",
    PlayerClass.KNIGHT to "KS",
    PlayerClass.PRIESTESS to "PRS",
    PlayerClass.SHAMAN to "SS",
    PlayerClass.MAGE to "MGS",
    PlayerClass.ARCHER to "AS"
)

private val CLASS_FULL_NAMES = mapOf(
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

private val TAG_DISPLAY_NAMES = mapOf(
    SWTag.ATTACK to "ATAQUE",
    SWTag.DEFENSE to "DEFESA",
    SWTag.ACADEMY to "ACADEMY"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiegeWarScreen(
    onNavigateBack: () -> Unit,
    viewModel: SiegeWarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Tab state for leaders
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Form state
    var selectedTag by remember { mutableStateOf<SWTag?>(null) }
    var selectedResponseType by remember { mutableStateOf<SWResponseType?>(null) }
    var gameId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedSharedClass by remember { mutableStateOf<PlayerClass?>(null) }
    var selectedPilotFor by remember { mutableStateOf<AvailableShare?>(null) }
    var selectedPreferredClass by remember { mutableStateOf<PlayerClass?>(null) }

    // Load available shares when PILOT is selected
    LaunchedEffect(selectedResponseType) {
        if (selectedResponseType == SWResponseType.PILOT) {
            viewModel.loadAvailableShares()
        }
    }

    // Determine history tab index based on leader status
    val historyTabIndex = if (uiState.isLeader) 2 else 1

    // Load history when Histórico tab is selected
    LaunchedEffect(selectedTabIndex, historyTabIndex) {
        if (selectedTabIndex == historyTabIndex) {
            viewModel.loadHistory()
        }
    }

    // Initialize form with existing response
    LaunchedEffect(uiState.userResponse) {
        uiState.userResponse?.let { response ->
            selectedTag = response.tag
            selectedResponseType = response.responseType
            if (response.responseType == SWResponseType.SHARED) {
                gameId = response.gameId ?: ""
                selectedSharedClass = response.sharedClass
            }
            if (response.responseType == SWResponseType.PILOT) {
                selectedPreferredClass = response.preferredClass
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Siege War") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (uiState.isLeader && uiState.siegeWar == null) {
                        IconButton(
                            onClick = { viewModel.createSiegeWar() },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Criar SW")
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
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                uiState.siegeWar == null -> {
                    NoSiegeWarContent(
                        isLeader = uiState.isLeader,
                        onCreateSiegeWar = { viewModel.createSiegeWar() }
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Card (similar to Google Form header)
                        FormHeader(siegeWar = uiState.siegeWar!!)

                        // Tabs
                        TabRow(selectedTabIndex = selectedTabIndex) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = { Text("Minha Resposta") }
                            )
                            if (uiState.isLeader) {
                                Tab(
                                    selected = selectedTabIndex == 1,
                                    onClick = { selectedTabIndex = 1 },
                                    text = { Text("Respostas (${uiState.responses.size})") }
                                )
                            }
                            Tab(
                                selected = selectedTabIndex == historyTabIndex,
                                onClick = { selectedTabIndex = historyTabIndex },
                                text = { Text("Histórico") }
                            )
                        }

                        // Content based on selected tab
                        when {
                            selectedTabIndex == 0 -> {
                                // Form Tab
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    // Form Content
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // TAG Selection
                                        FormSection(title = "TAG *", description = "Selecione seu time") {
                                            Column(modifier = Modifier.selectableGroup()) {
                                                SWTag.entries.forEach { tag ->
                                                    RadioOption(
                                                        text = TAG_DISPLAY_NAMES[tag] ?: tag.name,
                                                        selected = selectedTag == tag,
                                                        onClick = { selectedTag = tag }
                                                    )
                                                }
                                            }
                                        }

                                        // Response Type Selection
                                        FormSection(
                                            title = "Confirmado ou Shared? *",
                                            description = "Como voce vai participar?"
                                        ) {
                                            Column(modifier = Modifier.selectableGroup()) {
                                                RadioOption(
                                                    text = "Estarei la com certeza!",
                                                    description = "Vou participar com minha conta",
                                                    selected = selectedResponseType == SWResponseType.CONFIRMED,
                                                    onClick = { selectedResponseType = SWResponseType.CONFIRMED }
                                                )
                                                RadioOption(
                                                    text = "Vou deixar Shared!",
                                                    description = "Vou disponibilizar minha conta para outro pilotar",
                                                    selected = selectedResponseType == SWResponseType.SHARED,
                                                    onClick = { selectedResponseType = SWResponseType.SHARED }
                                                )
                                                RadioOption(
                                                    text = "Vou de Piloto!",
                                                    description = "Vou pilotar a conta de outro membro",
                                                    selected = selectedResponseType == SWResponseType.PILOT,
                                                    onClick = { selectedResponseType = SWResponseType.PILOT }
                                                )
                                            }
                                        }

                                        // SHARED Fields (conditional)
                                        if (selectedResponseType == SWResponseType.SHARED) {
                                            FormSection(
                                                title = "Dados da Conta Compartilhada",
                                                description = "Informe os dados para outro membro pilotar"
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    OutlinedTextField(
                                                        value = gameId,
                                                        onValueChange = { gameId = it },
                                                        label = { Text("ID do Jogo *") },
                                                        placeholder = { Text("Digite o ID da conta") },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    OutlinedTextField(
                                                        value = password,
                                                        onValueChange = { password = it },
                                                        label = { Text("Senha *") },
                                                        placeholder = { Text("Digite a senha") },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    Text(
                                                        text = "Classe da conta:",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    ClassSelectionGrid(
                                                        selectedClass = selectedSharedClass,
                                                        onClassSelected = { selectedSharedClass = it }
                                                    )
                                                }
                                            }
                                        }

                                        // PILOT Fields (conditional)
                                        if (selectedResponseType == SWResponseType.PILOT) {
                                            FormSection(
                                                title = "Selecionar Conta para Pilotar",
                                                description = "Escolha uma conta disponivel"
                                            ) {
                                                if (uiState.availableShares.isEmpty()) {
                                                    Text(
                                                        text = "Nenhuma conta disponivel para pilotagem no momento",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else {
                                                    Column(modifier = Modifier.selectableGroup()) {
                                                        uiState.availableShares.forEach { share ->
                                                            RadioOption(
                                                                text = share.nick,
                                                                description = share.sharedClass?.let { CLASS_FULL_NAMES[it] },
                                                                selected = selectedPilotFor == share,
                                                                onClick = { selectedPilotFor = share }
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            FormSection(
                                                title = "Classe Preferida *",
                                                description = "Qual classe voce prefere pilotar?"
                                            ) {
                                                ClassSelectionGrid(
                                                    selectedClass = selectedPreferredClass,
                                                    onClassSelected = { selectedPreferredClass = it }
                                                )
                                            }
                                        }

                                        // Current Response Card (show if user already responded)
                                        uiState.userResponse?.let { response ->
                                            CurrentResponseCard(response = response)
                                        }

                                        // Submit Button
                                        val canSubmit = selectedTag != null && selectedResponseType != null &&
                                                when (selectedResponseType) {
                                                    SWResponseType.SHARED -> gameId.isNotBlank() && password.isNotBlank() && selectedSharedClass != null
                                                    SWResponseType.PILOT -> selectedPilotFor != null && selectedPreferredClass != null
                                                    else -> true
                                                }

                                        Button(
                                            onClick = {
                                                viewModel.submitResponse(
                                                    responseType = selectedResponseType!!,
                                                    tag = selectedTag,
                                                    gameId = if (selectedResponseType == SWResponseType.SHARED) gameId else null,
                                                    password = if (selectedResponseType == SWResponseType.SHARED) password else null,
                                                    sharedClass = if (selectedResponseType == SWResponseType.SHARED) selectedSharedClass else null,
                                                    pilotingForId = if (selectedResponseType == SWResponseType.PILOT) selectedPilotFor?.userId else null,
                                                    preferredClass = if (selectedResponseType == SWResponseType.PILOT) selectedPreferredClass else null
                                                )
                                            },
                                            enabled = canSubmit && !uiState.isSubmitting,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (uiState.isSubmitting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text("ENVIAR")
                                            }
                                        }
                                    }
                                }
                            }
                            uiState.isLeader && selectedTabIndex == 1 -> {
                                // Responses Tab (Leader only)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp)
                                ) {
                                    LeaderPanel(
                                        summary = uiState.summary,
                                        responses = uiState.responses,
                                        notResponded = uiState.notResponded,
                                        onCloseSiegeWar = { viewModel.closeSiegeWar() },
                                        isActive = uiState.siegeWar?.isActive == true
                                    )
                                }
                            }
                            selectedTabIndex == historyTabIndex -> {
                                // History Tab
                                HistoryTab(
                                    history = uiState.history,
                                    isLoading = uiState.isLoadingHistory
                                )
                            }
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
private fun FormHeader(siegeWar: SiegeWar) {
    val eventDate = try {
        val zonedDateTime = ZonedDateTime.parse(siegeWar.weekEnd)
        // Convert to local timezone first
        val localDateTime = zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault())
        // Find the Sunday of this week (weekEnd is Monday early morning, so go back to previous Sunday)
        val dayOfWeek = localDateTime.dayOfWeek
        val daysToSubtract = when (dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> 1L
            java.time.DayOfWeek.TUESDAY -> 2L
            java.time.DayOfWeek.WEDNESDAY -> 3L
            java.time.DayOfWeek.THURSDAY -> 4L
            java.time.DayOfWeek.FRIDAY -> 5L
            java.time.DayOfWeek.SATURDAY -> 6L
            java.time.DayOfWeek.SUNDAY -> 0L
        }
        val sundayDateTime = localDateTime.minusDays(daysToSubtract)
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("pt", "BR"))
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))
        val dayName = sundayDateTime.format(dayOfWeekFormatter).replaceFirstChar { it.uppercase() }
        "$dayName, ${sundayDateTime.format(dateFormatter)}"
    } catch (e: Exception) {
        siegeWar.weekEnd
    }

    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = eventDate,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            AssistChip(
                onClick = {},
                label = { Text(if (siegeWar.isActive) "ATIVO" else "FECHADO") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (siegeWar.isActive)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.error,
                    labelColor = if (siegeWar.isActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onError
                )
            )
        }
    }
}

@Composable
private fun SharedConfigInfoCard() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header (always visible, clickable)
            Surface(
                onClick = { expanded = !expanded },
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modelo de Shared",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Como configurar sua conta para compartilhar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Recolher" else "Expandir",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Expandable content
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                    )

                    // Password and Duration
                    Text(
                        text = "Configuracao basica:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Share Password: senha temporária",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "• Duration: 5 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Limitations to BLOCK
                    Text(
                        text = "Limitacoes para BLOQUEAR:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val blockedItems = listOf(
                        "Grab Equipment",
                        "Open Warehouse",
                        "Open Clan Master",
                        "Open Caravan Pet",
                        "Setup Personal Shop",
                        "Spend Coins",
                        "Trade Coins",
                        "Open Item Distributor",
                        "Use Trade Chat",
                        "Pickup/Drop Items",
                        "Cancel Premium Item"
                    )

                    blockedItems.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Obs: Login User Panel e Create/Delete Characters ja sao bloqueados automaticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentResponseCard(response: SWUserResponse) {
    val responseText = when (response.responseType) {
        SWResponseType.CONFIRMED -> "Confirmado"
        SWResponseType.SHARED -> "Compartilhando conta"
        SWResponseType.PILOT -> "Pilotando: ${response.pilotingFor?.nick ?: "outro membro"}"
        SWResponseType.ABSENT -> "Ausente"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Sua resposta atual:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = responseText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                response.tag?.let { tag ->
                    Text(
                        text = "Time: ${TAG_DISPLAY_NAMES[tag]}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun RadioOption(
    text: String,
    description: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = if (isError) RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.error
            ) else RadioButtonDefaults.colors()
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError && selected) MaterialTheme.colorScheme.error else Color.Unspecified
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassSelectionGrid(
    selectedClass: PlayerClass?,
    onClassSelected: (PlayerClass) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlayerClass.entries.forEach { playerClass ->
            FilterChip(
                selected = selectedClass == playerClass,
                onClick = { onClassSelected(playerClass) },
                label = { Text(CLASS_DISPLAY_NAMES[playerClass] ?: playerClass.name) }
            )
        }
    }
}

@Composable
private fun NoSiegeWarContent(
    isLeader: Boolean,
    onCreateSiegeWar: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Nenhum Siege War ativo",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isLeader) {
                Button(onClick = onCreateSiegeWar) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Criar Siege War")
                }
            }
        }
    }
}

@Composable
private fun LeaderPanel(
    summary: com.madeinbraza.app.data.model.SWResponsesSummary?,
    responses: List<SWResponseItem>,
    notResponded: List<SWResponseUser>,
    onCloseSiegeWar: () -> Unit,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                Text(
                    text = "Painel do Lider",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    TextButton(
                        onClick = onCloseSiegeWar,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fechar SW")
                    }
                }
            }

            // Summary
            summary?.let { s ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryChip("Total", s.total)
                    SummaryChip("Resp.", s.responded, MaterialTheme.colorScheme.primary)
                    SummaryChip("Conf.", s.confirmed, MaterialTheme.colorScheme.primary)
                    SummaryChip("Comp.", s.shared, MaterialTheme.colorScheme.secondary)
                    SummaryChip("Pilot", s.pilots, MaterialTheme.colorScheme.tertiary)
                    SummaryChip("Aus.", s.absent, MaterialTheme.colorScheme.error)
                }
            }

            // Not responded
            if (notResponded.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nao responderam (${notResponded.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notResponded.joinToString(", ") { it.nick },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Responses list
            if (responses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Respostas:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                responses.forEach { response ->
                    ResponseListItem(response)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResponseListItem(response: SWResponseItem) {
    val responseColor = when (response.responseType) {
        SWResponseType.CONFIRMED -> MaterialTheme.colorScheme.primary
        SWResponseType.SHARED -> MaterialTheme.colorScheme.secondary
        SWResponseType.PILOT -> MaterialTheme.colorScheme.tertiary
        SWResponseType.ABSENT -> MaterialTheme.colorScheme.error
    }

    val responseLabel = when (response.responseType) {
        SWResponseType.CONFIRMED -> "CONFIRMADO"
        SWResponseType.SHARED -> "SHARED"
        SWResponseType.PILOT -> "PILOTO"
        SWResponseType.ABSENT -> "AUSENTE"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = responseColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, responseColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Nick + Class + Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (response.responseType) {
                            SWResponseType.CONFIRMED -> Icons.Filled.Check
                            SWResponseType.SHARED -> Icons.Filled.Share
                            SWResponseType.PILOT -> Icons.Filled.Person
                            SWResponseType.ABSENT -> Icons.Filled.Close
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = responseColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = response.user.nick,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${CLASS_DISPLAY_NAMES[response.user.playerClass] ?: response.user.playerClass.name})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Tag badge
                response.tag?.let { tag ->
                    Surface(
                        color = when (tag) {
                            SWTag.ATTACK -> Color(0xFFE53935)
                            SWTag.DEFENSE -> Color(0xFF1E88E5)
                            SWTag.ACADEMY -> Color(0xFF43A047)
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = TAG_DISPLAY_NAMES[tag] ?: tag.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Response type label
            Text(
                text = responseLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = responseColor
            )

            // Details based on response type
            when (response.responseType) {
                SWResponseType.CONFIRMED -> {
                    Text(
                        text = "Vai participar com sua propria conta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SWResponseType.SHARED -> {
                    Column {
                        Text(
                            text = "Deixando conta para pilotagem:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text(
                                text = "ID: ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = response.gameId ?: "-",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Row {
                            Text(
                                text = "Senha: ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = response.password ?: "***",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Row {
                            Text(
                                text = "Classe: ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = response.sharedClass?.let { CLASS_FULL_NAMES[it] } ?: "-",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                SWResponseType.PILOT -> {
                    Column {
                        Text(
                            text = "Vai pilotar conta de outro membro:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text(
                                text = "Pilotando: ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = response.pilotingFor?.nick ?: "-",
                                style = MaterialTheme.typography.labelSmall
                            )
                            response.pilotingFor?.playerClass?.let { pc ->
                                Text(
                                    text = " (${CLASS_DISPLAY_NAMES[pc]})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            Text(
                                text = "Classe preferida: ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = response.preferredClass?.let { CLASS_FULL_NAMES[it] } ?: "-",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                SWResponseType.ABSENT -> {
                    Text(
                        text = "Nao vai participar desta SW",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    history: List<SiegeWarHistoryItem>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhum historico disponivel",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            history.forEach { siegeWar ->
                HistoryItemCard(siegeWar = siegeWar)
            }
        }
    }
}

@Composable
private fun HistoryItemCard(siegeWar: SiegeWarHistoryItem) {
    val eventDate = try {
        val zonedDateTime = ZonedDateTime.parse(siegeWar.weekEnd)
        val localDateTime = zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault())
        val dayOfWeek = localDateTime.dayOfWeek
        val daysToSubtract = when (dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> 1L
            java.time.DayOfWeek.TUESDAY -> 2L
            java.time.DayOfWeek.WEDNESDAY -> 3L
            java.time.DayOfWeek.THURSDAY -> 4L
            java.time.DayOfWeek.FRIDAY -> 5L
            java.time.DayOfWeek.SATURDAY -> 6L
            java.time.DayOfWeek.SUNDAY -> 0L
        }
        val sundayDateTime = localDateTime.minusDays(daysToSubtract)
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
        "Domingo, ${sundayDateTime.format(dateFormatter)}"
    } catch (e: Exception) {
        siegeWar.weekEnd
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header (clickable)
            Surface(
                onClick = { expanded = !expanded },
                color = Color.Transparent
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
                        Text(
                            text = eventDate,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) "Recolher" else "Expandir"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Summary chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryChip("Resp.", siegeWar.summary.responded, MaterialTheme.colorScheme.primary)
                        SummaryChip("Conf.", siegeWar.summary.confirmed, MaterialTheme.colorScheme.primary)
                        SummaryChip("Comp.", siegeWar.summary.shared, MaterialTheme.colorScheme.secondary)
                        SummaryChip("Pilot", siegeWar.summary.pilots, MaterialTheme.colorScheme.tertiary)
                        SummaryChip("Aus.", siegeWar.summary.absent, MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Expandable content with responses
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    if (siegeWar.responses.isEmpty()) {
                        Text(
                            text = "Nenhuma resposta registrada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        siegeWar.responses.forEach { response ->
                            HistoryResponseItem(response = response)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryResponseItem(response: SWHistoryResponseItem) {
    val responseColor = when (response.responseType) {
        SWResponseType.CONFIRMED -> MaterialTheme.colorScheme.primary
        SWResponseType.SHARED -> MaterialTheme.colorScheme.secondary
        SWResponseType.PILOT -> MaterialTheme.colorScheme.tertiary
        SWResponseType.ABSENT -> MaterialTheme.colorScheme.error
    }

    val responseLabel = when (response.responseType) {
        SWResponseType.CONFIRMED -> "CONFIRMADO"
        SWResponseType.SHARED -> "SHARED"
        SWResponseType.PILOT -> "PILOTO"
        SWResponseType.ABSENT -> "AUSENTE"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (response.responseType) {
                    SWResponseType.CONFIRMED -> Icons.Filled.Check
                    SWResponseType.SHARED -> Icons.Filled.Share
                    SWResponseType.PILOT -> Icons.Filled.Person
                    SWResponseType.ABSENT -> Icons.Filled.Close
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = responseColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = response.user.nick,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "(${CLASS_DISPLAY_NAMES[response.user.playerClass] ?: response.user.playerClass.name})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tag badge
            response.tag?.let { tag ->
                Surface(
                    color = when (tag) {
                        SWTag.ATTACK -> Color(0xFFE53935)
                        SWTag.DEFENSE -> Color(0xFF1E88E5)
                        SWTag.ACADEMY -> Color(0xFF43A047)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = TAG_DISPLAY_NAMES[tag] ?: tag.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Response type chip
            Surface(
                color = responseColor.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = responseLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = responseColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
