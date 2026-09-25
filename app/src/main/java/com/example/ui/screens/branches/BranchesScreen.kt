package com.example.ui.screens.branches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.Branch
import com.example.data.repository.ApiResult
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchesScreen(
    owner: String,
    repoName: String,
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var branches by remember { mutableStateOf<List<Branch>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }
    var sourceBranch by remember { mutableStateOf<Branch?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    var branchToDelete by remember { mutableStateOf<Branch?>(null) }

    fun loadBranches() {
        isLoading = true
        coroutineScope.launch {
            when (val res = viewModel.repository.getBranches(owner, repoName)) {
                is ApiResult.Success -> {
                    branches = res.data
                    if (sourceBranch == null && res.data.isNotEmpty()) {
                        sourceBranch = res.data.first()
                    }
                }
                is ApiResult.Error -> viewModel.postMessage(res.message)
                is ApiResult.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadBranches()
    }

    val filteredBranches = remember(branches, searchQuery) {
        if (searchQuery.isBlank()) branches
        else branches.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Branches (${branches.size})", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadBranches() }) {
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
                Icon(Icons.Filled.Add, contentDescription = "New Branch")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search branches...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredBranches.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.AltRoute,
                    title = "No Branches Found",
                    description = "No branches match your search.",
                    actionButtonText = "New Branch",
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredBranches, key = { it.name }) { branch ->
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(branch.name, fontWeight = FontWeight.SemiBold)
                                    if (branch.isProtected) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "Protected",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            supportingContent = {
                                Text("SHA: ${branch.commit.sha.take(7)}", style = MaterialTheme.typography.bodySmall)
                            },
                            leadingContent = {
                                Icon(Icons.Outlined.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                if (branch.name != "main" && branch.name != "master") {
                                    IconButton(onClick = { branchToDelete = branch }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Delete branch",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
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
            title = { Text("Create New Branch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it.replace(" ", "-") },
                        label = { Text("Branch name") },
                        placeholder = { Text("feature/my-new-feature") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Source: ${sourceBranch?.name ?: "default"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sourceSha = sourceBranch?.commit?.sha
                        if (newBranchName.isNotBlank() && sourceSha != null) {
                            isCreating = true
                            coroutineScope.launch {
                                HapticUtils.performConfirm(context)
                                when (val res = viewModel.repository.createBranch(owner, repoName, newBranchName.trim(), sourceSha)) {
                                    is ApiResult.Success -> {
                                        viewModel.postMessage("Branch ${newBranchName} created")
                                        showCreateDialog = false
                                        newBranchName = ""
                                        loadBranches()
                                    }
                                    is ApiResult.Error -> viewModel.postMessage("Failed: ${res.message}")
                                    is ApiResult.Loading -> {}
                                }
                                isCreating = false
                            }
                        }
                    },
                    enabled = newBranchName.isNotBlank() && !isCreating
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (branchToDelete != null) {
        val b = branchToDelete!!
        AlertDialog(
            onDismissRequest = { branchToDelete = null },
            title = { Text("Delete Branch") },
            text = { Text("Are you sure you want to delete branch \"${b.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        branchToDelete = null
                        coroutineScope.launch {
                            HapticUtils.performConfirm(context)
                            when (val res = viewModel.repository.deleteBranch(owner, repoName, b.name)) {
                                is ApiResult.Success -> {
                                    viewModel.postMessage("Branch ${b.name} deleted")
                                    loadBranches()
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
                TextButton(onClick = { branchToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
