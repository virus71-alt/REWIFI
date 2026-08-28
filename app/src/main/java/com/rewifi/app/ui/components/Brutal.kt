package com.rewifi.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Snow

private val Radius = 18.dp
private const val BORDER = 3f

/** Card with a hard (non-blur) offset drop shadow — the brutalist signature. */
@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    bg: Color = Snow,
    shadow: Dp = 6.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val s = with(androidx.compose.ui.platform.LocalDensity.current) { shadow.toPx() }
    Box(
        modifier
            .drawBehind {
                drawRoundRectHard(Ink, Offset(s, s), size.width, size.height, Radius.toPx())
            }
            .clip(RoundedCornerShape(Radius))
            .background(bg)
            .border(BorderStroke(BORDER.dp, Ink), RoundedCornerShape(Radius))
            .padding(padding)
    ) { content() }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectHard(
    color: Color, topLeft: Offset, w: Float, h: Float, r: Float
) {
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
    )
}

/** Chunky button that physically "presses" into its shadow on tap. */
@Composable
fun BrutalButton(
    text: String,
    modifier: Modifier = Modifier,
    bg: Color = Ink,
    fg: Color = Snow,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth by animateDpAsState(if (pressed) 0.dp else 5.dp, spring(stiffness = 900f), label = "depth")

    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .offset(5.dp, 5.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Ink)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .offset(5.dp - depth, 5.dp - depth)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .border(BorderStroke(BORDER.dp, Ink), RoundedCornerShape(14.dp))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 22.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = fg, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

