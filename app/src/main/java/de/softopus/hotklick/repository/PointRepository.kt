// repository/PointRepository.kt
package de.softopus.hotklick.repository

import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.data.PointDao
import de.softopus.hotklick.data.PointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PointRepository(private val pointDao: PointDao) {

    fun getPointsForImage(imageUri: String?): Flow<List<DrawPoint>> {
        return pointDao.getPointsForImage(imageUri).map { entities ->
            entities.map { entity ->
                DrawPoint(
                    id = entity.id,
                    name = entity.name,
                    x = entity.x,
                    y = entity.y,
                    radius = entity.radius,
                    imageUri = entity.imageUri,
                    text = entity.text,
                    audioUri = entity.audioUri, // NEU: audioUri-Mapping hinzugefügt
                    exerciseName = entity.exerciseName  // NEU: exerciseName-Mapping hinzugefügt
                )
            }
        }
    }

    suspend fun getPointsForImageSync(imageUri: String?): List<DrawPoint> {
        return pointDao.getPointsForImageSync(imageUri).map { entity ->
            DrawPoint(
                id = entity.id,
                name = entity.name,
                x = entity.x,
                y = entity.y,
                radius = entity.radius,
                imageUri = entity.imageUri,
                text = entity.text,
                audioUri = entity.audioUri, // NEU: audioUri-Mapping hinzugefügt
                exerciseName = entity.exerciseName  // NEU: exerciseName-Mapping hinzugefügt
            )
        }
    }

    suspend fun insertPoint(point: DrawPoint) {
        val entity = PointEntity(
            name = point.name,
            x = point.x,
            y = point.y,
            radius = point.radius,
            imageUri = point.imageUri,
            text = point.text,
            audioUri = point.audioUri, // NEU: audioUri-Mapping hinzugefügt
            exerciseName = point.exerciseName  // NEU: exerciseName-Mapping hinzugefügt
        )
        pointDao.insertPoint(entity)
    }

    suspend fun deletePointWithRenumbering(pointId: Int, imageUri: String?) {
        // Sichere Löschung mit automatischer Umnummerierung
        pointDao.deletePointAndRenumber(pointId, imageUri)
    }

    suspend fun deleteAllPointsForImage(imageUri: String?) {
        pointDao.deleteAllPointsForImage(imageUri)
    }

    suspend fun getPointCountForImage(imageUri: String?): Int {
        return pointDao.getPointCountForImage(imageUri)
    }

    suspend fun clearDatabase() {
        pointDao.deleteAllPoints()
    }
}