package com.shaxsiy.player

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.shaxsiy.player.ui.components.FullPlayerSheet
import com.shaxsiy.player.ui.components.GlassAmbientBackground
import com.shaxsiy.player.ui.components.MiniPlayer
import com.shaxsiy.player.ui.components.iosBounceClickable
import com.shaxsiy.player.ui.screens.AllSongsScreen
import com.shaxsiy.player.ui.screens.GroupsTabScreen
import com.shaxsiy.player.ui.screens.SpotifyScreen
import com.shaxsiy.player.ui.screens.YandexMusicScreen
import com.shaxsiy.player.ui.theme.AccentSpotify
import com.shaxsiy.player.ui.theme.NeonCyan
import com.shaxsiy.player.ui.theme.NeonMagenta
import com.shaxsiy.player.ui.theme.ShaxsiyPlayerTheme
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary
import com.shaxsiy.player.ui.theme.YandexGold
import com.shaxsiy.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSpotifyRedirect(intent)
        setContent {
            ShaxsiyPlayerTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSpotifyRedirect(intent)
    }

    private fun handleSpotifyRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "shaxsiyplayer" && data.host == "callback") {
            lifecycleScope.launch {
                viewModel.spotifyManager.handleAuthCallback(data)
            }
        }
    }

}

private data class GlassNavItem(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val activeColor: Color
)

@Composable
private fun MainAppScreen(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showFullPlayer by remember { mutableStateOf(false) }

    val navItems = remember {
        listOf(
            GlassNavItem(0, "Guruhlar", Icons.Default.Folder, NeonCyan),
            GlassNavItem(1, "Musiqalar", Icons.Default.LibraryMusic, NeonMagenta),
            GlassNavItem(2, "Spotify", Icons.Default.Cloud, AccentSpotify),
            GlassNavItem(3, "Yandex", Icons.Default.MusicNote, YandexGold)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.scanLocalAudio()
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    GlassAmbientBackground(isPlaying = playbackState.isPlaying) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    if (playbackState.currentTrack != null) {
                        MiniPlayer(
                            state = playbackState,
                            onExpand = { showFullPlayer = true },
                            onPlayPause = { viewModel.togglePlayPause() },
                            onForward10 = { viewModel.seekForward10s() },
                            onBackward10 = { viewModel.seekBackward10s() }
                        )
                    }

                    // iPhone uslubidagi suzuvchi shisha Dock navigatsiyasi
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xD9121528),
                                        Color(0xCC0A0D1C)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.30f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { item ->
                                val isSelected = selectedTab == item.index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.linearGradient(
                                                    listOf(
                                                        item.activeColor.copy(alpha = 0.28f),
                                                        item.activeColor.copy(alpha = 0.10f)
                                                    )
                                                )
                                            } else {
                                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) item.activeColor.copy(alpha = 0.65f) else Color.Transparent,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .iosBounceClickable(scaleDown = 0.92f) { selectedTab = item.index }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) item.activeColor else TextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.label,
                                            color = if (isSelected) TextPrimary else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        0 -> GroupsTabScreen(viewModel = viewModel)
                        1 -> AllSongsScreen(viewModel = viewModel)
                        2 -> SpotifyScreen(viewModel = viewModel)
                        3 -> YandexMusicScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (showFullPlayer) {
        FullPlayerSheet(
            state = playbackState,
            onDismiss = { showFullPlayer = false },
            onPlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.nextTrack() },
            onPrevious = { viewModel.previousTrack() },
            onForward10 = { viewModel.seekForward10s() },
            onBackward10 = { viewModel.seekBackward10s() },
            onSeekTo = { viewModel.seekTo(it) },
            onCycleRepeat = { viewModel.cycleRepeatMode() },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
            onSetSleepMinutes = { viewModel.setSleepTimerMinutes(it) },
            onSetSleepEndOfTrack = { viewModel.setSleepTimerEndOfTrack() },
            onCancelSleep = { viewModel.cancelSleepTimer() },
            onCoverChanged = { viewModel.showToast(it) }
        )
    }
}
