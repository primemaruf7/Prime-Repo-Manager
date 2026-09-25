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
        if (filterUnreadOnly) notifications.filter { it.unread } else notifications
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications ($unreadCount unread)", fontWeight = FontWeight.Bold) },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllNotificationsRead() }) {
                            Icon(Icons.Outlined.DoneAll, contentDescription = "Mark all read")
                        }
                    }
                    IconButton(onClick = { viewModel.loadNotifications() }) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !filterUnreadOnly,
                    onClick = { filterUnreadOnly = false },
                    label = { Text("All (${notifications.size})") }
                )
                FilterChip(
                    selected = filterUnreadOnly,
                    onClick = { filterUnreadOnly = true },
                    label = { Text("Unread ($unreadCount)") }
                )
            }

            if (displayedList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "All Caught Up!",
                    description = "You don't have any unread notifications right now."
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(displayedList, key = { it.id }) { item ->
                        NotificationListItem(
                            item = item,
                            onClick = {
                                if (item.unread) {
                                    viewModel.markNotificationRead(item.id)
                                }
                                val parts = item.repository.full_name.split("/")
                                if (parts.size == 2) {
                                    onRepoClick(parts[0], parts[1])
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
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
        "Issue" -> Color(0xFF39D353)
        "PullRequest" -> Color(0xFFA371F7)
        "Release" -> Color(0xFF58A6FF)
        else -> MaterialTheme.colorScheme.primary
    }

    ListItem(
        headlineContent = {
            Text(
                text = item.subject.title,
                fontWeight = if (item.unread) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text(
                text = "${item.repository.full_name} • ${item.reason.replace("_", " ")} • ${DateUtils.formatRelativeTime(item.updated_at)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                if (item.unread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
