package com.secure.textpad.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private class GlassScene(
    val palette: ColorScheme,
    val dark: Boolean,
    val motion: Boolean,
    val phase: State<Float>
) {
    val dimensions = mutableStateOf(IntSize.Zero)
    val mesh = MeshPainter(palette, dark)
}
private val LocalGlassScene = staticCompositionLocalOf<GlassScene?> { null }
val LocalGlassMotionEnabled = staticCompositionLocalOf { false }
enum class GlassStyle { Panel, Writing }

@Composable
private fun motionAllowed(requested: Boolean): Boolean {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun animationScale() = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    var scale by remember { mutableFloatStateOf(animationScale()) }
    var started by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(context, lifecycle) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { scale = animationScale() }
        }
        val stateObserver = LifecycleEventObserver { _, _ ->
            started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            scale = animationScale()
        }
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        lifecycle.addObserver(stateObserver)
        onDispose { context.contentResolver.unregisterContentObserver(observer); lifecycle.removeObserver(stateObserver) }
    }
    return requested && scale > 0f && started
}

@Composable
fun LiquidGlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    animateBackdrop: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = remember(context, darkTheme, dynamicColor) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= 31 && darkTheme -> dynamicDarkColorScheme(context)
            dynamicColor && Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
            darkTheme -> darkColorScheme(
                primary = Color(0xFF8AD7C0), onPrimary = Color(0xFF00382B),
                primaryContainer = Color(0xFF175040), onPrimaryContainer = Color(0xFFC8F5E3),
                secondary = Color(0xFFB5D3C4), secondaryContainer = Color(0xFF304E43), onSecondaryContainer = Color(0xFFD0ECDD),
                background = Color(0xFF101514), onBackground = Color(0xFFE4EAE6),
                surface = Color(0xFF141A18), onSurface = Color(0xFFE4EAE6),
                surfaceVariant = Color(0xFF2B3932), onSurfaceVariant = Color(0xFFBFCDC5),
                outline = Color(0xFF83988D), outlineVariant = Color(0xFF3F5147),
                tertiary = Color(0xFF91C8DB), onTertiary = Color(0xFF003544)
            )
            else -> lightColorScheme(
                primary = Color(0xFF006B53), onPrimary = Color.White,
                primaryContainer = Color(0xFFA3F2D3), onPrimaryContainer = Color(0xFF002117),
                secondary = Color(0xFF48665B), secondaryContainer = Color(0xFFCEEADD), onSecondaryContainer = Color(0xFF102C20),
                background = Color(0xFFF5F8F6), onBackground = Color(0xFF17201B),
                surface = Color(0xFFF9FCF9), onSurface = Color(0xFF17201B),
                surfaceVariant = Color(0xFFDDE6DF), onSurfaceVariant = Color(0xFF414B44),
                outline = Color(0xFF687C70), outlineVariant = Color(0xFFBCCDC2),
                tertiary = Color(0xFF23677C), onTertiary = Color.White
            )
        }
    }
    val motion = motionAllowed(animateBackdrop)
    val phase: State<Float> = if (motion) {
        rememberInfiniteTransition(label = "liquid light").animateFloat(
            0f, 6.283185f, infiniteRepeatable(tween(22000, easing = LinearEasing)), label = "reflections"
        )
    } else remember { mutableFloatStateOf(0.7f) }
    val scene = remember(colors, darkTheme, motion, phase) { GlassScene(colors, darkTheme, motion, phase) }
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
        CompositionLocalProvider(LocalGlassScene provides scene, LocalGlassMotionEnabled provides motion) {
            Surface(Modifier.fillMaxSize(), color = colors.background, contentColor = colors.onSurface) {
                Box(Modifier.fillMaxSize().onSizeChanged { scene.dimensions.value = it }) {
                    MeshBackdrop(scene)
                    content()
                }
            }
        }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext !== this) baseContext.activity() else null
    else -> null
}

