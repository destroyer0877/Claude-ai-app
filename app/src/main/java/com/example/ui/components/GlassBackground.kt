package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.CrimsonDarkBg
import com.example.ui.theme.CrimsonMidBg
import kotlin.random.Random

data class StarSpec(val xFraction: Float, val yFraction: Float, val radius: Float, val alpha: Float)

@Composable
fun GlassBackground(
    isDarkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val stars = remember {
        val random = Random(42)
        List(45) {
            StarSpec(
                xFraction = random.nextFloat(),
                yFraction = random.nextFloat(),
                radius = random.nextFloat() * 1.5f + 0.8f,
                alpha = random.nextFloat() * 0.7f + 0.3f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) CrimsonDarkBg else Color(0xFFFAF5F6))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDarkMode) {
                // Luxury Crimson Bloom Wave & Dark Vignette
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0307),
                            Color(0xFF1F0611),
                            Color(0xFF0A0205)
                        )
                    )
                )

                // Organic Petal Waves (Windows 11 bloom inspiration)
                val wavePath = Path().apply {
                    moveTo(width * 0.2f, 0f)
                    cubicTo(
                        width * 0.9f, height * 0.15f,
                        width * 0.1f, height * 0.45f,
                        width * 0.85f, height * 0.65f
                    )
                    cubicTo(
                        width * 1.1f, height * 0.75f,
                        width * 0.4f, height * 0.9f,
                        width * 0.6f, height
                    )
                    lineTo(width, height)
                    lineTo(width, 0f)
                    close()
                }

                drawPath(
                    path = wavePath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x55E61E53 * pulseGlow.toInt()),
                            Color(0x338A0D2F),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.7f, height * 0.4f),
                        radius = width * 0.9f
                    )
                )

                // Secondary soft curved wave
                val wave2 = Path().apply {
                    moveTo(0f, height * 0.35f)
                    cubicTo(
                        width * 0.45f, height * 0.45f,
                        width * 0.75f, height * 0.30f,
                        width, height * 0.55f
                    )
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = wave2,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x2E700824),
                            Color(0x1A400414),
                            Color.Transparent
                        ),
                        start = Offset(0f, height * 0.4f),
                        end = Offset(width, height)
                    )
                )

                // Star specks in dark mode as requested in prompt!
                stars.forEach { star ->
                    drawCircle(
                        color = Color.White.copy(alpha = star.alpha * pulseGlow),
                        radius = star.radius,
                        center = Offset(width * star.xFraction, height * star.yFraction)
                    )
                }

                // Dark glass overlay for contrast
                drawRect(
                    color = Color(0x6608080A)
                )
            } else {
                // Day Mode: Frosted luminous warm-pearl light gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF7F9),
                            Color(0xFFFDECEF),
                            Color(0xFFF9E4E8)
                        )
                    )
                )

                // Soft pastel crimson light bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x26FF2D55),
                            Color(0x0DFF5E7E),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.8f, height * 0.25f),
                        radius = width * 0.85f
                    )
                )
            }
        }

        content()
    }
}
