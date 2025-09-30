// data/PointEntity.kt
package de.softopus.hotklick.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "points")
data class PointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val x: Float,
    val y: Float,
    val radius: Float,
    val imageUri: String? = null,
    val text: String? = null,
    val audioUri: String? = null // NEU: Audio-Feld hinzugefügt
)

data class DrawPoint(
    val id: Int = 0,
    val name: String,
    val x: Float,
    val y: Float,
    val radius: Float = 50f,
    val imageUri: String? = null,
    val text: String? = null,
    val audioUri: String? = null // NEU: Audio-Feld hinzugefügt
)