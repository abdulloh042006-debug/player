package com.shaxsiy.player.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.audiofx.Visualizer
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaxsiy.player.ui.theme.DangerRed
import com.shaxsiy.player.ui.theme.NeonCyan
import com.shaxsiy.player.ui.theme.NeonMagenta
import com.shaxsiy.player.ui.theme.SurfaceElevated
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ============================================================================
// 1. VIZUALIZATOR REJIMLARI
// ============================================================================
enum class VisualizerMode(val label: String) {
    CIRCULAR("Dumaloq"),
    WAVE("To‘lqin"),
    PULSE_LINE("Impuls"),
    OFF("O‘chiq")
}

// ============================================================================
// 2. HAR BIR QO‘SHIQ UCHUN RASM SAQLASH, TAHRIRLASH VA O‘CHIRISH REPOSITORYSI
// ============================================================================
class TrackCoverRepository(private val context: Context) {

    private val coverDir: File by lazy {
        File(context.filesDir, "track_covers").apply {
            if (!exists()) mkdirs()
        }
    }

    private val memoryCache = LruCache<String, Bitmap>(24)

    private val _changeVersion = MutableStateFlow(0)
    val changeVersion: StateFlow<Int> = _changeVersion.asStateFlow()

    private fun fileForTrack(trackUri: String): File {
        val hash = sha256(trackUri)
        return File(coverDir, "$hash.jpg")
    }

    suspend fun saveOrUpdateCover(trackUri: String, imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rawBitmap = context.contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return@withContext false

            val scaled = scaleDownBitmap(rawBitmap, maxDimension = 720)
            if (scaled !== rawBitmap) rawBitmap.recycle()

            val targetFile = fileForTrack(trackUri)
            FileOutputStream(targetFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }

            memoryCache.put(trackUri, scaled)
            _changeVersion.value += 1
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteCover(trackUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            memoryCache.remove(trackUri)
            val file = fileForTrack(trackUri)
            val deleted = if (file.exists()) file.delete() else true
            _changeVersion.value += 1
            deleted
        } catch (_: Exception) {
            false
        }
    }

    suspend fun loadCoverBitmap(trackUri: String): Bitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(trackUri)?.let { return@withContext it }
        val file = fileForTrack(trackUri)
        if (!file.exists()) return@withContext null
        try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) memoryCache.put(trackUri, bmp)
            bmp
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleDownBitmap(src: Bitmap, maxDimension: Int): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= maxDimension && height <= maxDimension) return src
        val ratio = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val newW = max(1, (width * ratio).toInt())
        val newH = max(1, (height * ratio).toInt())
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

