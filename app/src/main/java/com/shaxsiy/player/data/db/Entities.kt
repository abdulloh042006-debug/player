package com.shaxsiy.player.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedTrackUri: String? = null,
    val lastPlayedPositionMs: Long = 0L
)

@Entity(
    tableName = "group_tracks",
    primaryKeys = ["groupId", "trackUri"],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class GroupTrackCrossRef(
    val groupId: Long,
    val trackUri: String,
    val orderIndex: Int
)

data class GroupWithTrackCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val lastPlayedTrackUri: String?,
    val lastPlayedPositionMs: Long,
    val trackCount: Int
)

// JSON Backup / Restore modellari
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val groups: List<BackupGroup>
)

data class BackupGroup(
    val name: String,
    val createdAt: Long,
    val lastPlayedTrackUri: String?,
    val lastPlayedPositionMs: Long,
    val trackUris: List<String>
)