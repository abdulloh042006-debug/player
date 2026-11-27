package com.shaxsiy.player.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.shaxsiy.player.data.db.AppDatabase
import com.shaxsiy.player.data.db.BackupGroup
import com.shaxsiy.player.data.db.BackupPayload
import com.shaxsiy.player.data.db.GroupEntity
import com.shaxsiy.player.data.db.GroupTrackCrossRef
import com.shaxsiy.player.data.db.GroupWithTrackCount
import com.shaxsiy.player.data.local.AudioTrack
import com.shaxsiy.player.data.local.LocalAudioRepository
import com.shaxsiy.player.data.spotify.SpotifyManager
import com.shaxsiy.player.data.spotify.SpotifyUiState
import com.shaxsiy.player.data.yandex.YandexMusicManager
import com.shaxsiy.player.data.yandex.YandexMusicUiState
import com.shaxsiy.player.data.yandex.YandexTrackItem
import com.shaxsiy.player.service.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RepeatModeUi {
    REPEAT_OFF,
    REPEAT_ONE,
    REPEAT_GROUP
}

sealed class SleepTimerMode {
    data object Off : SleepTimerMode()
    data class Countdown(val remainingSeconds: Long) : SleepTimerMode()
    data object EndOfTrack : SleepTimerMode()
}

