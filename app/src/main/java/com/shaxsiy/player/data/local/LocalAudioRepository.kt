package com.shaxsiy.player.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioTrack(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val folderName: String
) {
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .build()
        return MediaItem.Builder()
            .setMediaId(uri)
            .setUri(Uri.parse(uri))
            .setMediaMetadata(metadata)
            .build()
    }
}

class LocalAudioRepository(private val context: Context) {

    private val supportedExtensions = setOf("mp3", "m4a", "wav", "flac", "ogg")

    suspend fun scanAudioFiles(hideShortAudio: Boolean): List<AudioTrack> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AudioTrack>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            projection.add(MediaStore.Audio.Media.DATA)
        }

        val selection = if (hideShortAudio) {
            "${MediaStore.Audio.Media.DURATION} >= ?"
        } else {
            "${MediaStore.Audio.Media.DURATION} > ?"
        }
        val selectionArgs = arrayOf(if (hideShortAudio) "10000" else "0")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection.toTypedArray(),
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    @Suppress("DEPRECATION")
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(nameCol).orEmpty()
                    val ext = displayName.substringAfterLast('.', "").lowercase()
                    if (ext.isNotEmpty() && ext !in supportedExtensions) continue

                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val rawTitle = cursor.getString(titleCol).orEmpty().trim()
                    val title = rawTitle.ifEmpty { displayName.substringBeforeLast('.') }
                    val rawArtist = cursor.getString(artistCol).orEmpty().trim()
                    val artist = if (rawArtist.isEmpty() || rawArtist == "<unknown>") "Noma'lum ijrochi" else rawArtist
                    val duration = cursor.getLong(durationCol)

                    val rawPath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                    val folderName = extractFolderName(rawPath)

                    result.add(
                        AudioTrack(
                            id = id,
                            uri = contentUri,
                            title = title,
                            artist = artist,
                            durationMs = duration,
                            folderName = folderName
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        }

        result
    }

    private fun extractFolderName(rawPath: String): String {
        if (rawPath.isBlank()) return "Ichki xotira"
        val cleaned = rawPath.trimEnd('/')
        val parts = cleaned.split('/').filter { it.isNotBlank() }
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> parts.lastOrNull() ?: "Ichki xotira"
            parts.size >= 2 -> parts[parts.size - 2]
            else -> parts.lastOrNull() ?: "Ichki xotira"
        }
    }
}