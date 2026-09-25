package com.example.ui.screens.releases

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
import com.example.data.model.CreateReleaseRequest
import com.example.data.model.Release
import com.example.data.repository.ApiResult
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MarkdownViewer
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.DateUtils
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleasesScreen(
    owner: String,
    repoName: String,
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var releases by remember { mutableStateOf<List<Release>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var tagName by remember { mutableStateOf("") }
    var releaseTitle by remember { mutableStateOf("") }
    var releaseBody by remember { mutableStateOf("") }
    var isPrerelease by remember { mutableStateOf(false) }
    var isDraft by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    var releaseToDelete by remember { mutableStateOf<Release?>(null) }

    fun loadReleases() {
        isLoading = true
        coroutineScope.launch {
            when (val res = viewModel.repository.getReleases(owner, repoName)) {
                is ApiResult.Success -> releases = res.data
                is ApiResult.Error -> viewModel.postMessage(res.message)
                is ApiResult.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadReleases()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Releases", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadReleases() }) {
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
                Icon(Icons.Filled.Add, contentDescription = "New Release")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (releases.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Tag,
                    title = "No Releases",
                    description = "There aren’t any releases or tags in this repository yet.",
                    actionButtonText = "Draft a new release",
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(releases, key = { it.id }) { rel ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Outlined.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(rel.name ?: rel.tag_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(rel.tag_name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    IconButton(onClick = { releaseToDelete = rel }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Text(
                                    text = "Released ${DateUtils.formatRelativeTime(rel.published_at ?: rel.created_at)} by ${rel.author?.login ?: "user"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (!rel.body.isNullOrBlank()) {
                                    HorizontalDivider(thickness = 0.5.dp)
                                    MarkdownViewer(markdown = rel.body)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Draft New Release") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        label = { Text("Tag version (e.g. v1.0.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = releaseTitle,
                        onValueChange = { releaseTitle = it },
                        label = { Text("Release title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = releaseBody,
                        onValueChange = { releaseBody = it },
                        label = { Text("Release notes (markdown)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrerelease, onCheckedChange = { isPrerelease = it })
                        Text("This is a pre-release")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tagName.isNotBlank()) {
                            isCreating = true
                            coroutineScope.launch {
                                HapticUtils.performConfirm(context)
                                val req = CreateReleaseRequest(
                                    tag_name = tagName.trim(),
                                    name = releaseTitle.ifBlank { tagName.trim() },
                                    body = releaseBody.ifBlank { null },
                                    prerelease = isPrerelease,
                                    draft = isDraft
                                )
                                when (val res = viewModel.repository.createRelease(owner, repoName, req)) {
                                    is ApiResult.Success -> {
                                        viewModel.postMessage("Release ${res.data.tag_name} created")
                                        showCreateDialog = false
                                        tagName = ""
                                        releaseTitle = ""
                                        releaseBody = ""
                                        loadReleases()
                                    }
                                    is ApiResult.Error -> viewModel.postMessage("Failed: ${res.message}")
                                    is ApiResult.Loading -> {}
                                }
                                isCreating = false
                            }
                        }
                    },
                    enabled = tagName.isNotBlank() && !isCreating
                ) {
                    Text("Publish Release")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (releaseToDelete != null) {
        val rel = releaseToDelete!!
        AlertDialog(
            onDismissRequest = { releaseToDelete = null },
            title = { Text("Delete Release") },
            text = { Text("Are you sure you want to delete release \"${rel.tag_name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = rel.id
                        releaseToDelete = null
                        coroutineScope.launch {
                            HapticUtils.performConfirm(context)
                            when (val res = viewModel.repository.deleteRelease(owner, repoName, id)) {
                                is ApiResult.Success -> {
                                    viewModel.postMessage("Release deleted")
                                    loadReleases()
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
                TextButton(onClick = { releaseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
