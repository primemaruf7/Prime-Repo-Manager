package com.example.ui.screens.issues

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
import androidx.compose.ui.unit.sp
import com.example.data.model.CreateIssueRequest
import com.example.data.model.Issue
import com.example.data.model.UpdateIssueRequest
import com.example.data.repository.ApiResult
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.DateUtils
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuesScreen(
    owner: String,
    repoName: String,
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var selectedState by remember { mutableStateOf("open") } // "open" or "closed"
    var isLoading by remember { mutableStateOf(true) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newIssueTitle by remember { mutableStateOf("") }
    var newIssueBody by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    var selectedIssue by remember { mutableStateOf<Issue?>(null) }

    fun loadIssues(state: String) {
        isLoading = true
        coroutineScope.launch {
            when (val res = viewModel.repository.getIssues(owner, repoName, state = state)) {
                is ApiResult.Success -> {
                    // Filter out pull requests (as GitHub Issues API returns both)
                    issues = res.data.filter { !it.isPullRequest }
                }
                is ApiResult.Error -> viewModel.postMessage(res.message)
                is ApiResult.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedState) {
        loadIssues(selectedState)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issues", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadIssues(selectedState) }) {
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
                Icon(Icons.Filled.Add, contentDescription = "New Issue")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = if (selectedState == "open") 0 else 1) {
                Tab(
                    selected = selectedState == "open",
                    onClick = { selectedState = "open" },
                    text = { Text("Open Issues") }
                )
                Tab(
                    selected = selectedState == "closed",
                    onClick = { selectedState = "closed" },
                    text = { Text("Closed Issues") }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (issues.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.CheckCircle,
                    title = if (selectedState == "open") "No open issues" else "No closed issues",
                    description = if (selectedState == "open") "Everything is clean! Create a new issue using +." else "No resolved issues found.",
                    actionButtonText = if (selectedState == "open") "New Issue" else null,
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(issues, key = { it.id }) { issue ->
                        ListItem(
                            headlineContent = {
                                Text(issue.title, fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    "#${issue.number} opened by ${issue.user?.login ?: "user"} • ${DateUtils.formatRelativeTime(issue.created_at)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (issue.state == "open") Icons.Outlined.Adjust else Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = if (issue.state == "open") Color(0xFF39D353) else Color(0xFFA371F7)
                                )
                            },
                            trailingContent = {
                                if (issue.comments > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("${issue.comments}", fontSize = 12.sp)
                                    }
                                }
                            },
                            modifier = Modifier.clickable { selectedIssue = issue }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // View Issue Dialog
    if (selectedIssue != null) {
        val iss = selectedIssue!!
        AlertDialog(
            onDismissRequest = { selectedIssue = null },
            title = {
                Text("#${iss.number} ${iss.title}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (iss.state == "open") Color(0xFF238636) else Color(0xFF8250DF)
                        ) {
                            Text(
                                text = iss.state.capitalize(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("by ${iss.user?.login ?: "user"}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!iss.body.isNullOrBlank()) {
                        Text(iss.body, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("No description provided.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                val nextState = if (iss.state == "open") "closed" else "open"
                Button(
                    onClick = {
                        coroutineScope.launch {
                            HapticUtils.performConfirm(context)
                            when (val res = viewModel.repository.updateIssue(owner, repoName, iss.number, UpdateIssueRequest(state = nextState))) {
                                is ApiResult.Success -> {
                                    viewModel.postMessage("Issue #${iss.number} $nextState")
                                    selectedIssue = null
                                    loadIssues(selectedState)
                                }
                                is ApiResult.Error -> viewModel.postMessage("Failed: ${res.message}")
                                is ApiResult.Loading -> {}
                            }
                        }
                    }
                ) {
                    Text(if (iss.state == "open") "Close Issue" else "Reopen Issue")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedIssue = null }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Create Issue Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Issue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newIssueTitle,
                        onValueChange = { newIssueTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newIssueBody,
                        onValueChange = { newIssueBody = it },
                        label = { Text("Description (markdown supported)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newIssueTitle.isNotBlank()) {
                            isCreating = true
                            coroutineScope.launch {
                                HapticUtils.performConfirm(context)
                                val req = CreateIssueRequest(
                                    title = newIssueTitle.trim(),
                                    body = newIssueBody.trim().ifBlank { null }
                                )
                                when (val res = viewModel.repository.createIssue(owner, repoName, req)) {
                                    is ApiResult.Success -> {
                                        viewModel.postMessage("Issue #${res.data.number} created")
                                        showCreateDialog = false
                                        newIssueTitle = ""
                                        newIssueBody = ""
                                        loadIssues("open")
                                    }
                                    is ApiResult.Error -> viewModel.postMessage("Error: ${res.message}")
                                    is ApiResult.Loading -> {}
                                }
                                isCreating = false
                            }
                        }
                    },
                    enabled = newIssueTitle.isNotBlank() && !isCreating
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
