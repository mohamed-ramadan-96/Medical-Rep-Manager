package com.medicalrep.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AttachmentsButton(
    count: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    val hasAttachments = count > 0
    val primary = MaterialTheme.colorScheme.primary
    val icon = remember(hasAttachments) {
        attachmentPaperclipIcon(if (hasAttachments) 2.8f else 1.6f)
    }

    IconButton(
        modifier = Modifier.size(32.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.size(32.dp)) {
            if (hasAttachments) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(primary.copy(alpha = 0.16f), CircleShape)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (hasAttachments) primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.Center).size(18.dp)
            )
            if (hasAttachments) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-1).dp, y = (-1).dp)
                        .defaultMinSize(minWidth = 16.dp)
                        .height(16.dp)
                        .background(primary, CircleShape)
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 9) "9+" else count.toString(),
                        color = MaterialTheme.colorScheme.surface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}

private fun attachmentPaperclipIcon(strokeWidth: Float): ImageVector =
    ImageVector.Builder(
        name = "AttachmentPaperclip",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-1.38 1.12-2.5 2.5-2.5s2.5 1.12 2.5 2.5v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6.5"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
