package com.example.ui.screens.code

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
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
import com.example.ui.theme.MonospaceCodeStyle
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.Base64Utils
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCreateEditScreen(
    owner: String,
    repoName: String,
    initialPath: String = "",
    initialSha: String = "",
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var filePath by remember { mutableStateOf(initialPath) }
    var currentSha by remember { mutableStateOf(initialSha) }
    var content by remember { mutableStateOf("") }
    var commitMessage by remember {
        mutableStateOf(if (initialSha.isNotBlank()) "Update $initialPath" else "Create new file")
    }
    var isLoadingContent by remember { mutableStateOf(initialSha.isNotBlank()) }
    var isSaving by remember { mutableStateOf(false) }

    val isEditing = initialSha.isNotBlank()

    LaunchedEffect(initialSha) {
        if (initialSha.isNotBlank() && initialPath.isNotBlank()) {
            isLoadingContent = true
            when (val res = viewModel.repository.getContents(owner, repoName, initialPath)) {
                is ApiResult.Success -> {
                    try {
                        val moshi = NetworkModule.provideMoshi()
                        val adapter = moshi.adapter(ContentItem::class.java)
                        val item = adapter.fromJson(res.data)
                        if (item != null) {
                            currentSha = item.sha
                            content = Base64Utils.decodeUtf8(item.content ?: "")
                        }
                    } catch (e: Exception) {
                        content = res.data
                    }
                }
                is ApiResult.Error -> viewModel.postMessage(res.message)
                is ApiResult.Loading -> {}
            }
            isLoadingContent = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit File" else "Create File", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (filePath.isNotBlank()) {
                                isSaving = true
                                coroutineScope.launch {
                                    HapticUtils.performConfirm(context)
                                    val encodedContent = Base64Utils.encodeUtf8(content)
                                    val req = CreateUpdateFileRequest(
                                        message = commitMessage.ifBlank { if (isEditing) "Update $filePath" else "Create $filePath" },
                                        content = encodedContent,
                                        sha = if (isEditing && currentSha.isNotBlank()) currentSha else null
                                    )
                                    when (val res = viewModel.repository.createOrUpdateFile(owner, repoName, filePath.trim(), req)) {
                                        is ApiResult.Success -> {
                                            viewModel.postMessage("Changes committed successfully")
                                            onBack()
                                        }
                                        is ApiResult.Error -> {
                                            viewModel.postMessage("Commit error: ${res.message}")
                                        }
                                        is ApiResult.Loading -> {}
                                    }
                                    isSaving = false
                                }
                            }
                        },
                        enabled = filePath.isNotBlank() && !isSaving && !isLoadingContent
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Commit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (isLoadingContent) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it },
                    label = { Text("File path (e.g. src/app.kt)") },
                    placeholder = { Text("folder/filename.ext") },
                    singleLine = true,
                    enabled = !isEditing, // path cannot be changed if editing existing file
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text("Commit message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "File Content",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Write code or text here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp),
                    textStyle = MonospaceCodeStyle
                )
            }
        }
    }
}
