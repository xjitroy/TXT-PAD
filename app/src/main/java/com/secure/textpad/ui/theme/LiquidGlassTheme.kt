package com.secure.textpad.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidGlassTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF80D5C0), secondary = Color(0xFFB2CCBE))
        else -> lightColorScheme(primary = Color(0xFF006B58), secondary = Color(0xFF48665B))
    }
    MaterialTheme(colorScheme = colors) {
        Box(Modifier.fillMaxSize().background(colors.background)) {
            MeshBackdrop()
            content()
        }
    }
}

@Composable
private fun MeshBackdrop() {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val motion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val phase = if (motion) {
        val transition = rememberInfiniteTransition(label = "mesh")
        transition.animateFloat(0f, 6.283185f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "phase").value
    } else 0f
    val effect = remember {
        if (Build.VERSION.SDK_INT >= 31) RenderEffect.createBlurEffect(55f, 55f, Shader.TileMode.CLAMP).asComposeRenderEffect() else null
    }
    // Only this background graphics layer is blurred. No text is inside it.
    Canvas(Modifier.fillMaxSize().graphicsLayer { renderEffect = effect }) {
        val radius = size.maxDimension * 0.7f
        listOf(colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer).forEachIndexed { i, color ->
            val angle = phase + i * 2.094f
            val center = Offset(size.width * (0.5f + cos(angle) * 0.4f), size.height * (0.5f + sin(angle) * 0.35f))
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.64f), color.copy(alpha = 0f)), center, radius), radius, center)
        }
    }
}

fun Modifier.liquidGlass() = liquidGlassCard()

fun Modifier.liquidGlassCard(): Modifier = composed {
    val shape = RoundedCornerShape(24.dp)
    val surface = MaterialTheme.colorScheme.surface
    clip(shape)
        .background(surface.copy(alpha = 0.72f))
        .drawWithContent {
            drawContent()
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)), size = size)
        }
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.06f))), shape)
}
