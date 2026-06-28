package com.namaaztracker.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.namaaztracker.domain.model.Posture

private val COLOR_DETECTED = Color(0xFF4CAF50)
private val COLOR_UNKNOWN = Color(0x88FFFFFF)

private fun arrowFor(posture: Posture): String = when (posture) {
    Posture.QIAM -> "↑"
    Posture.RUKOOH -> "↘"
    Posture.SAJDAH -> "↓"
    Posture.JULSA -> "↗"
    Posture.UNKNOWN -> "○"
}

@Composable
fun PostureIndicator(posture: Posture, modifier: Modifier = Modifier) {
    val isDetected = posture != Posture.UNKNOWN

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(if (isDetected) Color(0x3D4CAF50) else Color(0x18FFFFFF)),
        ) {
            AnimatedContent(
                targetState = posture,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.55f, animationSpec = tween(220))) togetherWith
                        (fadeOut(tween(160)) + scaleOut(targetScale = 0.55f, animationSpec = tween(160)))
                },
                label = "posture_arrow",
            ) { p ->
                Text(
                    text = arrowFor(p),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (p != Posture.UNKNOWN) COLOR_DETECTED else COLOR_UNKNOWN,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        AnimatedContent(
            targetState = posture,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "posture_label",
        ) { p ->
            Text(
                text = if (p == Posture.UNKNOWN) "DETECTING  ···" else p.displayName.uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (p != Posture.UNKNOWN) COLOR_DETECTED else COLOR_UNKNOWN,
                letterSpacing = 3.sp,
            )
        }
    }
}
