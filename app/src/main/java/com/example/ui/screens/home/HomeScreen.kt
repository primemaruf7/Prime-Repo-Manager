package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GitHubUser
import com.example.ui.components.*
import com.example.ui.viewmodel.PrimeRepoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PrimeRepoViewModel,
    onNavigateToCreateRepo: () -> Unit,
    onNavigateToGists: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val repos by viewModel.userRepos.collectAsState()
    val rateLimit by viewModel.rateLimit.collectAsState()

    val totalStars = remember(repos) { repos.sumOf { it.stargazers_count } }
    val totalForks = remember(repos) { repos.sumOf { it.forks_count } }
    val openIssues = remember(repos) { repos.sumOf { it.open_issues_count } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrimeRepoLogo(size = 28.dp)
                        Text(
                            text = "Prime Repo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OfflineNoticeBanner(isOffline = isOffline)
            }

            // User Profile Header Card
            item {
                if (currentUser != null) {
                    UserProfileHeaderCard(user = currentUser!!)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Outlined.AddBox,
                        label = "New Repo",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCreateRepo
                    )
                    QuickActionButton(
                        icon = Icons.Outlined.Code,
                        label = "New Gist",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToGists
                    )
                    QuickActionButton(
                        icon = Icons.Outlined.Search,
                        label = "Search",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSearch
                    )
                    QuickActionButton(
                        icon = Icons.Outlined.Notifications,
                        label = "Alerts",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToNotifications
                    )
                }
            }

            // Statistics Grid
            item {
                Text(
                    text = "Overview Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Repositories",
                            value = "${repos.size}",
                            icon = Icons.Outlined.Folder,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Stars Received",
                            value = "$totalStars",
                            icon = Icons.Outlined.StarOutline,
                            color = Color(0xFFD29922),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Forks",
                            value = "$totalForks",
                            icon = Icons.Outlined.ForkRight,
                            color = Color(0xFFA371F7),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Open Issues",
                            value = "$openIssues",
                            icon = Icons.Outlined.Adjust,
                            color = Color(0xFF39D353),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Contribution Heatmap
            item {
                ContributionHeatmap(
                    totalCommitsEstimate = 120 + repos.size * 8,
                    userSeed = currentUser?.login ?: "seed"
                )
            }

            // Rate Limit
            item {
                RateLimitCard(info = rateLimit)
            }
        }
    }
}

@Composable
fun UserProfileHeaderCard(user: GitHubUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AsyncImage(
                    model = user.avatar_url,
                    contentDescription = "Avatar of ${user.login}",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name ?: user.login,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user.login}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!user.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                UserMiniMetric(label = "Followers", count = user.followers)
                UserMiniMetric(label = "Following", count = user.following)
                UserMiniMetric(label = "Public Repos", count = user.public_repos)
                if (user.total_private_repos > 0) {
                    UserMiniMetric(label = "Private Repos", count = user.total_private_repos)
                }
            }
        }
    }
}

@Composable
fun UserMiniMetric(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