// ============================================================================
// 3. REAL AUDIO + SIMULYATSIYA AMPLITUDA MANBASI
// ============================================================================
@Composable
private fun rememberAudioAmplitudes(
    isPlaying: Boolean,
    audioSessionId: Int,
    barCount: Int = 48
): List<Float> {
    var amplitudes by remember(barCount) { mutableStateOf(List(barCount) { 0f }) }
    var realVisualizerActive by remember { mutableStateOf(false) }

    DisposableEffect(audioSessionId, isPlaying, barCount) {
        var visualizer: Visualizer? = null
        if (isPlaying && audioSessionId > 0) {
            try {
                visualizer = Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(256)
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                vis: Visualizer?,
                                waveform: ByteArray?,
                                samplingRate: Int
                            ) {
                                if (waveform == null || waveform.isEmpty()) return
                                val step = max(1, waveform.size / barCount)
                                val next = List(barCount) { i ->
                                    val idx = (i * step).coerceAtMost(waveform.lastIndex)
                                    val sample = ((waveform[idx].toInt() and 0xFF) - 128) / 128f
                                    abs(sample).coerceIn(0f, 1f)
                                }
                                amplitudes = next
                            }

                            override fun onFftDataCapture(
                                vis: Visualizer?,
                                fft: ByteArray?,
                                samplingRate: Int
                            ) {
                            }
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        true,
                        false
                    )
                    enabled = true
                }
                realVisualizerActive = true
            } catch (_: Exception) {
                realVisualizerActive = false
            }
        } else {
            realVisualizerActive = false
        }

        onDispose {
            try {
                visualizer?.enabled = false
                visualizer?.release()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(isPlaying, realVisualizerActive, barCount) {
        if (realVisualizerActive) return@LaunchedEffect
        var phase = 0f
        while (isActive) {
            if (isPlaying) {
                phase += 0.24f
                amplitudes = List(barCount) { idx ->
                    val norm = idx.toFloat() / barCount.toFloat()
                    val wave1 = sin(phase + norm * 12f)
                    val wave2 = cos(phase * 1.8f - norm * 20f)
                    val beat = abs(sin(phase * 0.7f))
                    val raw = (0.30f + 0.38f * abs(wave1) + 0.32f * abs(wave2)) * (0.45f + 0.55f * beat)
                    raw.coerceIn(0.08f, 1f)
                }
                delay(42L)
            } else {
                val decayed = amplitudes.map { if (it < 0.01f) 0f else it * 0.72f }
                amplitudes = decayed
                delay(if (decayed.all { it == 0f }) 180L else 40L)
            }
        }
    }

    return amplitudes
}

// ============================================================================
// 4. ASOSIY KOMPONENT: GLASSMORPHISM MUQOVA + NEON CYAN/MAGENTA VIZUALIZATOR
// ============================================================================
@Composable
fun TrackCoverVisualizerSection(
    trackUri: String?,
    isPlaying: Boolean,
    audioSessionId: Int = 0,
    accentColor: Color = NeonCyan,
    secondaryColor: Color = NeonMagenta,
    modifier: Modifier = Modifier,
    onCoverChanged: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val coverRepo = remember(context) { TrackCoverRepository(context) }
    val coverVersion by coverRepo.changeVersion.collectAsState()

    var visualizerMode by remember { mutableStateOf(VisualizerMode.CIRCULAR) }
    var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showCoverOptionsDialog by remember { mutableStateOf(false) }

    val amplitudes = rememberAudioAmplitudes(
        isPlaying = isPlaying,
        audioSessionId = audioSessionId,
        barCount = 48
    )

    LaunchedEffect(trackUri, coverVersion) {
        coverBitmap = if (trackUri != null) {
            coverRepo.loadCoverBitmap(trackUri)?.asImageBitmap()
        } else {
            null
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { pickedUri: Uri? ->
        if (pickedUri != null && trackUri != null) {
            scope.launch {
                val ok = coverRepo.saveOrUpdateCover(trackUri, pickedUri)
                if (ok) onCoverChanged?.invoke("Qo‘shiq rasmi saqlandi")
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(264.dp),
            contentAlignment = Alignment.Center
        ) {
            when (visualizerMode) {
                VisualizerMode.CIRCULAR -> {
                    CircularNeonVisualizerCanvas(
                        amplitudes = amplitudes,
                        isPlaying = isPlaying,
                        primaryColor = accentColor,
                        secondaryColor = secondaryColor,
                        modifier = Modifier.size(258.dp)
                    )
                    GlassCoverBox(
                        bitmap = coverBitmap,
                        primaryColor = accentColor,
                        secondaryColor = secondaryColor,
                        isCircular = true,
                        modifier = Modifier
                            .size(148.dp)
                            .iosBounceClickable(enabled = trackUri != null) {
                                showCoverOptionsDialog = true
                            }
                    )
                }

                VisualizerMode.WAVE -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        GlassCoverBox(
                            bitmap = coverBitmap,
                            primaryColor = accentColor,
                            secondaryColor = secondaryColor,
                            isCircular = false,
                            modifier = Modifier
                                .size(172.dp)
                                .iosBounceClickable(enabled = trackUri != null) {
                                    showCoverOptionsDialog = true
                                }
                        )
                        WaveNeonVisualizerCanvas(
                            amplitudes = amplitudes,
                            primaryColor = accentColor,
                            secondaryColor = secondaryColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(74.dp)
                                .padding(horizontal = 6.dp)
                        )
                    }
                }

                VisualizerMode.PULSE_LINE -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        GlassCoverBox(
                            bitmap = coverBitmap,
                            primaryColor = accentColor,
                            secondaryColor = secondaryColor,
                            isCircular = false,
                            modifier = Modifier
                                .size(172.dp)
                                .iosBounceClickable(enabled = trackUri != null) {
                                    showCoverOptionsDialog = true
                                }
                        )
                        PulseLineNeonVisualizerCanvas(
                            amplitudes = amplitudes,
                            isPlaying = isPlaying,
                            primaryColor = accentColor,
                            secondaryColor = secondaryColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(74.dp)
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                VisualizerMode.OFF -> {
                    GlassCoverBox(
                        bitmap = coverBitmap,
                        primaryColor = accentColor,
                        secondaryColor = secondaryColor,
                        isCircular = false,
                        modifier = Modifier
                            .size(220.dp)
                            .iosBounceClickable(enabled = trackUri != null) {
                                showCoverOptionsDialog = true
                            }
                    )
                }
            }

            // O‘ng yuqori burchakda shisha rasm tahrirlash tugmasi
            if (trackUri != null) {
                GlassCircleIconButton(
                    onClick = { showCoverOptionsDialog = true },
                    size = 40.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (coverBitmap != null) Icons.Default.Edit else Icons.Default.Image,
                        contentDescription = "Rasmni tahrirlash",
                        tint = accentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Vizualizator rejimini almashtirish shisha kapsulalari
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VisualizerMode.entries.forEach { mode ->
                val selected = visualizerMode == mode
                GlassPillButton(
                    onClick = { visualizerMode = mode },
                    isGlowing = selected,
                    glowColor = accentColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = mode.label,
                            color = if (selected) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showCoverOptionsDialog && trackUri != null) {
        val hasCover = coverBitmap != null
        AlertDialog(
            onDismissRequest = { showCoverOptionsDialog = false },
            containerColor = SurfaceElevated,
            title = {
                Text(
                    text = if (hasCover) "Qo‘shiq rasmini tahrirlash" else "Qo‘shiqqa rasm qo‘yish",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassCard(
                        cornerRadius = 14.dp,
                        onClick = {
                            showCoverOptionsDialog = false
                            pickImageLauncher.launch("image/*")
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (hasCover) "Galereyadan boshqa rasm tanlash" else "Galereyadan rasm tanlash",
                                color = TextPrimary
                            )
                        }
                    }

                    if (hasCover) {
                        GlassCard(
                            cornerRadius = 14.dp,
                            isHighlighted = true,
                            accentColor = DangerRed,
                            onClick = {
                                showCoverOptionsDialog = false
                                scope.launch {
                                    val ok = coverRepo.deleteCover(trackUri)
                                    if (ok) onCoverChanged?.invoke("Qo‘shiq rasmi o‘chirildi")
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Rasmni o‘chirish", color = DangerRed)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverOptionsDialog = false }) {
                    Text("Yopish", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun GlassCoverBox(
    bitmap: ImageBitmap?,
    primaryColor: Color,
    secondaryColor: Color,
    isCircular: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = if (isCircular) CircleShape else RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.8f), secondaryColor.copy(alpha = 0.8f))),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Qo‘shiq rasmi",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(if (isCircular) 56.dp else 72.dp)
            )
        }
    }
}

@Composable
private fun WaveNeonVisualizerCanvas(
    amplitudes: List<Float>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (amplitudes.isEmpty()) return@Canvas
        val barWidth = size.width / (amplitudes.size * 1.65f)
        val gap = (size.width - barWidth * amplitudes.size) / max(1, amplitudes.size - 1)
        val centerY = size.height / 2f

        val brush = Brush.horizontalGradient(
            colors = listOf(primaryColor, Color(0xFF7B61FF), secondaryColor)
        )

        amplitudes.forEachIndexed { index, amp ->
            val clamped = amp.coerceIn(0.06f, 1f)
            val barHeight = max(6f, size.height * clamped)
            val left = index * (barWidth + gap)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun PulseLineNeonVisualizerCanvas(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "pulse_shift")
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(modifier = modifier) {
        val midY = size.height / 2f
        val width = size.width
        val count = amplitudes.size.coerceAtLeast(2)
        val stepX = width / (count - 1).toFloat()

        val lineBrush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor))

        drawLine(
            color = primaryColor.copy(alpha = 0.22f),
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 2f
        )

        val path = Path()
        for (i in 0 until count) {
            val x = i * stepX
            val normalizedPos = i.toFloat() / (count - 1).toFloat()
            val yOffset = if (!isPlaying) {
                0f
            } else {
                val distToPulse = abs(normalizedPos - sweep)
                val pulseWindow = (1f - (distToPulse * 4.2f)).coerceIn(0f, 1f)
                val direction = if (i % 2 == 0) -1f else 1f
                val spike = amplitudes[i] * (0.30f + 0.70f * pulseWindow)
                direction * spike * (size.height * 0.44f)
            }
            val y = midY + yOffset
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.35f), secondaryColor.copy(alpha = 0.35f))),
            style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = path,
            brush = lineBrush,
            style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun CircularNeonVisualizerCanvas(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "circle_rot")
    val rotationDeg by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val innerRadius = min(size.width, size.height) * 0.32f
        val maxBarLen = min(size.width, size.height) * 0.16f

        val ringBrush = Brush.sweepGradient(
            colors = listOf(primaryColor, secondaryColor, primaryColor),
            center = center
        )

        // Tashqi yumshoq neon aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isPlaying) 0.22f else 0.10f),
                    secondaryColor.copy(alpha = if (isPlaying) 0.12f else 0.04f),
                    Color.Transparent
                ),
                center = center,
                radius = innerRadius + maxBarLen * 1.1f
            ),
            radius = innerRadius + maxBarLen * 1.1f,
            center = center
        )

        drawCircle(
            brush = ringBrush,
            radius = innerRadius,
            center = center,
            style = Stroke(width = 2.8f)
        )

        val count = amplitudes.size.coerceAtLeast(1)
        val angleStep = 360f / count
        val baseAngle = if (isPlaying) rotationDeg else 0f

        for (i in 0 until count) {
            val angleRad = ((baseAngle + i * angleStep) * PI / 180.0)
            val amp = if (isPlaying) amplitudes[i].coerceIn(0.08f, 1f) else 0.06f
            val length = maxBarLen * amp

            val startX = center.x + (innerRadius * cos(angleRad)).toFloat()
            val startY = center.y + (innerRadius * sin(angleRad)).toFloat()
            val endX = center.x + ((innerRadius + length) * cos(angleRad)).toFloat()
            val endY = center.y + ((innerRadius + length) * sin(angleRad)).toFloat()

            val fraction = i.toFloat() / count.toFloat()
            val barColor = if (fraction < 0.5f) {
                primaryColor.copy(alpha = 0.55f + 0.45f * amp)
            } else {
                secondaryColor.copy(alpha = 0.55f + 0.45f * amp)
            }

            drawLine(
                color = barColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4.2f,
                cap = StrokeCap.Round
            )
        }
    }
}