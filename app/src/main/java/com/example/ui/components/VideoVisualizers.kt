package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.TikRoxAccentCyan
import com.example.ui.theme.TikRoxAccentPink
import com.example.ui.theme.TikRoxWhite
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// Bubble heart item for interactive double-tap like clicks
data class FloatingHeart(
    val id: Long,
    val x: Float,
    val y: Float,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val angle: Float = 0f
)

@Composable
fun VideoVisualizerContainer(
    themeName: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onDoubleTap: (Offset) -> Unit = {},
    onSingleTap: () -> Unit = {}
) {
    var hearts by remember { mutableStateOf(listOf<FloatingHeart>()) }

    // Efficient animation loop without spawning/canceling coroutine on every single frame
    LaunchedEffect(hearts.isNotEmpty()) {
        if (hearts.isNotEmpty()) {
            while (hearts.isNotEmpty()) {
                delay(16)
                hearts = hearts.map {
                    it.copy(
                        y = it.y - 6f, // float up
                        alpha = it.alpha - 0.03f, // fade out
                        scale = it.scale + 0.015f, // expand slightly
                        angle = it.angle + (if (it.id % 2 == 0L) 1.5f else -1.5f)
                    )
                }.filter { it.alpha > 0f }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isPlaying) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        // Generate floating heart at click location
                        val newHeart = FloatingHeart(
                            id = System.nanoTime(),
                            x = offset.x,
                            y = offset.y,
                            scale = 0.5f,
                            alpha = 1.0f,
                            angle = (-15..15).random().toFloat()
                        )
                        hearts = hearts + newHeart
                        onDoubleTap(offset)
                    },
                    onTap = { _ ->
                        onSingleTap()
                    }
                )
            }
    ) {
        // Base visualizer
        when (themeName) {
            "NEON_BEAT" -> NeonBeatVisualizer(isPlaying = isPlaying)
            "CYBER_PARTICLES" -> CyberParticlesVisualizer(isPlaying = isPlaying)
            "COSMIC_HELIX" -> CosmicHelixVisualizer(isPlaying = isPlaying)
            "VAPOR_DRIVE" -> VaporDriveVisualizer(isPlaying = isPlaying)
            else -> NeonBeatVisualizer(isPlaying = isPlaying)
        }

        // Overlay double-tap hearts
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (heart in hearts) {
                rotate(degrees = heart.angle, pivot = Offset(heart.x, heart.y)) {
                    val sizeValue = 60f * heart.scale
                    val path = Path().apply {
                        // Custom heart drawing path center coordinate around (x, y)
                        val startX = heart.x
                        val startY = heart.y - sizeValue / 4

                        moveTo(startX, startY)
                        cubicTo(
                            startX - sizeValue / 2, startY - sizeValue / 2,
                            startX - sizeValue, startY + sizeValue / 3,
                            startX, startY + sizeValue * 0.9f
                        )
                        cubicTo(
                            startX + sizeValue, startY + sizeValue / 3,
                            startX + sizeValue / 2, startY - sizeValue / 2,
                            startX, startY
                        )
                    }
                    drawPath(
                        path = path,
                        color = TikRoxAccentPink.copy(alpha = heart.alpha)
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = heart.alpha * 0.8f),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

@Composable
fun NeonBeatVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "neon_beat")
    
    val pulseProgression by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val spinDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val maxRadius = width.coerceAtLeast(height) * 0.6f

        drawRect(color = Color(0xFF030303)) // Absolute pure black canvas for dynamic glowing depth

        val playPulse = if (isPlaying) pulseProgression else 0.5f
        val playSpin = if (isPlaying) spinDegrees else 45f

        // 1. Core ambient neon radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TikRoxAccentPink.copy(alpha = 0.25f * (1f - playPulse)),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = maxRadius * 0.7f
            ),
            radius = maxRadius * 0.6f,
            center = Offset(centerX, centerY)
        )

        // 2. Pulse waves (3 nested concentric ripples)
        for (i in 0..2) {
            val personalProgress = (playPulse + i / 3f) % 1.0f
            val alpha = 1.0f - personalProgress
            val radius = maxRadius * personalProgress
            val color = if (i % 2 == 0) TikRoxAccentCyan else TikRoxAccentPink

            drawCircle(
                color = color.copy(alpha = alpha * 0.4f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 4f + 3f * (1f - personalProgress))
            )
        }

        // 3. Central glowing Audio DJ Record visualizer
        rotate(degrees = playSpin, pivot = Offset(centerX, centerY)) {
            val diskRadius = 120f
            // Disk backing
            drawCircle(
                color = Color(0xFF151515),
                radius = diskRadius,
                center = Offset(centerX, centerY)
            )
            // Grooves
            for (g in 1..4) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = diskRadius * (g / 5f),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5f)
                )
            }
            // Vinyl Label
            drawCircle(
                color = TikRoxAccentCyan,
                radius = 35f,
                center = Offset(centerX, centerY)
            )
            // Center pinhole
            drawCircle(
                color = Color.Black,
                radius = 8f,
                center = Offset(centerX, centerY)
            )

            // Equalizer lines rotating from center disk rim
            val barCount = 16
            for (b in 0 until barCount) {
                val angleRad = Math.toRadians((b * (360f / barCount)).toDouble())
                // Base length pulses slightly to mimic sound waves
                val offsetPulse = if (isPlaying) {
                    1f + 0.35f * sin((playPulse * 15f + b * 2f).toDouble()).toFloat()
                } else 1.0f

                val barLength = 40f * offsetPulse
                val innerPoint = Offset(
                    (centerX + (diskRadius + 10f) * cos(angleRad)).toFloat(),
                    (centerY + (diskRadius + 10f) * sin(angleRad)).toFloat()
                )
                val outerPoint = Offset(
                    (centerX + (diskRadius + 10f + barLength) * cos(angleRad)).toFloat(),
                    (centerY + (diskRadius + 10f + barLength) * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = if (b % 2 == 0) TikRoxAccentPink else TikRoxAccentCyan,
                    start = innerPoint,
                    end = outerPoint,
                    strokeWidth = 6f
                )
            }
        }
    }
}

