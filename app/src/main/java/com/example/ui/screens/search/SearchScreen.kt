package com.example.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CodeSearchItem
import com.example.data.model.GitHubUser
import com.example.data.model.Issue
import com.example.data.model.Repository
import com.example.data.repository.ApiResult
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RateLimitCard
import com.example.ui.components.RepositoryCard
import com.example.ui.viewmodel.PrimeRepoViewModel
import kotlinx.coroutines.launch

enum class SearchTab(val label: String) {
    REPOS("Repositories"),
    USERS("Users"),
    CODE("Code"),
    ISSUES("Issues")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: PrimeRepoViewModel,
    onRepoClick: (owner: String, repo: String) -> Unit,
    onUserClick: (username: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val rateLimit by viewModel.rateLimit.collectAsState()

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedTab by remember {
        mutableStateOf(SearchTab.REPOS)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var repoResults by remember {
        mutableStateOf<List<Repository>>(emptyList())
    }

    var userResults by remember {
        mutableStateOf<List<GitHubUser>>(emptyList())
    }

    var codeResults by remember {
        mutableStateOf<List<CodeSearchItem>>(emptyList())
    }

    var issueResults by remember {
        mutableStateOf<List<Issue>>(emptyList())
    }

    fun executeSearch() {
        val query = searchQuery.trim()

        if (query.isBlank() || isLoading) return

        isLoading = true

        coroutineScope.launch {
            when (selectedTab) {

                SearchTab.REPOS -> {
                    when (
                        val result = viewModel.repository
                            .searchRepositories(query)
                    ) {
                        is ApiResult.Success -> {
                            repoResults = result.data.items
                        }

                        is ApiResult.Error -> {
                            viewModel.postMessage(result.message)
                        }

                        is ApiResult.Loading -> Unit
                    }
                }

                SearchTab.USERS -> {
                    when (
                        val result = viewModel.repository
                            .searchUsers(query)
                    ) {
                        is ApiResult.Success -> {
                            userResults = result.data.items
                        }

                        is ApiResult.Error -> {
                            viewModel.postMessage(result.message)
                        }

                        is ApiResult.Loading -> Unit
                    }
                }

                SearchTab.CODE -> {
                    when (
                        val result = viewModel.repository
                            .searchCode(query)
                    ) {
                        is ApiResult.Success -> {
                            codeResults = result.data.items
                        }

                        is ApiResult.Error -> {
                            viewModel.postMessage(result.message)
                        }

                        is ApiResult.Loading -> Unit
                    }
                }

                SearchTab.ISSUES -> {
                    when (
                        val result = viewModel.repository
                            .searchIssues(query)
                    ) {
                        is ApiResult.Success -> {
                            issueResults = result.data.items
                        }

                        is ApiResult.Error -> {
                            viewModel.postMessage(result.message)
                        }

                        is ApiResult.Loading -> Unit
                    }
                }
            }

            isLoading = false
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Global Search",
                        fontWeight = FontWeight.Bold
                    )
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    scrolledContainerColor = backgroundColor
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            OutlinedTextField(
                value = searchQuery,

                onValueChange = {
                    searchQuery = it
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),

                placeholder = {
                    Text(
                        text = "Search GitHub ${selectedTab.label.lowercase()}..."
                    )
                },

                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },

                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                executeSearch()
                            },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowForward,
                                contentDescription = "Run search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },

                singleLine = true,

                shape = RoundedCornerShape(14.dp)
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = backgroundColor,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                SearchTab.entries.forEach { tab ->

                    Tab(
                        selected = selectedTab == tab,

                        onClick = {
                            if (selectedTab != tab) {
                                selectedTab = tab

                                if (searchQuery.isNotBlank()) {
                                    executeSearch()
                                }
                            }
                        },

                        text = {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                when {

                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    searchQuery.isBlank() -> {

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),

                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 100.dp
                            ),

                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item(
                                key = "rate_limit",
                                contentType = "rate_limit"
                            ) {
                                RateLimitCard(
                                    info = rateLimit
                                )
                            }

                            item(
                                key = "search_empty",
                                contentType = "empty_state"
                            ) {
                                EmptyStateView(
                                    icon = Icons.Outlined.Search,
                                    title = "Search GitHub",
                                    description =
                                        "Search across public & private repositories, " +
                                        "developers, source code, and issues worldwide."
                                )
                            }
                        }
                    }

                    else -> {

                        when (selectedTab) {

                            SearchTab.REPOS -> {

                                if (repoResults.isEmpty()) {
                                    EmptyStateView(
                                        icon = Icons.Outlined.SearchOff,
                                        title = "No Repositories",
                                        description =
                                            "No repositories found for \"$searchQuery\"."
                                    )
                                } else {

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),

                                        contentPadding = PaddingValues(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = 12.dp,
                                            bottom = 100.dp
                                        ),

                                        verticalArrangement =
                                            Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = repoResults,
                                            key = { it.id },
                                            contentType = { "repository" }
                                        ) { repo ->

                                            RepositoryCard(
                                                repo = repo,

                                                onClick = {
                                                    val owner =
                                                        repo.owner?.login ?: "user"

                                                    onRepoClick(
                                                        owner,
                                                        repo.name
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            SearchTab.USERS -> {

                                if (userResults.isEmpty()) {
                                    EmptyStateView(
                                        icon = Icons.Outlined.PersonOff,
                                        title = "No Users",
                                        description =
                                            "No users found for \"$searchQuery\"."
                                    )
                                } else {

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),

                                        contentPadding = PaddingValues(
                                            bottom = 100.dp
                                        )
                                    ) {
                                        items(
                                            items = userResults,
                                            key = { it.id },
                                            contentType = { "user" }
                                        ) { user ->

                                            ListItem(
                                                headlineContent = {
                                                    Text(
                                                        text = user.login,
                                                        fontWeight =
                                                            FontWeight.SemiBold
                                                    )
                                                },

                                                leadingContent = {
                                                    AsyncImage(
                                                        model = user.avatar_url,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(CircleShape)
                                                    )
                                                },

                                                trailingContent = {
                                                    Icon(
                                                        imageVector =
                                                            Icons.Outlined.ChevronRight,
                                                        contentDescription = null
                                                    )
                                                },

                                                modifier = Modifier.clickable {
                                                    onUserClick(
                                                        user.login
                                                    )
                                                }
                                            )

                                            HorizontalDivider(
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                            }

                            SearchTab.CODE -> {

                                if (codeResults.isEmpty()) {
                                    EmptyStateView(
                                        icon = Icons.Outlined.CodeOff,
                                        title = "No Code Results",
                                        description =
                                            "No matching code found for \"$searchQuery\"."
                                    )
                                } else {

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),

                                        contentPadding = PaddingValues(
                                            bottom = 100.dp
                                        )
                                    ) {
                                        items(
                                            items = codeResults,
                                            key = {
                                                "${it.sha}:${it.path}"
                                            },
                                            contentType = { "code" }
                                        ) { item ->

                                            ListItem(
                                                headlineContent = {
                                                    Text(
                                                        text = item.name,
                                                        fontWeight =
                                                            FontWeight.SemiBold
                                                    )
                                                },

                                                supportingContent = {
                                                    Text(
                                                        text =
                                                            "${item.repository.full_name} • ${item.path}",
                                                        style =
                                                            MaterialTheme
                                                                .typography
                                                                .bodySmall
                                                    )
                                                },

                                                leadingContent = {
                                                    Icon(
                                                        imageVector =
                                                            Icons.Outlined.Code,
                                                        contentDescription = null,
                                                        tint =
                                                            MaterialTheme
                                                                .colorScheme
                                                                .primary
                                                    )
                                                },

                                                modifier = Modifier.clickable {

                                                    val parts =
                                                        item.repository
                                                            .full_name
                                                            .split("/", limit = 2)

                                                    if (parts.size == 2) {
                                                        onRepoClick(
                                                            parts[0],
                                                            parts[1]
                                                        )
                                                    }
                                                }
                                            )

                                            HorizontalDivider(
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                            }

                            SearchTab.ISSUES -> {

                                if (issueResults.isEmpty()) {
                                    EmptyStateView(
                                        icon = Icons.Outlined.HelpOutline,
                                        title = "No Issues",
                                        description =
                                            "No issues found matching \"$searchQuery\"."
                                    )
                                } else {

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),

                                        contentPadding = PaddingValues(
                                            bottom = 100.dp
                                        )
                                    ) {
                                        items(
                                            items = issueResults,
                                            key = { it.id },
                                            contentType = { "issue" }
                                        ) { issue ->

                                            ListItem(
                                                headlineContent = {
                                                    Text(
                                                        text = issue.title,
                                                        fontWeight =
                                                            FontWeight.SemiBold
                                                    )
                                                },

                                                supportingContent = {
                                                    Text(
                                                        text =
                                                            "#${issue.number} by ${issue.user?.login ?: "user"}",
                                                        style =
                                                            MaterialTheme
                                                                .typography
                                                                .bodySmall
                                                    )
                                                },

                                                leadingContent = {
                                                    Icon(
                                                        imageVector =
                                                            Icons.Outlined.Adjust,
                                                        contentDescription = null,
                                                        tint = Color(0xFF39D353)
                                                    )
                                                }
                                            )

                                            HorizontalDivider(
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}