package com.example.ui.screens.code

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.api.NetworkModule
import com.example.data.model.ContentItem
import com.example.data.model.CreateUpdateFileRequest
import com.example.data.repository.ApiResult
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.HapticUtils
import com.squareup.moshi.Types
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeBrowserScreen(
    owner: String,
    repoName: String,
    initialPath: String = "",
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit,
    onNavigateToFileViewer: (owner: String, repo: String, path: String, name: String) -> Unit,
    onNavigateToCreateFile: (owner: String, repo: String, path: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentPath by remember { mutableStateOf(initialPath) }
    var items by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadPath(path: String) {
        isLoading = true
        currentPath = path
        coroutineScope.launch {
            when (val res = viewModel.repository.getContents(owner, repoName, path)) {
                is ApiResult.Success -> {
                    try {
                        val moshi = NetworkModule.provideMoshi()
                        val type = Types.newParameterizedType(List::class.java, ContentItem::class.java)
                        val adapter = moshi.adapter<List<ContentItem>>(type)
                        val parsed = adapter.fromJson(res.data) ?: emptyList()
                        // Sort: folders first, then files alphabetically
                        items = parsed.sortedWith(compareBy({ it.type != "dir" }, { it.name.lowercase() }))
                    } catch (e: Exception) {
                        items = emptyList()
                    }
                }
                is ApiResult.Error -> {
                    viewModel.postMessage(res.message)
                    items = emptyList()
                }
                is ApiResult.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(currentPath) {
        loadPath(currentPath)
    }

    // System File Picker for upload (Requirement 17)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val contentResolver = context.contentResolver
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "uploaded_file"
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val targetPath = if (currentPath.isBlank()) fileName else "$currentPath/$fileName"
                        val req = CreateUpdateFileRequest(
                            message = "Upload $fileName via Prime Repo Manager",
                            content = base64
                        )
                        HapticUtils.performConfirm(context)
                        when (val uploadRes = viewModel.repository.createOrUpdateFile(owner, repoName, targetPath, req)) {
                            is ApiResult.Success -> {
                                viewModel.postMessage("Uploaded $fileName successfully")
                                loadPath(currentPath)
                            }
                            is ApiResult.Error -> viewModel.postMessage("Upload failed: ${uploadRes.message}")
                            is ApiResult.Loading -> {}
                        }
                    }
                } catch (e: Exception) {
                    viewModel.postMessage("Failed to read file: ${e.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$owner/$repoName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPath.isNotBlank()) {
                            val parent = currentPath.substringBeforeLast('/', "")
                            loadPath(parent)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToCreateFile(owner, repoName, currentPath) }) {
                        Icon(Icons.Outlined.NoteAdd, contentDescription = "New file")
                    }
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = "Upload file")
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
        ) {
            // Breadcrumbs Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = repoName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { loadPath("") }
                )

                if (currentPath.isNotBlank()) {
                    val segments = currentPath.split("/").filter { it.isNotBlank() }
                    var accumulated = ""
                    segments.forEachIndexed { idx, segment ->
                        Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        accumulated = if (accumulated.isEmpty()) segment else "$accumulated/$segment"
                        val target = accumulated
                        Text(
                            text = segment,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (idx == segments.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                            fontWeight = if (idx == segments.lastIndex) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable { loadPath(target) }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (items.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.FolderOpen,
                    title = "Empty Directory",
                    description = "This folder does not contain any files yet.",
                    actionButtonText = "Create File",
                    onActionClick = { onNavigateToCreateFile(owner, repoName, currentPath) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.sha + it.name }) { item ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = item.name,
                                    fontWeight = if (item.type == "dir") FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            supportingContent = {
                                if (item.type == "file") {
                                    Text("${item.size} bytes", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (item.type == "dir") Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (item.type == "dir") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                if (item.type == "dir") {
                                    Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable {
                                if (item.type == "dir") {
                                    loadPath(item.path)
                                } else {
                                    onNavigateToFileViewer(owner, repoName, item.path, item.name)
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
