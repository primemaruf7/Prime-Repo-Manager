package com.example.ui.screens.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Repository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.RepoCardSkeleton
import com.example.ui.components.RepositoryCard
import com.example.ui.components.getLanguageColor
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.DateUtils

enum class RepoFilter(val label: String) {
    ALL("All"),
    PUBLIC("Public"),
    PRIVATE("Private"),
    FORKS("Forks"),
    ARCHIVED("Archived")
}

enum class RepoSort(val label: String, val apiValue: String) {
    UPDATED("Updated", "updated"),
    NAME("Name", "full_name"),
    STARS("Stars", "stargazers_count")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoriesScreen(
    viewModel: PrimeRepoViewModel,
    onRepoClick: (owner: String, repo: String) -> Unit,
    onCreateRepoClick: () -> Unit
) {
    val repos by viewModel.userRepos.collectAsState()
    val isLoading by viewModel.isLoadingRepos.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RepoFilter.ALL) }
    var selectedSort by remember { mutableStateOf(RepoSort.UPDATED) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredRepos = remember(repos, searchQuery, selectedFilter, selectedSort) {
        var list = repos.filter { repo ->
            val matchQuery = searchQuery.isBlank() ||
                repo.name.contains(searchQuery, ignoreCase = true) ||
                (repo.description?.contains(searchQuery, ignoreCase = true) == true)

            val matchFilter = when (selectedFilter) {
                RepoFilter.ALL -> true
                RepoFilter.PUBLIC -> !repo.private
                RepoFilter.PRIVATE -> repo.private
                RepoFilter.FORKS -> repo.fork
                RepoFilter.ARCHIVED -> repo.archived
            }
            matchQuery && matchFilter
        }

        when (selectedSort) {
            RepoSort.UPDATED -> list.sortedByDescending { it.updated_at }
            RepoSort.NAME -> list.sortedBy { it.name.lowercase() }
            RepoSort.STARS -> list.sortedByDescending { it.stargazers_count }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repositories (${repos.size})", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = "Sort repositories")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            RepoSort.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text("Sort by ${sort.label}") },
                                    leadingIcon = {
                                        if (selectedSort == sort) {
                                            Icon(Icons.Outlined.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        selectedSort = sort
                                        showSortMenu = false
                                        viewModel.loadRepositories(sort = sort.apiValue)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.loadRepositories() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRepoClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Repository")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OfflineNoticeBanner(isOffline = isOffline)

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Find a repository...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RepoFilter.values()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) }
                    )
                }
            }

            // Repository List
            if (isLoading && repos.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(5) {
                        RepoCardSkeleton()
                    }
                }
            } else if (filteredRepos.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.FolderOff,
                    title = if (searchQuery.isNotBlank()) "No repositories found" else "No repositories yet",
                    description = if (searchQuery.isNotBlank()) "No matching repositories for \"$searchQuery\"." else "Create your first repository using the + button.",
                    actionButtonText = if (searchQuery.isBlank()) "Create Repository" else null,
                    onActionClick = onCreateRepoClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRepos, key = { it.id }) { repo ->
                        RepositoryCard(
                            repo = repo,
                            onClick = {
                                val owner = repo.owner?.login ?: "user"
                                onRepoClick(owner, repo.name)
                            }
                        )
                    }
                }
            }
        }
    }
}
