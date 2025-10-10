// viewmodel/DrawingViewModel.kt
package de.softopus.hotklick.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.data.PointDatabase
import de.softopus.hotklick.repository.PointRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

import android.content.Intent

class DrawingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PointRepository

    private val _currentSessionPoints = MutableStateFlow<List<DrawPoint>>(emptyList())
    val points: StateFlow<List<DrawPoint>> = _currentSessionPoints.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _backgroundImageUri = MutableStateFlow<Uri?>(null)
    val backgroundImageUri: StateFlow<Uri?> = _backgroundImageUri.asStateFlow()

    private val _pointRadius = MutableStateFlow(50f)
    val pointRadius: StateFlow<Float> = _pointRadius.asStateFlow()

    private val _showTextDialog = MutableStateFlow(false)
    val showTextDialog: StateFlow<Boolean> = _showTextDialog.asStateFlow()

    private val _selectedPointForText = MutableStateFlow<DrawPoint?>(null)
    val selectedPointForText: StateFlow<DrawPoint?> = _selectedPointForText.asStateFlow()

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _showAudioDialog = MutableStateFlow(false)
    val showAudioDialog: StateFlow<Boolean> = _showAudioDialog.asStateFlow()

    private val _selectedPointForAudio = MutableStateFlow<DrawPoint?>(null)
    val selectedPointForAudio: StateFlow<DrawPoint?> = _selectedPointForAudio.asStateFlow()

    private val _showRecorderDialog = MutableStateFlow(false)
    val showRecorderDialog: StateFlow<Boolean> = _showRecorderDialog.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    // Modus-Verwaltung (Edit/Practice)
    private val _isEditMode = MutableStateFlow(false)  // Start im Übungsmodus
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    private val _selectedHotspotText = MutableStateFlow("")
    val selectedHotspotText: StateFlow<String> = _selectedHotspotText.asStateFlow()

    private val _selectedHotspotName = MutableStateFlow("")
    val selectedHotspotName: StateFlow<String> = _selectedHotspotName.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private val _currentAudioUri = MutableStateFlow<String?>(null)
    val currentAudioUri: StateFlow<String?> = _currentAudioUri.asStateFlow()

    // Übungsname
    private val _exerciseName = MutableStateFlow<String>("")
    val exerciseName: StateFlow<String> = _exerciseName.asStateFlow()

    private val _showExerciseNameDialog = MutableStateFlow(false)
    val showExerciseNameDialog: StateFlow<Boolean> = _showExerciseNameDialog.asStateFlow()

    private val _exerciseNameInput = MutableStateFlow("")
    val exerciseNameInput: StateFlow<String> = _exerciseNameInput.asStateFlow()

    private val imageRadiusMap = mutableMapOf<String?, Float>()

    // Als StateFlow definieren (bei den anderen StateFlows)
    private val _currentRecordingPointName = MutableStateFlow<String?>(null)
    val currentRecordingPointName: StateFlow<String?> = _currentRecordingPointName.asStateFlow()


    // Vertiefungsmodus States
    private val _showDeepLearningButtons = MutableStateFlow(false)
    val showDeepLearningButtons: StateFlow<Boolean> = _showDeepLearningButtons.asStateFlow()

    private val _isDeepLearningMode = MutableStateFlow(false)
    val isDeepLearningMode: StateFlow<Boolean> = _isDeepLearningMode.asStateFlow()

    private val _deepLearningType = MutableStateFlow<String?>(null)
    val deepLearningType: StateFlow<String?> = _deepLearningType.asStateFlow()

    private val _showTaskCountDialog = MutableStateFlow(false)
    val showTaskCountDialog: StateFlow<Boolean> = _showTaskCountDialog.asStateFlow()

    private val _selectedDeepLearningType = MutableStateFlow<String?>(null)
    val selectedDeepLearningType: StateFlow<String?> = _selectedDeepLearningType.asStateFlow()

    private val _deepLearningTasksTotal = MutableStateFlow(0)
    val deepLearningTasksTotal: StateFlow<Int> = _deepLearningTasksTotal.asStateFlow()

    private val _deepLearningTasksCurrent = MutableStateFlow(0)
    val deepLearningTasksCurrent: StateFlow<Int> = _deepLearningTasksCurrent.asStateFlow()

    private val _deepLearningCorrect = MutableStateFlow(0)
    val deepLearningCorrect: StateFlow<Int> = _deepLearningCorrect.asStateFlow()

    private val _deepLearningWrong = MutableStateFlow(0)
    val deepLearningWrong: StateFlow<Int> = _deepLearningWrong.asStateFlow()

    private val _currentChallengePoint = MutableStateFlow<DrawPoint?>(null)
    val currentChallengePoint: StateFlow<DrawPoint?> = _currentChallengePoint.asStateFlow()

    private val _deepLearningFeedback = MutableStateFlow("")
    val deepLearningFeedback: StateFlow<String> = _deepLearningFeedback.asStateFlow()

    private val _showDeepLearningResult = MutableStateFlow(false)
    val showDeepLearningResult: StateFlow<Boolean> = _showDeepLearningResult.asStateFlow()


    init {
        val database = PointDatabase.getDatabase(application)
        repository = PointRepository(database.pointDao())

        // Lade Standard-Infobild beim Start (Übungsmodus)
        val infoImageUri =
            Uri.parse("android.resource://${application.packageName}/drawable/info_practice")
        _backgroundImageUri.value = infoImageUri

        println("DEBUG: ViewModel initialisiert mit Infobild")
    }


    fun loadInfoImage(isEditMode: Boolean) {
        val imageName = if (isEditMode) "info_edit" else "info_practice"
        val infoImageUri =
            Uri.parse("android.resource://${getApplication<Application>().packageName}/drawable/$imageName")
        _backgroundImageUri.value = infoImageUri
        _currentSessionPoints.value = emptyList()
    }

    fun addPoint(x: Float, y: Float) {
        val currentPoints = _currentSessionPoints.value.toMutableList()
        val pointName = "P${currentPoints.size + 1}"

        val newPoint = DrawPoint(
            id = 0,
            name = pointName,
            x = x,
            y = y,
            radius = _pointRadius.value,
            imageUri = _backgroundImageUri.value?.toString(),
            text = null,
            audioUri = null
        )

        currentPoints.add(newPoint)
        _currentSessionPoints.value = currentPoints

        println("DEBUG: Punkt $pointName erstellt")
        _message.value = "Punkt $pointName erstellt"

        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _deepLearningFeedback.value = ""

            println("DEBUG: Nach 2 Sekunden - tasksCurrent=${_deepLearningTasksCurrent.value}, tasksTotal=${_deepLearningTasksTotal.value}")

            if (_deepLearningTasksCurrent.value >= _deepLearningTasksTotal.value) {
                println("DEBUG: Alle Aufgaben fertig - exitDeepLearningMode")
                exitDeepLearningMode()
            } else {
                println("DEBUG: Rufe nextDeepLearningChallenge auf")
                nextDeepLearningChallenge()
            }
        }    }

    fun deletePoint(pointId: Int, pointName: String) {
        val currentPoints = _currentSessionPoints.value.toMutableList()
        val pointToRemove = currentPoints.find { it.name == pointName }

        if (pointToRemove != null) {
            currentPoints.remove(pointToRemove)
            val renumberedPoints = currentPoints.mapIndexed { index, point ->
                point.copy(name = "P${index + 1}")
            }
            _currentSessionPoints.value = renumberedPoints
            println("DEBUG: Punkt $pointName gelöscht")
            _message.value = "Punkt $pointName gelöscht"
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _message.value = ""
        }
    }

    fun setBackgroundImage(uri: Uri?) {
        viewModelScope.launch {
            val previousImageUri = _backgroundImageUri.value?.toString()
            imageRadiusMap[previousImageUri] = _pointRadius.value
            _backgroundImageUri.value = uri
            val newImageUri = uri?.toString()

            if (uri != null) {
                val savedPoints = repository.getPointsForImageSync(newImageUri)
                if (savedPoints.isNotEmpty()) {
                    _currentSessionPoints.value = savedPoints
                    // Lade Übungsname vom ersten Punkt
                    _exerciseName.value = savedPoints.firstOrNull()?.exerciseName ?: ""
                    _message.value = "Bild geladen - ${savedPoints.size} Punkte"
                } else {
                    _currentSessionPoints.value = emptyList()
                    _exerciseName.value = ""  // NEU
                    _message.value = "Neues Bild geladen"
                }
                val savedRadius = imageRadiusMap[newImageUri] ?: 50f
                _pointRadius.value = savedRadius
            } else {
                _currentSessionPoints.value = emptyList()
                _exerciseName.value = ""  // NEU
                _message.value = "Kein Bild"
            }

            kotlinx.coroutines.delay(2000)
            _message.value = ""
        }
    }
    fun saveDataAndClearImage() {
        viewModelScope.launch {
            val currentImageUri = _backgroundImageUri.value?.toString()
            val currentPoints = _currentSessionPoints.value

            if (currentImageUri != null && currentPoints.isNotEmpty()) {
                try {
                    repository.deleteAllPointsForImage(currentImageUri)
                    currentPoints.forEach { point ->
                        repository.insertPoint(point.copy(imageUri = currentImageUri))
                    }
                    _message.value = "${currentPoints.size} Punkte gespeichert"
                } catch (e: Exception) {
                    _message.value = "Fehler beim Speichern: ${e.message}"
                }
            } else {
                _message.value = "Keine Daten zum Speichern"
            }

            imageRadiusMap[currentImageUri] = _pointRadius.value
            _currentSessionPoints.value = emptyList()
            _backgroundImageUri.value = null

            kotlinx.coroutines.delay(3000)
            _message.value = ""
        }
    }

    fun increasePointRadius() {
        val currentRadius = _pointRadius.value
        if (currentRadius < 180f) {
            val newRadius = currentRadius + 20f
            _pointRadius.value = newRadius
            val currentImageUri = _backgroundImageUri.value?.toString()
            imageRadiusMap[currentImageUri] = newRadius
        }
    }

    fun decreasePointRadius() {
        val currentRadius = _pointRadius.value
        if (currentRadius > 10f) {
            val newRadius = currentRadius - 20f
            _pointRadius.value = newRadius
            val currentImageUri = _backgroundImageUri.value?.toString()
            imageRadiusMap[currentImageUri] = newRadius
        }
    }

    fun openTextDialog(point: DrawPoint) {
        _selectedPointForText.value = point
        _textInput.value = point.text ?: ""
        _showTextDialog.value = true
    }

    fun closeTextDialog() {
        _showTextDialog.value = false
        _selectedPointForText.value = null
        _textInput.value = ""
    }

    fun updateTextInput(text: String) {
        _textInput.value = text
    }

    fun saveTextToPoint() {
        val selectedPoint = _selectedPointForText.value
        val inputText = _textInput.value.trim()

        if (selectedPoint != null) {
            val currentPoints = _currentSessionPoints.value.toMutableList()
            val pointIndex = currentPoints.indexOfFirst { it.name == selectedPoint.name }

            if (pointIndex != -1) {
                val updatedPoint = currentPoints[pointIndex].copy(
                    text = if (inputText.isNotEmpty()) inputText else null
                )
                currentPoints[pointIndex] = updatedPoint
                _currentSessionPoints.value = currentPoints
                _message.value = "Text für ${selectedPoint.name} gespeichert"

                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _message.value = ""
                }
            }
        }
        closeTextDialog()
    }

    fun openAudioDialog(point: DrawPoint) {
        _selectedPointForAudio.value = point
        _showAudioDialog.value = true
        println("DEBUG: Audio-Dialog geöffnet für ${point.name}")
    }

    fun closeAudioDialog() {
        _showAudioDialog.value = false
        _selectedPointForAudio.value = null
        println("DEBUG: Audio-Dialog geschlossen")
    }

    fun saveAudioToPoint(audioUri: String) {
        println("DEBUG: saveAudioToPoint aufgerufen mit URI: $audioUri")
        val selectedPoint = _selectedPointForAudio.value
        println("DEBUG: selectedPoint: ${selectedPoint?.name}")

        if (selectedPoint != null) {
            // Kopiere Audio in App-Speicher
            val localUri = copyAudioToAppStorage(audioUri)

            if (localUri == null) {
                _message.value = "Fehler beim Kopieren der Audio-Datei"
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _message.value = ""
                }
                return
            }

            val currentPoints = _currentSessionPoints.value.toMutableList()
            val pointIndex = currentPoints.indexOfFirst { it.name == selectedPoint.name }

            if (pointIndex != -1) {
                val updatedPoint = currentPoints[pointIndex].copy(audioUri = localUri)
                currentPoints[pointIndex] = updatedPoint
                _currentSessionPoints.value = currentPoints
                println("DEBUG: Audio zugeordnet zu ${selectedPoint.name} - Lokale URI: $localUri")
                _message.value = "Audio für ${selectedPoint.name} zugeordnet"

                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _message.value = ""
                }
            }
        } else {
            println("DEBUG: FEHLER - selectedPoint ist NULL!")
        }
        closeAudioDialog()
    }
    fun saveAudioToPointById(audioUri: String, pointName: String) {
        println("DEBUG: saveAudioToPointById aufgerufen - URI: $audioUri, Name: $pointName")

        // Kopiere Audio in App-Speicher
        val localUri = copyAudioToAppStorage(audioUri)

        if (localUri == null) {
            println("DEBUG: FEHLER beim Kopieren der Audio-Datei")
            return
        }

        val currentPoints = _currentSessionPoints.value.toMutableList()
        val pointIndex = currentPoints.indexOfFirst { it.name == pointName }

        println("DEBUG: Gefundener Index: $pointIndex, Array-Größe: ${currentPoints.size}")

        if (pointIndex != -1) {
            val updatedPoint = currentPoints[pointIndex].copy(audioUri = localUri)
            currentPoints[pointIndex] = updatedPoint
            _currentSessionPoints.value = currentPoints
            println("DEBUG: Audio erfolgreich zugeordnet zu ${updatedPoint.name} - Lokale URI: $localUri")
            _message.value = "Audio für ${updatedPoint.name} zugeordnet"

            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _message.value = ""
            }
        } else {
            println("DEBUG: FEHLER - Punkt $pointName nicht gefunden!")
        }
    }
    fun removeAudioFromPoint() {
        val selectedPoint = _selectedPointForAudio.value

        if (selectedPoint != null) {
            val currentPoints = _currentSessionPoints.value.toMutableList()
            val pointIndex = currentPoints.indexOfFirst { it.name == selectedPoint.name }

            if (pointIndex != -1) {
                val updatedPoint = currentPoints[pointIndex].copy(audioUri = null)
                currentPoints[pointIndex] = updatedPoint
                _currentSessionPoints.value = currentPoints
                _message.value = "Audio für ${selectedPoint.name} entfernt"

                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _message.value = ""
                }
            }
        }
        closeAudioDialog()
    }

    fun openRecorderDialog() {
        // Punktname speichern BEVOR Audio-Dialog geschlossen wird
        _currentRecordingPointName.value = _selectedPointForAudio.value?.name
        println("DEBUG: openRecorderDialog - speichere Punktname: ${_currentRecordingPointName.value}")
        _showRecorderDialog.value = true
        println("DEBUG: Recorder-Dialog geöffnet")
    }

    fun closeRecorderDialog() {
        _showRecorderDialog.value = false
        _isRecording.value = false
        _recordingDuration.value = 0
        println("DEBUG: Recorder-Dialog geschlossen")
    }

    fun startRecording() {
        _isRecording.value = true
        _recordingDuration.value = 0
        println("DEBUG: Aufnahme gestartet")
    }

    fun stopRecording() {
        _isRecording.value = false
        println("DEBUG: Aufnahme gestoppt")
    }

    fun updateRecordingDuration(seconds: Int) {
        _recordingDuration.value = seconds
    }

    fun toggleToEditMode() {
        if (!_isEditMode.value) {
            _isEditMode.value = true
            closeTextDialog()
            closeAudioDialog()
            closeRecorderDialog()
            _selectedHotspotText.value = ""
            _selectedHotspotName.value = ""
            stopAudio()

            // GEÄNDERT: Lade IMMER Editiermodus-Infobild beim Wechsel
            loadInfoImage(true)

            _message.value = "Editiermodus aktiviert"

            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _message.value = ""
            }
        }
    }

    fun toggleToPracticeMode() {
        if (_isEditMode.value) {
            _isEditMode.value = false
            closeTextDialog()
            closeAudioDialog()
            closeRecorderDialog()
            _selectedHotspotText.value = ""
            _selectedHotspotName.value = ""

            // GEÄNDERT: Lade IMMER Übungsmodus-Infobild beim Wechsel
            loadInfoImage(false)

            _message.value = "Übungsmodus aktiviert"

            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _message.value = ""
            }
        }
    }

    fun showHotspotText(point: DrawPoint) {
        if (!_isEditMode.value) {  // <-- Diese Bedingung blockiert im Vertiefungsmodus!
            _selectedHotspotName.value = point.name
            if (point.text != null && point.text.isNotEmpty()) {
                _selectedHotspotText.value = point.text
            } else {
                _selectedHotspotText.value = "Kein Text verfügbar."
            }
        }
    }

    fun playAudio(audioUri: String) {
        _currentAudioUri.value = audioUri
        _isPlayingAudio.value = true
    }

    fun stopAudio() {
        _isPlayingAudio.value = false
        _currentAudioUri.value = null
    }

    fun clearHotspotText() {
        _selectedHotspotText.value = ""
        _selectedHotspotName.value = ""
        stopAudio()
    }

    // Vertiefungsmodus Funktionen
    fun toggleDeepLearningButtons() {
        _showDeepLearningButtons.value = !_showDeepLearningButtons.value
    }

    fun hideDeepLearningButtons() {
        _showDeepLearningButtons.value = false
    }

    fun openTaskCountDialog(type: String) {
        _selectedDeepLearningType.value = type
        _showTaskCountDialog.value = true
    }

    fun closeTaskCountDialog() {
        _showTaskCountDialog.value = false
        _selectedDeepLearningType.value = null
    }

    fun startDeepLearning(taskCount: Int, type: String) {
        println("DEBUG: startDeepLearning ANFANG - taskCount=$taskCount, type=$type")

        val isInfoImage = _backgroundImageUri.value?.toString()?.contains("info_") == true

        if (_currentSessionPoints.value.isEmpty() || isInfoImage) {
            println("DEBUG: VORZEITIGER EXIT - isEmpty oder isInfoImage")
            _message.value = "Bitte laden Sie zuerst ein Bild mit Hotspots!"
            closeTaskCountDialog()

            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                _message.value = ""
            }
            return
        }

        println("DEBUG: Setze isDeepLearningMode = true")
        _isDeepLearningMode.value = true
        println("DEBUG: isDeepLearningMode ist jetzt: ${_isDeepLearningMode.value}")

        _showDeepLearningButtons.value = false
        _deepLearningType.value = type
        _deepLearningTasksTotal.value = taskCount
        _deepLearningTasksCurrent.value = 0
        _deepLearningCorrect.value = 0
        _deepLearningWrong.value = 0
        _deepLearningFeedback.value = ""
        closeTaskCountDialog()

        nextDeepLearningChallenge()
    }

    fun nextDeepLearningChallenge() {
        println("DEBUG: ========== nextDeepLearningChallenge START ==========")

        val currentPoints = _currentSessionPoints.value

        println("DEBUG: nextDeepLearningChallenge - currentPoints: ${currentPoints.size}")
        println("DEBUG: deepLearningType: ${_deepLearningType.value}")

        val eligiblePoints = when (_deepLearningType.value) {
            "text" -> currentPoints.filter { it.text != null && it.text.isNotEmpty() }
            "audio" -> currentPoints.filter { it.audioUri != null }
            "both" -> currentPoints.filter {
                it.text != null && it.text.isNotEmpty() && it.audioUri != null
            }
            else -> emptyList()
        }

        println("DEBUG: eligiblePoints: ${eligiblePoints.size}")
        eligiblePoints.forEach { point ->
            println("DEBUG: Punkt ${point.name} - Text: ${point.text != null}, Audio: ${point.audioUri != null}, AudioUri: ${point.audioUri}")
        }

        if (eligiblePoints.isEmpty()) {
            _deepLearningFeedback.value = "Keine passenden Hotspots gefunden!"
            exitDeepLearningMode()
            return
        }

        val randomPoint = eligiblePoints.random()
        _currentChallengePoint.value = randomPoint
        _deepLearningTasksCurrent.value += 1

        println("DEBUG: Ausgewählter Punkt: ${randomPoint.name}, Text: ${randomPoint.text}, Audio: ${randomPoint.audioUri}")

        when (_deepLearningType.value) {
            "text" -> {
                _selectedHotspotText.value = randomPoint.text ?: ""
                _currentAudioUri.value = null
                println("DEBUG: Text-Modus - selectedHotspotText gesetzt: ${_selectedHotspotText.value}")
            }
            "audio" -> {
                _selectedHotspotText.value = ""
                _currentAudioUri.value = randomPoint.audioUri
                println("DEBUG: Audio-Modus - _currentAudioUri.value gesetzt auf: ${_currentAudioUri.value}")
            }
            "both" -> {
                _selectedHotspotText.value = randomPoint.text ?: ""
                _currentAudioUri.value = randomPoint.audioUri
                println("DEBUG: Both-Modus - Text + Audio gesetzt")
            }
        }    }

    fun checkDeepLearningAnswer(clickedPoint: DrawPoint) {
        println("DEBUG: checkDeepLearningAnswer aufgerufen - geklickter Punkt: ${clickedPoint.name}")

        val challengePoint = _currentChallengePoint.value
        println("DEBUG: challengePoint: ${challengePoint?.name}")

        if (challengePoint == null) {
            println("DEBUG: FEHLER - challengePoint ist NULL!")
            return
        }

        if (clickedPoint.name == challengePoint.name) {
            _deepLearningCorrect.value += 1
            _deepLearningFeedback.value = "✓ Richtig!"
            vibrateSuccess()  // HIER HINZUFÜGEN
            println("DEBUG: RICHTIG! correct=${_deepLearningCorrect.value}")
        } else {
            _deepLearningWrong.value += 1
            _deepLearningFeedback.value = "✗ Falsch! Richtig wäre: ${challengePoint.name}"
            println("DEBUG: FALSCH! wrong=${_deepLearningWrong.value}")
        }

        println("DEBUG: Nach 2 Sekunden - tasksCurrent=${_deepLearningTasksCurrent.value}, tasksTotal=${_deepLearningTasksTotal.value}")

        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _deepLearningFeedback.value = ""

            if (_deepLearningTasksCurrent.value >= _deepLearningTasksTotal.value) {
                println("DEBUG: Alle Aufgaben fertig - exitDeepLearningMode")
                exitDeepLearningMode()
            } else {
                println("DEBUG: Rufe nextDeepLearningChallenge auf")
                nextDeepLearningChallenge()
            }
        }
    }
    fun exitDeepLearningMode() {
        _showDeepLearningResult.value = true
        _isDeepLearningMode.value = false
        _currentChallengePoint.value = null
        _selectedHotspotText.value = ""
        stopAudio()
    }

    fun backToOverview() {
        _showDeepLearningResult.value = false
        _deepLearningType.value = null
        _deepLearningTasksTotal.value = 0
        _deepLearningTasksCurrent.value = 0
        _deepLearningCorrect.value = 0
        _deepLearningWrong.value = 0
    }

    private fun copyAudioToAppStorage(sourceUri: String): String? {
        return try {
            val sourceUriParsed = Uri.parse(sourceUri)

            // Erstelle Audio-Ordner falls nicht vorhanden
            val audioDir = File(getApplication<Application>().filesDir, "audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
                println("DEBUG: Audio-Ordner erstellt: ${audioDir.absolutePath}")
            }

            // Generiere eindeutigen Dateinamen basierend auf Hash
            val hash = sourceUri.hashCode().toString()
            val fileName = "hotklick_$hash.m4a"
            val destFile = File(audioDir, fileName)

            // Prüfe ob Datei bereits existiert
            if (destFile.exists()) {
                println("DEBUG: Audio existiert bereits: ${destFile.absolutePath}")
                return Uri.fromFile(destFile).toString()
            }

            // Kopiere Datei
            println("DEBUG: Kopiere Audio von $sourceUri nach ${destFile.absolutePath}")
            getApplication<Application>().contentResolver.openInputStream(sourceUriParsed)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            println("DEBUG: Audio erfolgreich kopiert")
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            println("DEBUG: Fehler beim Kopieren: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun vibrateSuccess() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getApplication<Application>().getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getApplication<Application>().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }

            println("DEBUG: Vibration ausgeführt")
        } catch (e: Exception) {
            println("DEBUG: Vibration fehlgeschlagen: ${e.message}")
        }
    }

    // Übungsname Funktionen
    fun openExerciseNameDialog() {
        _exerciseNameInput.value = _exerciseName.value
        _showExerciseNameDialog.value = true
    }

    fun closeExerciseNameDialog() {
        _showExerciseNameDialog.value = false
    }

    fun updateExerciseNameInput(name: String) {
        _exerciseNameInput.value = name
    }

    fun saveExerciseName() {
        val newName = _exerciseNameInput.value.trim()
        _exerciseName.value = newName

        // Speichere Namen in allen Punkten des aktuellen Bildes
        val currentPoints = _currentSessionPoints.value.toMutableList()
        val updatedPoints = currentPoints.map { point ->
            point.copy(exerciseName = newName)
        }
        _currentSessionPoints.value = updatedPoints

        closeExerciseNameDialog()
        _message.value = "Übungsname gespeichert"

        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _message.value = ""
        }
    }
}