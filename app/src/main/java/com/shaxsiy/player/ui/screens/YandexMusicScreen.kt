package com.shaxsiy.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaxsiy.player.ui.components.GlassCard
import com.shaxsiy.player.ui.components.GlassCircleIconButton
import com.shaxsiy.player.ui.components.GlassSearchBar
import com.shaxsiy.player.ui.components.formatTimeMs
import com.shaxsiy.player.ui.theme.DeepSpaceBg
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary
import com.shaxsiy.player.ui.theme.YandexGold
import com.shaxsiy.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

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
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.selectedPlaylistTitle != null) {
                    GlassCircleIconButton(
                        onClick = { viewModel.yandexMusicManager.closePlaylistDetail() },
                        size = 40.dp
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = state.selectedPlaylistTitle ?: "Yandex Music",
                        color = YandexGold,
                        fontSize = 26.sp,
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCircleIconButton(
                        onClick = { scope.launch { viewModel.yandexMusicManager.refreshAllData() } },
                        size = 40.dp
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = YandexGold, modifier = Modifier.size(19.dp))
                    }
                    GlassCircleIconButton(
                        onClick = { viewModel.yandexMusicManager.disconnect() },
                        size = 40.dp
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Chiqish", tint = TextSecondary, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }

        state.statusMessage?.let { msg ->
            GlassCard(
                cornerRadius = 16.dp,
                isHighlighted = true,
                accentColor = YandexGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = msg,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (!state.isConnected) {
            GlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Yandex Music hisobini ulash",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Pastdagi \"Brauzerda Token olish\" tugmasini bosing va Yandex hisobingizga kiring.\n" +
                            "2. Brauzer manzil satridagi to‘liq havolani nusxalab shu yerga qo‘ying.\n" +
                            "Qo‘shiqlar to‘g‘ridan-to‘g‘ri shu pleyerda chalinadi.",
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
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = YandexGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Brauzerda Token olish", color = YandexGold)
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
                            containerColor = YandexGold,
                            contentColor = DeepSpaceBg
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
                CircularProgressIndicator(color = YandexGold)
            }
        }

        if (state.selectedPlaylistTitle != null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.selectedPlaylistTracks, key = { it.id }) { track ->
                    GlassCard(
                        cornerRadius = 16.dp,
                        onClick = {
                            viewModel.playYandexTrack(
                                track = track,
                                sourceLabel = "Yandex • ${state.selectedPlaylistTitle}"
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(track.title, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${track.artist} • ${formatTimeMs(track.durationMs)}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
            return
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Yandex Music'dan qidirish...",
                accentColor = YandexGold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassCircleIconButton(
                onClick = { scope.launch { viewModel.yandexMusicManager.searchTracks(searchQuery) } },
                size = 46.dp,
                isGlowing = true,
                glowColor = YandexGold
            ) {
                Icon(Icons.Default.Search, contentDescription = "Qidirish", tint = TextPrimary)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.searchResults.isNotEmpty()) {
                item { Text("Qidiruv natijalari", color = YandexGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                items(state.searchResults, key = { "search_${it.id}" }) { track ->
                    GlassCard(
                        cornerRadius = 16.dp,
                        onClick = { viewModel.playYandexTrack(track, "Yandex Music") }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(track.title, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${track.artist} • ${formatTimeMs(track.durationMs)}", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            if (state.playlists.isNotEmpty()) {
                item { Text("Playlistlarim", color = YandexGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                items(state.playlists, key = { "pl_${it.kind}" }) { playlist ->
                    GlassCard(
                        cornerRadius = 16.dp,
                        onClick = { scope.launch { viewModel.yandexMusicManager.openPlaylist(playlist) } }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(playlist.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${playlist.trackCount} ta qo‘shiq", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (state.likedSongs.isNotEmpty()) {
                item { Text("Sevimli qo‘shiqlar", color = YandexGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                items(state.likedSongs, key = { "like_${it.id}" }) { track ->
                    GlassCard(
                        cornerRadius = 16.dp,
                        onClick = { viewModel.playYandexTrack(track, "Yandex • Sevimlilar") }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(track.title, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${track.artist} • ${formatTimeMs(track.durationMs)}", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}