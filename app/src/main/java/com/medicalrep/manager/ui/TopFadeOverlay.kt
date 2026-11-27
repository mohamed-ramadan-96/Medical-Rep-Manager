package com.medicalrep.manager.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Draws below the sticky search header from inside the header's own list item.
 * The fade and the cover strip therefore share the header's z-order and overscroll transform.
 */
internal fun Modifier.topFadeBehindStickyHeader(
    listState: LazyListState,
    backgroundColor: Color,
    fadeStartOffsetPx: Float
): Modifier = drawWithContent {
    drawContent()

    val stickyHeader = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 1 }
    if (stickyHeader != null) {
        val headerTop = stickyHeader.offset.toFloat()
        val fadeAlpha = ((fadeStartOffsetPx - headerTop) / fadeStartOffsetPx)
            .coerceIn(0f, 1f)

        // At rest the title row remains untouched: no cover or fade is drawn at zero alpha.
        if (fadeAlpha > 0f) {
            // In this header item's local coordinates, y = -headerTop maps to viewport y = 0.
            // Paint the exposed gap after the list content so stretched rows cannot show through it.
            if (headerTop > 0f) {
                drawRect(
                    color = backgroundColor.copy(alpha = fadeAlpha),
                    topLeft = Offset(0f, -headerTop),
                    size = Size(size.width, headerTop)
                )
            }

            val solidHeight = 16.dp.toPx()
            val fadeHeight = 72.dp.toPx()

            // The leading lip follows the same fade strength instead of hiding content at rest.
            drawRect(
                color = backgroundColor.copy(alpha = fadeAlpha),
                topLeft = Offset(0f, size.height),
                size = Size(size.width, solidHeight)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    0f to backgroundColor.copy(alpha = fadeAlpha),
                    0.65f to backgroundColor.copy(alpha = fadeAlpha * 0.96f),
                    1f to backgroundColor.copy(alpha = 0f),
                    startY = size.height + solidHeight,
                    endY = size.height + fadeHeight
                ),
                topLeft = Offset(0f, size.height + solidHeight),
                size = Size(size.width, fadeHeight - solidHeight)
            )
        }
    }
}
