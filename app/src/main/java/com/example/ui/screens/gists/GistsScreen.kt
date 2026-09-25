package com.example.ui.screens.gists

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CreateGistRequest
import com.example.data.model.Gist
import com.example.data.model.GistFileContent
import com.example.data.repository.ApiResult
import com.example.ui.components.CodeViewer
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.MonospaceCodeStyle
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.DateUtils
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GistsScreen(
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var gists by remember { mutableStateOf<List<Gist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newFilename by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    var selectedGist by remember { mutableStateOf<Gist?>(null) }
    var gistToDelete by remember { mutableStateOf<Gist?>(null) }

    fun loadGists() {
        isLoading = true
        coroutineScope.launch {
            when (val res = viewModel.repository.getGists()) {
                is ApiResult.Success -> gists = res.data
                is ApiResult.Error -> viewModel.postMessage(res.message)
                is ApiResult.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadGists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gists", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadGists() }) {
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
                Icon(Icons.Filled.Add, contentDescription = "New Gist")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (gists.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Code,
                    title = "No Gists",
                    description = "You haven't created any code snippets or gists yet.",
                    actionButtonText = "Create Gist",
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(gists, key = { it.id }) { gist ->
                        val firstFile = gist.files.values.firstOrNull()
                        val fileName = firstFile?.filename ?: "snippet.txt"
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = if (!gist.description.isNullOrBlank()) gist.description else fileName,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "$fileName • ${gist.files.size} file(s) • ${DateUtils.formatRelativeTime(gist.created_at)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (gist.public) Icons.Outlined.Public else Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { gistToDelete = gist }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete gist",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.clickable { selectedGist = gist }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // View Gist Dialog
    if (selectedGist != null) {
        val gist = selectedGist!!
        val file = gist.files.values.firstOrNull()
        AlertDialog(
            onDismissRequest = { selectedGist = null },
            title = {
                Text(file?.filename ?: "Gist", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!gist.description.isNullOrBlank()) {
                        Text(gist.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (file?.content != null) {
                        CodeViewer(fileName = file.filename, code = file.content)
                    } else {
                        Text("Open file in browser to view full content.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedGist = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Create Gist Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Gist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newFilename,
                        onValueChange = { newFilename = it },
                        label = { Text("Filename (e.g. solution.kt)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        textStyle = MonospaceCodeStyle
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                        Text("Public gist")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fname = if (newFilename.isNotBlank()) newFilename.trim() else "snippet.txt"
                        if (newContent.isNotBlank()) {
                            isCreating = true
                            coroutineScope.launch {
                                HapticUtils.performConfirm(context)
                                val req = CreateGistRequest(
                                    description = newDescription.ifBlank { null },
                                    public = isPublic,
                                    files = mapOf(fname to GistFileContent(newContent))
                                )
                                when (val res = viewModel.repository.createGist(req)) {
                                    is ApiResult.Success -> {
                                        viewModel.postMessage("Gist created successfully")
                                        showCreateDialog = false
                                        newFilename = ""
                                        newDescription = ""
                                        newContent = ""
                                        loadGists()
                                    }
                                    is ApiResult.Error -> viewModel.postMessage("Failed: ${res.message}")
                                    is ApiResult.Loading -> {}
                                }
                                isCreating = false
                            }
                        }
                    },
                    enabled = newContent.isNotBlank() && !isCreating
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

    // Delete Gist Dialog
    if (gistToDelete != null) {
        val g = gistToDelete!!
        AlertDialog(
            onDismissRequest = { gistToDelete = null },
            title = { Text("Delete Gist") },
            text = { Text("Are you sure you want to permanently delete this gist?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = g.id
                        gistToDelete = null
                        coroutineScope.launch {
                            HapticUtils.performConfirm(context)
                            when (val res = viewModel.repository.deleteGist(id)) {
                                is ApiResult.Success -> {
                                    viewModel.postMessage("Gist deleted")
                                    loadGists()
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
                TextButton(onClick = { gistToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
