package com.namaaztracker.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.namaaztracker.domain.model.Posture

private fun postureColor(posture: Posture): Color = when (posture) {
    Posture.QIAM -> Color(0xFF4CAF50)
    Posture.RUKOOH -> Color(0xFFFF9800)
    Posture.SAJDAH -> Color(0xFFF44336)
    Posture.JULSA -> Color(0xFF2196F3)
    Posture.UNKNOWN -> Color(0xFF616161)
}

@Composable
fun PostureBadge(posture: Posture, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = postureColor(posture),
        animationSpec = tween(300),
        label = "posture_color",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = posture.displayName,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
