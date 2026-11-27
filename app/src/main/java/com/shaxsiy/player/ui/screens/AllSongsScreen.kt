package com.shaxsiy.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.shaxsiy.player.ui.components.GlassCard
import com.shaxsiy.player.ui.components.GlassCircleIconButton
import com.shaxsiy.player.ui.components.GlassSearchBar
import com.shaxsiy.player.ui.components.formatTimeMs
import com.shaxsiy.player.ui.theme.NeonCyan
import com.shaxsiy.player.ui.theme.NeonMagenta
import com.shaxsiy.player.ui.theme.SurfaceElevated
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary
import com.shaxsiy.player.viewmodel.PlayerViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllSongsScreen(viewModel: PlayerViewModel) {
    val allTracks by viewModel.allTracks.collectAsState()
    val hideShort by viewModel.hideShortAudio.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val selectedUris = remember { mutableStateListOf<String>() }
    var showPickGroupDialog by remember { mutableStateOf(false) }

    val filtered = remember(allTracks, searchQuery) {
        if (searchQuery.isBlank()) allTracks
        else allTracks.filter {
            it.title.contains(searchQuery, true) ||
                it.artist.contains(searchQuery, true) ||
                it.folderName.contains(searchQuery, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectedUris.isNotEmpty()) {
                Text(
                    text = "${selectedUris.size} ta tanlandi",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCircleIconButton(
                        onClick = { showPickGroupDialog = true },
                        size = 40.dp,
                        isGlowing = true,
                        glowColor = NeonCyan
                    ) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Guruhga qo‘shish", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    GlassCircleIconButton(
                        onClick = { selectedUris.clear() },
                        size = 40.dp
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Bekor qilish", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Column {
                    Text(
                        text = "Musiqalar",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filtered.size} ta audio fayl topildi",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCircleIconButton(
                        onClick = { viewModel.toggleHideShortAudio() },
                        size = 40.dp,
                        isGlowing = hideShort,
                        glowColor = NeonCyan
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "10s filtr",
                            tint = if (hideShort) TextPrimary else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    GlassCircleIconButton(
                        onClick = { viewModel.scanLocalAudio() },
                        size = 40.dp
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = TextPrimary, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }

        GlassSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Nomi, ijrochi yoki papka bo‘yicha qidirish...",
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Musiqalar topilmadi", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(filtered, key = { _, t -> t.uri }) { index, track ->
                    val isSelected = track.uri in selectedUris
                    val isPlayingTrack = playbackState.activeGroupId == null &&
                        playbackState.currentTrack?.uri == track.uri

                    GlassCard(
                        cornerRadius = 18.dp,
                        isHighlighted = isSelected || isPlayingTrack,
                        accentColor = if (isSelected) NeonMagenta else NeonCyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectedUris.isNotEmpty()) {
                                        if (isSelected) selectedUris.remove(track.uri) else selectedUris.add(track.uri)
                                    } else {
                                        viewModel.playFromAllSongs(filtered, index)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelected) selectedUris.add(track.uri)
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedUris.isNotEmpty()) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (it) selectedUris.add(track.uri) else selectedUris.remove(track.uri)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = NeonMagenta)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    NeonCyan.copy(alpha = if (isPlayingTrack) 0.35f else 0.16f),
                                                    NeonMagenta.copy(alpha = if (isPlayingTrack) 0.35f else 0.16f)
                                                )
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isPlayingTrack) NeonCyan else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = if (isPlayingTrack) NeonCyan else TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${track.folderName} • ${formatTimeMs(track.durationMs)}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    if (showPickGroupDialog) {
        AlertDialog(
            onDismissRequest = { showPickGroupDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Qaysi guruhga qo‘shilsin?", color = TextPrimary) },
            text = {
                if (groups.isEmpty()) {
                    Text("Avval Guruhlar bo‘limida yangi guruh yarating.", color = TextSecondary)
                } else {
                    Column {
                        groups.forEach { group ->
                            TextButton(
                                onClick = {
                                    viewModel.addTracksToGroup(group.id, selectedUris.toList())
                                    viewModel.showToast("${selectedUris.size} ta qo‘shiq \"${group.name}\" guruhiga qo‘shildi")
                                    selectedUris.clear()
                                    showPickGroupDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${group.name} (${group.trackCount})", color = TextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickGroupDialog = false }) {
                    Text("Yopish", color = TextSecondary)
                }
            }
        )
    }
}