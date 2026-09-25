package com.example.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.security.LockTimeout
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PrimeRepoLogo
import com.example.ui.components.RateLimitCard
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rateLimit by viewModel.rateLimit.collectAsState()
    val currentTheme by viewModel.themeMode.collectAsState()

    var isLockEnabled by remember { mutableStateOf(viewModel.appLockManager.isLockEnabled()) }
    var selectedTimeout by remember { mutableStateOf(viewModel.appLockManager.getTimeout()) }
    var showTimeoutMenu by remember { mutableStateOf(false) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val rawToken = remember { viewModel.tokenManager.getToken() }
    val redactedToken = remember(rawToken) {
        if (rawToken != null) viewModel.tokenManager.redactToken(rawToken) else "None"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme Mode", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = currentTheme == mode,
                                onClick = {
                                    HapticUtils.performConfirm(context)
                                    viewModel.setThemeMode(mode)
                                },
                                label = { Text(mode.name.capitalize()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Security & App Lock Section (Requirement 37)
            Text("Security & App Lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Biometric App Lock", fontWeight = FontWeight.SemiBold)
                                Text("Require biometric or device credential", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isLockEnabled,
                            onCheckedChange = {
                                isLockEnabled = it
                                viewModel.appLockManager.setLockEnabled(it)
                                HapticUtils.performConfirm(context)
                                viewModel.postMessage(if (it) "App lock enabled" else "App lock disabled")
                            }
                        )
                    }

                    if (isLockEnabled) {
                        HorizontalDivider(thickness = 0.5.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimeoutMenu = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lock After", style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedTimeout.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = showTimeoutMenu,
                                onDismissRequest = { showTimeoutMenu = false }
                            ) {
                                LockTimeout.values().forEach { timeout ->
                                    DropdownMenuItem(
                                        text = { Text(timeout.label) },
                                        onClick = {
                                            selectedTimeout = timeout
                                            viewModel.appLockManager.setTimeout(timeout)
                                            showTimeoutMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                (context as? FragmentActivity)?.let { activity ->
                                    viewModel.appLockManager.showBiometricPrompt(
                                        activity = activity,
                                        onSuccess = { viewModel.postMessage("Biometric authentication successful!") },
                                        onError = { err -> viewModel.postMessage(err) }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Biometrics")
                        }
                    }
                }
            }

            // Keystore & Storage Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Credential Vault", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Token", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(redactedToken, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Encryption", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("AES-256-GCM / Hardware Keystore", style = MaterialTheme.typography.bodySmall, color = Color(0xFF238636), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Rate Limit
            Text("API Quota", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            RateLimitCard(info = rateLimit)

            // Cache & Danger Actions
            Text("Data & Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Button(
                onClick = { showClearCacheDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(Icons.Outlined.CleaningServices, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Offline Cache")
            }

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout & Wipe Token")
            }

            // About Footer
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimeRepoLogo(size = 36.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Prime Repo Manager v1.0", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Production-grade Native Android GitHub Client", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showLogoutDialog) {
        ConfirmActionDialog(
            title = "Log Out",
            message = "This will permanently remove the GitHub Personal Access Token from Android Keystore and clear all cached data on this device.",
            confirmText = "Log Out",
            isDestructive = true,
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout(onLoggedOut)
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showClearCacheDialog) {
        ConfirmActionDialog(
            title = "Clear Offline Cache",
            message = "This will remove all cached repositories, user profiles, and notification entries from Room storage. Fresh data will be downloaded next time.",
            confirmText = "Clear Cache",
            isDestructive = false,
            onConfirm = {
                showClearCacheDialog = false
                coroutineScope.launch {
                    viewModel.repository.clearCache()
                    viewModel.postMessage("Cache cleared successfully")
                }
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }
}
