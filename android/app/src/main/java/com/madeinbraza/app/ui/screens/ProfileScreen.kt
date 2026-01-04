package com.madeinbraza.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.*
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.madeinbraza.app.BuildConfig
import com.madeinbraza.app.R
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.ui.viewmodel.ProfileViewModel
import com.madeinbraza.app.util.LanguageManager
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private fun buildFullAvatarUrl(avatarUrl: String?): String? {
    if (avatarUrl == null) return null
    if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) return avatarUrl
    val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/api/").removeSuffix("/api")
    return "$baseUrl$avatarUrl"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToPendingMembers: () -> Unit = {},
    onNavigateToBannedUsers: () -> Unit = {},
    onLanguageChanged: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var editNick by remember { mutableStateOf("") }
    var editPlayerClass by remember { mutableStateOf<PlayerClass?>(null) }
    var showClassDropdown by remember { mutableStateOf(false) }

    // Notification permission state
    val context = LocalContext.current
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    // Language selection state
    val coroutineScope = rememberCoroutineScope()
    val currentLanguageCode by LanguageManager.getLanguageFlow(context).collectAsState(initial = "pt")
    var selectedLanguage by remember { mutableStateOf(LanguageManager.getDisplayNameFromCode(currentLanguageCode)) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    val languages = LanguageManager.languages.map { it.displayName }

    // Update selectedLanguage when currentLanguageCode changes
    LaunchedEffect(currentLanguageCode) {
        selectedLanguage = LanguageManager.getDisplayNameFromCode(currentLanguageCode)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    // Delete avatar confirmation dialog state
    var showDeleteAvatarDialog by remember { mutableStateOf(false) }

    // Refresh notification state when screen resumes
    LaunchedEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    LaunchedEffect(uiState.profile) {
        uiState.profile?.let {
            editNick = it.nick
            editPlayerClass = it.playerClass
        }
    }

    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            isEditing = false
            viewModel.clearUpdateSuccess()
        }
    }

    // Password change dialog
    if (uiState.showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { viewModel.hidePasswordDialog() },
            onChangePassword = { currentPassword, newPassword ->
                viewModel.changePassword(currentPassword, newPassword)
            },
            isChanging = uiState.isChangingPassword,
            error = if (uiState.showPasswordDialog) uiState.error else null
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val passwordChangedMsg = stringResource(R.string.password_changed)

    LaunchedEffect(uiState.passwordChangeSuccess) {
        if (uiState.passwordChangeSuccess) {
            snackbarHostState.showSnackbar(passwordChangedMsg)
            viewModel.clearPasswordChangeSuccess()
        }
    }

    val avatarUpdatedMsg = stringResource(R.string.avatar_updated)

    LaunchedEffect(uiState.avatarSuccess) {
        if (uiState.avatarSuccess) {
            snackbarHostState.showSnackbar(avatarUpdatedMsg)
            viewModel.clearAvatarSuccess()
        }
    }

    // Delete avatar confirmation dialog
    if (showDeleteAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAvatarDialog = false },
            title = { Text(stringResource(R.string.delete_avatar_title)) },
            text = { Text(stringResource(R.string.delete_avatar_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAvatar()
                        showDeleteAvatarDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAvatarDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_profile)) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    if (!isEditing && uiState.profile != null) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    } else if (isEditing) {
                        IconButton(
                            onClick = {
                                viewModel.updateProfile(
                                    nick = if (editNick != uiState.profile?.nick) editNick else null,
                                    playerClass = if (editPlayerClass != uiState.profile?.playerClass) editPlayerClass else null
                                )
                            },
                            enabled = !uiState.isUpdating
                        ) {
                            if (uiState.isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                            }
                        }
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
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.unknown_error),
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        // Profile Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                if (isEditing) {
                                    // Edit mode
                                    OutlinedTextField(
                                        value = editNick,
                                        onValueChange = { editNick = it },
                                        label = { Text(stringResource(R.string.nick)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    ExposedDropdownMenuBox(
                                        expanded = showClassDropdown,
                                        onExpandedChange = { showClassDropdown = it }
                                    ) {
                                        OutlinedTextField(
                                            value = editPlayerClass?.name ?: "",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.class_label)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showClassDropdown)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )

                                        ExposedDropdownMenu(
                                            expanded = showClassDropdown,
                                            onDismissRequest = { showClassDropdown = false }
                                        ) {
                                            PlayerClass.entries.forEach { playerClass ->
                                                DropdownMenuItem(
                                                    text = { Text(playerClass.name) },
                                                    onClick = {
                                                        editPlayerClass = playerClass
                                                        showClassDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    TextButton(
                                        onClick = {
                                            isEditing = false
                                            editNick = profile.nick
                                            editPlayerClass = profile.playerClass
                                        }
                                    ) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                } else {
                                    // View mode
                                    val leaderText = stringResource(R.string.leader)
                                    val counselorText = stringResource(R.string.counselor)
                                    val memberText = stringResource(R.string.member)

                                    // Avatar section
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = CircleShape
                                                )
                                                .clickable(
                                                    enabled = !uiState.isUploadingAvatar && !uiState.isDeletingAvatar
                                                ) {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (uiState.isUploadingAvatar || uiState.isDeletingAvatar) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(40.dp),
                                                    strokeWidth = 3.dp
                                                )
                                            } else if (profile.avatarUrl != null) {
                                                AsyncImage(
                                                    model = buildFullAvatarUrl(profile.avatarUrl),
                                                    contentDescription = stringResource(R.string.profile_picture),
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Camera icon overlay
                                        if (!uiState.isUploadingAvatar && !uiState.isDeletingAvatar) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .clickable {
                                                        photoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = stringResource(R.string.change_avatar),
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }

                                    // Delete avatar button
                                    if (profile.avatarUrl != null && !uiState.isUploadingAvatar && !uiState.isDeletingAvatar) {
                                        TextButton(
                                            onClick = { showDeleteAvatarDialog = true },
                                            modifier = Modifier.align(Alignment.CenterHorizontally),
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.delete_avatar))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = profile.nick,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = stringResource(R.string.class_info, profile.playerClass.name),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = stringResource(R.string.role_info, when (profile.role) {
                                            Role.LEADER -> leaderText
                                            Role.COUNSELOR -> counselorText
                                            else -> memberText
                                        }),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "${stringResource(R.string.status)}: ${profile.status.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    val formattedDate = try {
                                        val zonedDateTime = ZonedDateTime.parse(profile.createdAt)
                                        zonedDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    } catch (e: Exception) {
                                        profile.createdAt
                                    }

                                    Text(
                                        text = "${stringResource(R.string.member_since)}: $formattedDate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Stats Card
                        Text(
                            text = stringResource(R.string.statistics),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                title = stringResource(R.string.messages),
                                value = profile.stats.messagesCount.toString(),
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                title = stringResource(R.string.events),
                                value = profile.stats.eventsParticipated.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Change password button
                        OutlinedButton(
                            onClick = { viewModel.showPasswordDialog() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.change_password))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Notification settings button
                        OutlinedButton(
                            onClick = {
                                if (!notificationsEnabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        // Open app notification settings for older Android versions
                                        val intent = Intent().apply {
                                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    }
                                } else {
                                    // Open notification settings to let user disable if wanted
                                    val intent = Intent().apply {
                                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (notificationsEnabled) {
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            }
                        ) {
                            Icon(
                                if (notificationsEnabled) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (notificationsEnabled) stringResource(R.string.notifications_enabled) else stringResource(R.string.enable_notifications))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Language selection
                        ExposedDropdownMenuBox(
                            expanded = showLanguageDropdown,
                            onExpandedChange = { showLanguageDropdown = it }
                        ) {
                            OutlinedButton(
                                onClick = { showLanguageDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedLanguage)
                                Spacer(modifier = Modifier.weight(1f))
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLanguageDropdown)
                            }

                            ExposedDropdownMenu(
                                expanded = showLanguageDropdown,
                                onDismissRequest = { showLanguageDropdown = false }
                            ) {
                                languages.forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(language) },
                                        onClick = {
                                            if (language != selectedLanguage) {
                                                selectedLanguage = language
                                                showLanguageDropdown = false
                                                val languageCode = LanguageManager.getLanguageCodeFromDisplayName(language)
                                                coroutineScope.launch {
                                                    LanguageManager.setLanguage(context, languageCode)
                                                    onLanguageChanged()
                                                }
                                            } else {
                                                showLanguageDropdown = false
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Leader-only actions
                        if (profile.role == Role.LEADER) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = stringResource(R.string.leader_actions),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

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
                                Text(stringResource(R.string.approve_members))
                                if (uiState.pendingCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(
                                        containerColor = Color(0xFFFFC107) // Yellow
                                    ) {
                                        Text(
                                            text = if (uiState.pendingCount > 99) "99+" else uiState.pendingCount.toString(),
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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
                                Text(stringResource(R.string.banned_users))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    isChanging: Boolean,
    error: String?
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val enterCurrentPasswordError = stringResource(R.string.enter_current_password)
    val passwordMinLengthError = stringResource(R.string.password_min_length)
    val passwordsDontMatchError = stringResource(R.string.passwords_dont_match)

    AlertDialog(
        onDismissRequest = { if (!isChanging) onDismiss() },
        title = { Text(stringResource(R.string.change_password_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; localError = null },
                    label = { Text(stringResource(R.string.current_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isChanging
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; localError = null },
                    label = { Text(stringResource(R.string.new_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isChanging
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; localError = null },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isChanging
                )

                if (error != null || localError != null) {
                    Text(
                        text = localError ?: error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPassword.isBlank() -> localError = enterCurrentPasswordError
                        newPassword.length < 6 -> localError = passwordMinLengthError
                        newPassword != confirmPassword -> localError = passwordsDontMatchError
                        else -> onChangePassword(currentPassword, newPassword)
                    }
                },
                enabled = !isChanging
            ) {
                if (isChanging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.change))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isChanging
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
