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

    val totalStars = remember(repos) {
        repos.sumOf { it.stargazers_count }
    }

    val totalForks = remember(repos) {
        repos.sumOf { it.forks_count }
    }

    val openIssues = remember(repos) {
        repos.sumOf { it.open_issues_count }
    }

    val contributionSeed = remember(currentUser?.login) {
        currentUser?.login ?: "seed"
    }

    val estimatedContributions = remember(repos.size) {
        120 + repos.size * 8
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,

        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimeRepoLogo(
                            size = 32.dp
                        )

                        Text(
                            text = "Prime Repo",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },

                actions = {
                    IconButton(
                        onClick = onNavigateToNotifications
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.Notifications,
                            contentDescription =
                                "Notifications",
                            modifier = Modifier.size(27.dp),
                            tint =
                                MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.Settings,
                            contentDescription =
                                "Settings",
                            modifier = Modifier.size(27.dp),
                            tint =
                                MaterialTheme.colorScheme.onBackground
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    scrolledContainerColor = backgroundColor
                )
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),

            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            item(
                key = "offline_banner",
                contentType = "offline_banner"
            ) {
                OfflineNoticeBanner(
                    isOffline = isOffline
                )
            }

            item(
                key = "profile_header",
                contentType = "profile_header"
            ) {
                if (currentUser != null) {

                    UserProfileHeaderCard(
                        user = currentUser!!
                    )

                } else {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surface
                            ),
                        border =
                            CardDefaults.outlinedCardBorder()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            item(
                key = "quick_actions",
                contentType = "quick_actions"
            ) {
                Text(
                    text = "Quick Actions",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold,
                    modifier =
                        Modifier.padding(top = 2.dp)
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Outlined.AddBox,
                        label = "New Repo",
                        modifier =
                            Modifier.weight(1f),
                        onClick =
                            onNavigateToCreateRepo
                    )

                    QuickActionButton(
                        icon = Icons.Outlined.Code,
                        label = "New Gist",
                        modifier =
                            Modifier.weight(1f),
                        onClick =
                            onNavigateToGists
                    )

                    QuickActionButton(
                        icon = Icons.Outlined.Search,
                        label = "Search",
                        modifier =
                            Modifier.weight(1f),
                        onClick =
                            onNavigateToSearch
                    )

                    QuickActionButton(
                        icon =
                            Icons.Outlined.Notifications,
                        label = "Alerts",
                        modifier =
                            Modifier.weight(1f),
                        onClick =
                            onNavigateToNotifications
                    )
                }
            }

            item(
                key = "overview_statistics",
                contentType = "overview_statistics"
            ) {
                Text(
                    text = "Overview Statistics",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold,
                    modifier =
                        Modifier.padding(top = 2.dp)
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Repositories",
                            value =
                                repos.size.toString(),
                            icon =
                                Icons.Outlined.Folder,
                            color =
                                Color(0xFF58A6FF),
                            modifier =
                                Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Stars Received",
                            value =
                                totalStars.toString(),
                            icon =
                                Icons.Outlined.StarOutline,
                            color =
                                Color(0xFFD29922),
                            modifier =
                                Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Forks",
                            value =
                                totalForks.toString(),
                            icon =
                                Icons.Outlined.ForkRight,
                            color =
                                Color(0xFFA371F7),
                            modifier =
                                Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Open Issues",
                            value =
                                openIssues.toString(),
                            icon =
                                Icons.Outlined.Adjust,
                            color =
                                Color(0xFF39D353),
                            modifier =
                                Modifier.weight(1f)
                        )
                    }
                }
            }

            item(
                key = "contribution_heatmap",
                contentType = "contribution_heatmap"
            ) {
                ContributionHeatmap(
                    totalCommitsEstimate =
                        estimatedContributions,
                    userSeed =
                        contributionSeed
                )
            }

            item(
                key = "rate_limit",
                contentType = "rate_limit"
            ) {
                RateLimitCard(
                    info = rateLimit
                )
            }
        }
    }
}

@Composable
fun UserProfileHeaderCard(
    user: GitHubUser
) {
    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),

        border =
            CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                AsyncImage(
                    model = user.avatar_url,
                    contentDescription =
                        "Avatar of ${user.login}",
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary,
                            shape = CircleShape
                        ),
                    contentScale =
                        ContentScale.Crop
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            user.name ?: user.login,
                        fontSize = 20.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )

                    Text(
                        text = "@${user.login}",
                        fontSize = 14.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }

            if (!user.bio.isNullOrBlank()) {
                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = user.bio!!,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceAround
            ) {
                UserMiniMetric(
                    label = "Followers",
                    count = user.followers
                )

                UserMiniMetric(
                    label = "Following",
                    count = user.following
                )

                UserMiniMetric(
                    label = "Public Repos",
                    count = user.public_repos
                )
            }
        }
    }
}

@Composable
fun UserMiniMetric(
    label: String,
    count: Int
) {
    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 17.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = label,
            fontSize = 11.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
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
        modifier = modifier
            .height(96.dp)
            .clickable { onClick() },

        shape =
            RoundedCornerShape(14.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),

        border =
            CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,
                modifier =
                    Modifier.size(27.dp)
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Medium,
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
        modifier = modifier.height(94.dp),

        shape =
            RoundedCornerShape(14.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),

        border =
            CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
                    .background(
                        color.copy(alpha = 0.15f)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier =
                        Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

                Text(
                    text = title,
                    fontSize = 12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}