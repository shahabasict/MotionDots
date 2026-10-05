package com.motiondots.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.motiondots.app.estimator.VehicleMotion

@Composable
fun MotionCuePreview(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp),
    estimatorProvider: () -> VehicleMotion?
) {
    // Read latest estimate periodically
    val estimate by rememberUpdatedState(estimatorProvider())

    // Configuration / tunables
    val DOT_COUNT = 10 // default dot count
    val MOTION_DEADZONE = 0.02f // small dead-zone to ignore sensor noise
    val MAX_DISP_RATIO = 0.25f // max displacement ratio of half-dimension (tuned)
    val RESPOND_MS = 100 // quicker response
    val RETURN_MS = 250 // smoother return to neutral

    // Smooth motionIntensity for visual stability
    val smoothIntensity = remember { Animatable(0f) }
    LaunchedEffect(estimate?.motionIntensity) {
        val raw = estimate?.motionIntensity ?: 0f
        // apply dead-zone mapping
        val target = if (raw <= MOTION_DEADZONE) 0f else ((raw - MOTION_DEADZONE) / (1f - MOTION_DEADZONE)).coerceIn(0f, 1f)
        // choose animation duration: quick to respond, slower to return
        val duration = if (target > smoothIntensity.value) RESPOND_MS else RETURN_MS
        smoothIntensity.animateTo(target, animationSpec = tween(duration))
    }

    val dirX = estimate?.horizontalX ?: 0f
    val dirY = estimate?.horizontalY ?: 0f

    Column(modifier = modifier.padding(8.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val padding = 12f
            val rx = (w / 2f) - padding
            val ry = (h / 2f) - padding

            val nDots = DOT_COUNT
            val maxDisp = (minOf(rx, ry) * MAX_DISP_RATIO)

            // direction vector
            val dlen = kotlin.math.sqrt((dirX * dirX + dirY * dirY).toDouble()).toFloat()
            val dirNx = if (dlen > 1e-6f) dirX / dlen else 0f
            val dirNy = if (dlen > 1e-6f) dirY / dlen else 0f

            val intensity = smoothIntensity.value

            for (i in 0 until nDots) {
                val angle = (i.toFloat() / nDots.toFloat()) * (2f * kotlin.math.PI.toFloat())
                // base position on an inscribed ellipse near edges
                val bx = cx + kotlin.math.cos(angle) * rx
                val by = cy + kotlin.math.sin(angle) * ry
                val normalX = (bx - cx)
                val normalY = (by - cy)
                val nlen = kotlin.math.sqrt((normalX * normalX + normalY * normalY).toDouble()).toFloat()
                val nx = if (nlen > 1e-6f) normalX / nlen else 0f
                val ny = if (nlen > 1e-6f) normalY / nlen else 0f

                // how aligned the dot normal is with motion direction
                val align = (nx * dirNx + ny * dirNy).coerceIn(-1f, 1f)
                // positive alignment only (dots move toward motion direction)
                val positiveAlign = kotlin.math.max(0f, align)

                val disp = positiveAlign * intensity * maxDisp

                val dx = (nx * disp).coerceAtLeast(-rx + padding).coerceAtMost(rx - padding)
                val dy = (ny * disp).coerceAtLeast(-ry + padding).coerceAtMost(ry - padding)

                val px = (bx + dx).coerceIn(padding, w - padding)
                val py = (by + dy).coerceIn(padding, h - padding)

                // dot size
                val r = 6f
                drawCircle(color = Color(0xFFFFA726), radius = r, center = Offset(px, py))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        // simple numeric readout
        val horX = estimate?.horizontalX ?: 0f
        val horY = estimate?.horizontalY ?: 0f
        val mi = estimate?.motionIntensity ?: 0f
        Text(text = "motionIntensity: ${"%.3f".format(mi)}  horizontalX: ${"%.3f".format(horX)}  horizontalY: ${"%.3f".format(horY)}")
    }
}
