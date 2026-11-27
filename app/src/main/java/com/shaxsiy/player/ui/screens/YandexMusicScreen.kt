package com.shaxsiy.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaxsiy.player.ui.components.formatTimeMs
import com.shaxsiy.player.ui.theme.AmoledBlack
import com.shaxsiy.player.ui.theme.SurfaceDark
import com.shaxsiy.player.ui.theme.SurfaceElevated
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary
import com.shaxsiy.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

private val YandexYellow = Color(0xFFFFCC00)

@Composable
fun YandexMusicScreen(viewModel: PlayerViewModel) {
    val state by viewModel.yandexState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tokenInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(state.isConnected) {
        if (state.isConnected && state.likedSongs.isEmpty() && state.playlists.isEmpty()) {
            viewModel.yandexMusicManager.refreshAllData()
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
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.selectedPlaylistTitle != null) {
                    IconButton(onClick = { viewModel.yandexMusicManager.closePlaylistDetail() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga", tint = TextPrimary)
                    }
                }
                Column {
                    Text(
                        text = state.selectedPlaylistTitle ?: "Yandex Music",
                        color = YandexYellow,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.isConnected && state.accountLogin.isNotBlank() && state.selectedPlaylistTitle == null) {
                        Text(state.accountLogin, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            if (state.isConnected) {
                Row {
                    IconButton(onClick = { scope.launch { viewModel.yandexMusicManager.refreshAllData() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.yandexMusicManager.disconnect() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Chiqish", tint = TextSecondary)
                    }
                }
            }
        }

        state.statusMessage?.let { msg ->
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = msg,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (!state.isConnected) {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Yandex Music hisobini ulash",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Pastdagi \"Token olish\" tugmasini bosing va Yandex hisobingizga kiring.\n" +
                            "2. Brauzer manzil satridagi to‘liq havolani (yoki access_token qismini) nusxalab shu yerga qo‘ying.\n" +
                            "Qo‘shiqlar to‘g‘ridan-to‘g‘ri shu pleyerda (-10s/+10s, tezlik va taymer bilan) chalinadi.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(viewModel.yandexMusicManager.createGetTokenIntent())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = YandexYellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Brauzerda Token olish", color = YandexYellow)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Yandex OAuth Token yoki Redirect URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.yandexMusicManager.connectWithToken(tokenInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YandexYellow,
                            contentColor = AmoledBlack
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Yandex Music ulash", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        if (state.isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = YandexYellow)
            }
        }

        // Playlist ichidagi qo‘shiqlar ko‘rinishi
        if (state.selectedPlaylistTitle != null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.selectedPlaylistTracks, key = { it.id }) { track ->
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.playYandexTrack(
                                    track = track,
                                    sourceLabel = "Yandex • ${state.selectedPlaylistTitle}"
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(track.title, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${track.artist} • ${formatTimeMs(track.durationMs)}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
            return
        }

        // Qidiruv
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Yandex Music'dan qidirish...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { scope.launch { viewModel.yandexMusicManager.searchTracks(searchQuery) } }) {
                Icon(Icons.Default.Search, contentDescription = "Qidirish", tint = YandexYellow)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.searchResults.isNotEmpty()) {
                item { Text("Qidiruv natijalari", color = YandexYellow, fontWeight = FontWeight.SemiBold) }
                items(state.searchResults, key = { "search_${it.id}" }) { track ->
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playYandexTrack(track, "Yandex Music") }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(track.title, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${track.artist} • ${formatTimeMs(track.durationMs)}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (state.playlists.isNotEmpty()) {
                item { Text("Playlistlarim", color = YandexYellow, fontWeight = FontWeight.SemiBold) }
                items(state.playlists, key = { "pl_${it.kind}" }) { playlist ->
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { viewModel.yandexMusicManager.openPlaylist(playlist) } }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(playlist.title, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("${playlist.trackCount} ta qo‘shiq", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (state.likedSongs.isNotEmpty()) {
                item { Text("Sevimli qo‘shiqlar (Мне нравится)", color = YandexYellow, fontWeight = FontWeight.SemiBold) }
                items(state.likedSongs, key = { "like_${it.id}" }) { track ->
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playYandexTrack(track, "Yandex • Sevimlilar") }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(track.title, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${track.artist} • ${formatTimeMs(track.durationMs)}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}