data class PlaybackUiState(
    val currentTrack: AudioTrack? = null,
    val activeGroupId: Long? = null,
    val activeGroupName: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val audioSessionId: Int = 0,
    val repeatMode: RepeatModeUi = RepeatModeUi.REPEAT_OFF,
    val isShuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimer: SleepTimerMode = SleepTimerMode.Off
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val groupDao = db.groupDao()
    private val audioRepo = LocalAudioRepository(application)
    val spotifyManager = SpotifyManager(application)
    val yandexMusicManager = YandexMusicManager(application)
    private val gson = Gson()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _allTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val allTracks: StateFlow<List<AudioTrack>> = _allTracks.asStateFlow()

    private val _hideShortAudio = MutableStateFlow(true)
    val hideShortAudio: StateFlow<Boolean> = _hideShortAudio.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val groups: StateFlow<List<GroupWithTrackCount>> = groupDao.observeGroupsWithCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yandexState: StateFlow<YandexMusicUiState> = yandexMusicManager.state
    val spotifyState: StateFlow<SpotifyUiState> = spotifyManager.state

    private var sleepTimerJob: Job? = null
    private var positionTrackerJob: Job? = null
    private var lastSavedPositionTick = 0L

    init {
        connectToMediaService()
        startPositionTracker()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun scanLocalAudio() {
        viewModelScope.launch {
            val tracks = audioRepo.scanAudioFiles(_hideShortAudio.value)
            _allTracks.value = tracks
        }
    }

    fun toggleHideShortAudio() {
        _hideShortAudio.value = !_hideShortAudio.value
        scanLocalAudio()
    }

    private fun connectToMediaService() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    val controller = controllerFuture?.get() ?: return@addListener
                    mediaController = controller
                    controller.addListener(playerListener)
                    syncFromController(controller)
                } catch (_: Exception) {
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) {
                persistCurrentGroupPosition()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (_playbackState.value.sleepTimer is SleepTimerMode.EndOfTrack &&
                (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
            ) {
                stopAfterSleepTimerTrack()
            }
            mediaController?.let { syncFromController(it) }
            persistCurrentGroupPosition()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED &&
                _playbackState.value.sleepTimer is SleepTimerMode.EndOfTrack
            ) {
                stopAfterSleepTimerTrack()
            }
            mediaController?.let { syncFromController(it) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playbackState.update { it.copy(repeatMode = repeatMode.toUiRepeatMode()) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
        }
    }

    private fun syncFromController(controller: MediaController) {
        val currentUri = controller.currentMediaItem?.mediaId
        val matchedTrack = _allTracks.value.firstOrNull { it.uri == currentUri }
        _playbackState.update {
            it.copy(
                currentTrack = matchedTrack ?: it.currentTrack,
                isPlaying = controller.isPlaying,
                currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = controller.duration.coerceAtLeast(0L),
                audioSessionId = PlaybackService.audioSessionId,
                repeatMode = controller.repeatMode.toUiRepeatMode(),
                isShuffleEnabled = controller.shuffleModeEnabled
            )
        }
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.currentMediaItem != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.coerceAtLeast(0L)
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            isPlaying = controller.isPlaying,
                            audioSessionId = PlaybackService.audioSessionId
                        )
                    }
                    val now = System.currentTimeMillis()
                    if (controller.isPlaying && now - lastSavedPositionTick >= 2000L) {
                        lastSavedPositionTick = now
                        persistCurrentGroupPosition()
                    }
                }
                delay(500L)
            }
        }
    }

    private fun persistCurrentGroupPosition() {
        val groupId = _playbackState.value.activeGroupId ?: return
        val uri = mediaController?.currentMediaItem?.mediaId ?: return
        val pos = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.updateLastPlayed(groupId, uri, pos)
        }
    }

    // --- QAT'IY IZOLYATSIYALANGAN GURUH IJROSI ---
    fun playGroupTracks(
        groupId: Long,
        groupName: String,
        tracks: List<AudioTrack>,
        startIndex: Int,
        startPositionMs: Long = 0L
    ) {
        val controller = mediaController ?: return
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(tracks.indices)

        _playbackState.update {
            it.copy(
                activeGroupId = groupId,
                activeGroupName = groupName,
                currentTrack = tracks[safeIndex]
            )
        }

        controller.setMediaItems(tracks.map { it.toMediaItem() }, safeIndex, startPositionMs)
        controller.prepare()
        controller.play()
    }

    fun playFromAllSongs(tracks: List<AudioTrack>, startIndex: Int) {
        val controller = mediaController ?: return
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(tracks.indices)

        _playbackState.update {
            it.copy(
                activeGroupId = null,
                activeGroupName = "Barcha musiqalar",
                currentTrack = tracks[safeIndex]
            )
        }

        controller.setMediaItems(tracks.map { it.toMediaItem() }, safeIndex, 0L)
        controller.prepare()
        controller.play()
    }

    // --- YANDEX MUSIC TO‘G‘RIDAN-TO‘G‘RI MEDIA3 IJROSI ---
    fun playYandexTrack(track: YandexTrackItem, sourceLabel: String = "Yandex Music") {
        val controller = mediaController ?: return
        viewModelScope.launch {
            showToast("Yuklanmoqda: ${track.title}...")
            val streamUrl = yandexMusicManager.resolveStreamUrl(track.id)
            if (streamUrl.isNullOrBlank()) {
                showToast("Yandex Music audio oqimini olib bo‘lmadi. Yandex Plus yoki internetni tekshiring.")
                return@launch
            }
            val onlineTrack = AudioTrack(
                id = track.id.hashCode().toLong(),
                uri = streamUrl,
                title = track.title,
                artist = track.artist,
                durationMs = track.durationMs,
                folderName = sourceLabel
            )
            _playbackState.update {
                it.copy(
                    activeGroupId = null,
                    activeGroupName = sourceLabel,
                    currentTrack = onlineTrack
                )
            }
            controller.setMediaItem(onlineTrack.toMediaItem())
            controller.prepare()
            controller.play()
        }
    }

    // --- PLEYER BOSHQARUVI ---
    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0, 0L)
            }
            controller.play()
        }
    }

    fun nextTrack() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    fun previousTrack() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun seekForward10s() {
        val controller = mediaController ?: return
        val dur = controller.duration
        val target = if (dur > 0L) {
            (controller.currentPosition + 10_000L).coerceAtMost(dur)
        } else {
            controller.currentPosition + 10_000L
        }
        controller.seekTo(target)
        _playbackState.update { it.copy(currentPositionMs = target) }
    }

    fun seekBackward10s() {
        val controller = mediaController ?: return
        val target = (controller.currentPosition - 10_000L).coerceAtLeast(0L)
        controller.seekTo(target)
        _playbackState.update { it.copy(currentPositionMs = target) }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs.coerceAtLeast(0L))
        _playbackState.update { it.copy(currentPositionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val next = when (_playbackState.value.repeatMode) {
            RepeatModeUi.REPEAT_OFF -> Player.REPEAT_MODE_ONE
            RepeatModeUi.REPEAT_ONE -> Player.REPEAT_MODE_ALL
            RepeatModeUi.REPEAT_GROUP -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = next
        _playbackState.update { it.copy(repeatMode = next.toUiRepeatMode()) }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newValue = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newValue
        _playbackState.update { it.copy(isShuffleEnabled = newValue) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val controller = mediaController ?: return
        controller.playbackParameters = PlaybackParameters(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    // --- UYQU TAYMERI ---
    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playbackState.update { it.copy(sleepTimer = SleepTimerMode.Off) }
            return
        }
        var remainingSec = minutes * 60L
        _playbackState.update { it.copy(sleepTimer = SleepTimerMode.Countdown(remainingSec)) }
        sleepTimerJob = viewModelScope.launch {
            while (remainingSec > 0 && isActive) {
                delay(1000L)
                remainingSec--
                _playbackState.update { it.copy(sleepTimer = SleepTimerMode.Countdown(remainingSec)) }
            }
            if (isActive) {
                mediaController?.pause()
                _playbackState.update { it.copy(sleepTimer = SleepTimerMode.Off) }
                showToast("Uyqu taymeri tugadi, musiqa to‘xtatildi")
            }
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        _playbackState.update { it.copy(sleepTimer = SleepTimerMode.EndOfTrack) }
        showToast("Joriy qo‘shiq tugagach pleyer to‘xtaydi")
    }

    private fun stopAfterSleepTimerTrack() {
        mediaController?.pause()
        _playbackState.update { it.copy(sleepTimer = SleepTimerMode.Off) }
        showToast("Uyqu taymeri: qo‘shiq tugadi, pleyer to‘xtatildi")
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _playbackState.update { it.copy(sleepTimer = SleepTimerMode.Off) }
    }

    // --- GURUHLAR CRUD ---
    fun observeGroup(groupId: Long): Flow<GroupEntity?> = groupDao.observeGroupById(groupId)

    fun observeTracksForGroup(groupId: Long): Flow<List<AudioTrack>> {
        return combine(
            groupDao.observeGroupTracks(groupId),
            _allTracks
        ) { crossRefs, all ->
            val byUri = all.associateBy { it.uri }
            crossRefs.mapNotNull { byUri[it.trackUri] }
        }
    }

    fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.insertGroup(GroupEntity(name = trimmed))
        }
    }

    fun renameGroup(groupId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.renameGroup(groupId, trimmed)
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.deleteGroup(groupId)
        }
    }

    fun addTracksToGroup(groupId: Long, trackUris: List<String>) {
        if (trackUris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            var nextIndex = groupDao.getMaxOrderIndex(groupId) + 1
            val refs = trackUris.map { uri ->
                GroupTrackCrossRef(groupId = groupId, trackUri = uri, orderIndex = nextIndex++)
            }
            groupDao.insertGroupTracks(refs)
        }
    }

    fun removeTrackFromGroup(groupId: Long, trackUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.removeTrackFromGroup(groupId, trackUri)
        }
    }

    fun reorderGroupTracks(groupId: Long, orderedUris: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.replaceGroupTrackOrder(groupId, orderedUris)
        }
    }

    // --- JSON BACKUP / RESTORE ---
    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    val allGroups = groupDao.getAllGroupsOnce()
                    val backupGroups = allGroups.map { group ->
                        val tracks = groupDao.getGroupTracksOnce(group.id).map { it.trackUri }
                        BackupGroup(
                            name = group.name,
                            createdAt = group.createdAt,
                            lastPlayedTrackUri = group.lastPlayedTrackUri,
                            lastPlayedPositionMs = group.lastPlayedPositionMs,
                            trackUris = tracks
                        )
                    }
                    gson.toJson(BackupPayload(groups = backupGroups))
                }
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                showToast("Guruhlar zaxira nusxasi saqlandi")
            } catch (_: Exception) {
                showToast("Zaxira nusxa saqlashda xatolik yuz berdi")
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val importedCount = withContext(Dispatchers.IO) {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: return@withContext 0
                    val payload = gson.fromJson(text, BackupPayload::class.java)
                    payload.groups.forEach { bg ->
                        val newGroupId = groupDao.insertGroup(
                            GroupEntity(
                                name = bg.name,
                                createdAt = bg.createdAt,
                                lastPlayedTrackUri = bg.lastPlayedTrackUri,
                                lastPlayedPositionMs = bg.lastPlayedPositionMs
                            )
                        )
                        val refs = bg.trackUris.mapIndexed { idx, trackUri ->
                            GroupTrackCrossRef(groupId = newGroupId, trackUri = trackUri, orderIndex = idx)
                        }
                        groupDao.insertGroupTracks(refs)
                    }
                    payload.groups.size
                }
                showToast("$importedCount ta guruh tiklandi")
            } catch (_: Exception) {
                showToast("JSON faylni o‘qishda xatolik yuz berdi")
            }
        }
    }

    private fun Int.toUiRepeatMode(): RepeatModeUi = when (this) {
        Player.REPEAT_MODE_ONE -> RepeatModeUi.REPEAT_ONE
        Player.REPEAT_MODE_ALL -> RepeatModeUi.REPEAT_GROUP
        else -> RepeatModeUi.REPEAT_OFF
    }

    override fun onCleared() {
        persistCurrentGroupPosition()
        positionTrackerJob?.cancel()
        sleepTimerJob?.cancel()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
