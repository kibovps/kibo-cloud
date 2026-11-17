package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "linked_folders")
data class LinkedFolderEntity(
    @PrimaryKey val uriString: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val colorIndex: Int = 0,
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0,
    val iconKey: String = "folder",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "file_records")
data class FileRecordEntity(
    @PrimaryKey val fileUri: String,
    val fileName: String,
    val mimeType: String,
    val categoryName: String,
    val sourceName: String,
    val parentUri: String,
    val sizeBytes: Long,
    val isFavorite: Boolean = false,
    val lastOpenedAt: Long = 0L,
    val playbackPositionMs: Long = 0L,
)

@Entity(
    tableName = "file_collection_cross_ref",
    primaryKeys = ["fileUri", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("collectionId")],
)
data class FileCollectionCrossRef(
    val fileUri: String,
    val collectionId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)

data class CollectionWithCount(
    @Embedded val collection: CollectionEntity,
    val itemCount: Int,
)

data class CollectionWithFiles(
    @Embedded val collection: CollectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "fileUri",
        associateBy = Junction(
            value = FileCollectionCrossRef::class,
            parentColumn = "collectionId",
            entityColumn = "fileUri",
        ),
    )
    val files: List<FileRecordEntity>,
)
