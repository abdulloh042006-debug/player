package com.shaxsiy.player.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.shaxsiy.player.data.db.GroupWithTrackCount
import com.shaxsiy.player.ui.components.GlassCard
import com.shaxsiy.player.ui.components.GlassCircleIconButton
import com.shaxsiy.player.ui.components.formatTimeMs
import com.shaxsiy.player.ui.components.iosBounceClickable
import com.shaxsiy.player.ui.theme.DangerRed
import com.shaxsiy.player.ui.theme.DeepSpaceBg
import com.shaxsiy.player.ui.theme.NeonCyan
import com.shaxsiy.player.ui.theme.NeonMagenta
import com.shaxsiy.player.ui.theme.SurfaceElevated
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary
import com.shaxsiy.player.viewmodel.PlayerViewModel

@Composable
fun GroupsTabScreen(viewModel: PlayerViewModel) {
    var openedGroupId by remember { mutableStateOf<Long?>(null) }

    if (openedGroupId == null) {
        GroupsListScreen(
            viewModel = viewModel,
            onOpenGroup = { openedGroupId = it }
        )
    } else {
        GroupDetailScreen(
            viewModel = viewModel,
            groupId = openedGroupId!!,
            onBack = { openedGroupId = null }
        )
    }
}

@Composable
private fun GroupsListScreen(
    viewModel: PlayerViewModel,
    onOpenGroup: (Long) -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<GroupWithTrackCount?>(null) }
    var groupNameInput by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackupToUri(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackupFromUri(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Guruhlar",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Izolyatsiyalangan shaxsiy playlistlar",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCircleIconButton(
                        onClick = { exportLauncher.launch("shaxsiy_guruhlar_backup.json") },
                        size = 40.dp
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = "Zaxira nusxa", tint = NeonCyan, modifier = Modifier.size(19.dp))
                    }
                    GlassCircleIconButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        size = 40.dp
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "Tiklash", tint = NeonMagenta, modifier = Modifier.size(19.dp))
                    }
                }
            }

            if (groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard(
                        cornerRadius = 24.dp,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Hali guruh yaratilmagan",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pastdagi + tugmasini bosib shaxsiy guruh yarating.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(groups, key = { _, g -> g.id }) { _, group ->
                        GlassCard(
                            cornerRadius = 20.dp,
                            onClick = { onOpenGroup(group.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(NeonCyan.copy(alpha = 0.25f), NeonMagenta.copy(alpha = 0.25f))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = NeonCyan)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name,
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${group.trackCount} ta qo‘shiq",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(onClick = {
                                    editingGroup = group
                                    groupNameInput = group.name
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Tahrirlash", tint = TextSecondary, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { viewModel.deleteGroup(group.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "O‘chirish", tint = DangerRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(110.dp)) }
                }
            }
        }

        // Neon gradient suzib turuvchi + tugmasi
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 4.dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonCyan, NeonMagenta)))
                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                .iosBounceClickable(scaleDown = 0.90f) {
                    groupNameInput = ""
                    showCreateDialog = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Yangi guruh", tint = DeepSpaceBg, modifier = Modifier.size(28.dp))
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Yangi guruh yaratish", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Guruh nomi") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createGroup(groupNameInput)
                    showCreateDialog = false
                }) {
                    Text("Yaratish", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Bekor qilish", color = TextSecondary)
                }
            }
        )
    }

    editingGroup?.let { target ->
        AlertDialog(
            onDismissRequest = { editingGroup = null },
            containerColor = SurfaceElevated,
            title = { Text("Guruh nomini o‘zgartirish", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Yangi nom") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameGroup(target.id, groupNameInput)
                    editingGroup = null
                }) {
                    Text("Saqlash", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGroup = null }) {
                    Text("Bekor qilish", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun GroupDetailScreen(
    viewModel: PlayerViewModel,
    groupId: Long,
    onBack: () -> Unit
) {
    val group by viewModel.observeGroup(groupId).collectAsState(initial = null)
    val tracks by viewModel.observeTracksForGroup(groupId).collectAsState(initial = emptyList())
    val allTracks by viewModel.allTracks.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var showAddTracksDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCircleIconButton(onClick = onBack, size = 40.dp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group?.name ?: "Guruh",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${tracks.size} ta qo‘shiq • Izolyatsiyalangan ijro",
                    color = NeonCyan,
                    fontSize = 12.sp
                )
            }
            GlassCircleIconButton(
                onClick = { showAddTracksDialog = true },
                size = 42.dp,
                isGlowing = true,
                glowColor = NeonCyan
            ) {
                Icon(Icons.Default.Add, contentDescription = "Qo‘shiq qo‘shish", tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
        }

        val lastUri = group?.lastPlayedTrackUri
        val lastIndex = tracks.indexOfFirst { it.uri == lastUri }
        if (lastIndex >= 0 && group != null) {
            val lastTrack = tracks[lastIndex]
            GlassCard(
                cornerRadius = 18.dp,
                isHighlighted = true,
                accentColor = NeonCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                onClick = {
                    viewModel.playGroupTracks(
                        groupId = groupId,
                        groupName = group!!.name,
                        tracks = tracks,
                        startIndex = lastIndex,
                        startPositionMs = group!!.lastPlayedPositionMs
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Oxirgi joydan davom ettirish",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${lastTrack.title} (${formatTimeMs(group!!.lastPlayedPositionMs)})",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Davom ettirish", tint = TextPrimary)
                }
            }
        }

        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Bu guruhda qo‘shiq yo‘q.\nYuqoridagi + tugmasi orqali qo‘shiq qo‘shing.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(tracks, key = { _, t -> t.uri }) { index, track ->
                    val isCurrent = playbackState.activeGroupId == groupId &&
                        playbackState.currentTrack?.uri == track.uri
                    GlassCard(
                        cornerRadius = 16.dp,
                        isHighlighted = isCurrent,
                        accentColor = NeonCyan,
                        onClick = {
                            viewModel.playGroupTracks(
                                groupId = groupId,
                                groupName = group?.name ?: "Guruh",
                                tracks = tracks,
                                startIndex = index
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = if (isCurrent) NeonCyan else TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${formatTimeMs(track.durationMs)}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (index > 0) {
                                IconButton(
                                    onClick = {
                                        val mutable = tracks.map { it.uri }.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index - 1, item)
                                        viewModel.reorderGroupTracks(groupId, mutable)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Yuqoriga", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < tracks.lastIndex) {
                                IconButton(
                                    onClick = {
                                        val mutable = tracks.map { it.uri }.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index + 1, item)
                                        viewModel.reorderGroupTracks(groupId, mutable)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Pastga", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = { viewModel.removeTrackFromGroup(groupId, track.uri) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Olib tashlash", tint = DangerRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    if (showAddTracksDialog) {
        val existingUris = remember(tracks) { tracks.map { it.uri }.toSet() }
        val selectedUris = remember { mutableStateListOf<String>() }
        var search by remember { mutableStateOf("") }
        val candidates = allTracks.filter {
            it.uri !in existingUris &&
                (search.isBlank() || it.title.contains(search, true) || it.artist.contains(search, true))
        }

        AlertDialog(
            onDismissRequest = { showAddTracksDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Guruhga qo‘shiq qo‘shish", color = TextPrimary) },
            text = {
                Column(modifier = Modifier.height(380.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Qidirish") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        itemsIndexed(candidates) { _, track ->
                            val checked = track.uri in selectedUris
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedUris.remove(track.uri) else selectedUris.add(track.uri)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (it) selectedUris.add(track.uri) else selectedUris.remove(track.uri)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addTracksToGroup(groupId, selectedUris.toList())
                    showAddTracksDialog = false
                }) {
                    Text("Qo‘shish (${selectedUris.size})", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTracksDialog = false }) {
                    Text("Bekor qilish", color = TextSecondary)
                }
            }
        )
    }
}