/** One procedural scene, shared by the screen and every glass pane, in screen coordinates. */
private class MeshPainter(private val colors: ColorScheme, private val dark: Boolean) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ribbon = Path()
    fun draw(canvas: android.graphics.Canvas, width: Float, height: Float, phase: Float) {
        if (width <= 0 || height <= 0) return
        canvas.drawColor(colors.background.toArgb())
        val radius = maxOf(width, height) * 0.55f
        val tints = listOf(colors.primary, colors.tertiary, colors.secondary)
        tints.forEachIndexed { index, tint ->
            val angle = phase + index * 2.094f
            val x = width * (0.5f + cos(angle) * 0.47f)
            val y = height * (0.47f + sin(angle) * 0.43f)
            paint.color = android.graphics.Color.WHITE
            paint.shader = RadialGradient(x, y, radius,
                intArrayOf(tint.copy(alpha = if (dark) 0.30f else 0.16f).toArgb(), tint.copy(alpha = 0f).toArgb()),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, radius, paint)
        }
        // Broad ribbons give the frosted panels something visible to diffuse and refract.
        ribbon.reset()
        val drift = sin(phase) * height * 0.12f
        ribbon.moveTo(-width * 0.2f, height * 0.23f + drift)
        ribbon.cubicTo(width * 0.95f, -height * 0.07f, width * 0.05f, height * 0.92f, width * 1.2f, height * 0.68f - drift)
        paint.shader = null
        paint.color = colors.primary.copy(alpha = if (dark) 0.21f else 0.13f).toArgb()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * 0.12f
        canvas.drawPath(ribbon, paint)
        ribbon.reset()
        val secondDrift = cos(phase) * height * 0.10f
        ribbon.moveTo(-width * 0.15f, height * 0.70f - secondDrift)
        ribbon.cubicTo(width * 0.35f, height * 1.02f, width * 0.55f, height * 0.05f, width * 1.15f, height * 0.26f + secondDrift)
        paint.color = colors.tertiary.copy(alpha = if (dark) 0.13f else 0.08f).toArgb()
        paint.strokeWidth = width * 0.08f
        canvas.drawPath(ribbon, paint)
        paint.style = Paint.Style.FILL
    }
}

@Composable
private fun MeshBackdrop(scene: GlassScene) {
    val density = LocalDensity.current
    val effect = remember(density) {
        if (Build.VERSION.SDK_INT >= 31)
            RenderEffect.createBlurEffect(with(density) { 12.dp.toPx() }, with(density) { 12.dp.toPx() }, Shader.TileMode.CLAMP).asComposeRenderEffect()
        else null
    }
    Canvas(Modifier.fillMaxSize().graphicsLayer { renderEffect = effect }) {
        drawIntoCanvas { scene.mesh.draw(it.nativeCanvas, size.width, size.height, scene.phase.value) }
    }
}

private interface BackdropLayer {
    fun draw(canvas: android.graphics.Canvas, size: Size, origin: Offset, scene: GlassScene, phase: Float, press: Float)
    fun release()
}

/** This RenderNode records only background pixels. Foreground text never enters the blur node. */
@RequiresApi(31)
private class FrostedBackdrop(private val blurRadius: Float) : BackdropLayer {
    private val node = RenderNode("TXT background refraction")
    private var blur = -1f
    override fun draw(canvas: android.graphics.Canvas, size: Size, origin: Offset, scene: GlassScene, phase: Float, press: Float) {
        if (!canvas.isHardwareAccelerated || size.width <= 0 || size.height <= 0) return
        val scale = min(0.5f, 800f / maxOf(size.width, size.height)).coerceAtLeast(0.1f)
        val width = ceil(size.width * scale).toInt().coerceAtLeast(1)
        val height = ceil(size.height * scale).toInt().coerceAtLeast(1)
        val radius = blurRadius * scale
        if (radius != blur) { node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)); blur = radius }
        node.setPosition(0, 0, width, height)
        val recording = node.beginRecording(width, height)
        try {
            recording.scale(scale, scale)
            val lens = 1.035f + press * 0.025f
            recording.scale(lens, lens, size.width / 2f, size.height / 2f)
            recording.translate(-origin.x, -origin.y)
            val screen = scene.dimensions.value
            scene.mesh.draw(recording, screen.width.toFloat(), screen.height.toFloat(), phase)
        } finally { node.endRecording() }
        canvas.save()
        canvas.scale(1f / scale, 1f / scale)
        canvas.drawRenderNode(node)
        canvas.restore()
    }
    override fun release() { node.discardDisplayList() }
}

