package com.example.ui.screens.commits

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommitItem
import com.example.data.repository.ApiResult
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitsScreen(
    owner: String,
    repoName: String,
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var commits by remember { mutableStateOf<List<CommitItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCommitDetail by remember { mutableStateOf<CommitItem?>(null) }
    var isLoadingDetail by remember { mutableStateOf(false) }

    fun loadCommits() {
        isLoading = true
        coroutineScope.launch {
            when (val res = viewModel.repository.getCommits(owner, repoName)) {
                is ApiResult.Success -> commits = res.data
                is ApiResult.Error -> viewModel.postMessage(res.message)
                is ApiResult.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadCommits()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commits", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadCommits() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
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
            } else if (commits.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Commit,
                    title = "No Commits Found",
                    description = "No commits found in this repository branch."
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(commits, key = { it.sha }) { commitItem ->
                        CommitListItem(
                            commit = commitItem,
                            onClick = {
                                coroutineScope.launch {
                                    isLoadingDetail = true
                                    when (val res = viewModel.repository.getCommitDetail(owner, repoName, commitItem.sha)) {
                                        is ApiResult.Success -> selectedCommitDetail = res.data
                                        is ApiResult.Error -> viewModel.postMessage(res.message)
                                        is ApiResult.Loading -> {}
                                    }
                                    isLoadingDetail = false
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // Commit Detail Dialog
    if (selectedCommitDetail != null) {
        val detail = selectedCommitDetail!!
        AlertDialog(
            onDismissRequest = { selectedCommitDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Commit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Commit ${detail.sha.take(7)}", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = detail.commit.message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "By ${detail.commit.author.name} • ${DateUtils.formatRelativeTime(detail.commit.author.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (detail.stats != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("+${detail.stats.additions}", color = Color(0xFF238636), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("-${detail.stats.deletions}", color = Color(0xFFF85149), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${detail.stats.total} total changes", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (!detail.files.isNullOrEmpty()) {
                        Text("Changed Files (${detail.files.size}):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        detail.files.take(6).forEach { file ->
                            Text(
                                text = "• ${file.filename} (+${file.additions} -${file.deletions})",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCommitDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CommitListItem(
    commit: CommitItem,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = commit.commit.message.lineSequence().firstOrNull() ?: "",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = commit.author?.login ?: commit.commit.author.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = DateUtils.formatRelativeTime(commit.commit.author.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Commit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = commit.sha.take(7),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
