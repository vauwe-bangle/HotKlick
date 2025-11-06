// ExportImportManager.kt
// FINAL VERSION - Mit relativen Koordinaten (0.0 - 1.0)
package de.softopus.hotklick.util

import android.content.Context
import android.graphics.BitmapFactory
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
     * Exportiert eine Übung als ZIP-Datei mit RELATIVEN Koordinaten (WEB-KOMPATIBEL!)
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
                // 1. Kopiere Bild und hole Dimensionen
                val imageFile = copyImageToTemp(imageUri, tempDir)
                if (imageFile == null) {
                    println("DEBUG Export: Fehler beim Kopieren des Bildes")
                    return@withContext false
                }

                // 2. Hole Bild-Dimensionen für relative Koordinaten
                val imageDimensions = getImageDimensions(imageFile)
                val imageWidth = imageDimensions.first
                val imageHeight = imageDimensions.second
                println("DEBUG Export: Image dimensions: ${imageWidth}x${imageHeight}")

                // 3. Kopiere Audio-Dateien (WEB-FORMAT: audio_1.webm, audio_2.webm, ...)
                val audioFilesMap = copyAudioFilesToTemp(points, tempDir)

                // 4. Erstelle hotspots.json mit RELATIVEN Koordinaten (WEB-FORMAT!)
                val hotspotsJson = createHotspotsJson(
                    imageFile.name,
                    points,
                    exerciseName,
                    audioFilesMap,
                    imageWidth,
                    imageHeight
                )
                val hotspotsFile = File(tempDir, "hotspots.json")
                hotspotsFile.writeText(hotspotsJson.toString(2))

                // 5. Erstelle metadata.json (optional, für Web-Info)
                val metadata = createMetadata(exerciseName, points.size)
                val metadataFile = File(tempDir, "metadata.json")
                metadataFile.writeText(metadata.toString(2))

                // 6. Erstelle ZIP
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
     * Importiert eine Übung aus einer ZIP-Datei (WEB-APP KOMPATIBEL!)
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

                // 2. Prüfe Format (Web oder Android)
                val hotspotsFile = File(tempDir, "hotspots.json")
                val metadataFile = File(tempDir, "metadata.json")

                val (imageUri, points, exerciseName) = if (hotspotsFile.exists()) {
                    // WEB-FORMAT
                    println("DEBUG Import: Web-Format erkannt (hotspots.json)")
                    importWebFormat(tempDir, hotspotsFile)
                } else if (metadataFile.exists()) {
                    // ALTES ANDROID-FORMAT
                    println("DEBUG Import: Android-Format erkannt (metadata.json)")
                    importAndroidFormat(tempDir, metadataFile)
                } else {
                    println("DEBUG Import: Kein gültiges Format gefunden")
                    return@withContext null
                }

                if (imageUri == null || points.isEmpty()) {
                    println("DEBUG Import: Fehler beim Parsen der Daten")
                    return@withContext null
                }

                println("DEBUG Import: Erfolgreich - ${points.size} Punkte importiert")
                ImportResult(imageUri, points, exerciseName)
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

    /**
     * Importiert Web-App Format (hotspots.json) mit RELATIVEN Koordinaten
     */
    private fun importWebFormat(tempDir: File, hotspotsFile: File): Triple<String?, List<DrawPoint>, String> {
        val hotspotsArray = JSONArray(hotspotsFile.readText())
        val points = mutableListOf<DrawPoint>()

        if (hotspotsArray.length() == 0) {
            return Triple(null, emptyList(), "")
        }

        // Erste Hotspot für exerciseName und imageUri
        val firstHotspot = hotspotsArray.getJSONObject(0)
        val exerciseName = firstHotspot.optString("exerciseName", "Importierte Übung")
        val imageFileName = firstHotspot.optString("imageUri", "image.jpg")

        // Bild kopieren
        val imageFile = File(tempDir, imageFileName)
        val savedImageUri = if (imageFile.exists()) {
            saveImageToAppStorage(imageFile)
        } else null

        if (savedImageUri == null) {
            return Triple(null, emptyList(), exerciseName)
        }

        // Hole Bild-Dimensionen für Koordinaten-Konvertierung
        val imageDimensions = getImageDimensions(imageFile)
        val imageWidth = imageDimensions.first.toFloat()
        val imageHeight = imageDimensions.second.toFloat()
        println("DEBUG Import: Target image dimensions: ${imageWidth}x${imageHeight}")

        // Hotspots erstellen
        for (i in 0 until hotspotsArray.length()) {
            val pointJson = hotspotsArray.getJSONObject(i)

            // Audio-Datei kopieren (Web-Format: audio_1.webm, audio_2.webm)
            val audioUri = if (pointJson.has("audioUri") && !pointJson.isNull("audioUri")) {
                val audioFileName = pointJson.getString("audioUri")
                val audioFile = File(tempDir, audioFileName)
                if (audioFile.exists()) {
                    saveAudioToAppStorage(audioFile)
                } else null
            } else null

            // Konvertiere relative Koordinaten (0.0-1.0) zu absoluten Pixel-Werten
            val relX = pointJson.getDouble("x")
            val relY = pointJson.getDouble("y")
            val relRadius = pointJson.getDouble("radius")

            // Prüfe ob Werte relativ (< 1.0) oder absolut (> 1.0) sind
            val isRelative = relX <= 1.0 && relY <= 1.0

            val absoluteX = if (isRelative) (relX * imageWidth).toFloat() else relX.toFloat()
            val absoluteY = if (isRelative) (relY * imageHeight).toFloat() else relY.toFloat()
            val absoluteRadius = if (isRelative) (relRadius * imageWidth).toFloat() else relRadius.toFloat()

            println("DEBUG Import: ${pointJson.getString("name")}: relative($relX, $relY) -> absolute($absoluteX, $absoluteY)")

            val point = DrawPoint(
                id = 0,
                name = pointJson.getString("name"),
                x = absoluteX,
                y = absoluteY,
                radius = absoluteRadius,
                imageUri = savedImageUri,
                text = pointJson.optString("text", null),
                audioUri = audioUri,
                exerciseName = exerciseName
            )
            points.add(point)
        }

        return Triple(savedImageUri, points, exerciseName)
    }

    /**
     * Importiert altes Android-Format (metadata.json mit verschachtelter Struktur)
     */
    private fun importAndroidFormat(tempDir: File, metadataFile: File): Triple<String?, List<DrawPoint>, String> {
        val metadata = JSONObject(metadataFile.readText())
        val exerciseName = metadata.getString("exerciseName")
        val imageFileName = metadata.getString("imageFileName")

        // Bild kopieren
        val imageFile = File(tempDir, imageFileName)
        val savedImageUri = saveImageToAppStorage(imageFile)
        if (savedImageUri == null) {
            return Triple(null, emptyList(), exerciseName)
        }

        // Hotspots erstellen
        val pointsArray = metadata.getJSONArray("points")
        val points = mutableListOf<DrawPoint>()

        for (i in 0 until pointsArray.length()) {
            val pointJson = pointsArray.getJSONObject(i)

            // Audio-Datei kopieren (Android-Format: audio/A1_audio.m4a)
            val audioUri = if (pointJson.has("audioFileName") && !pointJson.isNull("audioFileName")) {
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
                text = pointJson.optString("text", null),
                audioUri = audioUri,
                exerciseName = exerciseName
            )
            points.add(point)
        }

        return Triple(savedImageUri, points, exerciseName)
    }

    /**
     * Erstellt hotspots.json im Web-App Format mit RELATIVEN Koordinaten
     */
    private fun createHotspotsJson(
        imageFileName: String,
        points: List<DrawPoint>,
        exerciseName: String,
        audioFiles: Map<Int, String>,
        imageWidth: Int,
        imageHeight: Int
    ): JSONArray {
        val hotspotsArray = JSONArray()

        points.forEachIndexed { index, point ->
            val pointJson = JSONObject()

            // Konvertiere absolute Koordinaten zu relativen (0.0 - 1.0)
            val relativeX = point.x / imageWidth.toFloat()
            val relativeY = point.y / imageHeight.toFloat()
            val relativeRadius = point.radius / imageWidth.toFloat()

            pointJson.put("id", point.id.toString())
            pointJson.put("name", point.name)
            pointJson.put("x", relativeX.toDouble())
            pointJson.put("y", relativeY.toDouble())
            pointJson.put("radius", relativeRadius.toDouble())
            pointJson.put("imageUri", imageFileName)
            pointJson.put("text", point.text ?: "")
            // Nur audioUri setzen wenn tatsächlich vorhanden
            if (audioFiles[index] != null) {
                pointJson.put("audioUri", audioFiles[index])
            }
            pointJson.put("exerciseName", exerciseName)
            pointJson.put("hasText", !point.text.isNullOrEmpty())
            pointJson.put("hasAudio", point.audioUri != null)

            println("DEBUG Export: ${point.name}: absolute(${point.x}, ${point.y}) -> relative($relativeX, $relativeY)")

            hotspotsArray.put(pointJson)
        }

        return hotspotsArray
    }

    /**
     * Holt Dimensionen eines Bildes
     */
    private fun getImageDimensions(imageFile: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imageFile.absolutePath, options)
        return Pair(options.outWidth, options.outHeight)
    }

    /**
     * Erstellt metadata.json (optional, für Web-Info)
     */
    private fun createMetadata(exerciseName: String, hotspotCount: Int): JSONObject {
        val metadata = JSONObject()
        metadata.put("version", "1.0")
        metadata.put("exportDate", System.currentTimeMillis())
        metadata.put("exerciseName", exerciseName)
        metadata.put("hotspotCount", hotspotCount)
        metadata.put("appType", "HotKlick-Android")
        return metadata
    }

    /**
     * Kopiert Audio-Dateien im Web-Format (audio_1.webm, audio_2.webm, ...)
     */
    private fun copyAudioFilesToTemp(points: List<DrawPoint>, tempDir: File): Map<Int, String> {
        val audioFileMap = mutableMapOf<Int, String>()
        var audioCounter = 1

        points.forEachIndexed { index, point ->
            if (point.audioUri != null) {
                try {
                    val uri = Uri.parse(point.audioUri)
                    val inputStream = context.contentResolver.openInputStream(uri)

                    if (inputStream != null) {
                        // WEB-FORMAT: audio_1.webm, audio_2.webm (direkt im ZIP-Root!)
                        val audioFileName = "audio_$audioCounter.webm"
                        val audioFile = File(tempDir, audioFileName)

                        inputStream.use { input ->
                            audioFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        audioFileMap[index] = audioFileName
                        audioCounter++
                        println("DEBUG copyAudio: ${point.name} -> $audioFileName")
                    }
                } catch (e: Exception) {
                    println("DEBUG copyAudio: Fehler bei ${point.name} - ${e.message}")
                }
            }
        }

        return audioFileMap
    }

    private fun copyImageToTemp(imageUri: String, tempDir: File): File? {
        return try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Web-Format: immer image.jpg
            val imageFile = File(tempDir, "image.jpg")
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
            val extension = audioFile.extension
            val destFile = File(audioDir, "audio_$timestamp.$extension")

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