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
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.shaxsiy.player.ui.theme.AccentSpotify
import com.shaxsiy.player.ui.theme.DeepSpaceBg
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary
import com.shaxsiy.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@Composable
fun SpotifyScreen(viewModel: PlayerViewModel) {
    val state by viewModel.spotifyState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(state.isConnected) {
        if (state.isConnected) {
            viewModel.spotifyManager.refreshAllData()
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
            Text(
                text = "Spotify",
                color = AccentSpotify,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (state.isConnected) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCircleIconButton(
                        onClick = { scope.launch { viewModel.spotifyManager.refreshAllData() } },
                        size = 40.dp
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = AccentSpotify, modifier = Modifier.size(19.dp))
                    }
                    GlassCircleIconButton(
                        onClick = { viewModel.spotifyManager.disconnect() },
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
                accentColor = AccentSpotify,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(text = msg, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
            }
        }

        if (!state.isConnected) {
            GlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Spotify hisobini ulash",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (!state.isConfigured) {
                            "SpotifyConfig.kt fayliga o‘z CLIENT_ID kodingizni kiriting. Oflayn pleyer usiz ham to‘liq ishlaydi."
                        } else if (!state.isSpotifyAppInstalled) {
                            "Qurilmada Spotify ilovasi topilmadi, lekin Web API orqali playlistlaringizni ko‘rishingiz mumkin."
                        } else {
                            "Playlistlar va sevimli qo‘shiqlaringizni ilova ichidan boshqarish uchun Spotify akkauntingizga kiring."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val intent = viewModel.spotifyManager.createAuthIntent()
                            if (intent != null) context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentSpotify,
                            contentColor = DeepSpaceBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Spotify bilan ulanish", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        GlassCard(
            cornerRadius = 20.dp,
            isHighlighted = state.isPlaying,
            accentColor = AccentSpotify,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = state.currentTrackTitle.ifBlank { "Spotify faol ijro yo‘q" },
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.currentTrackArtist.isNotBlank()) {
                    Text(state.currentTrackArtist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { viewModel.spotifyManager.previousTrack() } }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Oldingi", tint = TextPrimary)
                    }
                    IconButton(onClick = { scope.launch { viewModel.spotifyManager.seekBy(-10_000L) } }) {
                        Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = TextPrimary)
                    }
                    GlassCircleIconButton(
                        onClick = { scope.launch { viewModel.spotifyManager.togglePlayPause() } },
                        size = 46.dp,
                        isGlowing = true,
                        glowColor = AccentSpotify
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = TextPrimary
                        )
                    }
                    IconButton(onClick = { scope.launch { viewModel.spotifyManager.seekBy(10_000L) } }) {
                        Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = TextPrimary)
                    }
                    IconButton(onClick = { scope.launch { viewModel.spotifyManager.nextTrack() } }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Keyingi", tint = TextPrimary)
                    }
                }
            }
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
                placeholder = "Spotify'dan qidirish...",
                accentColor = AccentSpotify,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassCircleIconButton(
                onClick = { scope.launch { viewModel.spotifyManager.searchTracks(searchQuery) } },
                size = 46.dp,
                isGlowing = true,
                glowColor = AccentSpotify
            ) {
                Icon(Icons.Default.Search, contentDescription = "Qidirish", tint = TextPrimary)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.searchResults.isNotEmpty()) {
                item { Text("Qidiruv natijalari", color = AccentSpotify, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                items(state.searchResults) { track ->
                    GlassCard(
                        cornerRadius = 16.dp,
                        onClick = { scope.launch { viewModel.spotifyManager.playUri(track.uri, false) } }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(track.name, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            if (state.playlists.isNotEmpty()) {
                item { Text("Playlistlarim", color = AccentSpotify, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                items(state.playlists) { playlist ->
                    GlassCard(
                        cornerRadius = 16.dp,
                        onClick = { scope.launch { viewModel.spotifyManager.playUri(playlist.uri, true) } }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(playlist.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${playlist.trackCount} ta qo‘shiq", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (state.likedSongs.isNotEmpty()) {
                item { Text("Saqlangan qo‘shiqlar", color = AccentSpotify, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                items(state.likedSongs) { track ->
                    GlassCard(
                        cornerRadius = 16.dp,
                        onClick = { scope.launch { viewModel.spotifyManager.playUri(track.uri, false) } }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(track.name, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}