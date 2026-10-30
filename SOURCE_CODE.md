# TXT — nine requested source files in order

Full configuration, resources, tests and release instructions are in the repository.

## 1. app/build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.secure.textpad"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.secure.textpad"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    val signingPath = providers.environmentVariable("TXT_SIGNING_STORE").orNull
    if (signingPath != null) {
        signingConfigs.create("production") {
            storeFile = file(signingPath)
            storePassword = providers.environmentVariable("TXT_STORE_PASSWORD").get()
            keyAlias = "txt-release"
            keyPassword = providers.environmentVariable("TXT_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        release {
            if (signingPath != null) signingConfig = signingConfigs.getByName("production")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

## 2. app/src/main/AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <uses-permission android:name="android.permission.USE_FINGERPRINT" android:maxSdkVersion="27" />
    <application
        android:allowBackup="false"
        android:fullBackupContent="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:label="TXT"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.TXT">
        <activity android:name=".MainActivity" android:exported="true"
            android:launchMode="singleTop" android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <action android:name="android.intent.action.EDIT" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:mimeType="text/*" />
                <data android:mimeType="text/plain" />
                <data android:mimeType="application/json" />
                <data android:mimeType="application/xml" />
                <data android:mimeType="application/javascript" />
                <data android:mimeType="application/x-httpd-php" />
                <data android:mimeType="application/vnd.openxmlformats-officedocument.wordprocessingml.document" />
                <data android:mimeType="application/octet-stream" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <action android:name="android.intent.action.EDIT" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="content" />
                <data android:scheme="file" />
                <data android:host="*" />
                <data android:pathPattern=".*\\.txt" />
                <data android:pathPattern=".*\\.docx" />
                <data android:pathPattern=".*\\.stxt" />
                <data android:pathPattern=".*\\.md" />
                <data android:pathPattern=".*\\.tpbak" />
                <data android:pathPattern=".*\\.enc" />
                <data android:pathPattern=".*\\.log" />
                <data android:pathPattern=".*\\.json" />
                <data android:pathPattern=".*\\.xml" />
                <data android:pathPattern=".*\\.csv" />
                <data android:pathPattern=".*\\.html" />
                <data android:pathPattern=".*\\.php" />
                <data android:pathPattern=".*\\.css" />
                <data android:pathPattern=".*\\.js" />
                <data android:pathPattern=".*\\.kt" />
                <data android:pathPattern=".*\\.py" />
                <data android:pathPattern=".*\\.ini" />
                <data android:pathPattern=".*\\.cfg" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

## 3. app/src/main/java/com/secure/textpad/ui/theme/LiquidGlassTheme.kt

```kotlin
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
```

## 4. app/src/main/java/com/secure/textpad/security/CryptoEngine.kt

```kotlin
package com.secure.textpad.security

import java.io.*
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned formats. Caller owns streams; every supplied password is consumed and erased. */
object CryptoEngine {
    const val ITERATIONS = 600_000
    const val CHUNK_SIZE = 65_536
    private val random = SecureRandom()
    private val magic = byteArrayOf(83, 84, 88, 84)
    private val backupMagic = byteArrayOf(84, 80, 66, 75)

    fun isEncrypted(prefix: ByteArray) = prefix.size >= 4 && (0..3).all { prefix[it] == magic[it] }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword(); password.fill('\u0000') }
    }

    private fun header(backup: Boolean): ByteArray =
        (if (backup) backupMagic else magic) + byteArrayOf(1) +
            ByteArray(16).also(random::nextBytes) + ByteArray(12).also(random::nextBytes)

    private fun readHeader(input: InputStream, backup: Boolean): ByteArray {
        val h = ByteArray(33)
        DataInputStream(input).readFully(h)
        require(h.copyOfRange(0, 4).contentEquals(if (backup) backupMagic else magic) && h[4] == 1.toByte()) {
            "Unsupported or damaged ${if (backup) "backup" else "encrypted file"}."
        }
        return h
    }

    private fun cipher(mode: Int, key: ByteArray, iv: ByteArray, aad: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            updateAAD(aad)
        }

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        var key: ByteArray? = null
        return try {
            val h = header(false)
            key = derive(password, h.copyOfRange(5, 21))
            h + cipher(Cipher.ENCRYPT_MODE, key, h.copyOfRange(21, 33), h).doFinal(plain)
        } finally { key?.fill(0); password.fill('\u0000') }
    }

    fun decrypt(encoded: ByteArray, password: CharArray): ByteArray {
        var key: ByteArray? = null
        return try {
            require(encoded.size >= 49) { "Truncated encrypted file." }
            val h = readHeader(ByteArrayInputStream(encoded), false)
            key = derive(password, h.copyOfRange(5, 21))
            cipher(Cipher.DECRYPT_MODE, key, h.copyOfRange(21, 33), h).doFinal(encoded, 33, encoded.size - 33)
        } catch (e: GeneralSecurityException) {
            throw IOException("Wrong password or damaged encrypted file.", e)
        } finally { key?.fill(0); password.fill('\u0000') }
    }

    private fun nonce(h: ByteArray, counter: Long, final: Boolean): ByteArray {
        require(counter >= 0 && counter < Long.MAX_VALUE) { "Backup is too large." }
        return h.copyOfRange(21, 33).apply {
            if (final) this[0] = (this[0].toInt() xor 0x80).toByte()
            for (i in 0..7) this[4 + i] = (this[4 + i].toInt() xor (counter ushr (56 - i * 8)).toInt()).toByte()
        }
    }

    private fun aad(h: ByteArray, counter: Long, final: Boolean, length: Int): ByteArray =
        h + ByteBuffer.allocate(13).putLong(counter).put(if (final) 1.toByte() else 0.toByte()).putInt(length).array()

    /** Closes the encrypted wrapper even when the ZIP producer fails; caller deletes incomplete output. */
    fun writeBackup(output: OutputStream, password: CharArray, producer: (OutputStream) -> Unit) {
        var key: ByteArray? = null
        try {
            val h = header(true)
            key = derive(password, h.copyOfRange(5, 21))
            output.write(h)
            ChunkOutput(output, h, key).use(producer)
        } finally { key?.fill(0); password.fill('\u0000') }
    }

    /** The consumer must finish successfully AND the final authenticated frame must validate. */
    fun <T> readBackup(input: InputStream, password: CharArray, consumer: (InputStream) -> T): T {
        var key: ByteArray? = null
        try {
            val h = readHeader(input, true)
            key = derive(password, h.copyOfRange(5, 21))
            val stream = ChunkInput(input, h, key)
            return try {
                val value = consumer(stream)
                val drain = ByteArray(CHUNK_SIZE)
                try { while (stream.read(drain) != -1) Unit } finally { drain.fill(0) }
                value
            } finally { stream.erase() }
        } catch (e: GeneralSecurityException) {
            throw IOException("Wrong backup password or damaged backup.", e)
        } finally { key?.fill(0); password.fill('\u0000') }
    }

    private class ChunkOutput(val target: OutputStream, val h: ByteArray, val key: ByteArray) : OutputStream() {
        private val buffer = ByteArray(CHUNK_SIZE)
        private var used = 0
        private var counter = 0L
        private var closed = false
        override fun write(b: Int) { write(byteArrayOf(b.toByte()), 0, 1) }
        override fun write(b: ByteArray, off: Int, len: Int) {
            check(!closed)
            require(off >= 0 && len >= 0 && off <= b.size - len)
            var pos = off
            var left = len
            while (left > 0) {
                val n = minOf(left, CHUNK_SIZE - used)
                b.copyInto(buffer, used, pos, pos + n)
                used += n; pos += n; left -= n
                if (used == CHUNK_SIZE) emit(false)
            }
        }
        private fun emit(final: Boolean) {
            val encrypted = cipher(Cipher.ENCRYPT_MODE, key, nonce(h, counter, final), aad(h, counter, final, used)).doFinal(buffer, 0, used)
            try {
                val data = DataOutputStream(target)
                data.writeByte(if (final) 1 else 0)
                data.writeInt(used)
                data.write(encrypted)
                counter++
            } finally { encrypted.fill(0); buffer.fill(0); used = 0 }
        }
        override fun flush() { target.flush() }
        override fun close() {
            if (closed) return
            try { if (used > 0) emit(false); emit(true); target.flush() }
            finally { closed = true; buffer.fill(0) }
        }
    }

    private class ChunkInput(val source: InputStream, val h: ByteArray, val key: ByteArray) : InputStream() {
        private var buffer = ByteArray(0)
        private var position = 0
        private var counter = 0L
        private var ended = false
        private fun load(): Boolean {
            buffer.fill(0)
            if (ended) return false
            val data = DataInputStream(source)
            val flag = data.readUnsignedByte() // EOF here is an error: final frame is mandatory.
            val len = data.readInt()
            require(flag in 0..1 && len in 0..CHUNK_SIZE && (if (flag == 1) len == 0 else len > 0)) { "Invalid backup frame." }
            val encrypted = ByteArray(len + 16)
            data.readFully(encrypted)
            buffer = try { cipher(Cipher.DECRYPT_MODE, key, nonce(h, counter, flag == 1), aad(h, counter, flag == 1, len)).doFinal(encrypted) }
            finally { encrypted.fill(0) }
            counter++; position = 0
            if (flag == 1) {
                ended = true
                require(source.read() == -1) { "Unexpected data after backup." }
                return false
            }
            return true
        }
        override fun read(): Int {
            if (position == buffer.size && !load()) return -1
            return buffer[position++].toInt() and 0xff
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            require(off >= 0 && len >= 0 && off <= b.size - len)
            if (len == 0) return 0
            if (position == buffer.size && !load()) return -1
            val n = minOf(len, buffer.size - position)
            buffer.copyInto(b, off, position, position + n); position += n
            return n
        }
        fun erase() { buffer.fill(0) }
        // ZIP's close must not bypass validation or close the caller's stream.
        override fun close() { }
    }
}
```

## 5. app/src/main/java/com/secure/textpad/data/DocxHandler.kt

```kotlin
package com.secure.textpad.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Plain paragraph editor: exporting DOCX deliberately creates a fresh text-only document. */
object DocxHandler {
    private const val WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    const val MAX_TEXT = 2_000_000

    fun read(input: InputStream): String {
        ZipInputStream(input).use { zip ->
            var count = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++count <= 4096) { "Too many DOCX entries." }
                if (entry.name == "word/document.xml") {
                    val parser = Xml.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                        setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
                        setInput(LimitedInputStream(zip, 16L * 1024 * 1024), "UTF-8")
                    }
                    val text = StringBuilder()
                    var inText = false
                    var firstParagraph = true
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.DOCDECL) error("Document declarations are not permitted.")
                        if (event == XmlPullParser.START_TAG && parser.namespace == WORD) when (parser.name) {
                            "p" -> { if (!firstParagraph) text.append('\n'); firstParagraph = false }
                            "t" -> inText = true
                            "tab" -> text.append('\t')
                            "br", "cr" -> text.append('\n')
                        }
                        if (inText && (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT ||
                                event == XmlPullParser.ENTITY_REF || event == XmlPullParser.IGNORABLE_WHITESPACE)) {
                            text.append(parser.text ?: error("Unresolved XML entity in document text."))
                        }
                        if (event == XmlPullParser.END_TAG && parser.namespace == WORD && parser.name == "t") inText = false
                        require(text.length <= MAX_TEXT) { "Document exceeds the editor's 2 million character limit." }
                        event = parser.nextToken()
                    }
                    return text.toString()
                }
                // Bound skipped entries as well; never trust ZIP's advertised uncompressed sizes.
                val skip = ByteArray(8192)
                var total = 0L
                while (true) { val n = zip.read(skip); if (n < 0) break; total += n; require(total <= 64L * 1024 * 1024) { "DOCX entry too large." } }
                zip.closeEntry()
            }
        }
        error("DOCX has no word/document.xml.")
    }

    fun write(text: String, output: OutputStream) {
        require(text.length <= MAX_TEXT)
        ZipOutputStream(output).use { zip ->
            fun part(name: String, value: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            part("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
            part("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
            zip.putNextEntry(ZipEntry("word/document.xml"))
            val xml = Xml.newSerializer()
            xml.setOutput(zip, "UTF-8"); xml.startDocument("UTF-8", true)
            xml.setPrefix("w", WORD); xml.startTag(WORD, "document"); xml.startTag(WORD, "body")
            text.splitToSequence('\n').forEach { line ->
                xml.startTag(WORD, "p"); xml.startTag(WORD, "r")
                xml.startTag(WORD, "t"); xml.attribute("http://www.w3.org/XML/1998/namespace", "space", "preserve")
                xml.text(line); xml.endTag(WORD, "t"); xml.endTag(WORD, "r"); xml.endTag(WORD, "p")
            }
            xml.endTag(WORD, "body"); xml.endTag(WORD, "document"); xml.endDocument(); xml.flush(); zip.closeEntry()
        }
    }

    private class LimitedInputStream(input: InputStream, val limit: Long) : FilterInputStream(input) {
        private var total = 0L
        override fun read(): Int = super.read().also { if (it >= 0) { total++; require(total <= limit) { "DOCX XML too large." } } }
        override fun read(b: ByteArray, off: Int, len: Int): Int = `in`.read(b, off, len).also {
            if (it > 0) { total += it; require(total <= limit) { "DOCX XML too large." } }
        }
        override fun close() { }
    }
}
```

## 6. app/src/main/java/com/secure/textpad/data/StorageRepository.kt

```kotlin
package com.secure.textpad.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.Os
import com.secure.textpad.security.CryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class PadFile(
    val uri: Uri, val name: String, val vault: Boolean, val size: Long = 0,
    val modified: Long = 0, val path: String = name, val encrypted: Boolean? = null
)
enum class ConflictPolicy { OVERWRITE, KEEP_BOTH, SKIP }
@Serializable data class BackupItem(val entry: String, val path: String, val vault: Boolean, val modified: Long, val size: Long, val sha256: String)
@Serializable data class BackupManifest(val version: Int = 1, val files: List<BackupItem>)
data class RestoreResult(val restored: Int, val skipped: Int)
@Serializable data class TrashItem(val id: String, val name: String, val path: String, val vault: Boolean,
    val modified: Long, val deleted: Long, val size: Long, val sha256: String)

class StorageRepository(private val context: Context) {
    private val resolver = context.contentResolver
    private val prefs = context.getSharedPreferences("txt", Context.MODE_PRIVATE)
    private val vaultRoot = File(context.filesDir, "secure_vault").apply { mkdirs() }
    private val trashRoot = File(context.filesDir, "trash").apply { mkdirs() }
    private val workRoot = File(context.noBackupFilesDir, "operations").apply { mkdirs() }
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = false }
    val workspace: Uri? get() = prefs.getString("tree", null)?.let(Uri::parse)
    var defaultPrivate: Boolean
        get() = prefs.getBoolean("private", false)
        set(value) { prefs.edit().putBoolean("private", value).apply() }

    suspend fun chooseWorkspace(uri: Uri) = withContext(Dispatchers.IO) {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val root = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val files = children(uri, root)
        if (files.none { it.name == ".nomedia" }) {
            val created = DocumentsContract.createDocument(resolver, root, "application/octet-stream", ".nomedia")
                ?: error("Folder provider could not create .nomedia.")
            resolver.openOutputStream(created, "wt")?.use { it.flush() } ?: error("Folder is not writable.")
        }
        prefs.edit().putString("tree", uri.toString()).commit()
    }

    private data class Child(val uri: Uri, val name: String, val size: Long, val time: Long, val directory: Boolean)
    private fun children(tree: Uri, parent: Uri): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE)
        return resolver.query(uri, columns, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    require(size < 10000) { "Folder exceeds 10,000 entries." }
                    add(Child(DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)), cursor.getString(1) ?: "Untitled",
                        cursor.getLong(3), cursor.getLong(2), cursor.getString(4) == DocumentsContract.Document.MIME_TYPE_DIR))
                }
            }
        } ?: error("Folder permission was lost. Select the workspace again.")
    }

    private fun supported(name: String) = name.substringAfterLast('.', "").lowercase() in
        setOf("txt", "md", "log", "json", "xml", "csv", "html", "php", "css", "js", "kt", "py", "ini", "cfg", "docx", "stxt", "enc", "tpbak") || name.substringAfterLast('.', "").lowercase() in prefs.getStringSet("text_extensions", emptySet()).orEmpty()

    suspend fun scan(all: Boolean = false): List<PadFile> = withContext(Dispatchers.IO) { scanNow(all) }
    private fun scanNow(all: Boolean): List<PadFile> {
        val result = mutableListOf<PadFile>()
        fun walkPublic(tree: Uri, parent: Uri, prefix: String, depth: Int) {
            require(depth <= 32) { "Workspace nesting exceeds 32 folders." }
            for (child in children(tree, parent)) {
                validateName(child.name)
                if (child.name == ".nomedia") continue
                val path = prefix + child.name
                if (child.directory) walkPublic(tree, child.uri, "$path/", depth + 1)
                else if (all || supported(child.name)) result.add(PadFile(child.uri, child.name, false, child.size, child.time, path))
                require(result.size <= 10000) { "Too many files." }
            }
        }
        workspace?.let { tree -> walkPublic(tree, DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)), "", 0) }
        vaultRoot.walkTopDown().maxDepth(33).filter { it.isFile }.forEach { f ->
            if (all || supported(f.name)) result.add(PadFile(Uri.fromFile(f), f.name, true, f.length(), f.lastModified(), f.relativeTo(vaultRoot).invariantSeparatorsPath))
            require(result.size <= 10000)
        }
        return result.sortedWith(compareByDescending<PadFile> { it.modified }.thenBy { it.name.lowercase() })
    }

    suspend fun external(uri: Uri): PadFile = withContext(Dispatchers.IO) {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Untitled.txt"
        var size = 0L
        if (uri.scheme == "content") resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use {
            if (it.moveToFirst()) { name = it.getString(0) ?: name; size = it.getLong(1) }
        }
        PadFile(uri, name, false, size)
    }
    private fun input(file: PadFile): InputStream = if (file.vault) privateFile(file.path).inputStream()
        else resolver.openInputStream(file.uri) ?: error("Cannot open ${file.name}.")

    suspend fun detect(file: PadFile): Boolean = withContext(Dispatchers.IO) {
        input(file).use { stream ->
            val h = ByteArray(4); var n = 0
            while (n < 4) { val r = stream.read(h, n, 4 - n); if (r < 0) break; n += r }
            CryptoEngine.isEncrypted(h)
        }
    }

    suspend fun readText(file: PadFile, password: CharArray? = null): String = withContext(Dispatchers.IO) {
        try {
            val raw = input(file).use { limitedBytes(it, 12 * 1024 * 1024) }
            var plain: ByteArray? = null
            try {
                val encoded = CryptoEngine.isEncrypted(raw)
                plain = if (encoded) CryptoEngine.decrypt(raw, password ?: error("Password required.")) else raw
                val docx = plain.size >= 4 && plain[0] == 80.toByte() && plain[1] == 75.toByte()
                if (docx) DocxHandler.read(ByteArrayInputStream(plain))
                else {
                    val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    val text = decoder.decode(java.nio.ByteBuffer.wrap(plain)).toString().removePrefix("\uFEFF")
                    require(text.length <= DocxHandler.MAX_TEXT) { "Editor limit: 2 million characters." }
                    require(!text.contains('\u0000')) { "Binary files cannot be edited as text." }
                    text
                }
            } finally { raw.fill(0); plain?.fill(0) }
        } finally { password?.fill('\u0000') }
    }

    suspend fun create(name: String, private: Boolean): PadFile = withContext(Dispatchers.IO) {
        lock.withLock { createNow(name, private, unique = true) }
    }

    private fun createNow(path: String, private: Boolean, unique: Boolean): PadFile {
        validatePath(path)
        val extension = path.substringAfterLast('.', "").lowercase()
        prefs.edit().putStringSet("text_extensions", prefs.getStringSet("text_extensions", emptySet()).orEmpty() + extension).apply()
        var chosen = path
        var count = 1
        while (find(chosen, private) != null) {
            require(unique) { "File already exists." }
            val extension = path.substringAfterLast('.', "")
            val stem = if (extension.isEmpty()) path else path.dropLast(extension.length + 1)
            chosen = "$stem (${count++})" + if (extension.isEmpty()) "" else ".$extension"
        }
        val name = chosen.substringAfterLast('/')
        if (private) {
            val f = privateFile(chosen); f.parentFile?.mkdirs(); check(f.createNewFile())
            return PadFile(Uri.fromFile(f), name, true, path = chosen)
        }
        val tree = workspace ?: error("Select a workspace folder first.")
        var parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        chosen.substringBeforeLast('/', "").split('/').filter { it.isNotEmpty() }.forEach { segment ->
            parent = children(tree, parent).find { it.name == segment && it.directory }?.uri
                ?: DocumentsContract.createDocument(resolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, segment)
                ?: error("Cannot create folder $segment.")
        }
        val uri = DocumentsContract.createDocument(resolver, parent, mime(name), name) ?: error("Cannot create file.")
        return PadFile(uri, name, false, path = chosen)
    }

    private fun find(path: String, private: Boolean): PadFile? {
        if (private) {
            val f = privateFile(path)
            return if (f.isFile) PadFile(Uri.fromFile(f), f.name, true, f.length(), f.lastModified(), path) else null
        }
        val tree = workspace ?: return null
        var parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val segments = path.split('/')
        segments.forEachIndexed { i, s ->
            val c = children(tree, parent).find { it.name == s } ?: return null
            if (i == segments.lastIndex) return if (!c.directory) PadFile(c.uri, c.name, false, c.size, c.time, path) else null
            require(c.directory) { "A file blocks folder $s." }; parent = c.uri
        }
        return null
    }

    suspend fun save(file: PadFile, text: String, encrypted: Boolean, password: CharArray? = null): PadFile = withContext(Dispatchers.IO) {
        lock.withLock {
            val stage = temp("save")
            var bytes: ByteArray? = null
            try {
                require(text.length <= DocxHandler.MAX_TEXT)
                bytes = if (file.name.endsWith(".docx", true)) ByteArrayOutputStream().use { out -> DocxHandler.write(text, out); out.toByteArray() }
                    else text.toByteArray(Charsets.UTF_8)
                if (encrypted) {
                    val pass = password ?: error("Password required for each encrypted save.")
                    val existing = input(file).use { limitedBytes(it, 12 * 1024 * 1024) }
                    try { if (CryptoEngine.isEncrypted(existing)) CryptoEngine.decrypt(existing, pass.copyOf()).fill(0) }
                    finally { existing.fill(0) }
                    val protected = CryptoEngine.encrypt(bytes, pass)
                    try { stage.writeBytes(protected) } finally { protected.fill(0) }
                } else stage.writeBytes(bytes)
                writeNow(file, stage)
                val extension = file.name.substringAfterLast('.', "").lowercase()
                prefs.edit().putStringSet("text_extensions", prefs.getStringSet("text_extensions", emptySet()).orEmpty() + extension).apply()
                file.copy(size = stage.length(), modified = System.currentTimeMillis(), encrypted = encrypted)
            } finally { bytes?.fill(0); password?.fill('\u0000'); stage.delete() }
        }
    }

    private fun writeNow(file: PadFile, source: File) {
        if (file.vault) {
            val target = privateFile(file.path)
            val sibling = File(target.parentFile, ".${UUID.randomUUID()}.pending")
            try {
                source.inputStream().use { src -> FileOutputStream(sibling).use { dst -> src.copyTo(dst, 65536); dst.fd.sync() } }
                Os.rename(sibling.absolutePath, target.absolutePath)
            } finally { sibling.delete() }
            return
        }
        val rollback = temp("rollback")
        try {
            input(file).use { src -> rollback.outputStream().use { src.copyTo(it, 65536) } }
            try { writeUri(file.uri, source) }
            catch (failure: Exception) {
                try { writeUri(file.uri, rollback) }
                catch (recovery: Exception) { throw IOException("Save failed; recovery copy retained at ${rollback.name} in app storage.", recovery) }
                rollback.delete(); throw failure
            }
            rollback.delete()
        } catch (e: Exception) { throw e }
    }
    private fun writeUri(uri: Uri, source: File) {
        resolver.openOutputStream(uri, "rwt")?.use { out -> source.inputStream().use { it.copyTo(out, 65536) }; out.flush() }
            ?: error("This provider did not grant write access. Move a copy into your workspace.")
    }

    suspend fun move(file: PadFile, private: Boolean): PadFile = withContext(Dispatchers.IO) {
        lock.withLock {
            if (file.vault == private) return@withLock file
            val stage = temp("move")
            var dest: PadFile? = null
            try {
                input(file).use { src -> stage.outputStream().use { src.copyTo(it, 65536) } }
                dest = createNow(file.path, private, true)
                writeNow(dest, stage)
                val expected = digest(stage)
                val actual = input(dest).use { hashStream(it) }
                check(expected == actual) { "Copy verification failed; original was kept." }
                deleteNow(file)
                dest.copy(size = stage.length(), encrypted = file.encrypted)
            } catch (e: Exception) { dest?.let { runCatching { deleteNow(it) } }; throw e }
            finally { stage.delete() }
        }
    }
    suspend fun rename(file: PadFile, newName: String): PadFile = withContext(Dispatchers.IO) {
        lock.withLock {
            validateName(newName)
            // Changing extensions can change the editor's export format; keep it explicit via Save As.
            require(newName.substringAfterLast('.', "").equals(file.name.substringAfterLast('.', ""), true)) {
                "Keep the same file extension when renaming. Use Save As to change format."
            }
            if (newName == file.name) return@withLock file
            val path = file.path.substringBeforeLast('/', "").let { if (it.isEmpty()) newName else "$it/$newName" }
            require(find(path, file.vault) == null) { "A file with that name already exists." }
            val stage = temp("rename")
            try {
                input(file).use { src -> stage.outputStream().use { src.copyTo(it, 65536) } }
                val target = createNow(path, file.vault, false)
                writeNow(target, stage)
                check(input(target).use(::hashStream) == digest(stage)) { "Rename verification failed. Original kept." }
                deleteNow(file)
                target.copy(size = stage.length(), encrypted = file.encrypted)
            } finally { stage.delete() }
        }
    }

    suspend fun listTrash(): List<TrashItem> = withContext(Dispatchers.IO) {
        trashRoot.listFiles().orEmpty().filter { it.isDirectory && File(it, "item.json").isFile }.map { dir ->
            val item = json.decodeFromString<TrashItem>(File(dir, "item.json").readText())
            require(item.id == dir.name && File(dir, "content").isFile) { "Damaged Trash record. Keep app data for recovery." }
            item
        }.sortedByDescending { it.deleted }
    }
    private fun trashDirectory(id: String): File {
        require(id.matches(Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")))
        return File(trashRoot, id)
    }
    suspend fun moveToTrash(file: PadFile) = withContext(Dispatchers.IO) {
        lock.withLock {
            val id = UUID.randomUUID().toString()
            val dir = trashDirectory(id).apply { check(mkdir()) }
            val content = File(dir, "content")
            var recorded = false
            try {
                input(file).use { src -> FileOutputStream(content).use { dst -> src.copyTo(dst, 65536); dst.fd.sync() } }
                val hash = digest(content)
                check(input(file).use(::hashStream) == hash) { "File changed during copy. Original kept." }
                val item = TrashItem(id, file.name, file.path, file.vault, file.modified, System.currentTimeMillis(), content.length(), hash)
                FileOutputStream(File(dir, "item.json")).use { it.write(json.encodeToString(item).toByteArray()); it.fd.sync() }
                recorded = true
                // The verified recovery copy and metadata exist before removing the original.
                deleteNow(file)
            } finally { if (!recorded) dir.deleteRecursively() }
        }
    }
    suspend fun restoreTrash(id: String): PadFile = withContext(Dispatchers.IO) {
        lock.withLock {
            val dir = trashDirectory(id)
            val item = json.decodeFromString<TrashItem>(File(dir, "item.json").readText())
            require(item.id == id)
            val content = File(dir, "content")
            require(content.length() == item.size && digest(content) == item.sha256) { "Trash copy is damaged; original recovery data kept." }
            val target = createNow(item.path, item.vault, true)
            writeNow(target, content)
            check(input(target).use(::hashStream) == item.sha256) { "Restore verification failed. Trash copy kept." }
            if (item.vault) privateFile(target.path).setLastModified(item.modified.coerceAtLeast(0))
            check(dir.deleteRecursively()) { "Restored file, but could not remove its Trash copy." }
            target.copy(size = item.size)
        }
    }
    suspend fun deleteTrashPermanently(id: String) = withContext(Dispatchers.IO) {
        lock.withLock { check(trashDirectory(id).deleteRecursively()) { "Could not permanently delete this Trash item." } }
    }

    private fun deleteNow(file: PadFile) {
        check(if (file.vault) privateFile(file.path).delete() else DocumentsContract.deleteDocument(resolver, file.uri)) { "Could not remove original file." }
    }

    suspend fun backup(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        lock.withLock {
            try {
                val files = scanNow(true).filterNot { sameDocument(it.uri, uri) }
                resolver.openOutputStream(uri, "rwt")?.use { out ->
                    CryptoEngine.writeBackup(out, password) { encrypted ->
                        ZipOutputStream(encrypted).use { zip ->
                            val manifest = files.mapIndexed { index, f ->
                                val path = "files/$index"
                                zip.putNextEntry(ZipEntry(path).apply { time = f.modified.coerceAtLeast(0) })
                                val digest = MessageDigest.getInstance("SHA-256")
                                var size = 0L
                                input(f).use { src ->
                                    val buffer = ByteArray(65536)
                                    try { while (true) { val n = src.read(buffer); if (n < 0) break; zip.write(buffer, 0, n); digest.update(buffer, 0, n); size += n } }
                                    finally { buffer.fill(0) }
                                }
                                zip.closeEntry()
                                BackupItem(path, f.path, f.vault, f.modified, size, hex(digest.digest()))
                            }
                            zip.putNextEntry(ZipEntry("manifest.json"))
                            zip.write(json.encodeToString(BackupManifest(files = manifest)).toByteArray(Charsets.UTF_8)); zip.closeEntry()
                        }
                    }
                } ?: error("Cannot write backup destination.")
            } catch (e: Exception) { runCatching { DocumentsContract.deleteDocument(resolver, uri) }; throw e }
            finally { password.fill('\u0000') }
        }
    }

    suspend fun restore(uri: Uri, password: CharArray, policy: ConflictPolicy): RestoreResult = withContext(Dispatchers.IO) {
        lock.withLock {
            val dir = File(workRoot, "restore-${UUID.randomUUID()}").apply { mkdirs() }
            try {
                var manifest: BackupManifest? = null
                val staged = mutableMapOf<String, File>()
                resolver.openInputStream(uri)?.use { source ->
                    CryptoEngine.readBackup(source, password) { decrypted ->
                        ZipInputStream(decrypted).use { zip ->
                            var total = 0L
                            while (true) {
                                val e = zip.nextEntry ?: break
                                require(!e.isDirectory && (e.name == "manifest.json" || e.name.matches(Regex("files/[0-9]{1,5}")))) { "Unsafe backup entry." }
                                require(e.name !in staged && !(e.name == "manifest.json" && manifest != null)) { "Duplicate backup entry." }
                                if (e.name == "manifest.json") {
                                    manifest = json.decodeFromString<BackupManifest>(limitedBytes(zip, 8 * 1024 * 1024).toString(Charsets.UTF_8))
                                } else {
                                    require(staged.size < 10000) { "Too many backup entries." }
                                    val f = File(dir, staged.size.toString())
                                    f.outputStream().use { dst ->
                                        val buffer = ByteArray(65536)
                                        try { while (true) {
                                            val n = zip.read(buffer); if (n < 0) break
                                            total += n
                                            require(total <= 100L * 1024 * 1024 * 1024 && dir.usableSpace > n + 16L * 1024 * 1024) { "Not enough space for staged restore." }
                                            dst.write(buffer, 0, n)
                                        } } finally { buffer.fill(0) }
                                    }
                                    staged[e.name] = f
                                }
                                zip.closeEntry()
                            }
                        }
                    }
                } ?: error("Cannot open backup.")
                val m = manifest ?: error("Backup manifest missing.")
                require(m.version == 1 && m.files.size == staged.size && m.files.map { it.entry }.toSet().size == m.files.size) { "Invalid manifest." }
                val paths = mutableSetOf<String>()
                m.files.forEach { item ->
                    validatePath(item.path)
                    require(paths.add("${item.vault}:${item.path}")) { "Duplicate destination." }
                    val f = staged[item.entry] ?: error("Missing backup file.")
                    require(item.size == f.length() && item.sha256 == digest(f)) { "Backup checksum mismatch." }
                    if (!item.vault) require(workspace != null) { "Select a workspace before restoring public files." }
                }
                // Authentication and every checksum have passed before the first destination write.
                data class Undo(val target: PadFile, val original: File?)
                val undo = mutableListOf<Undo>()
                var skipped = 0
                try {
                    m.files.forEach { item ->
                        val existing = find(item.path, item.vault)
                        if (existing != null && policy == ConflictPolicy.SKIP) { skipped++; return@forEach }
                        val target = if (existing != null && policy == ConflictPolicy.OVERWRITE) existing else createNow(item.path, item.vault, true)
                        val original = if (target == existing) File(dir, "undo-${undo.size}").also { copy -> input(target).use { src -> copy.outputStream().use { src.copyTo(it, 65536) } } } else null
                        undo.add(Undo(target, original))
                        writeNow(target, staged.getValue(item.entry))
                        if (target.vault) privateFile(target.path).setLastModified(item.modified.coerceAtLeast(0))
                    }
                } catch (failure: Exception) {
                    var failed = false
                    undo.asReversed().forEach { u -> runCatching { if (u.original == null) deleteNow(u.target) else writeNow(u.target, u.original) }.onFailure { failed = true } }
                    if (failed) { File(dir, "retain").writeText("Recovery data: do not delete."); error("Restore interrupted; some rollback files remain in ${dir.name}. Keep app data and retry from the backup.") }
                    throw failure
                }
                RestoreResult(undo.size, skipped)
            } finally { password.fill('\u0000'); if (!File(dir, "retain").exists()) dir.deleteRecursively() }
        }
    }

    fun autoTitle(text: String, extension: String = "txt"): String {
        val line = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val clean = line.filter {
            val type = Character.getType(it)
            it.isLetterOrDigit() || type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt() || type == Character.ENCLOSING_MARK.toInt() ||
                it == ' ' || it == '-' || it == '_'
        }.trim().take(60)
        val title = clean.ifBlank { java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date()) }
        return "$title.$extension"
    }
    private fun sameDocument(a: Uri, b: Uri): Boolean = a == b || runCatching {
        a.authority == b.authority && DocumentsContract.getDocumentId(a) == DocumentsContract.getDocumentId(b)
    }.getOrDefault(false)

    private fun privateFile(path: String): File {
        validatePath(path)
        val f = File(vaultRoot, path).canonicalFile
        require(f.path.startsWith(vaultRoot.canonicalPath + File.separator))
        return f
    }
    private fun validateName(name: String) { require(name.isNotBlank() && name != "." && name != ".." && name.length <= 240 && name.none { it == '/' || it == '\\' || it.code < 32 }) { "Invalid filename." } }
    private fun validatePath(path: String) { require(path.length <= 4096 && !path.startsWith('/')); path.split('/').forEach(::validateName) }
    private fun temp(prefix: String) = File.createTempFile(prefix, ".tmp", workRoot)
    private fun mime(name: String) = if (name.endsWith(".docx", true)) "application/vnd.openxmlformats-officedocument.wordprocessingml.document" else if (name.endsWith(".stxt", true) || name.endsWith(".enc", true)) "application/octet-stream" else "text/plain"
    private fun limitedBytes(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        try { while (true) { val n = input.read(buffer); if (n < 0) break; require(output.size() + n <= limit) { "File exceeds safe size limit." }; output.write(buffer, 0, n) }; return output.toByteArray() }
        finally { buffer.fill(0); output.reset() }
    }
    private fun digest(file: File) = file.inputStream().use(::hashStream)
    private fun hashStream(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256"); val b = ByteArray(65536)
        try { while (true) { val n = input.read(b); if (n < 0) break; digest.update(b, 0, n) }; return hex(digest.digest()) }
        finally { b.fill(0) }
    }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
```

## 7. app/src/main/java/com/secure/textpad/ui/home/HomeScreen.kt

```kotlin
package com.secure.textpad.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.secure.textpad.ui.theme.GlassButton as Button
import com.secure.textpad.ui.theme.GlassTextButton as TextButton
import com.secure.textpad.ui.theme.GlassOutlinedButton as OutlinedButton
import com.secure.textpad.ui.theme.GlassTonalButton as FilledTonalButton
import com.secure.textpad.ui.theme.GlassIconButton as IconButton
import com.secure.textpad.ui.theme.GlassFilterChip as FilterChip

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.secure.textpad.data.PadFile
import com.secure.textpad.data.TrashItem
import com.secure.textpad.ui.theme.liquidGlass
import com.secure.textpad.ui.theme.glassPress
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    files: List<PadFile>, busy: Boolean, workspace: Boolean, defaultPrivate: Boolean, appLock: Boolean,
    onOpen: (PadFile) -> Unit, onFolder: () -> Unit, onCreate: () -> Unit,
    onMove: (PadFile) -> Unit, onBackup: () -> Unit, onRestore: () -> Unit,
    onPrivateDefault: (Boolean) -> Unit, onAppLock: (Boolean) -> Unit, onRefresh: () -> Unit,
    onRename: (PadFile, String) -> Unit, onTrash: (PadFile) -> Unit, onShowTrash: () -> Unit,
    motionEnabled: Boolean = true, onMotionChange: (Boolean) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf<PadFile?>(null) }
    var rename by remember { mutableStateOf<PadFile?>(null) }
    var trash by remember { mutableStateOf<PadFile?>(null) }
    var move by remember { mutableStateOf<PadFile?>(null) }
    var backup by remember { mutableStateOf(false) }
    val visible = remember(files, search, filter) {
        files.filter { it.name.contains(search, true) && when (filter) {
            1 -> it.encrypted == false && !it.name.endsWith(".docx", true)
            2 -> it.encrypted == true
            3 -> it.name.endsWith(".docx", true)
            4 -> it.vault
            else -> true
        } }
    }
    if (!workspace && files.isEmpty()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.liquidGlass().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TXT", style = MaterialTheme.typography.displayMedium)
                Text("A space for your words", style = MaterialTheme.typography.titleLarge)
                Text("Choose or create a folder to keep your documents. Everything works offline.", Modifier.padding(vertical = 20.dp))
                Button(onClick = onFolder, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Choose workspace folder") }
                TextButton(onClick = onShowTrash) { Text("Open Trash") }
            }
        }
        return
    }
    val colors = MaterialTheme.colorScheme
    val fabInteraction = remember { MutableInteractionSource() }
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentColor = colors.onSurface,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate, modifier = Modifier.heightIn(min = 64.dp).widthIn(min = 160.dp).liquidGlass(fabInteraction).glassPress(fabInteraction),
                containerColor = Color.Transparent, contentColor = colors.onSurface, interactionSource = fabInteraction,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Add, null)
                    Text("New File", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                }
            }
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().liquidGlass().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { filter = 0; search = "" }, modifier = Modifier.weight(1f)) { Text("Files") }
                    TextButton(onClick = { filter = 4 }, modifier = Modifier.weight(1f)) { Text("Vault") }
                    TextButton(onClick = onShowTrash, modifier = Modifier.weight(1f)) { Text("Trash") }
                }
            }

        }
    ) { padding ->
        // The complete header scrolls with the list on short screens / enlarged fonts.
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = colors.primaryContainer) {
                        Icon(Icons.Outlined.Description, null, Modifier.padding(12.dp).size(26.dp), tint = colors.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("TXT", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text("Notes, documents & private files", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Settings, "Settings") }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().liquidGlass().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        label = { Text("Search files") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Plain", "Locked", "Docs", "Private Vault").forEachIndexed { i, title ->
                            FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(title) })
                        }
                    }
                    val largeLabels = LocalDensity.current.fontScale > 1.25f
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { backup = true }, enabled = !busy, modifier = (if (largeLabels) Modifier.fillMaxWidth() else Modifier.weight(1f)).heightIn(min = 48.dp)) {
                            Icon(Icons.Outlined.Backup, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Backup", maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(onClick = onFolder, enabled = !busy, modifier = (if (largeLabels) Modifier.fillMaxWidth() else Modifier.weight(1f)).heightIn(min = 48.dp)) {
                            Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Workspace", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Documents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("${visible.size} ${if (visible.size == 1) "file" else "files"} · On this device", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onRefresh, enabled = !busy) { Icon(Icons.Outlined.Refresh, "Refresh file list") }
                }
            }
            if (!workspace) item {
                Column(Modifier.fillMaxWidth().liquidGlass().padding(20.dp)) {
                    Text("Choose your workspace", style = MaterialTheme.typography.titleLarge)
                    Text("Select or create a folder for your files. Private vault files stay in this app's storage.", Modifier.padding(vertical = 8.dp))
                    Button(onClick = onFolder, enabled = !busy) { Text("Choose folder") }
                }
            }
            if (visible.isEmpty() && !busy) item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Description, null, Modifier.size(44.dp), tint = colors.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(if (search.isBlank() && filter == 0) "A fresh page awaits" else "No matching files",
                        style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(if (search.isBlank() && filter == 0) "Tap New File to capture an idea. Your words stay on your device." else "Try another search or choose a different filter.",
                        Modifier.padding(top = 8.dp), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            items(visible, key = { it.uri.toString() }) { file ->
                val interaction = remember(file.uri) { MutableInteractionSource() }
                Row(Modifier.fillMaxWidth().animateItem().liquidGlass(interaction).glassPress(interaction).clickable(interactionSource = interaction, indication = null, enabled = !busy) { onOpen(file) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = colors.primaryContainer) {
                        Icon(if (file.encrypted == true) Icons.Outlined.Lock else Icons.Outlined.Description,
                            null, Modifier.padding(10.dp).size(22.dp), tint = colors.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FileBadge("." + file.name.substringAfterLast('.', "txt"))
                            FileBadge(if (file.encrypted == true) "Password locked" else if (file.encrypted == false) "Plain text" else "Checking…")
                            if (file.vault) FileBadge("Private vault")
                        }
                        Text(formatSize(file.size) + if (file.modified > 0) " · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(file.modified)) else "",
                            color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { focusManager.clearFocus(); keyboard?.hide(); options = file }, enabled = !busy) {
                        Icon(Icons.Outlined.MoreVert, "File options: ${file.name}")
                    }
                }
            }
        }
    }
    options?.let { file ->
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.Transparent, onDismissRequest = { options = null }) {
            Column(Modifier.fillMaxWidth().liquidGlass().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                Text(file.name, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { options = null; onOpen(file) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.Description, null); Spacer(Modifier.width(8.dp)); Text("Open") }
                TextButton(onClick = { options = null; rename = file }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(8.dp)); Text("Rename") }
                TextButton(onClick = { options = null; move = file }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Move to…") }
                TextButton(onClick = { options = null; trash = file }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text("Move to Trash") }
            }
        }
    }
    rename?.let { file ->
        var name by remember(file.uri) { mutableStateOf(file.name) }
        AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { rename = null }, title = { Text("Rename file") }, text = {
            OutlinedTextField(name, { name = it }, label = { Text("Filename") }, supportingText = { Text("Keep the same extension. Use Save As to change format.") })
        }, confirmButton = { TextButton(onClick = { rename = null; onRename(file, name.trim()) }, enabled = name.isNotBlank()) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { rename = null }) { Text("Cancel") } })
    }
    move?.let { file -> AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { move = null }, title = { Text("Move file") }, text = {
        Text(if (file.vault) "Move this file to the active workspace? Other apps with folder access may see it." else "Move this file to the private vault? It will be hidden from ordinary file managers.")
    }, confirmButton = { TextButton(onClick = { move = null; onMove(file) }, enabled = !file.vault || workspace) { Text(if (file.vault) "Move to workspace" else "Move to private vault") } },
        dismissButton = { TextButton(onClick = { move = null }) { Text("Cancel") } }) }
    trash?.let { file -> AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { trash = null }, title = { Text("Move to Trash?") }, text = {
        Text("${file.name} will leave its folder. You can restore it from Trash later. Locked files keep their password protection.")
    }, confirmButton = { TextButton(onClick = { trash = null; onTrash(file) }) { Text("Move to Trash") } },
        dismissButton = { TextButton(onClick = { trash = null }) { Text("Cancel") } }) }
    if (backup) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { backup = false }, title = { Text("Backup & restore") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Keep a .tpbak copy outside this phone before resetting it. You will need its master password on your next device.")
            FilledTonalButton(onClick = { backup = false; onBackup() }, modifier = Modifier.fillMaxWidth()) { Text("Create full offline backup") }
            OutlinedButton(onClick = { backup = false; onRestore() }, modifier = Modifier.fillMaxWidth()) { Text("Restore offline backup") }
        }
    }, confirmButton = { TextButton(onClick = { backup = false }) { Text("Close") } })
    if (settings) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { settings = false }, title = { Text("TXT settings") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Animated glass", Modifier.weight(1f)); Switch(motionEnabled, onMotionChange) }
            Text("Appearance follows your device’s Light / Dark setting.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("New files in private vault", Modifier.weight(1f)); Switch(defaultPrivate, onPrivateDefault) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Fingerprint / screen lock", Modifier.weight(1f)); Switch(appLock, onAppLock) }
            Text("Off by default. When enabled, Android verifies your fingerprint or device screen lock whenever TXT returns from the background.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Text("Private vault uses Android app isolation. Uninstalling TXT erases it. Export a backup first.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { settings = false }) { Text("Done") } })
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
private fun FileBadge(label: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)) {
        Text(label, Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun TrashScreen(files: List<TrashItem>, busy: Boolean, onBack: () -> Unit, onRestore: (TrashItem) -> Unit, onDelete: (TrashItem) -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    var deleting by remember { mutableStateOf<TrashItem?>(null) }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextButton(onClick = onBack) { Text("‹ Back to documents") }
                Text("Trash", style = MaterialTheme.typography.headlineLarge)
                Text("Restore files to their original storage. Public files return to the active workspace. Matching names are kept as separate copies.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Trash is not included in backups. Restore anything you need before exporting a backup.", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (files.isEmpty()) item { Text("Trash is empty", Modifier.padding(vertical = 40.dp), style = MaterialTheme.typography.titleLarge) }
            items(files, key = { it.id }) { file ->
                Column(Modifier.fillMaxWidth().liquidGlass().padding(16.dp)) {
                    Text(file.name, style = MaterialTheme.typography.titleMedium)
                    Text("${if (file.vault) "Private vault" else "Workspace"} · ${formatSize(file.size)}", style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { onRestore(file) }, enabled = !busy) { Icon(Icons.Outlined.Restore, null); Spacer(Modifier.width(8.dp)); Text("Restore") }
                        TextButton(onClick = { deleting = file }, enabled = !busy) { Icon(Icons.Outlined.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("Delete") }
                    }
                }
            }
        }
    }
    deleting?.let { file -> AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { deleting = null }, title = { Text("Delete permanently?") },
        text = { Text("${file.name} cannot be restored after this. This removes only its Trash copy.") },
        confirmButton = { TextButton(onClick = { deleting = null; onDelete(file) }) { Text("Delete permanently") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}
```

## 8. app/src/main/java/com/secure/textpad/ui/editor/EditorScreen.kt

```kotlin
package com.secure.textpad.ui.editor

import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import com.secure.textpad.ui.theme.LocalGlassMotionEnabled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import com.secure.textpad.security.SessionPassword
import kotlin.math.roundToInt
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.secure.textpad.ui.theme.GlassButton as Button
import com.secure.textpad.ui.theme.GlassTextButton as TextButton
import com.secure.textpad.ui.theme.GlassOutlinedButton as OutlinedButton
import com.secure.textpad.ui.theme.GlassTonalButton as FilledTonalButton
import com.secure.textpad.ui.theme.GlassIconButton as IconButton
import com.secure.textpad.ui.theme.GlassFilterChip as FilterChip

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import com.secure.textpad.ui.theme.GlassStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.secure.textpad.data.DocxHandler
import com.secure.textpad.data.PadFile
import com.secure.textpad.ui.theme.liquidGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class EditorDocument(file: PadFile, text: String, locked: Boolean, fresh: Boolean = false) {
    val sessionId: String = java.util.UUID.randomUUID().toString()
    var file by mutableStateOf(file)
    var value by mutableStateOf(TextFieldValue(text))
        private set
    var baseline by mutableStateOf(text)
        private set
    var encrypted by mutableStateOf(locked)
        private set
    var sessionPassword: SessionPassword? = null
    var hasSessionPassword by mutableStateOf(false)
    var saveFailure by mutableStateOf(false)
    var historyChange by mutableStateOf<TextRange?>(null)
    var fresh by mutableStateOf(fresh)
    val dirty: Boolean get() = fresh || value.text != baseline
    private val undo = ArrayDeque<TextFieldValue>()
    private val redo = ArrayDeque<TextFieldValue>()
    var revision by mutableIntStateOf(0)
        private set
    val canUndo: Boolean get() { revision; return undo.isNotEmpty() }
    val canRedo: Boolean get() { revision; return redo.isNotEmpty() }
    fun edit(next: TextFieldValue) {
        if (next.text.length > DocxHandler.MAX_TEXT) return
        if (next.text != value.text) {
            historyChange = null
            undo.addLast(value.copy(composition = null)); redo.clear()
            var bytes = undo.sumOf { it.text.length.toLong() * 2 }
            while (undo.size > 100 || bytes > 16L * 1024 * 1024) bytes -= undo.removeFirst().text.length * 2L
            revision++
        }
        value = next
    }
    fun select(start: Int, end: Int) { value = value.copy(selection = TextRange(start, end), composition = null) }
    private fun restoreHistory(target: TextFieldValue) {
        val before = value.text
        val after = target.text
        var start = 0
        val common = minOf(before.length, after.length)
        while (start < common && before[start] == after[start]) start++
        var endBefore = before.length
        var endAfter = after.length
        while (endBefore > start && endAfter > start && before[endBefore - 1] == after[endAfter - 1]) { endBefore--; endAfter-- }
        historyChange = TextRange(start, endAfter)
        value = target.copy(selection = TextRange(start.coerceAtMost(after.length), endAfter), composition = null)
        revision++
    }
    fun undo() { if (undo.isNotEmpty()) { redo.addLast(value.copy(composition = null)); restoreHistory(undo.removeLast()) } }
    fun redo() { if (redo.isNotEmpty()) { undo.addLast(value.copy(composition = null)); restoreHistory(redo.removeLast()) } }
    fun setSession(password: SessionPassword?) { sessionPassword?.clear(); sessionPassword = password; hasSessionPassword = password != null }
    fun saved(file: PadFile, text: String, encrypted: Boolean) { this.file = file; baseline = text; this.encrypted = encrypted; fresh = false; saveFailure = false }
    fun clear() { setSession(null); undo.clear(); redo.clear(); value = TextFieldValue(); baseline = ""; revision++ }
}

private data class Hit(val start: Int, val end: Int)
private data class SearchResult(val hits: List<Hit> = emptyList(), val error: String? = null, val source: String = "")

/** Checks time from the regex engine's character reads, preventing pathological patterns hanging a worker. */
private class TimedSequence(private val value: String, private val deadline: Long) : CharSequence {
    override val length get() = value.length
    override fun get(index: Int): Char {
        check(System.nanoTime() <= deadline) { "Pattern took too long. Simplify the expression." }
        return value[index]
    }
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = TimedSequence(value.substring(startIndex, endIndex), deadline)
    override fun toString() = value
}

private fun search(text: String, query: String, case: Boolean, regex: Boolean): SearchResult {
    if (query.isEmpty()) return SearchResult()
    return try {
        require(query.length <= 1000) { "Search expression is too long." }
        val flags = if (case) 0 else Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        val pattern = Pattern.compile(if (regex) query else Pattern.quote(query), flags)
        val matcher = pattern.matcher(TimedSequence(text, System.nanoTime() + 150_000_000))
        val hits = mutableListOf<Hit>()
        while (matcher.find()) {
            require(hits.size < 10000) { "More than 10,000 matches. Use a narrower search." }
            hits.add(Hit(matcher.start(), matcher.end()))
        }
        SearchResult(hits, source = text)
    } catch (e: Exception) { SearchResult(error = e.message ?: "Invalid expression.") }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    doc: EditorDocument, busy: Boolean, saving: Boolean = false, onBack: () -> Unit,
    onSave: (Boolean, CharArray?) -> Unit, onMove: () -> Unit,
    onSaveAs: (String, Boolean) -> Unit,
    onSensitivePrompt: (Boolean) -> Unit
) {
    val motion = LocalGlassMotionEnabled.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { if (doc.fresh) { focus.requestFocus(); keyboard?.show() } }
    var saveAs by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    var saveAsEncrypted by remember { mutableStateOf(false) }
    var wrap by remember { mutableStateOf(true) }
    var numbers by remember { mutableStateOf(false) }
    var size by remember { mutableIntStateOf(16) }
    var family by remember { mutableIntStateOf(1) }
    var tools by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var case by remember { mutableStateOf(false) }
    var regex by remember { mutableStateOf(false) }
    var current by remember { mutableIntStateOf(0) }
    var autoSave by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf(false) }
    var plainConfirm by remember { mutableStateOf(false) }
    val result by produceState(SearchResult(), doc.value.text, query, case, regex, finding) {
        value = if (finding) withContext(Dispatchers.Default) { search(doc.value.text, query, case, regex) } else SearchResult()
    }
    val stats by produceState("", doc.value.text) {
        val text = doc.value.text
        value = withContext(Dispatchers.Default) {
            var words = 0; var inWord = false; var lines = 1
            for (c in text) { if (c == '\n') lines++; if (c.isWhitespace()) inWord = false else if (!inWord) { words++; inWord = true } }
            "$lines lines · $words words · ${text.codePointCount(0, text.length)} chars"
        }
    }
    LaunchedEffect(doc.value.text, autoSave, busy, saving, doc.hasSessionPassword, doc.saveFailure) {
        if (autoSave && (!doc.encrypted || doc.hasSessionPassword) && doc.dirty && !busy && !saving && !doc.saveFailure) { delay(1500); onSave(doc.encrypted, null) }
    }
    BackHandler(onBack = onBack)
    fun save() { doc.saveFailure = false; if (doc.encrypted && !doc.hasSessionPassword) password = true else onSave(doc.encrypted, null) }
    fun navigate(delta: Int) {
        if (result.hits.isEmpty() || result.source != doc.value.text) return
        current = Math.floorMod(current + delta, result.hits.size)
        result.hits[current].let { doc.select(it.start, it.end) }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().liquidGlass().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !busy && !saving) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to files") }
            Column(Modifier.weight(1f)) {
                Text(doc.file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                val saveLabel = if (doc.saveFailure) "Save failed · use Save As" else if (saving) "Saving…" else if (busy) "Working…" else if (doc.dirty) "Unsaved changes" else "Saved on this device"
                Crossfade(saveLabel, animationSpec = if (motion) tween(130) else snap(), label = "save status") { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            TextButton(onClick = { doc.undo() }, enabled = !busy && doc.canUndo, modifier = Modifier.testTag("editor.undo")) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.AutoMirrored.Outlined.Undo, "Undo", Modifier.size(20.dp)); Text("Undo", style = MaterialTheme.typography.labelSmall) }
            }
            TextButton(onClick = { doc.redo() }, enabled = !busy && doc.canRedo, modifier = Modifier.testTag("editor.redo")) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.AutoMirrored.Outlined.Redo, "Redo", Modifier.size(20.dp)); Text("Redo", style = MaterialTheme.typography.labelSmall) }
            }
        }
        if (finding) Column(Modifier.fillMaxWidth().liquidGlass().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it; current = -1 }, Modifier.weight(1f), label = { Text("Find") }, singleLine = true)
                IconButton(onClick = { navigate(-1) }) { Icon(Icons.Outlined.KeyboardArrowUp, "Previous match") }
                IconButton(onClick = { navigate(1) }) { Icon(Icons.Outlined.KeyboardArrowDown, "Next match") }
                IconButton(onClick = { finding = false }) { Icon(Icons.Outlined.Close, "Close find & replace") }
            }
            Text(result.error ?: if (result.hits.isEmpty()) "0 matches" else "${(current + 1).coerceIn(1, result.hits.size)} of ${result.hits.size}", style = MaterialTheme.typography.labelSmall,
                color = if (result.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(replacement, { replacement = it }, Modifier.fillMaxWidth(), label = { Text("Replace with (literal text)") }, singleLine = true)
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(case, { case = !case }, label = { Text("Aa") })
                Spacer(Modifier.width(6.dp)); FilterChip(regex, { regex = !regex }, label = { Text("Regex") })
                TextButton(enabled = !busy && result.hits.isNotEmpty() && result.source == doc.value.text, onClick = {
                    result.hits.getOrNull(current.coerceAtLeast(0))?.let { h ->
                        val text = doc.value.text.replaceRange(h.start, h.end, replacement)
                        doc.edit(TextFieldValue(text, TextRange(h.start + replacement.length)))
                    }
                }) { Text("Replace") }
                TextButton(enabled = !busy && result.hits.isNotEmpty() && result.source == doc.value.text, onClick = {
                    val old = doc.value.text
                    val estimated = old.length.toLong() + result.hits.sumOf { replacement.length.toLong() - (it.end - it.start) }
                    if (estimated <= DocxHandler.MAX_TEXT) {
                        val text = buildString { var start = 0; result.hits.forEach { append(old, start, it.start); append(replacement); start = it.end }; append(old, start, old.length) }
                        doc.edit(TextFieldValue(text))
                    }
                }) { Text("All") }
            }
        }
        val vertical = rememberScrollState()
        val horizontal = rememberScrollState()
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
        var viewport by remember { mutableStateOf(IntSize.Zero) }
        var fieldWidth by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val imeBottom = WindowInsets.ime.getBottom(density)
        val font = when (family) { 1 -> FontFamily.SansSerif; 2 -> FontFamily.Serif; else -> FontFamily.Monospace }
        val textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = size.sp, lineHeight = (size * 1.5f).sp, fontFamily = font)
        val gutterColor = MaterialTheme.colorScheme.onSurfaceVariant
        val activeLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.055f)
        val margin = with(density) { 30.dp.toPx() }
        LaunchedEffect(doc.value.text, doc.value.selection, layout, viewport, imeBottom, wrap, numbers) {
            withFrameNanos { }
            val result = layout ?: return@LaunchedEffect
            if (viewport.height <= 0 || result.layoutInput.text.text != doc.value.text) return@LaunchedEffect
            val cursor = result.getCursorRect((if (doc.historyChange != null) doc.value.selection.start else doc.value.selection.end).coerceIn(0, doc.value.text.length))
            val target = when {
                cursor.bottom + margin > vertical.value + viewport.height -> cursor.bottom + margin - viewport.height
                cursor.top - margin < vertical.value -> cursor.top - margin
                else -> vertical.value.toFloat()
            }
            vertical.scrollTo(target.roundToInt().coerceIn(0, vertical.maxValue))
            if (!wrap && fieldWidth > 0) {
                val x = when {
                    cursor.right + margin > horizontal.value + fieldWidth -> cursor.right + margin - fieldWidth
                    cursor.left - margin < horizontal.value -> cursor.left - margin
                    else -> horizontal.value.toFloat()
                }
                horizontal.scrollTo(x.roundToInt().coerceIn(0, horizontal.maxValue))
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp).liquidGlass(style = GlassStyle.Writing).padding(horizontal = 18.dp, vertical = 20.dp)
            .testTag("editor.viewport").onSizeChanged { viewport = it }) {
            Row(Modifier.fillMaxWidth().verticalScroll(vertical).padding(bottom = 40.dp)) {
                if (numbers) {
                    val text = doc.value.text
                    val starts = remember(text) { buildList { add(0); text.forEachIndexed { i, c -> if (c == '\n') add(i + 1) } } }
                    val height = with(density) { (layout?.size?.height ?: 0).toDp() }
                    val paint = remember(gutterColor, size, density) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = gutterColor.toArgb(); textSize = with(density) { size.sp.toPx() }; typeface = android.graphics.Typeface.MONOSPACE
                    } }
                    val numberWidth = with(density) { paint.measureText(starts.size.toString()).toDp() } + 12.dp
                    Canvas(Modifier.width(numberWidth.coerceAtLeast(40.dp)).height(height).testTag("editor.gutter")) {
                        layout?.let { l ->
                            val top = vertical.value.toFloat()
                            val firstLine = l.getLineForVerticalPosition(top.coerceAtLeast(0f))
                            val firstOffset = l.getLineStart(firstLine)
                            var index = starts.binarySearch(firstOffset).let { if (it >= 0) it else (-it - 2).coerceAtLeast(0) }
                            while (index < starts.size) {
                                val line = l.getLineForOffset(starts[index].coerceAtMost(text.length))
                                if (l.getLineTop(line) > top + viewport.height + 80) break
                                drawContext.canvas.nativeCanvas.drawText((index + 1).toString(), 0f, l.getLineBaseline(line), paint)
                                index++
                            }
                        }
                    }
                }
                Box(Modifier.weight(1f).onSizeChanged { fieldWidth = it.width }
                    .then(if (!wrap) Modifier.horizontalScroll(horizontal) else Modifier)) {
                    BasicTextField(value = doc.value, onValueChange = { doc.edit(it) },
                        modifier = Modifier.fillMaxWidth().widthIn(min = with(density) { fieldWidth.toDp() }).focusRequester(focus).defaultMinSize(minHeight = 240.dp).testTag("editor.input"),
                        readOnly = busy, textStyle = textStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        onTextLayout = { layout = it },
                        decorationBox = { input ->
                            Box(Modifier.drawBehind {
                                layout?.takeIf { it.layoutInput.text.text == doc.value.text }?.let { measured ->
                                    val line = measured.getLineForOffset(doc.value.selection.end.coerceIn(0, doc.value.text.length))
                                    drawRect(activeLineColor, topLeft = Offset(0f, measured.getLineTop(line)),
                                        size = androidx.compose.ui.geometry.Size(this.size.width, measured.getLineBottom(line) - measured.getLineTop(line)))
                                }
                            }) {
                                if (doc.value.text.isEmpty()) Text("Start writing…", style = textStyle.copy(color = gutterColor))
                                input()
                            }
                        })
                }
            }
        }
        doc.historyChange?.let { change ->
            Text("History change at character ${change.start + 1}" + if (change.collapsed) " · text removed" else " · changed text selected",
                Modifier.padding(top = 4.dp, start = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        val actionColumns = if (density.fontScale > 1.25f || LocalConfiguration.current.screenWidthDp < 360) 2 else 4
        val actionPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
        FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp).liquidGlass().padding(horizontal = 8.dp),
            maxItemsInEachRow = actionColumns, horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { save() }, enabled = !busy && !saving, modifier = Modifier.weight(1f), contentPadding = actionPadding) { Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(5.dp)); Text("Save", maxLines = 1, softWrap = false) }
            TextButton(onClick = { password = true }, enabled = !busy && !saving, modifier = Modifier.weight(1f), contentPadding = actionPadding) { Icon(Icons.Outlined.Lock, null); Spacer(Modifier.width(5.dp)); Text("Lock", maxLines = 1, softWrap = false) }
            TextButton(onClick = { finding = !finding }, modifier = Modifier.weight(1f), contentPadding = actionPadding) { Icon(Icons.Outlined.Search, null); Spacer(Modifier.width(5.dp)); Text("Find", maxLines = 1, softWrap = false) }
            TextButton(onClick = { tools = true }, modifier = Modifier.weight(1f), contentPadding = actionPadding) { Icon(Icons.Outlined.Tune, null); Text("Tools ⋯", maxLines = 1, softWrap = false) }
        }
        val securityStatus = "${if (doc.encrypted) "Encrypted" else "Plain"} · ${if (doc.file.vault) "Private vault" else "Workspace"}"
        Text(if (imeBottom > 0) "$securityStatus · $stats" else "$stats\nUTF-8 · $securityStatus",
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall,
            maxLines = if (imeBottom > 0) 1 else 2, overflow = TextOverflow.Ellipsis)
    }
    if (password) PasswordDialog(title = if (doc.encrypted) "Password to save" else "Lock this document", confirm = !doc.encrypted,
        onDismiss = { password = false }, onSensitive = onSensitivePrompt,
        onPassword = { password = false; onSave(true, it) })
    if (tools) ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.Transparent, onDismissRequest = { tools = false }) {
        Column(Modifier.fillMaxWidth().liquidGlass().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text("Tools", style = MaterialTheme.typography.titleLarge)
            Row {
                TextButton(onClick = { doc.undo(); tools = false }, enabled = !busy && doc.canUndo) { Text("Undo") }
                TextButton(onClick = { doc.redo(); tools = false }, enabled = !busy && doc.canRedo) { Text("Redo") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Word wrap", Modifier.weight(1f)); Switch(wrap, { wrap = it }, modifier = Modifier.testTag("editor.wordWrap")) }
            Text("Comfort font size · ${size}sp")
            Slider(value = size.toFloat(), onValueChange = { size = it.toInt() }, valueRange = 12f..30f, steps = 17)
            TextButton(onClick = { tools = false; onMove() }, enabled = !busy && !saving && !doc.dirty) { Text(if (doc.file.vault) "Move to workspace" else "Isolate from other apps") }
            Text("Save As", style = MaterialTheme.typography.labelLarge)
            Row {
                TextButton(onClick = { tools = false; fileName = if (doc.fresh) "Untitled.txt" else doc.file.name; saveAsEncrypted = doc.encrypted; saveAs = true }, enabled = !busy) { Text("Save As…") }
            }
            Text(stats + " · UTF-8", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Line numbers", Modifier.weight(1f)); Switch(numbers, { numbers = it; tools = false }, modifier = Modifier.testTag("editor.lineNumbers")) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Auto-save", Modifier.weight(1f)); Switch(autoSave, { autoSave = it }) }
            Text("Font family", style = MaterialTheme.typography.labelLarge)
            listOf("Monospace", "Sans serif", "Serif").forEachIndexed { i, name -> TextButton(onClick = { family = i }) { Text(if (family == i) "✓ $name" else name) } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { size = (size - 1).coerceAtLeast(12) }) { Text("A−") }
                Text("${size}sp")
                TextButton(onClick = { size = (size + 1).coerceAtMost(30) }) { Text("A+") }
            }
            if (doc.encrypted) TextButton(onClick = { tools = false; plainConfirm = true }) { Text("Remove file encryption") }
            if (doc.file.name.endsWith(".docx", true)) Text("DOCX saves keep paragraph text. Formatting, images and tables are not preserved.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { tools = false }) { Text("Done") }
        }
    }
    if (saveAs) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { saveAs = false }, title = { Text("Save As") }, text = {
        Column {
            OutlinedTextField(fileName, { fileName = it }, label = { Text("Filename and extension") }, singleLine = true,
                supportingText = { Text("Any text filename: index.html, script.php, notes.txt, or your own extension. DOCX exports paragraph text.") })
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Password protection", Modifier.weight(1f)); Switch(saveAsEncrypted, { saveAsEncrypted = it }) }
        }
    }, confirmButton = { TextButton(onClick = {
        saveAs = false
        onSaveAs(fileName.trim(), saveAsEncrypted || fileName.endsWith(".stxt", true) || fileName.endsWith(".enc", true))
    }, enabled = fileName.isNotBlank() && fileName.length <= 240 && fileName.none { it == '/' || it == '\\' || it.code < 32 }) { Text("Choose location") } },
        dismissButton = { TextButton(onClick = { saveAs = false }) { Text("Cancel") } })
    if (plainConfirm) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { plainConfirm = false }, title = { Text("Save without encryption?") },
        text = { Text("The saved file will no longer need a password. Anyone who can access its folder can read it.") },
        confirmButton = { TextButton(onClick = { plainConfirm = false; onSave(false, null) }) { Text("Save plain file") } },
        dismissButton = { TextButton(onClick = { plainConfirm = false }) { Text("Cancel") } })
}

/** Native password fields support Android Autofill; editable buffers and submitted arrays are erased. */
@Composable
fun PasswordDialog(title: String, confirm: Boolean = false, onDismiss: () -> Unit,
    onSensitive: (Boolean) -> Unit, onPassword: (CharArray) -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintInk = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    var first by remember { mutableStateOf<EditText?>(null) }
    var second by remember { mutableStateOf<EditText?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onSensitive(true)
        onDispose { first?.text?.clear(); second?.text?.clear(); onSensitive(false) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)) {
        Surface(modifier = Modifier.liquidGlass(), shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (confirm) Text("Use a long, unique password. TXT cannot recover it.", Modifier.padding(vertical = 12.dp))
                fun chars(edit: EditText?): CharArray {
                    val text = edit?.text ?: return CharArray(0)
                    return CharArray(text.length) { text[it] }
                }
                AndroidView(factory = { c -> EditText(c).apply {
                    hint = "Password"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setSingleLine(); setAutofillHints(View.AUTOFILL_HINT_PASSWORD); importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                    first = this
                } }, update = { view ->
                    view.setTextColor(ink); view.setHintTextColor(hintInk)
                    view.backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
                }, modifier = Modifier.fillMaxWidth())
                if (confirm) AndroidView(factory = { c -> EditText(c).apply {
                    hint = "Repeat password"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setSingleLine(); importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO; second = this
                } }, update = { view ->
                    view.setTextColor(ink); view.setHintTextColor(hintInk)
                    view.backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
                }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        val p = chars(first); val repeat = chars(second)
                        if (p.isEmpty() || (confirm && p.size < 10)) { error = "Use at least ${if (confirm) 10 else 1} characters."; p.fill('\u0000') }
                        else if (confirm && !p.contentEquals(repeat)) { error = "Passwords do not match."; p.fill('\u0000') }
                        else { first?.text?.clear(); second?.text?.clear(); onPassword(p) }
                        repeat.fill('\u0000')
                    }) { Text("Continue") }
                }
            }
        }
    }
}
```

## 9. app/src/main/java/com/secure/textpad/MainActivity.kt

```kotlin
package com.secure.textpad

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.secure.textpad.ui.theme.GlassButton as Button
import com.secure.textpad.ui.theme.GlassTextButton as TextButton
import com.secure.textpad.ui.theme.GlassOutlinedButton as OutlinedButton
import com.secure.textpad.ui.theme.GlassTonalButton as FilledTonalButton
import com.secure.textpad.ui.theme.GlassIconButton as IconButton
import com.secure.textpad.ui.theme.GlassFilterChip as FilterChip

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.secure.textpad.data.*
import com.secure.textpad.ui.editor.*
import com.secure.textpad.ui.home.HomeScreen
import com.secure.textpad.ui.home.TrashScreen
import com.secure.textpad.ui.theme.LiquidGlassTheme
import com.secure.textpad.ui.theme.liquidGlass
import com.secure.textpad.ui.theme.LocalGlassMotionEnabled
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.secure.textpad.security.SessionPassword
import java.security.KeyStore
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

class PadViewModel(app: Application) : AndroidViewModel(app) {
    val repository = StorageRepository(app)
    var files by mutableStateOf<List<PadFile>>(emptyList()); private set
    var trash by mutableStateOf<List<TrashItem>>(emptyList()); private set
    var showTrash by mutableStateOf(false)
    var editor by mutableStateOf<EditorDocument?>(null); private set
    var busy by mutableStateOf(false); private set
    var saving by mutableStateOf(false); private set
    var savingProtected by mutableStateOf(false); private set
    var notice by mutableStateOf<String?>(null)
    var unlockFile by mutableStateOf<PadFile?>(null)
    var workspace by mutableStateOf(repository.workspace != null); private set
    var defaultPrivate by mutableStateOf(repository.defaultPrivate)
    var pendingNavigation by mutableStateOf<(() -> Unit)?>(null)

    init { refresh() }
    fun report(error: Throwable) { notice = error.message ?: "Operation failed. Your editor text is still available." }
    fun runOperation(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch { try { block() } catch (e: Exception) { report(e) } finally { busy = false } }
    }
    fun refresh() = runOperation { reload() }
    private suspend fun reload() {
        trash = repository.listTrash()
        files = repository.scan()
        // Publish the directory cursor results first; inspect magic bytes on I/O afterwards.
        val detected = ArrayList<PadFile>()
        for (f in files) detected.add(f.copy(encrypted = runCatching { repository.detect(f) }.getOrNull()))
        files = detected
    }
    fun navigate(action: () -> Unit) {
        if (busy || saving) return
        if (editor?.dirty == true) pendingNavigation = action else { editor?.clear(); editor = null; action() }
    }
    fun discardAndNavigate() {
        val action = pendingNavigation; pendingNavigation = null
        editor?.clear(); editor = null; action?.invoke()
    }
    fun open(file: PadFile) = navigate {
        runOperation {
            if (repository.detect(file)) unlockFile = file
            else editor = EditorDocument(file, repository.readText(file), false)
        }
    }
    fun unlock(password: CharArray) {
        val file = unlockFile ?: run { password.fill('\u0000'); return }
        unlockFile = null
        runOperation {
            var sealed: SessionPassword? = null
            try {
                sealed = withContext(Dispatchers.IO) { SessionPassword.seal(password.copyOf()) }
                val text = repository.readText(file, password)
                editor = EditorDocument(file, text, true).also { it.setSession(sealed) }
                sealed = null
            } finally { sealed?.clear(); password.fill('\u0000') }
        }
    }
    fun newFile() {
        if (busy) return
        editor = EditorDocument(PadFile(Uri.EMPTY, "New document", repository.defaultPrivate || !workspace, path = ""), "", false, fresh = true)
    }
    fun back() {
        if (busy || saving) return
        val doc = editor
        if (doc?.fresh == true && !doc.encrypted) { pendingNavigation = { refresh() }; save(false, null, true) }
        else navigate { refresh() }
    }
    fun saveAs(uri: Uri, password: CharArray?) {
        val doc = editor
        if (busy || doc == null) { password?.fill('\u0000'); return }
        val text = doc.value.text
        runOperation {
            var sealed: SessionPassword? = null
            try {
                // Finish an in-flight auto-save before switching the active destination.
                while (saving) kotlinx.coroutines.delay(20)
                if (password != null) sealed = withContext(Dispatchers.IO) { SessionPassword.seal(password.copyOf()) }
                val destination = repository.external(uri)
                val saved = repository.save(destination, text, password != null, password)
                doc.saved(saved, text, password != null)
                doc.setSession(sealed); sealed = null
                notice = "Saved as ${destination.name}. Auto-save now uses this file."
                reload()
            } finally { sealed?.clear(); password?.fill('\u0000') }
        }
    }
    fun save(encrypted: Boolean, password: CharArray?, navigateAfter: Boolean = false) {
        val doc = editor
        if (busy || saving || doc == null) { password?.fill('\u0000'); return }
        val text = doc.value.text
        saving = true
        savingProtected = encrypted
        viewModelScope.launch {
            var sealed: SessionPassword? = null
            var actualPassword: CharArray? = password
            try {
                if (encrypted && password != null) sealed = withContext(Dispatchers.IO) { SessionPassword.seal(password.copyOf()) }
                if (encrypted && actualPassword == null) actualPassword = withContext(Dispatchers.IO) { doc.sessionPassword?.open() }
                val target = if (doc.fresh) repository.create(repository.autoTitle(text, if (encrypted) "stxt" else "txt"), doc.file.vault) else doc.file
                val saved = repository.save(target, text, encrypted, actualPassword)
                doc.saved(saved, text, encrypted)
                if (!encrypted) doc.setSession(null)
                else if (sealed != null) { doc.setSession(sealed); sealed = null }
                files = (files.filterNot { it.uri == saved.uri } + saved).sortedByDescending { it.modified }
                if (navigateAfter && !doc.dirty) {
                    val action = pendingNavigation; pendingNavigation = null
                    doc.clear(); editor = null
                    viewModelScope.launch { kotlinx.coroutines.yield(); action?.invoke() }
                }
            } catch (e: Exception) {
                doc.saveFailure = true
                if (encrypted && password == null) doc.setSession(null)
                report(e)
            }
            finally { sealed?.clear(); actualPassword?.fill('\u0000'); password?.fill('\u0000'); saving = false; savingProtected = false }
            // An IME can commit text after Back/Save has taken its snapshot.
            // Save that newer revision too before leaving the document.
            if (navigateAfter && editor === doc && doc.dirty && !doc.saveFailure) save(encrypted, null, true)
        }
    }
    fun move(file: PadFile) = runOperation {
        val moved = repository.move(file, !file.vault)
        editor?.takeIf { it.file.uri == file.uri }?.let { it.file = moved }
        reload(); notice = if (moved.vault) "Moved to private vault." else "Moved to workspace."
    }
    fun rename(file: PadFile, name: String) = runOperation {
        val renamed = repository.rename(file, name)
        editor?.takeIf { it.file.uri == file.uri }?.let { it.file = renamed }
        reload()
    }
    fun trash(file: PadFile) = runOperation { repository.moveToTrash(file); reload(); notice = "Moved to Trash. You can restore it later." }
    fun restoreTrash(item: TrashItem) = runOperation { repository.restoreTrash(item.id); reload(); notice = "Restored ${item.name}." }
    fun deleteTrash(item: TrashItem) = runOperation { repository.deleteTrashPermanently(item.id); reload() }
    fun folder(uri: Uri) = runOperation { repository.chooseWorkspace(uri); workspace = true; reload() }
    fun backup(uri: Uri, password: CharArray) = runOperation {
        try { repository.backup(uri, password); notice = "Offline backup created. Keep it and its master password safe." }
        finally { password.fill('\u0000') }
    }
    fun restore(uri: Uri, password: CharArray, policy: ConflictPolicy) = runOperation {
        try { val r = repository.restore(uri, password, policy); reload(); notice = "Restored ${r.restored} files. Skipped ${r.skipped}." }
        finally { password.fill('\u0000') }
    }
    suspend fun external(uri: Uri) = repository.external(uri)
    override fun onCleared() { editor?.clear() }
}

class MainActivity : FragmentActivity() {
    private val prefs by lazy { getSharedPreferences("txt", MODE_PRIVATE) }
    private var animateGlass by mutableStateOf(true)
    private var appLock by mutableStateOf(false)
    private var unlocked by mutableStateOf(true)
    private var incoming by mutableStateOf<Uri?>(null)
    private var gateMessage by mutableStateOf<String?>(null)
    private var prompt: BiometricPrompt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        animateGlass = prefs.getBoolean("animated_glass", true)
        appLock = prefs.getBoolean("app_lock", false)
        unlocked = !appLock
        incoming = externalUri(intent)
        setContent { LiquidGlassTheme(animateBackdrop = animateGlass) { App() } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); incoming = externalUri(intent) }
    private fun externalUri(intent: Intent?): Uri? = if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_EDIT) intent?.data else null
    override fun onStop() {
        super.onStop()
        if (appLock && !isChangingConfigurations) unlocked = false
    }
    override fun onDestroy() { prompt?.cancelAuthentication(); prompt = null; super.onDestroy() }

    private fun authenticate(onSuccess: () -> Unit) {
        try {
            val auth = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                if (Build.VERSION.SDK_INT >= 30) BiometricManager.Authenticators.DEVICE_CREDENTIAL else 0
            check(BiometricManager.from(this).canAuthenticate(auth) == BiometricManager.BIOMETRIC_SUCCESS) {
                "Set up a strong fingerprint${if (Build.VERSION.SDK_INT >= 30) " or device PIN/password" else ""} in Android settings first."
            }
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val alias = "txt.app.authentication.v1"
            val key = (store.getKey(alias, null) as? SecretKey) ?: run {
                val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(false)
                if (Build.VERSION.SDK_INT >= 30) builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
                else builder.setUserAuthenticationValidityDurationSeconds(-1)
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(builder.build()) }.generateKey()
            }
            val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore").getKeySpec(key, KeyInfo::class.java) as KeyInfo
            check(info.isInsideSecureHardware) { "Hardware-backed authentication is unavailable on this device." }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
            prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
                        try { checkNotNull(result.cryptoObject?.cipher).doFinal(challenge).fill(0) } finally { challenge.fill(0) }
                        gateMessage = null; onSuccess()
                    } catch (e: Exception) { gateMessage = "Authentication could not be verified. ${e.message.orEmpty()}" }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { gateMessage = errString.toString() }
            })
            val builder = BiometricPrompt.PromptInfo.Builder().setTitle("Unlock TXT").setSubtitle("Verify with Android to access your documents").setAllowedAuthenticators(auth)
            if (Build.VERSION.SDK_INT < 30) builder.setNegativeButtonText("Cancel")
            prompt?.authenticate(builder.build(), BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) { gateMessage = e.message ?: "Authentication unavailable."
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun App(vm: PadViewModel = viewModel()) {
        var sensitive by remember { mutableStateOf(false) }
        var backupUri by remember { mutableStateOf<Uri?>(null) }
        var restoreUri by remember { mutableStateOf<Uri?>(null) }
        var restorePassword by remember { mutableStateOf(false) }
        var conflict by remember { mutableStateOf(ConflictPolicy.KEEP_BOTH) }
        var saveAsUri by remember { mutableStateOf<Uri?>(null) }
        var saveAsEncrypted by remember { mutableStateOf(false) }
        var saveBeforeLeaving by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(vm::folder) }
        val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { backupUri = it }
        val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { restoreUri = it }
        val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) { if (saveAsEncrypted) saveAsUri = uri else vm.saveAs(uri, null) }
        }
        val secure = appLock || vm.savingProtected || saveAsUri != null || vm.busy || sensitive || vm.editor?.encrypted == true || vm.unlockFile != null || !unlocked || backupUri != null || restorePassword
        SideEffect {
            if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        LaunchedEffect(incoming, unlocked, vm.busy, vm.saving) {
            val uri = incoming
            if (uri != null && unlocked && !vm.busy && !vm.saving) {
                incoming = null
                scope.launch {
                    try {
                        val file = vm.external(uri)
                        if (file.name.endsWith(".tpbak", true)) vm.navigate { restoreUri = uri }
                        else vm.open(file)
                    } catch (e: Exception) { vm.report(e) }
                }
            }
        }
        if (!unlocked) {
            LaunchedEffect(Unit) { authenticate { unlocked = true } }
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(Modifier.liquidGlass().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TXT", style = MaterialTheme.typography.displayMedium)
                    Text("Your documents are locked.", Modifier.padding(vertical = 16.dp))
                    gateMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { authenticate { unlocked = true } }) { Text("Unlock with Android") }
                }
            }
            return
        }
        val navigationMotion = LocalGlassMotionEnabled.current
        AnimatedContent(targetState = vm.editor, contentKey = { it?.sessionId ?: "home" }, label = "document expansion",
            transitionSpec = { if (!navigationMotion) EnterTransition.None.togetherWith(ExitTransition.None) else (fadeIn(tween(180)) + slideInVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.88f, stiffness = 420f), initialOffsetY = { it / 24 })).togetherWith(fadeOut(tween(120))) }) { doc ->
            if (doc == null && vm.showTrash) TrashScreen(vm.trash, vm.busy,
                onBack = { vm.showTrash = false }, onRestore = vm::restoreTrash, onDelete = vm::deleteTrash)
            else if (doc == null) HomeScreen(vm.files, vm.busy, vm.workspace, vm.defaultPrivate, appLock,
                onOpen = { if (it.name.endsWith(".tpbak", true)) restoreUri = it.uri else vm.open(it) },
                onFolder = { folderPicker.launch(vm.repository.workspace) }, onCreate = vm::newFile, onMove = vm::move,
                onBackup = { backupPicker.launch("TXT_Backup_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.tpbak") },
                onRestore = { restorePicker.launch(arrayOf("*/*")) },
                onPrivateDefault = { vm.defaultPrivate = it; vm.repository.defaultPrivate = it },
                onAppLock = { enable -> authenticate { appLock = enable; unlocked = true; prefs.edit().putBoolean("app_lock", enable).apply() } },
                onRefresh = vm::refresh, onRename = vm::rename, onTrash = vm::trash, onShowTrash = { vm.showTrash = true; vm.refresh() },
                motionEnabled = animateGlass, onMotionChange = { animateGlass = it; prefs.edit().putBoolean("animated_glass", it).apply() })
            else EditorScreen(doc, vm.busy, saving = vm.saving, onBack = vm::back,
                onSave = { e, p -> vm.save(e, p) }, onSaveAs = { name, encrypted -> saveAsEncrypted = encrypted; exportPicker.launch(name) }, onMove = { vm.move(doc.file) }, onSensitivePrompt = { sensitive = it })
        }
        saveAsUri?.let { uri -> PasswordDialog("Password for saved copy", confirm = true,
            onDismiss = { saveAsUri = null; scope.launch { runCatching { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) } } },
            onSensitive = { sensitive = it }, onPassword = { saveAsUri = null; vm.saveAs(uri, it) }) }
        vm.unlockFile?.let { f -> PasswordDialog("Unlock ${f.name}", onDismiss = { vm.unlockFile = null }, onSensitive = { sensitive = it }, onPassword = vm::unlock) }
        backupUri?.let { uri -> PasswordDialog("Backup master password", confirm = true,
            onDismiss = { backupUri = null; scope.launch { runCatching { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) } } },
            onSensitive = { sensitive = it }, onPassword = { backupUri = null; vm.backup(uri, it) }) }
        restoreUri?.let { uri ->
            if (!restorePassword) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { restoreUri = null }, title = { Text("Restore offline backup") }, text = {
                Column {
                    Text("Choose how to handle matching filenames. The backup is verified before files are changed.")
                    ConflictPolicy.entries.forEach { policy ->
                        TextButton(onClick = { conflict = policy }) { Text((if (conflict == policy) "✓ " else "") + when (policy) {
                            ConflictPolicy.KEEP_BOTH -> "Keep both (auto-rename)"; ConflictPolicy.OVERWRITE -> "Overwrite existing"; ConflictPolicy.SKIP -> "Skip existing"
                        }) }
                    }
                    if (!vm.workspace) OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Choose workspace for public files") }
                }
            }, confirmButton = { TextButton(onClick = { restorePassword = true }, enabled = !vm.busy) { Text("Enter master password") } }, dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Cancel") } })
            else PasswordDialog("Backup master password", onDismiss = { restorePassword = false; restoreUri = null }, onSensitive = { sensitive = it },
                onPassword = { restorePassword = false; restoreUri = null; vm.restore(uri, it, conflict) })
        }
        if (vm.pendingNavigation != null && !saveBeforeLeaving && !vm.saving) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { vm.pendingNavigation = null }, title = { Text("Save your changes?") },
            text = { Text("Your latest edits have not been saved.") },
            confirmButton = { TextButton(onClick = { if (vm.editor?.encrypted == true) saveBeforeLeaving = true else vm.save(false, null, true) }) { Text("Save") } },
            dismissButton = { Row {
                TextButton(onClick = vm::discardAndNavigate) { Text("Discard") }
                TextButton(onClick = { vm.pendingNavigation = null }) { Text("Keep editing") }
            } })
        if (saveBeforeLeaving) PasswordDialog("Password to save", onDismiss = { saveBeforeLeaving = false }, onSensitive = { sensitive = it },
            onPassword = { saveBeforeLeaving = false; vm.save(true, it, true) })
        vm.notice?.let { notice -> AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { vm.notice = null }, title = { Text("TXT") }, text = { Text(notice) },
            confirmButton = { TextButton(onClick = { vm.notice = null }) { Text("OK") } }) }
        if (gateMessage != null && unlocked) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { gateMessage = null }, title = { Text("Android authentication") }, text = { Text(gateMessage.orEmpty()) },
            confirmButton = { TextButton(onClick = { gateMessage = null }) { Text("OK") } })
    }
}
```

## Supporting source: app/src/main/java/com/secure/textpad/ui/theme/GlassControls.kt

```kotlin
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
    interactionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    TextButton(onClick, modifier.heightIn(min = 48.dp).glassPress(source), enabled = enabled,
        shape = RoundedCornerShape(18.dp), interactionSource = source,
        contentPadding = contentPadding, content = content)
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
```

## Supporting source: app/src/main/java/com/secure/textpad/security/SessionPassword.kt

```kotlin
package com.secure.textpad.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** RAM-only sealed credential for an explicitly unlocked document; no password is written to disk. */
class SessionPassword private constructor(private val nonce: ByteArray, private val sealed: ByteArray) {
    private var cleared = false
    @Synchronized fun open(): CharArray {
        check(!cleared) { "Unlock the document again to resume auto-save." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, nonce))
        val bytes = cipher.doFinal(sealed)
        return try {
            val decoded = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes))
            try { CharArray(decoded.remaining()).also { decoded.get(it) } }
            finally { if (decoded.hasArray()) decoded.array().fill('\u0000') }
        } finally { bytes.fill(0) }
    }
    @Synchronized fun clear() { nonce.fill(0); sealed.fill(0); cleared = true }
    companion object {
        private const val ALIAS = "txt.ram.session.password.v1"
        @Synchronized private fun wrappingKey(): SecretKey {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            return (store.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).setRandomizedEncryptionRequired(true).build())
            }.generateKey()
        }
        fun seal(password: CharArray): SessionPassword {
            var encoded: ByteBuffer? = null
            try {
                encoded = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(password))
                val bytes = ByteArray(encoded.remaining())
                try {
                    encoded.get(bytes)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
                    return SessionPassword(cipher.iv.copyOf(), cipher.doFinal(bytes))
                } finally { bytes.fill(0) }
            } finally {
                password.fill('\u0000')
                if (encoded?.hasArray() == true) encoded.array().fill(0)
            }
        }
    }
}
```