@Composable
fun CyberParticlesVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_particles")
    val ticker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timer"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color(0xFF04020A)) // Ambient cyberpunk violet-black
        
        val width = size.width
        val height = size.height
        val t = if (isPlaying) ticker else 150f

        // Draw deep vector grid backing
        val gridStep = 80f
        for (x in 0..(width / gridStep).toInt()) {
            val screenX = x * gridStep
            drawLine(
                color = Color(0xFF140D2D).copy(alpha = 0.35f),
                start = Offset(screenX, 0f),
                end = Offset(screenX, height),
                strokeWidth = 1f
            )
        }
        for (y in 0..(height / gridStep).toInt()) {
            val screenY = y * gridStep
            drawLine(
                color = Color(0xFF140D2D).copy(alpha = 0.35f),
                start = Offset(0f, screenY),
                end = Offset(width, screenY),
                strokeWidth = 1f
            )
        }

        // Draw 30 floating particles with customized motion paths
        val particleCount = 35
        for (i in 0 until particleCount) {
            val seed = i * 142.3f
            val speed = 2f + (i % 4) * 1.5f
            val sizeValue = 4f + (i % 3) * 6f

            // Animate y going upwards loop
            val startY = height + 100f
            val deltaY = (t * speed) % (height + 200f)
            val currentY = startY - deltaY

            // Animate x with wavy lateral sine wind
            val baseStartX = (width / (particleCount + 1)) * (i + 1)
            val waveAmplitude = 50f
            val currentX = baseStartX + waveAmplitude * sin((t * 0.05f + seed).toDouble()).toFloat()

            val alpha = if (currentY < 150f) {
                // fade out at screen top
                (currentY / 150f).coerceIn(0f, 1f)
            } else if (currentY > height - 100f) {
                // fade in at screen bottom
                ((height - currentY) / 100f).coerceIn(0f, 1f)
            } else 1.0f

            val color = when (i % 3) {
                0 -> TikRoxAccentCyan
                1 -> TikRoxAccentPink
                else -> Color(0xFFBD00FF) // Purple highlights
            }

            // Outer glow ring
            drawCircle(
                color = color.copy(alpha = alpha * 0.3f),
                radius = sizeValue * 2.2f,
                center = Offset(currentX, currentY)
            )
            // Hard core
            drawCircle(
                color = TikRoxWhite.copy(alpha = alpha),
                radius = sizeValue,
                center = Offset(currentX, currentY)
            )
        }

        // Ambient cyber spotlight gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFBD00FF).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(width * 0.8f, height * 0.2f),
                radius = width * 0.8f
            ),
            radius = width * 0.8f,
            center = Offset(width * 0.8f, height * 0.2f)
        )
    }
}

