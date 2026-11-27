package com.shaxsiy.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaxsiy.player.ui.theme.DeepSpaceBg
import com.shaxsiy.player.ui.theme.NeonCyan
import com.shaxsiy.player.ui.theme.NeonMagenta
import com.shaxsiy.player.ui.theme.SurfaceElevated
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary
import com.shaxsiy.player.viewmodel.PlaybackUiState
import com.shaxsiy.player.viewmodel.RepeatModeUi
import com.shaxsiy.player.viewmodel.SleepTimerMode

fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (hours > 0L) {
        "%02d:%02d:%02d".format(hours, minutes % 60L, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
fun MiniPlayer(
    state: PlaybackUiState,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onForward10: () -> Unit,
    onBackward10: () -> Unit
) {
    val track = state.currentTrack ?: return
    val progress = if (state.durationMs > 0L) {
        (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        cornerRadius = 22.dp,
        isHighlighted = state.isPlaying,
        onClick = onExpand
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(NeonCyan.copy(alpha = 0.28f), NeonMagenta.copy(alpha = 0.28f))
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeonCyan)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${state.activeGroupName ?: "Oflayn"} • ${track.artist}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onBackward10, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = TextPrimary, modifier = Modifier.size(21.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                GlassCircleIconButton(
                    onClick = onPlayPause,
                    size = 40.dp,
                    isGlowing = true,
                    glowColor = NeonCyan
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onForward10, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = TextPrimary, modifier = Modifier.size(21.dp))
                }
            }

            // Neon gradient progress chizig‘i
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta)))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    state: PlaybackUiState,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onForward10: () -> Unit,
    onBackward10: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSetSleepMinutes: (Int) -> Unit,
    onSetSleepEndOfTrack: () -> Unit,
    onCancelSleep: () -> Unit,
    onCoverChanged: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepSpaceBg,
        dragHandle = null
    ) {
        GlassAmbientBackground(isPlaying = state.isPlaying) {
            val track = state.currentTrack
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // iOS uslubidagi yuqori handle + sarlavha
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GlassCircleIconButton(onClick = onDismiss, size = 40.dp) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Yopish",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("JORIY GURUH", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.2.sp)
                            Text(
                                text = state.activeGroupName ?: "Barcha musiqalar",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                }

                // Markaziy rasm + 3 xil neon vizualizator
                TrackCoverVisualizerSection(
                    trackUri = track?.uri,
                    isPlaying = state.isPlaying,
                    accentColor = NeonCyan,
                    secondaryColor = NeonMagenta,
                    onCoverChanged = onCoverChanged
                )

                // Pastki Glassmorphism boshqaruv paneli
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = track?.title ?: "Qo‘shiq tanlanmagan",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track?.artist ?: "",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val maxDur = state.durationMs.coerceAtLeast(1L).toFloat()
                    Slider(
                        value = state.currentPositionMs.toFloat().coerceIn(0f, maxDur),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..maxDur,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTimeMs(state.currentPositionMs), color = TextSecondary, fontSize = 12.sp)
                        Text(formatTimeMs(state.durationMs), color = TextSecondary, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Katta shisha -10s va +10s tugmalari
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassPillButton(onClick = onBackward10) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = NeonCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("-10s", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }

                        GlassPillButton(onClick = onForward10) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("+10s", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = NeonMagenta, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Asosiy tugmalar: Shuffle | Prev | Play/Pause | Next | Repeat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassCircleIconButton(
                            onClick = onToggleShuffle,
                            size = 44.dp,
                            isGlowing = state.isShuffleEnabled,
                            glowColor = NeonCyan
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Aralashtirish",
                                tint = if (state.isShuffleEnabled) TextPrimary else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        GlassCircleIconButton(onClick = onPrevious, size = 50.dp) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Oldingi",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Neon nurlanib turuvchi katta Play/Pause tugmasi
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(NeonCyan, NeonMagenta))
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                                .iosBounceClickable(scaleDown = 0.90f, onClick = onPlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = DeepSpaceBg,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        GlassCircleIconButton(onClick = onNext, size = 50.dp) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Keyingi",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        val repeatActive = state.repeatMode != RepeatModeUi.REPEAT_OFF
                        GlassCircleIconButton(
                            onClick = onCycleRepeat,
                            size = 44.dp,
                            isGlowing = repeatActive,
                            glowColor = NeonMagenta
                        ) {
                            val icon = when (state.repeatMode) {
                                RepeatModeUi.REPEAT_ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            }
                            Icon(
                                icon,
                                contentDescription = "Takrorlash",
                                tint = if (repeatActive) TextPrimary else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pastki qo‘shimcha panel: Tezlik | Takrorlash holati | Uyqu taymeri
                    GlassCard(cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showSpeedDialog = true }) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("${state.playbackSpeed}x", color = TextPrimary, fontSize = 13.sp)
                            }

                            Text(
                                text = when (state.repeatMode) {
                                    RepeatModeUi.REPEAT_OFF -> "Takrorlash: O‘chiq"
                                    RepeatModeUi.REPEAT_ONE -> "1 qo‘shiq takrori"
                                    RepeatModeUi.REPEAT_GROUP -> "Butun guruh takrori"
                                },
                                color = if (state.repeatMode == RepeatModeUi.REPEAT_OFF) TextSecondary else NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            TextButton(onClick = { showSleepDialog = true }) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (state.sleepTimer is SleepTimerMode.Off) TextSecondary else NeonMagenta,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                val timerLabel = when (val t = state.sleepTimer) {
                                    is SleepTimerMode.Off -> "Taymer"
                                    is SleepTimerMode.EndOfTrack -> "Oxirida"
                                    is SleepTimerMode.Countdown -> formatTimeMs(t.remainingSeconds * 1000L)
                                }
                                Text(timerLabel, color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Ijro tezligi", color = TextPrimary) },
            text = {
                Column {
                    speeds.forEach { spd ->
                        TextButton(
                            onClick = {
                                onSelectSpeed(spd)
                                showSpeedDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${spd}x",
                                color = if (state.playbackSpeed == spd) NeonCyan else TextPrimary,
                                fontWeight = if (state.playbackSpeed == spd) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Yopish", color = TextSecondary)
                }
            }
        )
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Uyqu taymeri", color = TextPrimary) },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { min ->
                        TextButton(
                            onClick = {
                                onSetSleepMinutes(min)
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$min daqiqa", color = TextPrimary)
                        }
                    }
                    TextButton(
                        onClick = {
                            onSetSleepEndOfTrack()
                            showSleepDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Joriy qo‘shiq tugagach", color = NeonCyan)
                    }
                    if (state.sleepTimer !is SleepTimerMode.Off) {
                        TextButton(
                            onClick = {
                                onCancelSleep()
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Taymerni o‘chirish", color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Yopish", color = TextSecondary)
                }
            }
        )
    }
}
