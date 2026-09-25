package com.example.ui.screens.webhooks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CreateWebhookRequest
import com.example.data.model.Webhook
import com.example.data.model.WebhookConfig
import com.example.data.repository.ApiResult
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebhooksScreen(
    owner: String,
    repoName: String,
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var webhooks by remember { mutableStateOf<List<Webhook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var payloadUrl by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var webhookToDelete by remember { mutableStateOf<Webhook?>(null) }

    fun loadWebhooks() {
        isLoading = true
        coroutineScope.launch {
            when (val res = viewModel.repository.getWebhooks(owner, repoName)) {
                is ApiResult.Success -> webhooks = res.data
                is ApiResult.Error -> viewModel.postMessage(res.message)
                is ApiResult.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadWebhooks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Webhooks", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadWebhooks() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Webhook")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (webhooks.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Webhook,
                    title = "No Webhooks",
                    description = "Webhooks allow external services to be notified when certain events happen on GitHub.",
                    actionButtonText = "Add Webhook",
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(webhooks, key = { it.id }) { hook ->
                        ListItem(
                            headlineContent = { Text(hook.config.url, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                            supportingContent = {
                                Text("Events: ${hook.events.joinToString(", ")} • Active: ${if (hook.active) "Yes" else "No"}", style = MaterialTheme.typography.bodySmall)
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.Webhook,
                                    contentDescription = null,
                                    tint = if (hook.active) Color(0xFF238636) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { webhookToDelete = hook }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Add Webhook") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = payloadUrl,
                        onValueChange = { payloadUrl = it },
                        label = { Text("Payload URL") },
                        placeholder = { Text("https://example.com/postreceive") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { secret = it },
                        label = { Text("Secret (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (payloadUrl.isNotBlank()) {
                            isCreating = true
                            coroutineScope.launch {
                                HapticUtils.performConfirm(context)
                                val req = CreateWebhookRequest(
                                    config = WebhookConfig(url = payloadUrl.trim(), secret = secret.ifBlank { null })
                                )
                                when (val res = viewModel.repository.createWebhook(owner, repoName, req)) {
                                    is ApiResult.Success -> {
                                        viewModel.postMessage("Webhook created")
                                        showCreateDialog = false
                                        payloadUrl = ""
                                        secret = ""
                                        loadWebhooks()
                                    }
                                    is ApiResult.Error -> viewModel.postMessage("Failed: ${res.message}")
                                    is ApiResult.Loading -> {}
                                }
                                isCreating = false
                            }
                        }
                    },
                    enabled = payloadUrl.isNotBlank() && !isCreating
                ) {
                    Text("Add Webhook")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (webhookToDelete != null) {
        val h = webhookToDelete!!
        AlertDialog(
            onDismissRequest = { webhookToDelete = null },
            title = { Text("Delete Webhook") },
            text = { Text("Are you sure you want to delete this webhook for ${h.config.url}?") },
            confirmButton = {
                Button(
                    onClick = {
                        webhookToDelete = null
                        coroutineScope.launch {
                            HapticUtils.performConfirm(context)
                            when (val res = viewModel.repository.deleteWebhook(owner, repoName, h.id)) {
                                is ApiResult.Success -> {
                                    viewModel.postMessage("Webhook deleted")
                                    loadWebhooks()
                                }
                                is ApiResult.Error -> viewModel.postMessage("Failed: ${res.message}")
                                is ApiResult.Loading -> {}
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { webhookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
