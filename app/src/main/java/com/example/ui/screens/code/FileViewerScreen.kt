package com.example.ui.screens.code

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.DeleteFileRequest
import com.example.data.repository.ApiResult
import com.example.ui.components.CodeViewer
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.Base64Utils
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    owner: String,
    repoName: String,
    path: String,
    fileName: String,
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit,
    onEditFile: (owner: String, repo: String, path: String, sha: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var contentItem by remember { mutableStateOf<ContentItem?>(null) }
    var decodedCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteCommitMessage by remember { mutableStateOf("Delete $fileName") }

    LaunchedEffect(path) {
        isLoading = true
        when (val res = viewModel.repository.getContents(owner, repoName, path)) {
            is ApiResult.Success -> {
                try {
                    val moshi = NetworkModule.provideMoshi()
                    val adapter = moshi.adapter(ContentItem::class.java)
                    val item = adapter.fromJson(res.data)
                    contentItem = item
                    decodedCode = Base64Utils.decodeUtf8(item?.content ?: "")
                } catch (e: Exception) {
                    decodedCode = res.data
                }
            }
            is ApiResult.Error -> {
                viewModel.postMessage(res.message)
            }
            is ApiResult.Loading -> {}
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sha = contentItem?.sha ?: ""
                        onEditFile(owner, repoName, path, sha)
                    }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit file")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete file", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    CodeViewer(
                        fileName = fileName,
                        code = decodedCode,
                        onEditClick = {
                            val sha = contentItem?.sha ?: ""
                            onEditFile(owner, repoName, path, sha)
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Are you sure you want to delete \"$fileName\" from $owner/$repoName?")
                    OutlinedTextField(
                        value = deleteCommitMessage,
                        onValueChange = { deleteCommitMessage = it },
                        label = { Text("Commit message") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        coroutineScope.launch {
                            HapticUtils.performConfirm(context)
                            val sha = contentItem?.sha ?: ""
                            val req = DeleteFileRequest(
                                message = deleteCommitMessage.ifBlank { "Delete $fileName" },
                                sha = sha
                            )
                            when (val res = viewModel.repository.deleteFile(owner, repoName, path, req)) {
                                is ApiResult.Success -> {
                                    viewModel.postMessage("File $fileName deleted successfully")
                                    onBack()
                                }
                                is ApiResult.Error -> viewModel.postMessage("Delete failed: ${res.message}")
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
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
