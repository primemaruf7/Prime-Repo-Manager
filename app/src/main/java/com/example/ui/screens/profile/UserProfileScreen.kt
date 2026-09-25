package com.example.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.GitHubUser
import com.example.data.model.Repository
import com.example.data.repository.ApiResult
import com.example.ui.components.RepositoryCard
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    username: String,
    viewModel: PrimeRepoViewModel,
    onBack: (() -> Unit)? = null,
    onEditProfileClick: (() -> Unit)? = null,
    onRepoClick: (owner: String, repo: String) -> Unit,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val loggedInUser by viewModel.currentUser.collectAsState()

    var user by remember { mutableStateOf<GitHubUser?>(null) }
    var repos by remember { mutableStateOf<List<Repository>>(emptyList()) }
    var isFollowing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val isSelf = loggedInUser?.login.equals(username, ignoreCase = true) || username.isEmpty()

    LaunchedEffect(username) {
        isLoading = true
        val targetUser = if (isSelf && loggedInUser != null) loggedInUser!!.login else username

        if (targetUser.isNotBlank()) {
            when (val res = viewModel.repository.getUserProfile(targetUser)) {
                is ApiResult.Success -> {
                    user = res.data
                }
                is ApiResult.Error -> {
                    if (isSelf) user = loggedInUser
                    else viewModel.postMessage(res.message)
                }
                is ApiResult.Loading -> {}
            }

            if (!isSelf) {
                isFollowing = viewModel.repository.checkFollowing(targetUser)
            }

            when (val repoRes = viewModel.repository.getUserPublicRepos(targetUser)) {
                is ApiResult.Success -> repos = repoRes.data
                else -> {}
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelf) "My Profile" else "@$username", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelf && onEditProfileClick != null) {
                        IconButton(onClick = onEditProfileClick) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile")
                        }
                    }
                    if (isSelf && onNavigateToSettings != null) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                    IconButton(onClick = {
                        user?.html_url?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }) {
                        Icon(Icons.Outlined.OpenInBrowser, contentDescription = "Open in GitHub")
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
        } else if (user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("User not found.")
            }
        } else {
            val u = user!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Profile details card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AsyncImage(
                                    model = u.avatar_url,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = u.name ?: u.login,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "@${u.login}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (!u.bio.isNullOrBlank()) {
                                Text(text = u.bio, style = MaterialTheme.typography.bodyMedium)
                            }

                            // Meta info
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!u.company.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Outlined.Business, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(u.company, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (!u.location.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(u.location, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (!u.blog.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(u.blog, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("${u.followers} followers", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${u.following} following", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            if (!isSelf) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            HapticUtils.performConfirm(context)
                                            when (val res = viewModel.repository.toggleFollow(u.login, isFollowing)) {
                                                is ApiResult.Success -> {
                                                    isFollowing = res.data
                                                    viewModel.postMessage(if (isFollowing) "Followed @${u.login}" else "Unfollowed @${u.login}")
                                                }
                                                is ApiResult.Error -> viewModel.postMessage(res.message)
                                                is ApiResult.Loading -> {}
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = if (isFollowing) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors()
                                ) {
                                    Text(if (isFollowing) "Unfollow" else "Follow")
                                }
                            }
                        }
                    }
                }

                // Pinned Repos note (Requirement 27)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "GitHub pinned repositories are exclusive to the web GraphQL profile. Showing recent public repositories below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Public Repositories (${repos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(repos, key = { it.id }) { repo ->
                    RepositoryCard(
                        repo = repo,
                        onClick = {
                            val ownerLogin = repo.owner?.login ?: u.login
                            onRepoClick(ownerLogin, repo.name)
                        }
                    )
                }
            }
        }
    }
}
