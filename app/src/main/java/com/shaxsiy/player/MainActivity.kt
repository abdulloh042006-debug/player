package com.shaxsiy.player

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.shaxsiy.player.ui.components.FullPlayerSheet
import com.shaxsiy.player.ui.components.MiniPlayer
import com.shaxsiy.player.ui.screens.AllSongsScreen
import com.shaxsiy.player.ui.screens.GroupsTabScreen
import com.shaxsiy.player.ui.screens.YandexMusicScreen
import com.shaxsiy.player.ui.theme.AccentGreen
import com.shaxsiy.player.ui.theme.AmoledBlack
import com.shaxsiy.player.ui.theme.ShaxsiyPlayerTheme
import com.shaxsiy.player.ui.theme.SurfaceDark
import com.shaxsiy.player.ui.theme.TextSecondary
import com.shaxsiy.player.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShaxsiyPlayerTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun MainAppScreen(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showFullPlayer by remember { mutableStateOf(false) }

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
        perms.add(Manifest.permission.RECORD_AUDIO)
        permissionLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = AmoledBlack,
        bottomBar = {
            Column {
                if (playbackState.currentTrack != null) {
                    MiniPlayer(
                        state = playbackState,
                        onExpand = { showFullPlayer = true },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onForward10 = { viewModel.seekForward10s() },
                        onBackward10 = { viewModel.seekBackward10s() }
                    )
                }
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Guruhlar") },
                        label = { Text("Guruhlar") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentGreen,
                            selectedTextColor = AccentGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = AmoledBlack
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Musiqalar") },
                        label = { Text("Musiqalar") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentGreen,
                            selectedTextColor = AccentGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = AmoledBlack
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = "Yandex") },
                        label = { Text("Yandex") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFFCC00),
                            selectedTextColor = Color(0xFFFFCC00),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = AmoledBlack
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> GroupsTabScreen(viewModel = viewModel)
                1 -> AllSongsScreen(viewModel = viewModel)
                2 -> YandexMusicScreen(viewModel = viewModel)
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
