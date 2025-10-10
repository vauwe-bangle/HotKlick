// data/PointDatabase.kt
package de.softopus.hotklick.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PointEntity::class],
    version = 4, // NEU: Version von 3 auf 4 erhöht für audioUri-Feld
    exportSchema = false
)
abstract class PointDatabase : RoomDatabase() {
    abstract fun pointDao(): PointDao

    companion object {
        @Volatile
        private var INSTANCE: PointDatabase? = null

        fun getDatabase(context: Context): PointDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PointDatabase::class.java,
                    "point_database"
                )
                    .fallbackToDestructiveMigration()  // NEU
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}