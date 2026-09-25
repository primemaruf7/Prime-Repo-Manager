package com.example.ui.screens.repo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.RepoCardSkeleton
import com.example.ui.components.RepositoryCard
import com.example.ui.viewmodel.PrimeRepoViewModel

enum class RepoFilter(val label: String) {
    ALL("All"),
    PUBLIC("Public"),
    PRIVATE("Private"),
    FORKS("Forks"),
    ARCHIVED("Archived")
}

enum class RepoSort(
    val label: String,
    val apiValue: String
) {
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

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedFilter by remember {
        mutableStateOf(RepoFilter.ALL)
    }

    var selectedSort by remember {
        mutableStateOf(RepoSort.UPDATED)
    }

    var showSortMenu by remember {
        mutableStateOf(false)
    }

    val filteredRepos = remember(
        repos,
        searchQuery,
        selectedFilter,
        selectedSort
    ) {
        val query = searchQuery.trim()

        val filtered = repos.filter { repo ->
            val matchQuery = query.isBlank() ||
                repo.name.contains(
                    query,
                    ignoreCase = true
                ) ||
                repo.description?.contains(
                    query,
                    ignoreCase = true
                ) == true

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
            RepoSort.UPDATED -> {
                filtered.sortedByDescending {
                    it.updated_at
                }
            }

            RepoSort.NAME -> {
                filtered.sortedBy {
                    it.name.lowercase()
                }
            }

            RepoSort.STARS -> {
                filtered.sortedByDescending {
                    it.stargazers_count
                }
            }
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Repositories (${repos.size})",
                        fontWeight = FontWeight.Bold
                    )
                },

                actions = {
                    androidx.compose.foundation.layout.Box {
                        IconButton(
                            onClick = {
                                showSortMenu = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sort,
                                contentDescription = "Sort repositories"
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = {
                                showSortMenu = false
                            }
                        ) {
                            RepoSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Sort by ${sort.label}"
                                        )
                                    },
                                    onClick = {
                                        selectedSort = sort
                                        showSortMenu = false

                                        viewModel.loadRepositories(
                                            sort = sort.apiValue
                                        )
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.loadRepositories()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    scrolledContainerColor = backgroundColor
                )
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRepoClick,
                modifier = Modifier.size(58.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Repository"
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            OfflineNoticeBanner(
                isOffline = isOffline
            )

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
                        bottom = 6.dp
                    ),

                placeholder = {
                    Text("Find a repository...")
                },

                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },

                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },

                singleLine = true,

                shape = RoundedCornerShape(14.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 8.dp
                    ),

                horizontalArrangement = Arrangement.spacedBy(10.dp),

                contentPadding = PaddingValues(
                    horizontal = 2.dp
                )
            ) {
                items(
                    items = RepoFilter.entries,
                    key = { it.name },
                    contentType = { "filter" }
                ) { filter ->

                    FilterChip(
                        selected = selectedFilter == filter,

                        onClick = {
                            selectedFilter = filter
                        },

                        label = {
                            Text(filter.label)
                        },

                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            when {

                isLoading && repos.isEmpty() -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),

                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 100.dp
                        ),

                        verticalArrangement = Arrangement.spacedBy(
                            10.dp
                        )
                    ) {
                        items(
                            count = 5,
                            key = { it },
                            contentType = { "skeleton" }
                        ) {
                            RepoCardSkeleton()
                        }
                    }
                }

                filteredRepos.isEmpty() -> {

                    EmptyStateView(
                        icon = Icons.Outlined.FolderOff,

                        title = if (searchQuery.isNotBlank()) {
                            "No repositories found"
                        } else {
                            "No repositories yet"
                        },

                        description = if (searchQuery.isNotBlank()) {
                            "No matching repositories for \"$searchQuery\"."
                        } else {
                            "Create your first repository using the + button."
                        },

                        actionButtonText = if (searchQuery.isBlank()) {
                            "Create Repository"
                        } else {
                            null
                        },

                        onActionClick = onCreateRepoClick
                    )
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),

                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 100.dp
                        ),

                        verticalArrangement = Arrangement.spacedBy(
                            10.dp
                        )
                    ) {
                        items(
                            items = filteredRepos,
                            key = { repo -> repo.id },
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
        }
    }
}