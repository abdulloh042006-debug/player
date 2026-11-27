package com.shaxsiy.player.ui.components

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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaxsiy.player.ui.theme.AccentGreen
import com.shaxsiy.player.ui.theme.AmoledBlack
import com.shaxsiy.player.ui.theme.SurfaceDark
import com.shaxsiy.player.ui.theme.SurfaceElevated
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary
import com.shaxsiy.player.viewmodel.PlaybackUiState
import com.shaxsiy.player.viewmodel.RepeatModeUi
import com.shaxsiy.player.viewmodel.SleepTimerMode

fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
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

    Surface(
        color = SurfaceElevated,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand() }
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = AccentGreen,
                trackColor = SurfaceDark
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp)
                ) {
                    TrackCoverThumbnail(
                        trackUri = track.uri,
                        modifier = Modifier.fillMaxSize(),
                        accentColor = AccentGreen,
                        surfaceColor = SurfaceDark
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
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
                IconButton(onClick = onBackward10) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = TextPrimary)
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentGreen,
                        contentColor = AmoledBlack
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }
                IconButton(onClick = onForward10) {
                    Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = TextPrimary)
                }
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
    onCoverChanged: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AmoledBlack,
        dragHandle = null
    ) {
        val track = state.currentTrack
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Yuqori sarlavha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Yopish",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("JORIY GURUH", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = state.activeGroupName ?: "Barcha musiqalar",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(48.dp))
            }

            TrackCoverVisualizerSection(
                trackUri = track?.uri,
                isPlaying = state.isPlaying,
                audioSessionId = state.audioSessionId,
                accentColor = AccentGreen,
                surfaceColor = SurfaceElevated,
                onCoverChanged = onCoverChanged
            )

            // Pastki bir qo‘lda boshqaruv zonasi
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

                Spacer(modifier = Modifier.height(16.dp))

                // Seekbar + Aniq vaqt (01:24 / 03:45)
                val maxDur = state.durationMs.coerceAtLeast(1L).toFloat()
                Slider(
                    value = state.currentPositionMs.toFloat().coerceIn(0f, maxDur),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..maxDur,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGreen,
                        activeTrackColor = AccentGreen,
                        inactiveTrackColor = SurfaceElevated
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

                // Katta bir qo‘l uchun -10s va +10s tugmalari
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { onBackward10() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("-10s", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { onForward10() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("+10s", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Asosiy tugmalar: Shuffle | Prev | Play/Pause | Next | Repeat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Aralashtirish",
                            tint = if (state.isShuffleEnabled) AccentGreen else TextSecondary
                        )
                    }

                    IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Oldingi",
                            tint = TextPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onPlayPause,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AccentGreen,
                            contentColor = AmoledBlack
                        ),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Keyingi",
                            tint = TextPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(onClick = onCycleRepeat) {
                        val (icon, tint) = when (state.repeatMode) {
                            RepeatModeUi.REPEAT_OFF -> Icons.Default.Repeat to TextSecondary
                            RepeatModeUi.REPEAT_ONE -> Icons.Default.RepeatOne to AccentGreen
                            RepeatModeUi.REPEAT_GROUP -> Icons.Default.Repeat to AccentGreen
                        }
                        Icon(icon, contentDescription = "Takrorlash", tint = tint)
                    }
                }

                // Takrorlash holati yozuvi + Tezlik + Uyqu taymeri
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showSpeedDialog = true }) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${state.playbackSpeed}x", color = TextPrimary)
                    }

                    Text(
                        text = when (state.repeatMode) {
                            RepeatModeUi.REPEAT_OFF -> "Takrorlash: O‘chiq"
                            RepeatModeUi.REPEAT_ONE -> "1 qo‘shiq takrori"
                            RepeatModeUi.REPEAT_GROUP -> "Butun guruh takrori"
                        },
                        color = if (state.repeatMode == RepeatModeUi.REPEAT_OFF) TextSecondary else AccentGreen,
                        fontSize = 12.sp
                    )

                    TextButton(onClick = { showSleepDialog = true }) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (state.sleepTimer is SleepTimerMode.Off) TextSecondary else AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val timerLabel = when (val t = state.sleepTimer) {
                            is SleepTimerMode.Off -> "Taymer"
                            is SleepTimerMode.EndOfTrack -> "Oxirida"
                            is SleepTimerMode.Countdown -> formatTimeMs(t.remainingSeconds * 1000L)
                        }
                        Text(timerLabel, color = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                                color = if (state.playbackSpeed == spd) AccentGreen else TextPrimary,
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
                        Text("Joriy qo‘shiq tugagach", color = AccentGreen)
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
