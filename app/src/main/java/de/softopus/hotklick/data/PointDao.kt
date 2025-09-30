// data/PointDao.kt
package de.softopus.hotklick.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PointDao {
    @Query("SELECT * FROM points WHERE imageUri = :imageUri OR (:imageUri IS NULL AND imageUri IS NULL) ORDER BY id ASC")
    fun getPointsForImage(imageUri: String?): Flow<List<PointEntity>>

    @Insert
    suspend fun insertPoint(point: PointEntity)

    @Query("DELETE FROM points WHERE id = :pointId")
    suspend fun deletePointById(pointId: Int)

    @Query("DELETE FROM points WHERE imageUri = :imageUri OR (:imageUri IS NULL AND imageUri IS NULL)")
    suspend fun deleteAllPointsForImage(imageUri: String?)

    @Query("SELECT COUNT(*) FROM points WHERE imageUri = :imageUri OR (:imageUri IS NULL AND imageUri IS NULL)")
    suspend fun getPointCountForImage(imageUri: String?): Int

    @Query("SELECT * FROM points WHERE imageUri = :imageUri OR (:imageUri IS NULL AND imageUri IS NULL) ORDER BY id ASC")
    suspend fun getPointsForImageSync(imageUri: String?): List<PointEntity>

    // Sichere Umnummerierung nach Löschung
    @Transaction
    suspend fun deletePointAndRenumber(pointId: Int, imageUri: String?) {
        // 1. Punkt löschen
        deletePointById(pointId)

        // 2. Alle verbleibenden Punkte für dieses Bild holen
        val remainingPoints = getPointsForImageSync(imageUri)

        // 3. Punkte neu nummerieren (P1, P2, P3, ...)
        remainingPoints.forEachIndexed { index, point ->
            val newName = "P${index + 1}"
            updatePointName(point.id, newName)
        }
    }

    @Query("UPDATE points SET name = :newName WHERE id = :pointId")
    suspend fun updatePointName(pointId: Int, newName: String)

    // Debug-Queries
    @Query("SELECT * FROM points ORDER BY imageUri, id ASC")
    suspend fun getAllPointsDebug(): List<PointEntity>

    @Query("DELETE FROM points")
    suspend fun deleteAllPoints()
}