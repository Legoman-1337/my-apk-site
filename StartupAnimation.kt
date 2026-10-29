package ru.momnotes.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay

internal val StartupAnimationBackground = Color(0xFF0D1110)

@Composable
internal fun StartupAnimation(
    accent: Color,
    onFinished: () -> Unit
) {
    val firstLine = remember { Animatable(0f) }
    val secondLine = remember { Animatable(0f) }
    val appIcon = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(100)
        firstLine.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        delay(110)
        secondLine.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        delay(110)
        appIcon.animateTo(1f, tween(430, easing = FastOutSlowInEasing))
        delay(260)
        appIcon.animateTo(0f, tween(330, easing = FastOutSlowInEasing))
        delay(90)
        secondLine.animateTo(0f, tween(330, easing = FastOutSlowInEasing))
        delay(80)
        firstLine.animateTo(0f, tween(330, easing = FastOutSlowInEasing))
        delay(120)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(StartupAnimationBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                "Удобные",
                modifier = Modifier.graphicsLayer {
                    alpha = firstLine.value
                    translationY = (1f - firstLine.value) * 12.dp.toPx()
                },
                color = Color(0xFFF1F4F1),
                fontSize = 31.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "заметки",
                modifier = Modifier.graphicsLayer {
                    alpha = secondLine.value
                    translationY = (1f - secondLine.value) * 12.dp.toPx()
                },
                color = accent,
                fontSize = 31.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        alpha = appIcon.value
                        scaleX = 0.82f + 0.18f * appIcon.value
                        scaleY = 0.82f + 0.18f * appIcon.value
                    }
                    .clip(RoundedCornerShape(15.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer {
                            scaleX = 1.44f
                            scaleY = 1.44f
                        }
                )
            }
        }
    }
}
