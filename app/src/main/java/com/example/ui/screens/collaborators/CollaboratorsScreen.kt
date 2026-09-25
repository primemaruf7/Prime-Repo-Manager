package com.example.ui.screens.collaborators

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Collaborator
import com.example.data.repository.ApiResult
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

private val PermissionsList = listOf("push", "pull", "triage", "maintain", "admin")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaboratorsScreen(
    owner: String,
    repoName: String,
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var collaborators by remember { mutableStateOf<List<Collaborator>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var selectedPerm by remember { mutableStateOf("push") }
    var isAdding by remember { mutableStateOf(false) }

    var collaboratorToRemove by remember { mutableStateOf<Collaborator?>(null) }

    fun loadCollaborators() {
        isLoading = true
        coroutineScope.launch {
            when (val res = viewModel.repository.getCollaborators(owner, repoName)) {
                is ApiResult.Success -> collaborators = res.data
                is ApiResult.Error -> viewModel.postMessage(res.message)
                is ApiResult.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadCollaborators()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collaborators", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadCollaborators() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Collaborator")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (collaborators.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.PeopleOutline,
                    title = "No Collaborators",
                    description = "Invite collaborators to contribute to this repository.",
                    actionButtonText = "Add Collaborator",
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(collaborators, key = { it.id }) { c ->
                        ListItem(
                            headlineContent = { Text(c.login, fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Text("Permission: ${c.role_name ?: "push"}", style = MaterialTheme.typography.bodySmall)
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = c.avatar_url,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { collaboratorToRemove = c }) {
                                    Icon(Icons.Outlined.PersonRemove, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Collaborator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("GitHub Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Permission Level:")
                    PermissionsList.forEach { perm ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(selected = selectedPerm == perm, onClick = { selectedPerm = perm })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(perm.capitalize())
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank()) {
                            isAdding = true
                            coroutineScope.launch {
                                HapticUtils.performConfirm(context)
                                when (val res = viewModel.repository.addCollaborator(owner, repoName, newUsername.trim(), selectedPerm)) {
                                    is ApiResult.Success -> {
                                        viewModel.postMessage("Collaborator invitation sent to @$newUsername")
                                        showAddDialog = false
                                        newUsername = ""
                                        loadCollaborators()
                                    }
                                    is ApiResult.Error -> viewModel.postMessage("Failed: ${res.message}")
                                    is ApiResult.Loading -> {}
                                }
                                isAdding = false
                            }
                        }
                    },
                    enabled = newUsername.isNotBlank() && !isAdding
                ) {
                    Text("Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (collaboratorToRemove != null) {
        val c = collaboratorToRemove!!
        AlertDialog(
            onDismissRequest = { collaboratorToRemove = null },
            title = { Text("Remove Collaborator") },
            text = { Text("Are you sure you want to remove @${c.login} from this repository?") },
            confirmButton = {
                Button(
                    onClick = {
                        collaboratorToRemove = null
                        coroutineScope.launch {
                            HapticUtils.performConfirm(context)
                            when (val res = viewModel.repository.removeCollaborator(owner, repoName, c.login)) {
                                is ApiResult.Success -> {
                                    viewModel.postMessage("Removed @${c.login}")
                                    loadCollaborators()
                                }
                                is ApiResult.Error -> viewModel.postMessage("Failed: ${res.message}")
                                is ApiResult.Loading -> {}
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { collaboratorToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
