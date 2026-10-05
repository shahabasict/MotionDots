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
import com.motiondots.app.sensor.MotionProcessor
import com.motiondots.app.sensor.WorldAcceleration
import com.motiondots.app.estimator.VehicleMotionEstimator
import com.motiondots.app.estimator.VehicleMotion
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

        // Gyroscope - live
        val gyroContext = LocalContext.current
        val gyroSensor = remember { com.motiondots.app.sensor.GyroscopeSensor(gyroContext) }
        DiagnosticCard(title = "Gyroscope") {
            if (!gyroSensor.hasSensor()) {
                Text("Gyroscope not available on this device.", color = Color.Gray)
            } else {
                DisposableEffect(gyroSensor) {
                    gyroSensor.start()
                    onDispose { gyroSensor.stop() }
                }

                val gyroSamples = remember { mutableStateListOf<FloatArray>() }
                val latestGyro by gyroSensor.reading.collectAsState(initial = null)

                LaunchedEffect(latestGyro) {
                    latestGyro?.let {
                        gyroSamples.add(floatArrayOf(it.x, it.y, it.z))
                        if (gyroSamples.size > 80) gyroSamples.removeAt(0)
                    }
                }

                latestGyro?.let {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("X: ${String.format("%.3f", it.x)}")
                        Text("Y: ${String.format("%.3f", it.y)}")
                        Text("Z: ${String.format("%.3f", it.z)}")
                    }
                } ?: Text("Waiting for gyroscope data...", color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))
                GyroscopeGraph(samples = gyroSamples)
            }
        }
        
        // Orientation (rotation-vector)
        val orientationContext = LocalContext.current
        val orientationSensor = remember { com.motiondots.app.sensor.OrientationSensor(orientationContext) }
        DiagnosticCard(title = "Orientation") {
            if (!orientationSensor.hasSensor()) {
                Text("Rotation-vector sensor not available on this device.", color = Color.Gray)
            } else {
                DisposableEffect(orientationSensor) {
                    orientationSensor.start()
                    onDispose { orientationSensor.stop() }
                }

                val latestOrient by orientationSensor.reading.collectAsState(initial = null)
                latestOrient?.let { o ->
                    // show roll/pitch/yaw in degrees and quaternion components
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Roll: ${String.format("%.1f", Math.toDegrees(o.roll.toDouble()))}°")
                            Text("Pitch: ${String.format("%.1f", Math.toDegrees(o.pitch.toDouble()))}°")
                            Text("Yaw: ${String.format("%.1f", Math.toDegrees(o.yaw.toDouble()))}°")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Quaternion:")
                        Text("w: ${String.format("%.3f", o.qw)} x: ${String.format("%.3f", o.qx)} y: ${String.format("%.3f", o.qy)} z: ${String.format("%.3f", o.qz)}", fontSize = 12.sp, color = Color.Gray)
                    }
                } ?: Text("Waiting for orientation data...", color = Color.Gray)
            }
        }

        // Vehicle motion estimation (Phase 8): derive a simple motion proxy from filtered world-frame acceleration
        val motionProcessor = remember { MotionProcessor(sensor.reading, orientationSensor.reading) }
        DiagnosticCard(title = "Vehicle motion estimate (experimental)") {
            // start/stop processor with lifecycle
            DisposableEffect(motionProcessor) {
                motionProcessor.start()
                onDispose { motionProcessor.stop() }
            }

            val estimator = remember { VehicleMotionEstimator { motionProcessor.latest } }
            val estimates = remember { mutableStateListOf<com.motiondots.app.estimator.VehicleMotion>() }

            // poll estimator periodically and keep a short buffer for plotting
            LaunchedEffect(Unit) {
                while (true) {
                    val e = estimator.estimate()
                    e?.let {
                        estimates.add(it)
                        if (estimates.size > 80) estimates.removeAt(0)
                    }
                    kotlinx.coroutines.delay(40)
                }
            }

            if (estimates.isEmpty()) {
                Text("Waiting for vehicle-motion estimates...", color = Color.Gray)
            } else {
                val last = estimates.last()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Filtered World X: ${String.format("%.3f", last.filteredX)} m/s²")
                        Text("Filtered World Y: ${String.format("%.3f", last.filteredY)} m/s²")
                        Text("Filtered World Z: ${String.format("%.3f", last.filteredZ)} m/s²")
                    }
                    Column {
                        Text("Horizontal magnitude: ${String.format("%.3f", last.horizontalMagnitude)} m/s²")
                        Text("Motion intensity: ${String.format("%.3f", last.motionIntensity)} (0..1)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // draw horizontal magnitude (raw) and motion intensity (scaled) as a simple graph
                Canvas(modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()) {
                    val w = size.width
                    val h = size.height
                    val n = estimates.size
                    if (n >= 2) {
                        val step = w / (n - 1).coerceAtLeast(1)
                        var maxMag = 0f
                        estimates.forEach { arr ->
                            maxMag = kotlin.math.max(maxMag, kotlin.math.abs(arr.horizontalMagnitude))
                        }
                        if (maxMag == 0f) maxMag = 1f

                        val pathMag = Path()
                        val pathIntensity = Path()

                        estimates.forEachIndexed { i, arr ->
                            val x = i * step
                            val vmag = (arr.horizontalMagnitude / maxMag) * (h / 2f)
                            // intensity is already normalized 0..1; scale to canvas height
                            val vint = (arr.motionIntensity) * (h / 2f)

                            val px = x.toFloat()
                            val ymag = h / 2f - vmag
                            val yint = h / 2f - vint

                            if (i == 0) {
                                pathMag.moveTo(px, ymag)
                                pathIntensity.moveTo(px, yint)
                            } else {
                                pathMag.lineTo(px, ymag)
                                pathIntensity.lineTo(px, yint)
                            }
                        }

                        // draw horizontal magnitude in orange and intensity as purple
                        drawPath(path = pathMag, color = Color(0xFFFFA726), style = Stroke(width = 2.5f))
                        drawPath(path = pathIntensity, color = Color(0xFF8E24AA), style = Stroke(width = 2.5f))
                    }
                }
            }
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
fun GyroscopeGraph(samples: List<FloatArray>) {
    Canvas(modifier = Modifier
        .height(120.dp)
        .fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val n = samples.size
        if (n < 2) return@Canvas

        val step = w / (n - 1).coerceAtLeast(1)
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

        drawPath(path = pathX, color = Color.Magenta, style = Stroke(width = 2f))
        drawPath(path = pathY, color = Color.Cyan, style = Stroke(width = 2f))
        drawPath(path = pathZ, color = Color.Yellow, style = Stroke(width = 2f))
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
