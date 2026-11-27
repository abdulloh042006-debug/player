package com.shaxsiy.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query(
        """
        SELECT g.id, g.name, g.createdAt, g.lastPlayedTrackUri, g.lastPlayedPositionMs,
               COUNT(gt.trackUri) AS trackCount
        FROM groups g
        LEFT JOIN group_tracks gt ON g.id = gt.groupId
        GROUP BY g.id
        ORDER BY g.createdAt DESC
        """
    )
    fun observeGroupsWithCount(): Flow<List<GroupWithTrackCount>>

    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    suspend fun getAllGroupsOnce(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: Long): GroupEntity?

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    fun observeGroupById(groupId: Long): Flow<GroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Query("UPDATE groups SET name = :newName WHERE id = :groupId")
    suspend fun renameGroup(groupId: Long, newName: String)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Query(
        """
        UPDATE groups
        SET lastPlayedTrackUri = :trackUri, lastPlayedPositionMs = :positionMs
        WHERE id = :groupId
        """
    )
    suspend fun updateLastPlayed(groupId: Long, trackUri: String?, positionMs: Long)

    @Query("SELECT * FROM group_tracks WHERE groupId = :groupId ORDER BY orderIndex ASC")
    fun observeGroupTracks(groupId: Long): Flow<List<GroupTrackCrossRef>>

    @Query("SELECT * FROM group_tracks WHERE groupId = :groupId ORDER BY orderIndex ASC")
    suspend fun getGroupTracksOnce(groupId: Long): List<GroupTrackCrossRef>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM group_tracks WHERE groupId = :groupId")
    suspend fun getMaxOrderIndex(groupId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupTracks(items: List<GroupTrackCrossRef>)

    @Query("DELETE FROM group_tracks WHERE groupId = :groupId AND trackUri = :trackUri")
    suspend fun removeTrackFromGroup(groupId: Long, trackUri: String)

    @Query("DELETE FROM group_tracks WHERE groupId = :groupId")
    suspend fun clearGroupTracks(groupId: Long)

    @Transaction
    suspend fun replaceGroupTrackOrder(groupId: Long, orderedUris: List<String>) {
        clearGroupTracks(groupId)
        val entities = orderedUris.mapIndexed { index, uri ->
            GroupTrackCrossRef(groupId = groupId, trackUri = uri, orderIndex = index)
        }
        insertGroupTracks(entities)
    }
}