// data/ExerciseManager.kt - Verwaltet Übungen und Thumbnails
package de.softopus.hotklick.data

import android.content.Context
import android.net.Uri
import de.softopus.hotklick.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * Manager für Übungen - speichert Übungsliste mit Thumbnails
 */
class ExerciseManager(private val context: Context) {

    private val exercisesFile = File(context.filesDir, "exercises.json")

    /**
     * Lädt alle gespeicherten Übungen
     */
    suspend fun loadExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        try {
            if (!exercisesFile.exists()) {
                return@withContext emptyList()
            }

            val json = exercisesFile.readText()
            val jsonArray = JSONArray(json)

            val exercises = mutableListOf<Exercise>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val imageUriString = obj.optString("imageUri", null)
                val imageUri = if (imageUriString != null && imageUriString != "null") {
                    Uri.parse(imageUriString)
                } else null

                exercises.add(
                    Exercise(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        imageUri = imageUri,
                        hotspotCount = obj.optInt("hotspotCount", 0),
                        lastModified = Date(obj.getLong("lastModified"))
                    )
                )
            }

            println("DEBUG: ${exercises.size} Übungen geladen")
            exercises.forEach { ex ->
                println("DEBUG: Übung '${ex.name}' - URI: ${ex.imageUri} - Hotspots: ${ex.hotspotCount}")
            }

            exercises
        } catch (e: Exception) {
            println("DEBUG: Fehler beim Laden der Übungen: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Speichert eine Übung
     */
    suspend fun saveExercise(
        name: String,
        imageUri: Uri?,
        hotspotCount: Int = 0
    ): Exercise = withContext(Dispatchers.IO) {
        try {
            println("DEBUG saveExercise: name='$name', imageUri=$imageUri, hotspots=$hotspotCount")

            // Kopiere Bild ins interne Speicher (Persistenz)
            val persistentImageUri = imageUri?.let { uri ->
                copyImageToInternalStorage(uri, name)
            }

            // Lade bestehende Übungen
            val exercises = loadExercises().toMutableList()

            // Suche nach existierender Übung mit gleichem Namen
            val existingIndex = exercises.indexOfFirst { it.name == name }

            val exercise = if (existingIndex >= 0) {
                // Update bestehende Übung
                val existing = exercises[existingIndex]
                val updated = Exercise(
                    id = existing.id,
                    name = name,
                    imageUri = persistentImageUri ?: existing.imageUri,  // Behalte alte URI falls keine neue
                    hotspotCount = hotspotCount,
                    lastModified = Date()
                )
                exercises[existingIndex] = updated
                println("DEBUG: Übung '$name' aktualisiert - URI: ${updated.imageUri}")
                updated
            } else {
                // Neue Übung erstellen
                val newExercise = Exercise(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    imageUri = persistentImageUri,
                    hotspotCount = hotspotCount,
                    lastModified = Date()
                )
                exercises.add(newExercise)
                println("DEBUG: Neue Übung '$name' erstellt - URI: ${newExercise.imageUri}")
                newExercise
            }

            // Speichere aktualisierte Liste
            saveExercisesList(exercises)

            exercise
        } catch (e: Exception) {
            println("DEBUG: Fehler beim Speichern: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Löscht eine Übung
     */
    suspend fun deleteExercise(exerciseId: String) = withContext(Dispatchers.IO) {
        try {
            val exercises = loadExercises().toMutableList()
            exercises.removeIf { it.id == exerciseId }
            saveExercisesList(exercises)
            println("DEBUG: Übung $exerciseId gelöscht")
        } catch (e: Exception) {
            println("DEBUG: Fehler beim Löschen: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Speichert die Übungsliste als JSON
     */
    private fun saveExercisesList(exercises: List<Exercise>) {
        try {
            val jsonArray = JSONArray()

            exercises.forEach { exercise ->
                val obj = JSONObject().apply {
                    put("id", exercise.id)
                    put("name", exercise.name)
                    put("imageUri", exercise.imageUri?.toString() ?: "")
                    put("hotspotCount", exercise.hotspotCount)
                    put("lastModified", exercise.lastModified.time)
                }
                jsonArray.put(obj)
            }

            exercisesFile.writeText(jsonArray.toString(2))
            println("DEBUG: ${exercises.size} Übungen gespeichert in ${exercisesFile.absolutePath}")
        } catch (e: Exception) {
            println("DEBUG: Fehler beim Speichern der Liste: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Kopiert Bild ins interne Speicher für Persistenz
     */
    private fun copyImageToInternalStorage(sourceUri: Uri, exerciseName: String): Uri? {
        try {
            // Erstelle Bilder-Verzeichnis
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            // Generiere Dateinamen basierend auf URI-Hash
            val hash = sourceUri.hashCode().toString()
            val fileName = "exercise_${hash}.jpg"
            val destFile = File(imagesDir, fileName)

            // Prüfe ob Datei bereits existiert
            if (destFile.exists()) {
                println("DEBUG: Bild existiert bereits: ${destFile.absolutePath}")
                return Uri.fromFile(destFile)
            }

            // Kopiere Bild
            println("DEBUG: Kopiere Bild von $sourceUri nach ${destFile.absolutePath}")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            println("DEBUG: Bild erfolgreich kopiert")
            return Uri.fromFile(destFile)
        } catch (e: Exception) {
            println("DEBUG: Fehler beim Kopieren des Bildes: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}