package com.secure.textpad.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidGlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    animateBackdrop: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme(
            primary = Color(0xFF8AD7C0), onPrimary = Color(0xFF00382B),
            primaryContainer = Color(0xFF175040), onPrimaryContainer = Color(0xFFC8F5E3),
            background = Color(0xFF101514), onBackground = Color(0xFFE4EAE6),
            surface = Color(0xFF141A18), onSurface = Color(0xFFE4EAE6),
            surfaceVariant = Color(0xFF3D4943), onSurfaceVariant = Color(0xFFC0CCC4)
        )
        else -> lightColorScheme(
            primary = Color(0xFF006B53), onPrimary = Color.White,
            primaryContainer = Color(0xFFA3F2D3), onPrimaryContainer = Color(0xFF002117),
            background = Color(0xFFF5F8F6), onBackground = Color(0xFF17201B),
            surface = Color(0xFFF9FCF9), onSurface = Color(0xFF17201B),
            surfaceVariant = Color(0xFFDDE6DF), onSurfaceVariant = Color(0xFF414B44)
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        context.activity()?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colors) {
        // MaterialTheme alone does not establish LocalContentColor. Surface supplies it
        // for bare Text/Icon composables on every route, including transparent Scaffolds.
        Surface(Modifier.fillMaxSize(), color = colors.background, contentColor = colors.onSurface) {
            Box(Modifier.fillMaxSize()) {
                MeshBackdrop(animateBackdrop)
                content()
            }
        }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext !== this) baseContext.activity() else null
    else -> null
}

@Composable
private fun MeshBackdrop(animate: Boolean) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val motion = remember(context, animate) {
        animate && Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val phase: State<Float> = if (motion) {
        rememberInfiniteTransition(label = "mesh").animateFloat(
            0f, 6.283185f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "phase"
        )
    } else remember { mutableFloatStateOf(0f) }
    val effect = remember {
        if (Build.VERSION.SDK_INT >= 31)
            RenderEffect.createBlurEffect(48f, 48f, Shader.TileMode.CLAMP).asComposeRenderEffect()
        else null
    }
    val tints = listOf(colors.primary, colors.secondary, colors.primaryContainer)
    // The animation state is read during drawing, avoiding screen-wide recompositions.
    // This canvas is a sibling of all foreground content; only its layer receives blur.
    Canvas(Modifier.fillMaxSize().graphicsLayer { renderEffect = effect }) {
        val radius = size.maxDimension * 0.65f
        tints.forEachIndexed { i, tint ->
            val angle = phase.value + i * 2.094f
            val center = Offset(size.width * (0.5f + cos(angle) * 0.4f), size.height * (0.45f + sin(angle) * 0.4f))
            drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.09f), Color.Transparent), center, radius), radius, center)
        }
    }
}

fun Modifier.liquidGlass() = liquidGlassCard()

fun Modifier.liquidGlassCard(): Modifier = composed {
    val shape = RoundedCornerShape(22.dp)
    clip(shape)
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
        .drawBehind {
            // Highlights stay behind the text, never painted over its antialiased edges.
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.025f), Color.Transparent)))
        }
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.06f))), shape)
}
