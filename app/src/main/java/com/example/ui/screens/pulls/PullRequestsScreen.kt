package com.example.ui.screens.pulls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.data.model.MergePullRequestRequest
import com.example.data.model.PullRequest
import com.example.data.repository.ApiResult
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.DateUtils
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRequestsScreen(
    owner: String,
    repoName: String,
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var prs by remember { mutableStateOf<List<PullRequest>>(emptyList()) }
    var selectedState by remember { mutableStateOf("open") }
    var isLoading by remember { mutableStateOf(true) }

    var selectedPr by remember { mutableStateOf<PullRequest?>(null) }
    var prDetail by remember { mutableStateOf<PullRequest?>(null) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeMethod by remember { mutableStateOf("merge") } // "merge", "squash", "rebase"
    var isMerging by remember { mutableStateOf(false) }

    fun loadPRs(state: String) {
        isLoading = true
        coroutineScope.launch {
            when (val res = viewModel.repository.getPullRequests(owner, repoName, state = state)) {
                is ApiResult.Success -> prs = res.data
                is ApiResult.Error -> viewModel.postMessage(res.message)
                is ApiResult.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedState) {
        loadPRs(selectedState)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pull Requests", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadPRs(selectedState) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
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
            TabRow(selectedTabIndex = if (selectedState == "open") 0 else 1) {
                Tab(
                    selected = selectedState == "open",
                    onClick = { selectedState = "open" },
                    text = { Text("Open PRs") }
                )
                Tab(
                    selected = selectedState == "closed",
                    onClick = { selectedState = "closed" },
                    text = { Text("Closed PRs") }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (prs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.CallMerge,
                    title = if (selectedState == "open") "No open pull requests" else "No closed pull requests",
                    description = "There are no pull requests in this repository state."
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(prs, key = { it.id }) { pr ->
                        ListItem(
                            headlineContent = {
                                Text(pr.title, fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    "#${pr.number} by ${pr.user?.login ?: "user"} • ${DateUtils.formatRelativeTime(pr.created_at)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.CallMerge,
                                    contentDescription = null,
                                    tint = if (pr.merged_at != null) Color(0xFFA371F7) else if (pr.state == "open") Color(0xFF39D353) else Color(0xFFF85149)
                                )
                            },
                            trailingContent = {
                                Text(
                                    "${pr.head.ref} → ${pr.base.ref}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable {
                                selectedPr = pr
                                coroutineScope.launch {
                                    when (val res = viewModel.repository.getPullRequestDetail(owner, repoName, pr.number)) {
                                        is ApiResult.Success -> prDetail = res.data
                                        else -> prDetail = pr
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

    // View PR Detail Dialog
    if (selectedPr != null) {
        val currentPr = prDetail ?: selectedPr!!
        AlertDialog(
            onDismissRequest = {
                selectedPr = null
                prDetail = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.CallMerge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("#${currentPr.number} ${currentPr.title}", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (currentPr.state == "open") Color(0xFF238636) else Color(0xFF8250DF)
                        ) {
                            Text(
                                text = currentPr.state.capitalize(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("${currentPr.head.ref} into ${currentPr.base.ref}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }

                    if (currentPr.mergeable != null) {
                        Text(
                            text = if (currentPr.mergeable == true) "✓ Mergeable without conflicts" else "⚠ Conflicts detected",
                            color = if (currentPr.mergeable == true) Color(0xFF238636) else Color(0xFFF85149),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("+${currentPr.additions}", color = Color(0xFF238636), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("-${currentPr.deletions}", color = Color(0xFFF85149), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${currentPr.changed_files} changed files", style = MaterialTheme.typography.bodySmall)
                    }

                    if (!currentPr.body.isNullOrBlank()) {
                        Text(currentPr.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                if (currentPr.state == "open") {
                    Button(
                        onClick = { showMergeDialog = true },
                        enabled = currentPr.mergeable != false
                    ) {
                        Text("Merge Pull Request")
                    }
                } else {
                    TextButton(onClick = { selectedPr = null; prDetail = null }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPr = null; prDetail = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Merge Confirmation Dialog
    if (showMergeDialog && selectedPr != null) {
        val prToMerge = selectedPr!!
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Merge Pull Request #${prToMerge.number}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select merge method:")
                    listOf("merge" to "Create a merge commit", "squash" to "Squash and merge", "rebase" to "Rebase and merge").forEach { (method, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mergeMethod = method }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = mergeMethod == method, onClick = { mergeMethod = method })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isMerging = true
                        coroutineScope.launch {
                            HapticUtils.performConfirm(context)
                            val req = MergePullRequestRequest(merge_method = mergeMethod)
                            when (val res = viewModel.repository.mergePullRequest(owner, repoName, prToMerge.number, req)) {
                                is ApiResult.Success -> {
                                    viewModel.postMessage("Pull request #${prToMerge.number} merged successfully")
                                    showMergeDialog = false
                                    selectedPr = null
                                    prDetail = null
                                    loadPRs("open")
                                }
                                is ApiResult.Error -> viewModel.postMessage("Merge failed: ${res.message}")
                                is ApiResult.Loading -> {}
                            }
                            isMerging = false
                        }
                    },
                    enabled = !isMerging
                ) {
                    Text("Confirm Merge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
