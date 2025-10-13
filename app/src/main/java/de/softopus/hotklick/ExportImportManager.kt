// ExportImportManager.kt
package de.softopus.hotklick.util

import android.content.Context
import android.net.Uri
import de.softopus.hotklick.data.DrawPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ExportImportManager(private val context: Context) {

    /**
     * Exportiert eine Übung als ZIP-Datei
     * @param imageUri URI des Bildes
     * @param points Liste aller Hotspots
     * @param exerciseName Name der Übung
     * @param destinationUri Ziel-URI für die ZIP-Datei
     * @return true bei Erfolg
     */
    suspend fun exportExercise(
        imageUri: String,
        points: List<DrawPoint>,
        exerciseName: String,
        destinationUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            println("DEBUG Export: Start - imageUri=$imageUri, points=${points.size}")

            // Erstelle temporäres Verzeichnis
            val tempDir = File(context.cacheDir, "export_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                // 1. Kopiere Bild
                val imageFile = copyImageToTemp(imageUri, tempDir)
                if (imageFile == null) {
                    println("DEBUG Export: Fehler beim Kopieren des Bildes")
                    return@withContext false
                }

                // 2. Kopiere Audio-Dateien
                val audioFiles = copyAudioFilesToTemp(points, tempDir)

                // 3. Erstelle metadata.json
                val metadata = createMetadata(imageFile.name, points, exerciseName, audioFiles)
                val metadataFile = File(tempDir, "metadata.json")
                metadataFile.writeText(metadata.toString(2))

                // 4. Erstelle ZIP
                createZipFile(tempDir, destinationUri)

                println("DEBUG Export: Erfolgreich abgeschlossen")
                true
            } finally {
                // Aufräumen
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            println("DEBUG Export: Fehler - ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Importiert eine Übung aus einer ZIP-Datei
     * @param zipUri URI der ZIP-Datei
     * @return Pair aus Bild-URI und Liste der Hotspots, oder null bei Fehler
     */
    suspend fun importExercise(zipUri: Uri): ImportResult? = withContext(Dispatchers.IO) {
        try {
            println("DEBUG Import: Start - zipUri=$zipUri")

            // Erstelle temporäres Verzeichnis
            val tempDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                // 1. Entpacke ZIP
                unzipFile(zipUri, tempDir)

                // 2. Lese metadata.json
                val metadataFile = File(tempDir, "metadata.json")
                if (!metadataFile.exists()) {
                    println("DEBUG Import: metadata.json nicht gefunden")
                    return@withContext null
                }

                val metadata = JSONObject(metadataFile.readText())
                val exerciseName = metadata.getString("exerciseName")
                val imageFileName = metadata.getString("imageFileName")

                // 3. Kopiere Bild ins App-Verzeichnis
                val imageFile = File(tempDir, imageFileName)
                val savedImageUri = saveImageToAppStorage(imageFile)
                if (savedImageUri == null) {
                    println("DEBUG Import: Fehler beim Speichern des Bildes")
                    return@withContext null
                }

                // 4. Erstelle Hotspots
                val pointsArray = metadata.getJSONArray("points")
                val points = mutableListOf<DrawPoint>()

                for (i in 0 until pointsArray.length()) {
                    val pointJson = pointsArray.getJSONObject(i)

                    // Kopiere Audio-Datei falls vorhanden
                    val audioUri = if (pointJson.has("audioFileName") &&
                        !pointJson.isNull("audioFileName")) {
                        val audioFileName = pointJson.getString("audioFileName")
                        val audioFile = File(tempDir, "audio/$audioFileName")
                        if (audioFile.exists()) {
                            saveAudioToAppStorage(audioFile)
                        } else null
                    } else null

                    val point = DrawPoint(
                        id = 0,
                        name = pointJson.getString("name"),
                        x = pointJson.getDouble("x").toFloat(),
                        y = pointJson.getDouble("y").toFloat(),
                        radius = pointJson.getDouble("radius").toFloat(),
                        imageUri = savedImageUri,
                        text = if (pointJson.has("text") && !pointJson.isNull("text"))
                            pointJson.getString("text") else null,
                        audioUri = audioUri,
                        exerciseName = exerciseName
                    )
                    points.add(point)
                }

                println("DEBUG Import: Erfolgreich - ${points.size} Punkte importiert")
                ImportResult(savedImageUri, points, exerciseName)
            } finally {
                // Aufräumen
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            println("DEBUG Import: Fehler - ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun copyImageToTemp(imageUri: String, tempDir: File): File? {
        return try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Bestimme Dateiendung
            val mimeType = context.contentResolver.getType(uri)
            val extension = when {
                mimeType?.contains("png") == true -> "png"
                mimeType?.contains("jpg") == true || mimeType?.contains("jpeg") == true -> "jpg"
                else -> "jpg"
            }

            val imageFile = File(tempDir, "image.$extension")
            inputStream.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            imageFile
        } catch (e: Exception) {
            println("DEBUG copyImageToTemp: Fehler - ${e.message}")
            null
        }
    }

    private fun copyAudioFilesToTemp(points: List<DrawPoint>, tempDir: File): Map<String, String> {
        val audioDir = File(tempDir, "audio")
        audioDir.mkdirs()

        val audioFileMap = mutableMapOf<String, String>()

        points.forEachIndexed { index, point ->
            if (point.audioUri != null) {
                try {
                    val uri = Uri.parse(point.audioUri)
                    val inputStream = context.contentResolver.openInputStream(uri)

                    if (inputStream != null) {
                        val audioFileName = "${point.name}_audio.m4a"
                        val audioFile = File(audioDir, audioFileName)

                        inputStream.use { input ->
                            audioFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        audioFileMap[point.name] = audioFileName
                        println("DEBUG copyAudio: ${point.name} -> $audioFileName")
                    }
                } catch (e: Exception) {
                    println("DEBUG copyAudio: Fehler bei ${point.name} - ${e.message}")
                }
            }
        }

        return audioFileMap
    }

    private fun createMetadata(
        imageFileName: String,
        points: List<DrawPoint>,
        exerciseName: String,
        audioFiles: Map<String, String>
    ): JSONObject {
        val metadata = JSONObject()
        metadata.put("version", 1)
        metadata.put("exerciseName", exerciseName)
        metadata.put("imageFileName", imageFileName)

        val pointsArray = JSONArray()
        points.forEach { point ->
            val pointJson = JSONObject()
            pointJson.put("name", point.name)
            pointJson.put("x", point.x)
            pointJson.put("y", point.y)
            pointJson.put("radius", point.radius)
            pointJson.put("text", point.text)

            if (audioFiles.containsKey(point.name)) {
                pointJson.put("audioFileName", audioFiles[point.name])
            }

            pointsArray.put(pointJson)
        }

        metadata.put("points", pointsArray)
        return metadata
    }

    private fun createZipFile(sourceDir: File, destinationUri: Uri) {
        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zipOut ->
                sourceDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val relativePath = file.relativeTo(sourceDir).path
                        val entry = ZipEntry(relativePath)
                        zipOut.putNextEntry(entry)

                        FileInputStream(file).use { input ->
                            input.copyTo(zipOut)
                        }

                        zipOut.closeEntry()
                    }
                }
            }
        }
    }

    private fun unzipFile(zipUri: Uri, destDir: File) {
        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry

                while (entry != null) {
                    val file = File(destDir, entry.name)

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { output ->
                            zipIn.copyTo(output)
                        }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }
    }

    private fun saveImageToAppStorage(imageFile: File): String? {
        return try {
            val imagesDir = File(context.filesDir, "images")
            imagesDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val extension = imageFile.extension
            val destFile = File(imagesDir, "image_$timestamp.$extension")

            imageFile.copyTo(destFile, overwrite = true)
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            println("DEBUG saveImageToAppStorage: Fehler - ${e.message}")
            null
        }
    }

    private fun saveAudioToAppStorage(audioFile: File): String? {
        return try {
            val audioDir = File(context.filesDir, "audio")
            audioDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val destFile = File(audioDir, "audio_$timestamp.m4a")

            audioFile.copyTo(destFile, overwrite = true)
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            println("DEBUG saveAudioToAppStorage: Fehler - ${e.message}")
            null
        }
    }
}

data class ImportResult(
    val imageUri: String,
    val points: List<DrawPoint>,
    val exerciseName: String
)