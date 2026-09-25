package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PrimeRepoLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    animated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val pulse by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        rememberUpdatedState(1f)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * pulse)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = (w.coerceAtMost(h) / 2f) * 0.9f

            // Outer Shield / Hexagon
            val hexPath = Path().apply {
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60.0 * i - 30.0))
                    val px = cx + (r * cos(angle)).toFloat()
                    val py = cy + (r * sin(angle)).toFloat()
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }

            // Draw Hexagon outline with cyan-emerald gradient
            drawPath(
                path = hexPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF58A6FF), Color(0xFF238636), Color(0xFF39D353)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(width = r * 0.08f)
            )

            // Inner Git Branch nodes & connecting curves
            val mainBranchX = cx - (r * 0.32f)
            val featureBranchX = cx + (r * 0.32f)
            val topY = cy - (r * 0.45f)
            val midY = cy
            val botY = cy + (r * 0.45f)
            val nodeRadius = r * 0.12f

            // Main vertical trunk line
            drawLine(
                color = Color(0xFF58A6FF),
                start = Offset(mainBranchX, topY),
                end = Offset(mainBranchX, botY),
                strokeWidth = r * 0.07f,
                cap = StrokeCap.Round
            )

            // Branch curve from mid to feature
            val branchCurve = Path().apply {
                moveTo(mainBranchX, botY - (r * 0.35f))
                cubicTo(
                    mainBranchX, midY,
                    featureBranchX, midY,
                    featureBranchX, topY + (r * 0.2f)
                )
            }
            drawPath(
                path = branchCurve,
                color = Color(0xFF39D353),
                style = Stroke(width = r * 0.07f, cap = StrokeCap.Round)
            )

            // Root Node
            drawCircle(
                color = Color(0xFF58A6FF),
                radius = nodeRadius,
                center = Offset(mainBranchX, botY)
            )
            drawCircle(
                color = Color(0xFF0D1117),
                radius = nodeRadius * 0.5f,
                center = Offset(mainBranchX, botY)
            )

            // Main Branch Head Node
            drawCircle(
                color = Color(0xFF58A6FF),
                radius = nodeRadius,
                center = Offset(mainBranchX, topY)
            )
            drawCircle(
                color = Color(0xFF0D1117),
                radius = nodeRadius * 0.5f,
                center = Offset(mainBranchX, topY)
            )

            // Feature Branch Head Node (Glowing Emerald)
            drawCircle(
                color = Color(0xFF39D353),
                radius = nodeRadius,
                center = Offset(featureBranchX, topY + (r * 0.2f))
            )
            drawCircle(
                color = Color(0xFF0D1117),
                radius = nodeRadius * 0.5f,
                center = Offset(featureBranchX, topY + (r * 0.2f))
            )
        }
    }
}