fun Modifier.liquidGlass(interactionSource: MutableInteractionSource? = null, style: GlassStyle = GlassStyle.Panel): Modifier = liquidGlassCard(interactionSource, style)

fun Modifier.liquidGlassCard(interactionSource: MutableInteractionSource? = null, style: GlassStyle = GlassStyle.Panel): Modifier = composed {
    val scene = LocalGlassScene.current
    val colors = MaterialTheme.colorScheme
    val origin = remember { mutableStateOf(Offset.Zero) }
    val pressed = interactionSource?.collectIsPressedAsState()?.value ?: false
    val press = animateFloatAsState(if (pressed) 1f else 0f,
        animationSpec = if (scene?.motion == true) spring(dampingRatio = 0.62f, stiffness = 420f) else snap(), label = "glass pressure")
    val density = LocalDensity.current
    val layer: BackdropLayer? = remember(density) { if (Build.VERSION.SDK_INT >= 31) FrostedBackdrop(with(density) { 24.dp.toPx() }) else null }
    DisposableEffect(layer) { onDispose { layer?.release() } }
    val shape = RoundedCornerShape(26.dp)
    val dark = scene?.dark ?: isSystemInDarkTheme()
    val writing = style == GlassStyle.Writing
    shadow(if (writing) 10.dp else 18.dp, shape, clip = false,
        ambientColor = Color.Black.copy(alpha = if (dark) 0.35f else 0.13f),
        spotColor = Color.Black.copy(alpha = if (dark) 0.48f else 0.20f))
        .onGloballyPositioned { origin.value = it.positionInRoot() }
        .clip(shape)
        .drawWithCache {
            val corner = CornerRadius(26.dp.toPx())
            val inset = 1.5.dp.toPx()
            val stroke = 1.dp.toPx()
            onDrawBehind {
                val phase = scene?.phase?.value ?: 0.7f
                val pressure = press.value
                if (scene != null && layer != null) drawIntoCanvas {
                    layer.draw(it.nativeCanvas, size, origin.value, scene, phase, pressure)
                }
                // The tint guarantees readable text while the moving light remains visible underneath.
                drawRect(colors.surface.copy(alpha = if (writing) 0.96f else if (layer == null) 0.92f else 0.72f))
                val shift = sin(phase) * size.width * 0.25f
                drawRect(Brush.linearGradient(
                    listOf(Color.White.copy(alpha = if (writing) 0.015f else if (dark) 0.085f else 0.32f), Color.White.copy(alpha = 0.015f), Color.Transparent),
                    start = Offset(shift, 0f), end = Offset(size.width + shift, size.height * 0.8f)))
                // A moving diagonal specular reflection and a bright inner rim create the lens edge.
                drawRect(Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = (if (writing) 0.012f else 0.08f) + pressure * 0.12f), Color.Transparent),
                    start = Offset(size.width * (0.35f + sin(phase) * 0.40f), 0f),
                    end = Offset(size.width * (0.8f + sin(phase) * 0.40f), size.height)))
                drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f + pressure * 0.18f), Color.White.copy(alpha = 0.06f))),
                    cornerRadius = corner, style = Stroke(stroke))
                if (size.width > inset * 2 && size.height > inset * 2) drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f + pressure * 0.20f), Color.Transparent, Color.Black.copy(alpha = 0.10f))),
                    topLeft = Offset(inset, inset), size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius((corner.x - inset).coerceAtLeast(0f)), style = Stroke(stroke * 0.65f))
            }
        }
}
