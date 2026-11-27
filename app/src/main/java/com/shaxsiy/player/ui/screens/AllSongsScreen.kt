package com.shaxsiy.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaxsiy.player.ui.components.TrackCoverThumbnail
import com.shaxsiy.player.ui.components.formatTimeMs
import com.shaxsiy.player.ui.theme.AccentGreen
import com.shaxsiy.player.ui.theme.AmoledBlack
import com.shaxsiy.player.ui.theme.SurfaceDark
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
            .background(AmoledBlack)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectedUris.isNotEmpty()) {
                Text(
                    text = "${selectedUris.size} ta tanlandi",
                    color = AccentGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { showPickGroupDialog = true }) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Guruhga qo‘shish", tint = AccentGreen)
                    }
                    IconButton(onClick = { selectedUris.clear() }) {
                        Icon(Icons.Default.Close, contentDescription = "Bekor qilish", tint = TextPrimary)
                    }
                }
            } else {
                Text(
                    text = "Barcha musiqalar (${filtered.size})",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { viewModel.toggleHideShortAudio() }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "10s filtr",
                            tint = if (hideShort) AccentGreen else TextSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.scanLocalAudio() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = TextSecondary)
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Nomi, ijrochi yoki papka bo‘yicha qidirish...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Musiqalar topilmadi", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(filtered, key = { _, t -> t.uri }) { index, track ->
                    val isSelected = track.uri in selectedUris
                    val isPlayingTrack = playbackState.activeGroupId == null &&
                        playbackState.currentTrack?.uri == track.uri

                    Surface(
                        color = if (isSelected || isPlayingTrack) SurfaceElevated else SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
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
                                    colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            TrackCoverThumbnail(
                                trackUri = track.uri,
                                modifier = Modifier.size(48.dp),
                                accentColor = AccentGreen,
                                surfaceColor = SurfaceElevated
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = if (isPlayingTrack) AccentGreen else TextPrimary,
                                    fontWeight = FontWeight.Medium,
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
                item { Spacer(modifier = Modifier.height(80.dp)) }
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
