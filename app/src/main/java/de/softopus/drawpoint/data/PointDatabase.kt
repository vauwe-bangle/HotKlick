// data/PointDatabase.kt
package de.softopus.drawpoint.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PointEntity::class],
    version = 2, // Version für Text-Spalte erhöht
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
                    "point_database_v5" // Neue DB für Text-Feature
                )
                    .fallbackToDestructiveMigration() // Bei Problemen alles löschen
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}