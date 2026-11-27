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
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
    WAVE("To‘lqin"),
    PULSE_LINE("Impuls chizig‘i"),
    CIRCULAR("Dumaloq"),
    OFF("O‘chiq");

    fun next(): VisualizerMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

// ============================================================================
// 2. HAR BIR QO‘SHIQ UCHUN RASM SAQLASH, TAHRIRLASH VA O‘CHIRISH REPOSITORYSI
// ============================================================================
class TrackCoverRepository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: TrackCoverRepository? = null

        fun get(context: Context): TrackCoverRepository {
            return instance ?: synchronized(this) {
                instance ?: TrackCoverRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val coverDir: File by lazy {
        File(context.filesDir, "track_covers").apply {
            if (!exists()) mkdirs()
        }
    }

    // Tezkor xotira keshi (RAM tejash uchun max 24 ta rasm)
    private val memoryCache = LruCache<String, Bitmap>(24)

    // Rasm o‘zgarganda UI avtomatik yangilanishi uchun versiya oqimi
    private val _changeVersion = MutableStateFlow(0)
    val changeVersion: StateFlow<Int> = _changeVersion.asStateFlow()

    private fun fileForTrack(trackUri: String): File {
        val hash = sha256(trackUri)
        return File(coverDir, "$hash.jpg")
    }

    fun hasCustomCover(trackUri: String): Boolean {
        return fileForTrack(trackUri).exists()
    }

    /**
     * Galereyadan tanlangan rasmni siqib, ichki xotiraga saqlaydi (qo‘shish yoki tahrirlash).
     */
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

    /**
     * Qo‘shiqqa biriktirilgan shaxsiy rasmni o‘chiradi.
     */
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

    /**
     * Qo‘shiqning shaxsiy rasmini yuklaydi (avval keshdan, keyin diskdan).
     */
    suspend fun loadCoverBitmap(trackUri: String): Bitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(trackUri)?.let { return@withContext it }
        val file = fileForTrack(trackUri)
        if (!file.exists()) return@withContext null
        try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                memoryCache.put(trackUri, bmp)
            }
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

@Composable
fun TrackCoverThumbnail(
    trackUri: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF1DB954),
    surfaceColor: Color = Color(0xFF1A1A22),
    isCircular: Boolean = false
) {
    val context = LocalContext.current
    val repository = remember(context) { TrackCoverRepository.get(context) }
    val coverVersion by repository.changeVersion.collectAsState()
    var bitmap by remember(trackUri) { mutableStateOf<ImageBitmap?>(null) }
    val shape = if (isCircular) CircleShape else RoundedCornerShape(12.dp)

    LaunchedEffect(trackUri, coverVersion) {
        bitmap = repository.loadCoverBitmap(trackUri)?.asImageBitmap()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(surfaceColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Qo‘shiq rasmi",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================================================
// 3. REAL AUDIO + SIMULYATSIYA AMPLITUDA MANBASI (XATOLARGA CHIDAMLI)
// ============================================================================
@Composable
private fun rememberAudioAmplitudes(
    isPlaying: Boolean,
    audioSessionId: Int,
    barCount: Int = 40
): List<Float> {
    var amplitudes by remember(barCount) { mutableStateOf(List(barCount) { 0f }) }
    var realVisualizerActive by remember { mutableStateOf(false) }

    // 1) Agar RECORD_AUDIO ruxsati va audioSessionId mavjud bo‘lsa, haqiqiy audio to‘lqinni o‘qish
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
                                // Waveform yetarli
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

    // 2) Agar real Visualizer ishlamasa (ruxsat yo‘q yoki pauza) — silliq dinamik fallback
    LaunchedEffect(isPlaying, realVisualizerActive, barCount) {
        if (realVisualizerActive) return@LaunchedEffect
        var phase = 0f
        while (isActive) {
            if (isPlaying) {
                phase += 0.22f
                amplitudes = List(barCount) { idx ->
                    val norm = idx.toFloat() / barCount.toFloat()
                    val wave1 = sin(phase + norm * 10f)
                    val wave2 = cos(phase * 1.7f - norm * 18f)
                    val beat = abs(sin(phase * 0.65f))
                    val raw = (0.35f + 0.35f * abs(wave1) + 0.30f * abs(wave2)) * (0.45f + 0.55f * beat)
                    raw.coerceIn(0.08f, 1f)
                }
                delay(45L)
            } else {
                // Pauza holatida tekis chiziqqa silliq tushish
                val decayed = amplitudes.map { if (it < 0.01f) 0f else it * 0.72f }
                amplitudes = decayed
                if (decayed.all { it == 0f }) {
                    delay(180L)
                } else {
                    delay(40L)
                }
            }
        }
    }

    return amplitudes
}

// ============================================================================
// 4. ASOSIY KOMPONENT: RASM QO‘YISH/TAHRIRLASH/O‘CHIRISH + 3 XIL VIZUALIZATOR
// ============================================================================
@Composable
fun TrackCoverVisualizerSection(
    trackUri: String?,
    isPlaying: Boolean,
    audioSessionId: Int = 0,
    accentColor: Color = Color(0xFF1DB954),
    surfaceColor: Color = Color(0xFF1A1A22),
    modifier: Modifier = Modifier,
    onCoverChanged: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val coverRepo = remember(context) { TrackCoverRepository.get(context) }
    val coverVersion by coverRepo.changeVersion.collectAsState()

    var visualizerMode by remember { mutableStateOf(VisualizerMode.CIRCULAR) }
    var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showCoverOptionsDialog by remember { mutableStateOf(false) }

    val amplitudes = rememberAudioAmplitudes(
        isPlaying = isPlaying,
        audioSessionId = audioSessionId,
        barCount = 44
    )

    // Qo‘shiq yoki rasm versiyasi o‘zgarganda rasmni qayta yuklash
    LaunchedEffect(trackUri, coverVersion) {
        coverBitmap = if (trackUri != null) {
            coverRepo.loadCoverBitmap(trackUri)?.asImageBitmap()
        } else {
            null
        }
    }

    // Galereyadan rasm tanlash launcheri
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
        // --- MARKAZIY RASM + VIZUALIZATOR MAYDONI ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            when (visualizerMode) {
                VisualizerMode.CIRCULAR -> {
                    // Ekran o‘rtasida dumaloq rasm va uning atrofida aylanuvchi/pulsatsiyali vizualizator
                    CircularVisualizerCanvas(
                        amplitudes = amplitudes,
                        isPlaying = isPlaying,
                        accentColor = accentColor,
                        modifier = Modifier.size(240.dp)
                    )
                    CoverImageBox(
                        bitmap = coverBitmap,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isCircular = true,
                        modifier = Modifier
                            .size(142.dp)
                            .clickable(enabled = trackUri != null) { showCoverOptionsDialog = true }
                    )
                }

                VisualizerMode.WAVE -> {
                    // To‘rtburchak yumaloq muqova + pastida dinamik to‘lqin vizualizatori
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        CoverImageBox(
                            bitmap = coverBitmap,
                            accentColor = accentColor,
                            surfaceColor = surfaceColor,
                            isCircular = false,
                            modifier = Modifier
                                .size(164.dp)
                                .clickable(enabled = trackUri != null) { showCoverOptionsDialog = true }
                        )
                        WaveVisualizerCanvas(
                            amplitudes = amplitudes,
                            accentColor = accentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(horizontal = 8.dp)
                        )
                    }
                }

                VisualizerMode.PULSE_LINE -> {
                    // Tekis chiziqdan impuls (ossillograf) kabi sakrovchi vizualizator
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        CoverImageBox(
                            bitmap = coverBitmap,
                            accentColor = accentColor,
                            surfaceColor = surfaceColor,
                            isCircular = false,
                            modifier = Modifier
                                .size(164.dp)
                                .clickable(enabled = trackUri != null) { showCoverOptionsDialog = true }
                        )
                        PulseLineVisualizerCanvas(
                            amplitudes = amplitudes,
                            isPlaying = isPlaying,
                            accentColor = accentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                VisualizerMode.OFF -> {
                    CoverImageBox(
                        bitmap = coverBitmap,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isCircular = false,
                        modifier = Modifier
                            .size(210.dp)
                            .clickable(enabled = trackUri != null) { showCoverOptionsDialog = true }
                    )
                }
            }

            // O‘ng yuqori burchakda rasmni tahrirlash tugmasi
            if (trackUri != null) {
                Surface(
                    color = surfaceColor.copy(alpha = 0.9f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    IconButton(
                        onClick = { showCoverOptionsDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (coverBitmap != null) Icons.Default.Edit else ImageIcon,
                            contentDescription = "Rasmni tahrirlash",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- VIZUALIZATOR REJIMINI ALMASHTIRISH TUGMALARI ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VisualizerMode.entries.forEach { mode ->
                val selected = visualizerMode == mode
                Surface(
                    color = if (selected) accentColor else surfaceColor,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { visualizerMode = mode }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = mode.label,
                            color = if (selected) Color.Black else Color(0xFFB5B5C0),
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // --- RASM QO‘YISH / TAHRIRLASH / O‘CHIRISH DIALOGI ---
    if (showCoverOptionsDialog && trackUri != null) {
        val hasCover = coverBitmap != null
        AlertDialog(
            onDismissRequest = { showCoverOptionsDialog = false },
            containerColor = surfaceColor,
            title = {
                Text(
                    text = if (hasCover) "Qo‘shiq rasmini tahrirlash" else "Qo‘shiqqa rasm qo‘yish",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color(0xFF252530),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCoverOptionsDialog = false
                                pickImageLauncher.launch("image/*")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(ImageIcon, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (hasCover) "Galereyadan boshqa rasm tanlash" else "Galereyadan rasm tanlash",
                                color = Color.White
                            )
                        }
                    }

                    if (hasCover) {
                        Surface(
                            color = Color(0xFF2A181B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCoverOptionsDialog = false
                                    scope.launch {
                                        val ok = coverRepo.deleteCover(trackUri)
                                        if (ok) onCoverChanged?.invoke("Qo‘shiq rasmi o‘chirildi")
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Rasmni o‘chirish", color = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverOptionsDialog = false }) {
                    Text("Yopish", color = Color(0xFF9A9AA6))
                }
            }
        )
    }
}

// ============================================================================
// 5. MUQOVA RASMI KONTAYNERI
// ============================================================================
@Composable
private fun CoverImageBox(
    bitmap: ImageBitmap?,
    accentColor: Color,
    surfaceColor: Color,
    isCircular: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = if (isCircular) CircleShape else RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(surfaceColor),
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
                tint = accentColor,
                modifier = Modifier.size(if (isCircular) 56.dp else 72.dp)
            )
        }
    }
}

// ============================================================================
// 6. VIZUALIZATOR #1: TO‘LQIN (WAVE / SPEKTR USTUNLARI)
// ============================================================================
@Composable
private fun WaveVisualizerCanvas(
    amplitudes: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (amplitudes.isEmpty()) return@Canvas
        val barWidth = size.width / (amplitudes.size * 1.6f)
        val gap = (size.width - barWidth * amplitudes.size) / max(1, amplitudes.size - 1)
        val centerY = size.height / 2f

        val brush = Brush.verticalGradient(
            colors = listOf(accentColor, accentColor.copy(alpha = 0.45f), accentColor)
        )

        amplitudes.forEachIndexed { index, amp ->
            val clamped = amp.coerceIn(0.05f, 1f)
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

// ============================================================================
// 7. VIZUALIZATOR #2: TEKIS CHIZIQ + IMPULS (OSSILLOGRAF USLUBIDA)
// ============================================================================
@Composable
private fun PulseLineVisualizerCanvas(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "pulse_shift")
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(modifier = modifier) {
        val midY = size.height / 2f
        val width = size.width
        val count = amplitudes.size.coerceAtLeast(2)
        val stepX = width / (count - 1).toFloat()

        // Asosiy fon tekis chizig‘i
        drawLine(
            color = accentColor.copy(alpha = 0.22f),
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 2f
        )

        // Dinamik impuls chizig‘i (pauzada tekis, ijroda o‘tkir impulslar bilan)
        val path = Path()
        for (i in 0 until count) {
            val x = i * stepX
            val normalizedPos = i.toFloat() / (count - 1).toFloat()
            val yOffset = if (!isPlaying) {
                0f
            } else {
                // Markaz va harakatlanuvchi impuls oynasida aniq sakrashlar
                val distToPulse = abs(normalizedPos - sweep)
                val pulseWindow = (1f - (distToPulse * 4.5f)).coerceIn(0f, 1f)
                val direction = if (i % 2 == 0) -1f else 1f
                val baseAmp = amplitudes[i]
                val spike = baseAmp * (0.28f + 0.72f * pulseWindow)
                direction * spike * (size.height * 0.44f)
            }
            val y = midY + yOffset
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Yengil nur (glow) qatlami
        drawPath(
            path = path,
            color = accentColor.copy(alpha = 0.28f),
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Asosiy o‘tkir impuls chizig‘i
        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ============================================================================
// 8. VIZUALIZATOR #3: MARKAZIY RASM ATROFIDA DUMALOQ IMPULS HALQASI
// ============================================================================
@Composable
private fun CircularVisualizerCanvas(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "circle_rot")
    val rotationDeg by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val innerRadius = min(size.width, size.height) * 0.32f
        val maxBarLen = min(size.width, size.height) * 0.16f

        // Ichki nafis halqa
        drawCircle(
            color = accentColor.copy(alpha = 0.30f),
            radius = innerRadius,
            center = center,
            style = Stroke(width = 2.5f)
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

            drawLine(
                color = accentColor.copy(alpha = 0.45f + 0.55f * amp),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4.2f,
                cap = StrokeCap.Round
            )
        }
    }
}
