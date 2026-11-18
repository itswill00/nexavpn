package com.nexavpn.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.nexavpn.app.vpn.TunnelState

/**
 * Large circular power-button used on the dashboard, Proton-style:
 * gradient fill when connected, outlined when not, animated pulse rings
 * while connecting / connected.
 */
@Composable
fun ConnectButton(
    state: TunnelState,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
) {
    val connected = state == TunnelState.CONNECTED
    val connecting = state == TunnelState.CONNECTING

    val pulse = rememberInfiniteTransition(label = "pulse")
    val ringProgress by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring",
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "ringAlpha",
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.96f,
        label = "scale",
    )

    val fillTop by animateColorAsState(
        targetValue = when {
            connected -> NexaConnectColors.ConnectedTop
            connecting -> NexaConnectColors.ConnectingTop
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "fillTop",
    )
    val fillBottom by animateColorAsState(
        targetValue = when {
            connected -> NexaConnectColors.ConnectedBottom
            connecting -> NexaConnectColors.ConnectingBottom
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "fillBottom",
    )
    val accent = if (connected || connecting) NexaConnectColors.RingActive
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .size(size)
            .scale(buttonScale)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Pulse rings (connecting / connected only)
        if (connected || connecting) {
            Canvas(Modifier.size(size)) {
                val strokeWidth = 6.dp.toPx()
                drawArc(
                    color = accent.copy(alpha = ringAlpha.coerceIn(0f, 1f)),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        // Main disc with gradient + inner stroke
        Canvas(Modifier.size(size)) {
            val stroke = 3.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(fillTop, fillBottom),
                    center = Offset(size.toPx() / 2f, size.toPx() / 2f),
                    radius = size.toPx() / 1.6f,
                ),
                radius = this.size.minDimension / 2f - stroke,
            )
            drawCircle(
                color = accent,
                radius = this.size.minDimension / 2f - stroke / 2,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        // Power glyph
        PowerGlyph(
            tint = if (connected || connecting) Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size / 3),
        )
    }
}

private object NexaConnectColors {
    val ConnectedTop = Color(0xFF3AB99A)
    val ConnectedBottom = Color(0xFF1F8F76)
    val ConnectingTop = Color(0xFF8A6FFF)
    val ConnectingBottom = Color(0xFF5D3FEF)
    val RingActive = Color(0xFF6D4AFF)
}

/** Minimal power symbol drawn with arcs/lines (no icon-font dependency). */
@Composable
private fun PowerGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = this.size.minDimension * 0.09f, cap = StrokeCap.Round)
        val cx = this.size.width / 2f
        val r = this.size.minDimension * 0.32f

        // Vertical line of the power symbol
        drawLine(
            color = tint,
            start = Offset(cx, this.size.height * 0.18f),
            end = Offset(cx, this.size.height * 0.48f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )

        // Arc (open at top)
        drawArc(
            color = tint,
            startAngle = -80f,
            sweepAngle = 340f,
            useCenter = false,
            topLeft = Offset(cx - r, this.size.height / 2f - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = stroke,
        )
    }
}
