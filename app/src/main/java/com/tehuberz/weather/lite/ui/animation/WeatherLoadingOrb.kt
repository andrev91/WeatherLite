package com.tehuberz.weather.lite.ui.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tehuberz.weather.lite.ui.theme.AdventureTheme

@Composable
fun WeatherLoadingOrb(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WeatherOrbTransition")

    val orbScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "OrbScale",
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "HaloAlpha",
    )

    val cloudOffsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "CloudFloatY",
    )

    val cloudOffsetX by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "CloudFloatX",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier =
                    Modifier
                        .size(80.dp)
                        .graphicsLayer(
                            scaleX = orbScale,
                            scaleY = orbScale,
                        ),
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension / 2f
                val innerRadius = outerRadius * 0.55f

                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFFFFD54F).copy(alpha = haloAlpha),
                                    Color(0xFFFF9800).copy(alpha = haloAlpha * 0.5f),
                                    Color.Transparent,
                                ),
                            center = center,
                            radius = outerRadius,
                        ),
                    radius = outerRadius,
                    center = center,
                )

                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFFFFF176),
                                    Color(0xFFFFB300),
                                ),
                            center = center,
                            radius = innerRadius,
                        ),
                    radius = innerRadius,
                    center = center,
                )
            }

            Icon(
                imageVector = Icons.Filled.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier =
                    Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            translationX = (cloudOffsetX + 6).dp.toPx()
                            translationY = (cloudOffsetY + 6).dp.toPx()
                        },
            )
        }

        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherLoadingOrbPreview() {
    AdventureTheme {
        WeatherLoadingOrb(message = "Loading…")
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherLoadingOrbPreviewDark() {
    AdventureTheme(darkTheme = true) {
        WeatherLoadingOrb(message = "Loading…")
    }
}
