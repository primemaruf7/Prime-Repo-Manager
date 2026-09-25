package com.example.ui.screens.repo

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentItem
import com.example.data.model.Repository
import com.example.data.model.UpdateRepoRequest
import com.example.data.repository.ApiResult
import com.example.ui.components.*
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.Base64Utils
import com.example.utils.DateUtils
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    owner: String,
    repoName: String,
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit,
    onNavigateToCodeBrowser: (owner: String, repo: String) -> Unit,
    onNavigateToBranches: (owner: String, repo: String) -> Unit,
    onNavigateToCommits: (owner: String, repo: String) -> Unit,
    onNavigateToIssues: (owner: String, repo: String) -> Unit,
    onNavigateToPullRequests: (owner: String, repo: String) -> Unit,
    onNavigateToReleases: (owner: String, repo: String) -> Unit,
    onNavigateToCollaborators: (owner: String, repo: String) -> Unit,
    onNavigateToWebhooks: (owner: String, repo: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var repo by remember { mutableStateOf<Repository?>(null) }
    var languages by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var readmeItem by remember { mutableStateOf<ContentItem?>(null) }
    var isStarred by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    var showCloneSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Settings

    // Edit fields for settings
    var editDescription by remember { mutableStateOf("") }
    var isArchived by remember { mutableStateOf(false) }
    var isUpdatingSettings by remember { mutableStateOf(false) }

    LaunchedEffect(owner, repoName) {
        isLoading = true
        when (val res = viewModel.repository.getRepo(owner, repoName)) {
            is ApiResult.Success -> {
                repo = res.data
                editDescription = res.data.description ?: ""
                isArchived = res.data.archived
            }
            is ApiResult.Error -> viewModel.postMessage(res.message)
            is ApiResult.Loading -> {}
        }

        // Check star
        isStarred = viewModel.repository.checkStar(owner, repoName)

        // Load languages
        when (val langRes = viewModel.repository.getRepoLanguages(owner, repoName)) {
            is ApiResult.Success -> languages = langRes.data
            else -> {}
        }

        // Load README
        when (val readmeRes = viewModel.repository.getReadme(owner, repoName)) {
            is ApiResult.Success -> readmeItem = readmeRes.data
            else -> {}
        }

        isLoading = false
    }

    val readmeMarkdown = remember(readmeItem) {
        val encoded = readmeItem?.content
        if (!encoded.isNullOrBlank()) {
            Base64Utils.decodeUtf8(encoded)
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = repoName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = owner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            HapticUtils.performConfirm(context)
                            when (val res = viewModel.repository.toggleStar(owner, repoName, isStarred)) {
                                is ApiResult.Success -> {
                                    isStarred = res.data
                                    viewModel.postMessage(if (isStarred) "Starred $repoName" else "Unstarred $repoName")
                                }
                                is ApiResult.Error -> viewModel.postMessage(res.message)
                                is ApiResult.Loading -> {}
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isStarred) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Star repository",
                            tint = if (isStarred) Color(0xFFD29922) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { showCloneSheet = true }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Clone options")
                    }

                    IconButton(onClick = {
                        repo?.html_url?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }) {
                        Icon(Icons.Outlined.OpenInBrowser, contentDescription = "Open in browser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (repo == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Repository details could not be loaded.")
            }
        } else {
            val currentRepo = repo!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview & Code") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Settings & Admin") }
                    )
                }

                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header description & stats
                        if (!currentRepo.description.isNullOrBlank()) {
                            Text(
                                text = currentRepo.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Meta details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = Color(0xFFD29922), modifier = Modifier.size(16.dp))
                                Text("${currentRepo.stargazers_count} stars", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.ForkRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("${currentRepo.forks_count} forks", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.Adjust, contentDescription = null, tint = Color(0xFF39D353), modifier = Modifier.size(16.dp))
                                Text("${currentRepo.open_issues_count} open issues", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Navigation Grid to Submodules
                        Text("Repository Navigation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubmoduleButton(
                                    icon = Icons.Outlined.Folder,
                                    title = "Browse Code",
                                    subtitle = "Files & folders",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToCodeBrowser(owner, repoName) }
                                )
                                SubmoduleButton(
                                    icon = Icons.Outlined.AltRoute,
                                    title = "Branches",
                                    subtitle = "Default: ${currentRepo.default_branch}",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToBranches(owner, repoName) }
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubmoduleButton(
                                    icon = Icons.Outlined.Commit,
                                    title = "Commits",
                                    subtitle = "History & diffs",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToCommits(owner, repoName) }
                                )
                                SubmoduleButton(
                                    icon = Icons.Outlined.Adjust,
                                    title = "Issues",
                                    subtitle = "${currentRepo.open_issues_count} open",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToIssues(owner, repoName) }
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubmoduleButton(
                                    icon = Icons.Outlined.CallMerge,
                                    title = "Pull Requests",
                                    subtitle = "Review & merge",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToPullRequests(owner, repoName) }
                                )
                                SubmoduleButton(
                                    icon = Icons.Outlined.Tag,
                                    title = "Releases",
                                    subtitle = "Tags & assets",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToReleases(owner, repoName) }
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubmoduleButton(
                                    icon = Icons.Outlined.People,
                                    title = "Collaborators",
                                    subtitle = "Access & permissions",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToCollaborators(owner, repoName) }
                                )
                                SubmoduleButton(
                                    icon = Icons.Outlined.Webhook,
                                    title = "Webhooks",
                                    subtitle = "Integrations",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToWebhooks(owner, repoName) }
                                )
                            }
                        }

                        // Language Breakdown
                        if (languages.isNotEmpty()) {
                            LanguageBreakdownBar(languages = languages)
                        }

                        // README native viewer
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("README.md", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }

                                HorizontalDivider(thickness = 0.5.dp)

                                if (readmeMarkdown != null) {
                                    MarkdownViewer(markdown = readmeMarkdown)
                                } else {
                                    Text(
                                        text = "No README file found in this repository.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Settings Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("General Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Archive this repository", fontWeight = FontWeight.SemiBold)
                                Text("Mark this repository as read-only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = isArchived, onCheckedChange = { isArchived = it })
                        }

                        Button(
                            onClick = {
                                isUpdatingSettings = true
                                coroutineScope.launch {
                                    HapticUtils.performConfirm(context)
                                    val req = UpdateRepoRequest(
                                        description = editDescription,
                                        archived = isArchived
                                    )
                                    when (val res = viewModel.repository.updateRepo(owner, repoName, req)) {
                                        is ApiResult.Success -> {
                                            repo = res.data
                                            viewModel.postMessage("Repository settings updated")
                                        }
                                        is ApiResult.Error -> viewModel.postMessage(res.message)
                                        is ApiResult.Loading -> {}
                                    }
                                    isUpdatingSettings = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUpdatingSettings
                        ) {
                            Text("Save Changes")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Danger Zone
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "Danger Zone",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Once you delete a repository, there is no going back. Please be certain.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { showDeleteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete this repository")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCloneSheet && repo != null) {
        CloneRepositorySheet(
            cloneUrlHttps = repo!!.clone_url,
            cloneUrlSsh = repo!!.ssh_url,
            onDismiss = { showCloneSheet = false },
            onCopied = { msg -> viewModel.postMessage(msg) }
        )
    }

    if (showDeleteDialog) {
        ConfirmDeleteRepoDialog(
            expectedFullName = "$owner/$repoName",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteRepository(owner, repoName, onSuccess = onBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
fun SubmoduleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}
