package com.motiondots.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MotionDotsApp() {
    var selectedTab by remember { mutableStateOf(Tab.Home) }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                BottomNavigation {
                    BottomNavigationItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = selectedTab == Tab.Home,
                        onClick = { selectedTab = Tab.Home }
                    )
                    BottomNavigationItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = "Diagnostics") },
                        label = { Text("Diagnostics") },
                        selected = selectedTab == Tab.Diagnostics,
                        onClick = { selectedTab = Tab.Diagnostics }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (selectedTab) {
                    Tab.Home -> HomeScreen()
                    Tab.Diagnostics -> DiagnosticsScreen()
                }
            }
        }
    }
}

enum class Tab { Home, Diagnostics }

@Composable
fun HomeScreen() {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)) {

        Text(
            text = "MotionDots",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ON/OFF card
        Card(elevation = 6.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                var enabled by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Status", fontWeight = FontWeight.Medium)
                        Text("MotionDots is currently", color = Color.Gray, fontSize = 12.sp)
                    }
                    // Primary toggle (visual only)
                    Button(
                        onClick = { enabled = !enabled },
                        colors = ButtonDefaults.buttonColors(backgroundColor = if (enabled) Color(0xFF00C853) else Color(0xFFB0BEC5)),
                        modifier = Modifier.height(48.dp).width(120.dp)
                    ) {
                        Text(if (enabled) "ON" else "OFF", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(if (enabled) "Status: Enabled — will show motion cues over other apps (overlay coming in a later phase)." else "Status: Disabled", color = if (enabled) Color(0xFF00C853) else Color.DarkGray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Settings placeholders
        Text("Settings", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        SettingRow(title = "Dot count", description = "Placeholder control")
        SettingRow(title = "Dot size", description = "Placeholder control")
        SettingRow(title = "Dot opacity", description = "Placeholder control")
        SettingRow(title = "Dot color", description = "Placeholder control")
        SettingRow(title = "Edge distance", description = "Placeholder control")
        SettingRow(title = "Motion sensitivity", description = "Placeholder control")

        Spacer(modifier = Modifier.height(12.dp))

        Text("Note: MotionDots will eventually display motion cues as an overlay on top of other apps.", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun SettingRow(title: String, description: String) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(description, color = Color.Gray, fontSize = 12.sp)
            }
            // Visual placeholder: small pill
            Box(modifier = Modifier
                .size(44.dp, 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE0E0E0))
                .clickable { /* placeholder */ }, contentAlignment = Alignment.Center) {
                Text("—", color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun DiagnosticsScreen() {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Diagnostics", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        val context = LocalContext.current
        val sensor = remember { com.motiondots.app.sensor.AccelerometerSensor(context) }

        DiagnosticCard(title = "Accelerometer") {
            if (!sensor.hasSensor()) {
                Text("Accelerometer not available on this device.", color = Color.Gray)
            } else {
                DisposableEffect(sensor) {
                    sensor.start()
                    onDispose {
                        sensor.stop()
                    }
                }
                // maintain a small buffer of recent samples for X/Y/Z
                val samples = remember { mutableStateListOf<FloatArray>() }
                val latest by sensor.reading.collectAsState(initial = null)

                // update samples when latest changes
                LaunchedEffect(latest) {
                    latest?.let {
                        samples.add(floatArrayOf(it.x, it.y, it.z))
                        if (samples.size > 80) samples.removeAt(0)
                    }
                }

                // numeric readout
                latest?.let {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("X: ${String.format("%.2f", it.x)}")
                        Text("Y: ${String.format("%.2f", it.y)}")
                        Text("Z: ${String.format("%.2f", it.z)}")
                    }
                } ?: Text("Waiting for data...", color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))
                AccelerometerGraph(samples = samples)
            }
        }

    DiagnosticCard(title = "Gyroscope") {
        SimpleLineGraph(sample = listOf(0f, -0.3f, 0.4f, -0.1f, 0.5f, -0.6f, 0.2f))
    }

        DiagnosticCard(title = "Processed motion") {
            SimpleLineGraph(sample = listOf(0f, 0.2f, 0.1f, 0.3f, 0.0f, -0.1f, 0.05f))
        }
    }
}

@Composable
fun DiagnosticCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp), elevation = 4.dp, shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun AccelerometerGraph(samples: List<FloatArray>) {
    // samples: list of [x,y,z]
    Canvas(modifier = Modifier
        .height(120.dp)
        .fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val n = samples.size
        if (n < 2) return@Canvas

        val step = w / (n - 1).coerceAtLeast(1)
        // find max absolute among samples for scaling
        var maxAbs = 0f
        samples.forEach { arr ->
            maxAbs = kotlin.math.max(maxAbs, kotlin.math.abs(arr[0]))
            maxAbs = kotlin.math.max(maxAbs, kotlin.math.abs(arr[1]))
            maxAbs = kotlin.math.max(maxAbs, kotlin.math.abs(arr[2]))
        }
        if (maxAbs == 0f) maxAbs = 1f

        val pathX = Path()
        val pathY = Path()
        val pathZ = Path()

        samples.forEachIndexed { i, arr ->
            val x = i * step
            val vx = (arr[0] / maxAbs) * (h / 2f)
            val vy = (arr[1] / maxAbs) * (h / 2f)
            val vz = (arr[2] / maxAbs) * (h / 2f)

            val px = x.toFloat()
            val yx = h / 2f - vx
            val yy = h / 2f - vy
            val yz = h / 2f - vz

            if (i == 0) {
                pathX.moveTo(px, yx); pathY.moveTo(px, yy); pathZ.moveTo(px, yz)
            } else {
                pathX.lineTo(px, yx); pathY.lineTo(px, yy); pathZ.lineTo(px, yz)
            }
        }

        drawPath(path = pathX, color = Color.Red, style = Stroke(width = 2f))
        drawPath(path = pathY, color = Color.Green, style = Stroke(width = 2f))
        drawPath(path = pathZ, color = Color.Blue, style = Stroke(width = 2f))
    }
}


@Composable
fun SimpleLineGraph(sample: List<Float>) {
    // simple mocked graph drawn using Canvas
    val max = (sample.maxOrNull() ?: 1f).let { kotlin.math.max(it, kotlin.math.abs(sample.minOrNull() ?: 0f)) }
    val normalized = sample.map { (it / (if (max != 0f) max else 1f)) * 0.9f }

    Canvas(modifier = Modifier
        .height(100.dp)
        .fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val step = if (normalized.size > 1) w / (normalized.size - 1).toFloat() else w

        val path = Path()
        normalized.forEachIndexed { i, v ->
            val x = i.toFloat() * step
            val y = h / 2f - (v * (h / 2f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = Color(0xFF1E88E5), style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}
