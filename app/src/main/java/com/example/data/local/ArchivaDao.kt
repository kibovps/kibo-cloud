package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchivaDao {

    // Linked SAF Folders
    @Query("SELECT * FROM linked_folders ORDER BY addedAt DESC")
    fun observeLinkedFolders(): Flow<List<LinkedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLinkedFolder(folder: LinkedFolderEntity)

    @Query("DELETE FROM linked_folders WHERE uriString = :uriString")
    suspend fun deleteLinkedFolder(uriString: String)

    // Collections
    @Query(
        """
        SELECT c.*, COUNT(x.fileUri) AS itemCount
        FROM collections c
        LEFT JOIN file_collection_cross_ref x ON c.id = x.collectionId
        GROUP BY c.id
        ORDER BY c.createdAt DESC
        """
    )
    fun observeCollectionsWithCount(): Flow<List<CollectionWithCount>>

    @Transaction
    @Query("SELECT * FROM collections WHERE id = :collectionId")
    fun observeCollectionWithFiles(collectionId: Long): Flow<CollectionWithFiles?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: Long)

    // File Records (Favorites, Recents, Playback resume)
    @Query("SELECT * FROM file_records")
    fun observeAllFileRecords(): Flow<List<FileRecordEntity>>

    @Query("SELECT * FROM file_records WHERE isFavorite = 1 ORDER BY lastOpenedAt DESC, fileName ASC")
    fun observeFavorites(): Flow<List<FileRecordEntity>>

    @Query("SELECT * FROM file_records WHERE lastOpenedAt > 0 ORDER BY lastOpenedAt DESC LIMIT 20")
    fun observeRecentFiles(): Flow<List<FileRecordEntity>>

    @Query("SELECT * FROM file_records WHERE fileUri = :fileUri LIMIT 1")
    suspend fun getFileRecord(fileUri: String): FileRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFileRecord(record: FileRecordEntity)

    @Query("UPDATE file_records SET isFavorite = :isFavorite WHERE fileUri = :fileUri")
    suspend fun updateFavorite(fileUri: String, isFavorite: Boolean)

    @Query("UPDATE file_records SET playbackPositionMs = :positionMs WHERE fileUri = :fileUri")
    suspend fun updatePlaybackPosition(fileUri: String, positionMs: Long)

    @Query("DELETE FROM file_records WHERE fileUri = :fileUri")
    suspend fun deleteFileRecord(fileUri: String)

    // Collection Cross References
    @Query("SELECT * FROM file_collection_cross_ref")
    fun observeAllCrossRefs(): Flow<List<FileCollectionCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFileToCollection(crossRef: FileCollectionCrossRef)

    @Query("DELETE FROM file_collection_cross_ref WHERE fileUri = :fileUri AND collectionId = :collectionId")
    suspend fun removeFileFromCollection(fileUri: String, collectionId: Long)

    @Query("DELETE FROM file_collection_cross_ref WHERE fileUri = :fileUri")
    suspend fun clearFileCollections(fileUri: String)
}
