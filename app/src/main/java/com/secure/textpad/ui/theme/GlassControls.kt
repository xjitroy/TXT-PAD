package com.secure.textpad.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Per-control feedback; it never records or blurs foreground content. */
fun Modifier.glassPress(source: MutableInteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val motion = LocalGlassMotionEnabled.current
    val amount = animateFloatAsState(if (pressed) 1f else 0f,
        animationSpec = if (motion) spring(dampingRatio = 0.72f, stiffness = 600f) else snap(), label = "button pressure")
    val accent = MaterialTheme.colorScheme.primary
    graphicsLayer {
        val pressure = if (motion) amount.value else 0f
        scaleX = 1f - pressure * 0.035f
        scaleY = 1f - pressure * 0.035f
        translationY = pressure * 0.5.dp.toPx()
    }.drawWithCache {
        onDrawBehind {
            val pressure = amount.value
            if (pressure > 0.001f) drawRoundRect(
                Brush.radialGradient(listOf(accent.copy(alpha = 0.20f * pressure), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.35f), radius = maxOf(size.width, size.height)),
                cornerRadius = CornerRadius(22.dp.toPx()))
            if (focused) drawRoundRect(accent, cornerRadius = CornerRadius(22.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
fun GlassButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    Button(onClick, modifier.heightIn(min = 48.dp).glassPress(source), enabled = enabled,
        shape = RoundedCornerShape(18.dp), interactionSource = source, content = content)
}

@Composable
fun GlassTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    TextButton(onClick, modifier.heightIn(min = 48.dp).glassPress(source), enabled = enabled,
        shape = RoundedCornerShape(18.dp), interactionSource = source,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), content = content)
}

@Composable
fun GlassOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    val source = remember { MutableInteractionSource() }
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp).glassPress(source), enabled = enabled,
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = source, content = content)
}

@Composable
fun GlassTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    val source = remember { MutableInteractionSource() }
    FilledTonalButton(onClick, modifier.heightIn(min = 48.dp).glassPress(source), enabled = enabled,
        shape = RoundedCornerShape(18.dp), interactionSource = source, content = content)
}

@Composable
fun GlassIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    IconButton(onClick, modifier.glassPress(source), enabled = enabled, interactionSource = source, content = content)
}

@Composable
fun GlassFilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    FilterChip(selected, onClick, label, modifier.glassPress(source), interactionSource = source)
}
