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
 * The fade therefore shares the header's z-order and the list's overscroll stretch.
 */
internal fun Modifier.topFadeBehindStickyHeader(
    listState: LazyListState,
    backgroundColor: Color,
    fadeStartOffsetPx: Float
): Modifier = drawWithContent {
    drawContent()

    val stickyHeader = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 1 }
    if (stickyHeader != null) {
        val fadeTop = size.height
        val solidHeight = 16.dp.toPx()
        val fadeHeight = 72.dp.toPx()
        val fadeAlpha = ((fadeStartOffsetPx - stickyHeader.offset).toFloat() / fadeStartOffsetPx)
            .coerceIn(0f, 1f)

        // A short opaque lip prevents a card edge from showing through at the header boundary.
        drawRect(
            color = backgroundColor,
            topLeft = Offset(0f, fadeTop),
            size = Size(size.width, solidHeight)
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to backgroundColor.copy(alpha = fadeAlpha),
                0.65f to backgroundColor.copy(alpha = fadeAlpha * 0.96f),
                1f to backgroundColor.copy(alpha = 0f),
                startY = fadeTop + solidHeight,
                endY = fadeTop + fadeHeight
            ),
            topLeft = Offset(0f, fadeTop + solidHeight),
            size = Size(size.width, fadeHeight - solidHeight)
        )
    }
}
