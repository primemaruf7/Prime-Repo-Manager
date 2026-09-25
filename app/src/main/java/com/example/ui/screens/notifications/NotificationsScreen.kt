package com.example.ui.screens.notifications

import androidx.compose.foundation.background
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
import com.example.data.model.NotificationItem
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.DateUtils

private val NotificationCard = Color(0xFF211F26)
private val UnreadDot = Color(0xFF8B5CF6)
private val IssueColor = Color(0xFF39D353)
private val PullRequestColor = Color(0xFFA371F7)
private val ReleaseColor = Color(0xFF58A6FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: PrimeRepoViewModel,
    onRepoClick: (owner: String, repo: String) -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()

    var filterUnreadOnly by remember { mutableStateOf(false) }

    val displayedList = remember(notifications, filterUnreadOnly) {
        if (filterUnreadOnly) {
            notifications.filter { it.unread }
        } else {
            notifications
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = {
                                viewModel.markAllNotificationsRead()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DoneAll,
                                contentDescription = "Mark all as read",
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.loadNotifications()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(23.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                FilterChip(
                    selected = !filterUnreadOnly,
                    onClick = {
                        filterUnreadOnly = false
                    },
                    label = {
                        Text(
                            text = "All ${notifications.size}",
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = NotificationCard,
                        selectedContainerColor = Color(0xFF5A506F),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLabelColor = Color.White
                    ),
                    border = null
                )

                FilterChip(
                    selected = filterUnreadOnly,
                    onClick = {
                        filterUnreadOnly = true
                    },
                    label = {
                        Text(
                            text = "Unread $unreadCount",
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = NotificationCard,
                        selectedContainerColor = Color(0xFF5A506F),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLabelColor = Color.White
                    ),
                    border = null
                )
            }

            if (displayedList.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Outlined.NotificationsNone,
                        title = "All Caught Up!",
                        description = "You don't have any unread notifications right now."
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    items(
                        items = displayedList,
                        key = { it.id }
                    ) { item ->

                        NotificationListItem(
                            item = item,
                            onClick = {

                                if (item.unread) {
                                    viewModel.markNotificationRead(item.id)
                                }

                                val parts =
                                    item.repository.full_name.split("/")

                                if (parts.size == 2) {
                                    onRepoClick(
                                        parts[0],
                                        parts[1]
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationListItem(
    item: NotificationItem,
    onClick: () -> Unit
) {

    val icon = when (item.subject.type) {
        "Issue" -> Icons.Outlined.Adjust
        "PullRequest" -> Icons.Outlined.CallMerge
        "Commit" -> Icons.Outlined.Commit
        "Release" -> Icons.Outlined.Tag
        else -> Icons.Outlined.Notifications
    }

    val iconColor = when (item.subject.type) {
        "Issue" -> IssueColor
        "PullRequest" -> PullRequestColor
        "Release" -> ReleaseColor
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                onClick()
            },
        color = NotificationCard,
        tonalElevation = 0.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 14.dp
                ),
            verticalAlignment = Alignment.Top
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        iconColor.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = item.subject.title,
                    fontSize = 14.sp,
                    fontWeight = if (item.unread) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = item.repository.full_name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "${item.reason.replace("_", " ")} • ${
                        DateUtils.formatRelativeTime(item.updated_at)
                    }",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (item.unread) {

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(UnreadDot)
                )
            }
        }
    }
}