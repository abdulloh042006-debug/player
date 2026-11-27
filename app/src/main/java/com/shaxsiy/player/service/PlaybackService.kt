package com.shaxsiy.player.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.shaxsiy.player.MainActivity

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        const val ACTION_REWIND_10 = "com.shaxsiy.player.ACTION_REWIND_10"
        const val ACTION_FORWARD_10 = "com.shaxsiy.player.ACTION_FORWARD_10"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Qo‘ng‘iroq va boshqa audio manbalar uchun AudioFocus
            .setHandleAudioBecomingNoisy(true) // Quloqchin uzilganda avtomatik pauza
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val rewindButton = CommandButton.Builder()
            .setDisplayName("-10s")
            .setIconResId(android.R.drawable.ic_media_rew)
            .setSessionCommand(SessionCommand(ACTION_REWIND_10, Bundle.EMPTY))
            .build()

        val forwardButton = CommandButton.Builder()
            .setDisplayName("+10s")
            .setIconResId(android.R.drawable.ic_media_ff)
            .setSessionCommand(SessionCommand(ACTION_FORWARD_10, Bundle.EMPTY))
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .setCustomLayout(ImmutableList.of(rewindButton, forwardButton))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(SessionCommand(ACTION_REWIND_10, Bundle.EMPTY))
                        .add(SessionCommand(ACTION_FORWARD_10, Bundle.EMPTY))
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        ACTION_REWIND_10 -> {
                            val target = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                            player.seekTo(target)
                        }
                        ACTION_FORWARD_10 -> {
                            val duration = player.duration
                            val target = if (duration > 0L) {
                                (player.currentPosition + 10_000L).coerceAtMost(duration)
                            } else {
                                player.currentPosition + 10_000L
                            }
                            player.seekTo(target)
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}