@Composable
fun CosmicHelixVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_helix")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color(0xFF00030A)) // Abyssal space dark deep blue
        
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        val rSpin = if (isPlaying) rotation else 60f

        // 1. Draw Nebula clusters in the backdrop
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(TikRoxAccentCyan.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(centerX - 100f, centerY + 150f),
                radius = width * 0.5f
            ),
            radius = width * 0.5f,
            center = Offset(centerX - 100f, centerY + 150f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(TikRoxAccentPink.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(centerX + 120f, centerY - 200f),
                radius = width * 0.5f
            ),
            radius = width * 0.5f,
            center = Offset(centerX + 120f, centerY - 200f)
        )

        // 2. Draw stars & geometric coordinate orbits
        rotate(degrees = rSpin, pivot = Offset(centerX, centerY)) {
            // Background starry ring
            drawCircle(
                color = TikRoxWhite.copy(alpha = 0.1f),
                radius = width * 0.35f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f))
            )

            // Dynamic cosmic helix strings. Let's trace polar curves
            val pointCount = 120
            for (p in 0 until pointCount) {
                val t = (p / pointCount.toFloat()) * Math.PI * 4 // 2 loops
                val scaleFactor = 150f + 60f * sin(t * 3.0).toFloat()
                
                val x = (centerX + scaleFactor * cos(t)).toFloat()
                val y = (centerY + scaleFactor * sin(t)).toFloat()
                
                val progress = p / pointCount.toFloat()
                val glowColor = if (p % 2 == 0) TikRoxAccentCyan else TikRoxAccentPink
                
                drawCircle(
                    color = glowColor.copy(alpha = progress * 0.8f),
                    radius = 4f + progress * 8f,
                    center = Offset(x, y)
                )

                // Central links
                if (p % 8 == 0) {
                    drawLine(
                        color = TikRoxWhite.copy(alpha = 0.08f),
                        start = Offset(centerX, centerY),
                        end = Offset(x, y),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // Inner glowing core
        drawCircle(
            color = TikRoxWhite.copy(alpha = 0.9f),
            radius = 16f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = TikRoxAccentCyan.copy(alpha = 0.4f),
            radius = 35f,
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
fun VaporDriveVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "vapor_drive")
    val progressOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "road_offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Fill background with elegant dark violet night
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0C0720), Color(0xFF22083D))
            )
        )

        val runProgress = if (isPlaying) progressOffset else 0.5f

        // Draw iconic split neon Retrowave Sunset
        val sunRadius = width * 0.28f
        val sunX = width / 2
        val sunY = height * 0.4f

        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF0D87), Color(0xFFFF9400)),
                startY = sunY - sunRadius,
                endY = sunY + sunRadius
            ),
            radius = sunRadius,
            center = Offset(sunX, sunY)
        )

        // horizontal split bars over sunset to represent scanlines
        val scanlineCount = 12
        for (s in 0 until scanlineCount) {
            val topOffset = sunY - sunRadius + (sunRadius * 2 * (s / scanlineCount.toFloat()))
            val sliceHeight = 4f + s * 1.5f
            if (topOffset > sunY - 40f) {
                drawRect(
                    color = Color(0xFF0C0720),
                    topLeft = Offset(sunX - sunRadius - 10f, topOffset),
                    size = Size(sunRadius * 2 + 20f, sliceHeight)
                )
            }
        }

        // Vanishing perspective point for 3D grid road
        val vPointX = width / 2
        val vPointY = height * 0.55f

        // Draw retrowave city grid skyline sillhouettes in distance
        val dStartX = 0f
        val dHeight = 45f
        drawRect(
            color = Color(0xFF080415),
            topLeft = Offset(dStartX, vPointY - dHeight),
            size = Size(width, dHeight)
        )
        // Skyscrapers silhouettes
        val skys = listOf(
            Offset(20f, 80f), Offset(100f, 60f), Offset(140f, 100f),
            Offset(220f, 75f), Offset(320f, 110f), Offset(400f, 90f),
            Offset(480f, 50f), Offset(550f, 115f), Offset(650f, 70f)
        )
        for (sky in skys) {
            val skyX = (sky.x / 700f) * width
            val skyH = sky.y
            drawRect(
                color = Color(0xFF04020B),
                topLeft = Offset(skyX, vPointY - skyH),
                size = Size(width * 0.08f, skyH)
            )
        }

        // Draw reflective highway grid
        // Horizon separator line
        drawLine(
            color = Color(0xFFFF0D87),
            start = Offset(0f, vPointY),
            end = Offset(width, vPointY),
            strokeWidth = 3f
        )

        // 3D Perspective Lines going from horizon to bottom edges
        val perspectiveLineCount = 14
        for (p in 0..perspectiveLineCount) {
            val bottomGridX = (width / perspectiveLineCount) * p
            drawLine(
                color = TikRoxAccentCyan.copy(alpha = 0.5f),
                start = Offset(vPointX, vPointY),
                end = Offset(bottomGridX, height),
                strokeWidth = 2.5f
            )
        }

        // Infinite horizontal lines with perspective logarithmic steps
        val horizontalBarCount = 8
        for (h in 0 until horizontalBarCount) {
            // Progression with infinite transition ticker offset
            val itemProgress = (h.toFloat() + runProgress) / horizontalBarCount.toFloat()
            // Square mathematical spacing to simulate 3D scrolling speed
            val yProgress = itemProgress * itemProgress
            val currentGridY = vPointY + (height - vPointY) * yProgress

            val gridAlpha = (yProgress * 0.7f).coerceIn(0.1f, 1.0f)
            drawLine(
                color = Color(0xFFFF0D87).copy(alpha = gridAlpha),
                start = Offset(0f, currentGridY),
                end = Offset(width, currentGridY),
                strokeWidth = 2f + (yProgress * 4f)
            )
        }
    }
}
