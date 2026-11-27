package com.medicalrep.manager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Fixed overlay below the pinned search row, above the list's overscroll transform. */
@Composable
internal fun TopFadeOverlay(alpha: Float, modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .drawBehind {
                val solidHeight = 20.dp.toPx()
                drawRect(color = backgroundColor)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to backgroundColor,
                        0.65f to backgroundColor.copy(alpha = 0.96f),
                        1f to backgroundColor.copy(alpha = 0f),
                        startY = solidHeight,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, solidHeight),
                    size = Size(size.width, size.height - solidHeight)
                )
            }
            .graphicsLayer { this.alpha = alpha }
    )
}
