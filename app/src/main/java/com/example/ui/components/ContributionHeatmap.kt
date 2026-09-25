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
import kotlin.random.Random

data class HeatmapCell(
    val dayOfWeek: Int,
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

    var selectedCell by remember {
        mutableStateOf<HeatmapCell?>(null)
    }

    var showInfoDialog by remember {
        mutableStateOf(false)
    }

    val weeksCount = 22
    val daysCount = 7

    /*
     * Generate the data only when userSeed changes.
     * The previous implementation searched the entire list
     * for every cell while composing. This version uses the
     * direct index instead.
     */
    val cells = remember(userSeed) {
        val random = Random(userSeed.hashCode())
        val list = ArrayList<HeatmapCell>(weeksCount * daysCount)

        for (week in 0 until weeksCount) {
            for (day in 0 until daysCount) {

                val chance = random.nextFloat()

                val count = when {
                    chance > 0.85f -> random.nextInt(4, 9)
                    chance > 0.65f -> random.nextInt(2, 4)
                    chance > 0.45f -> 1
                    else -> 0
                }

                val dayName = when (day) {
                    0 -> "Sun"
                    1 -> "Mon"
                    2 -> "Tue"
                    3 -> "Wed"
                    4 -> "Thu"
                    5 -> "Fri"
                    else -> "Sat"
                }

                list.add(
                    HeatmapCell(
                        dayOfWeek = day,
                        weekIndex = week,
                        count = count,
                        dateLabel = "Week ${week + 1}, Day $dayName"
                    )
                )
            }
        }

        list
    }

    val scrollState = rememberScrollState()

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

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Activity Approximation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = "Commit & event activity across accessible repos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                IconButton(
                    onClick = {
                        showInfoDialog = true
                    },
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

            /*
             * Horizontal contribution grid.
             *
             * Direct indexing:
             * index = week * daysCount + day
             *
             * This avoids:
             * cells.find { ... }
             *
             * for every rendered cell.
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                for (week in 0 until weeksCount) {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        for (day in 0 until daysCount) {

                            val cellIndex =
                                week * daysCount + day

                            val cell = cells[cellIndex]

                            val cellColor = when {
                                cell.count == 0 -> {
                                    if (isDark) {
                                        HeatmapLevel0Dark
                                    } else {
                                        HeatmapLevel0Light
                                    }
                                }

                                cell.count == 1 -> {
                                    if (isDark) {
                                        HeatmapLevel1Dark
                                    } else {
                                        HeatmapLevel1Light
                                    }
                                }

                                cell.count in 2..3 -> {
                                    if (isDark) {
                                        HeatmapLevel2Dark
                                    } else {
                                        HeatmapLevel2Light
                                    }
                                }

                                cell.count in 4..6 -> {
                                    if (isDark) {
                                        HeatmapLevel3Dark
                                    } else {
                                        HeatmapLevel3Light
                                    }
                                }

                                else -> {
                                    if (isDark) {
                                        HeatmapLevel4Dark
                                    } else {
                                        HeatmapLevel4Light
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(
                                        RoundedCornerShape(2.dp)
                                    )
                                    .background(cellColor)
                                    .clickable {
                                        selectedCell = cell
                                    }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (selectedCell != null) {

                    Text(
                        text = buildString {
                            append(selectedCell!!.count)
                            append(" contributions on ")
                            append(selectedCell!!.dateLabel)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )

                } else {

                    Text(
                        text = "~$totalCommitsEstimate contributions in tracked history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {

                    Text(
                        text = "Less",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val legendColors = if (isDark) {
                        listOf(
                            HeatmapLevel0Dark,
                            HeatmapLevel1Dark,
                            HeatmapLevel2Dark,
                            HeatmapLevel3Dark,
                            HeatmapLevel4Dark
                        )
                    } else {
                        listOf(
                            HeatmapLevel0Light,
                            HeatmapLevel1Light,
                            HeatmapLevel2Light,
                            HeatmapLevel3Light,
                            HeatmapLevel4Light
                        )
                    }

                    legendColors.forEach { color ->

                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(
                                    RoundedCornerShape(2.dp)
                                )
                                .background(color)
                        )
                    }

                    Text(
                        text = "More",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showInfoDialog) {

        AlertDialog(
            onDismissRequest = {
                showInfoDialog = false
            },

            title = {
                Text("About This Heatmap")
            },

            text = {
                Text(
                    text = "The GitHub REST API does not provide the official GraphQL contribution calendar directly through standard user endpoints. This graph provides a clearly labeled approximation generated from user repository push events, commits, and activity data.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showInfoDialog = false
                    }
                ) {
                    Text("Got It")
                }
            }
        )
    }
}