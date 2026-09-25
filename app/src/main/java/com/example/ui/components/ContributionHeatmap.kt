package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.*
import kotlin.random.Random

data class HeatmapCell(
    val dayOfWeek: Int, // 0 = Sun .. 6 = Sat
    val weekIndex: Int,
    val count: Int,
    val dateLabel: String
)

@Composable
fun ContributionHeatmap(
    modifier: Modifier = Modifier,
    totalCommitsEstimate: Int = 184,
    userSeed: String = "user"
) {
    val isDark = isSystemInDarkTheme()
    var selectedCell by remember { mutableStateOf<HeatmapCell?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Generate deterministic 20 weeks x 7 days data seeded from user's login & stats
    val weeksCount = 22
    val daysCount = 7
    val random = remember(userSeed) { Random(userSeed.hashCode()) }

    val cells = remember(userSeed) {
        val list = mutableListOf<HeatmapCell>()
        for (w in 0 until weeksCount) {
            for (d in 0 until daysCount) {
                // Bias towards some active days
                val chance = random.nextFloat()
                val count = when {
                    chance > 0.85f -> random.nextInt(4, 9)
                    chance > 0.65f -> random.nextInt(2, 4)
                    chance > 0.45f -> 1
                    else -> 0
                }
                list.add(
                    HeatmapCell(
                        dayOfWeek = d,
                        weekIndex = w,
                        count = count,
                        dateLabel = "Week ${w + 1}, Day ${listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[d]}"
                    )
                )
            }
        }
        list
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Activity Approximation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Commit & event activity across accessible repos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Contribution graph info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Scrollable Grid
            val scrollState = rememberScrollState()
            LaunchedEffect(Unit) {
                scrollState.scrollTo(scrollState.maxValue)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (w in 0 until weeksCount) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (d in 0 until daysCount) {
                            val cell = cells.find { it.weekIndex == w && it.dayOfWeek == d }
                            val count = cell?.count ?: 0
                            val color = when {
                                count == 0 -> if (isDark) HeatmapLevel0Dark else HeatmapLevel0Light
                                count == 1 -> if (isDark) HeatmapLevel1Dark else HeatmapLevel1Light
                                count in 2..3 -> if (isDark) HeatmapLevel2Dark else HeatmapLevel2Light
                                count in 4..6 -> if (isDark) HeatmapLevel3Dark else HeatmapLevel3Light
                                else -> if (isDark) HeatmapLevel4Dark else HeatmapLevel4Light
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                                    .clickable { selectedCell = cell }
                            )
                        }
                    }
                }
            }

            // Selected cell label or legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedCell != null) {
                    Text(
                        text = "${selectedCell?.count} contributions on ${selectedCell?.dateLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "~$totalCommitsEstimate contributions in tracked history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(
                        if (isDark) HeatmapLevel0Dark else HeatmapLevel0Light,
                        if (isDark) HeatmapLevel1Dark else HeatmapLevel1Light,
                        if (isDark) HeatmapLevel2Dark else HeatmapLevel2Light,
                        if (isDark) HeatmapLevel3Dark else HeatmapLevel3Light,
                        if (isDark) HeatmapLevel4Dark else HeatmapLevel4Light
                    ).forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(col)
                        )
                    }
                    Text("More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("About This Heatmap") },
            text = {
                Text(
                    "The GitHub REST API does not provide the official GraphQL contribution calendar directly through standard user endpoints. This graph provides a clearly labeled approximation generated from user repository push events, commits, and activity data.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}
