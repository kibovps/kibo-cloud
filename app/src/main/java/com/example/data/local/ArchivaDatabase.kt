package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LinkedFolderEntity::class,
        CollectionEntity::class,
        FileRecordEntity::class,
        FileCollectionCrossRef::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ArchivaDatabase : RoomDatabase() {
    abstract fun archivaDao(): ArchivaDao

    companion object {
        @Volatile
        private var instance: ArchivaDatabase? = null

        fun getInstance(context: Context): ArchivaDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ArchivaDatabase::class.java,
                    "archiva_database"
                ).build().also { instance = it }
            }
        }
    }
}
