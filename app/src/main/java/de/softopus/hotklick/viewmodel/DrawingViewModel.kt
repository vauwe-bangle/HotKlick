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

class DrawingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PointRepository

    // Arbeits-Array für aktuelle Sitzung (nicht in DB!)
    private val _currentSessionPoints = MutableStateFlow<List<DrawPoint>>(emptyList())
    val points: StateFlow<List<DrawPoint>> = _currentSessionPoints.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _backgroundImageUri = MutableStateFlow<Uri?>(null)
    val backgroundImageUri: StateFlow<Uri?> = _backgroundImageUri.asStateFlow()

    private val _pointRadius = MutableStateFlow(50f)
    val pointRadius: StateFlow<Float> = _pointRadius.asStateFlow()

    // Text-Eingabe Dialog State
    private val _showTextDialog = MutableStateFlow(false)
    val showTextDialog: StateFlow<Boolean> = _showTextDialog.asStateFlow()

    private val _selectedPointForText = MutableStateFlow<DrawPoint?>(null)
    val selectedPointForText: StateFlow<DrawPoint?> = _selectedPointForText.asStateFlow()

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    // Audio-Dialog State
    private val _showAudioDialog = MutableStateFlow(false)
    val showAudioDialog: StateFlow<Boolean> = _showAudioDialog.asStateFlow()

    private val _selectedPointForAudio = MutableStateFlow<DrawPoint?>(null)
    val selectedPointForAudio: StateFlow<DrawPoint?> = _selectedPointForAudio.asStateFlow()

    // Audio-Recorder State
    private val _showRecorderDialog = MutableStateFlow(false)
    val showRecorderDialog: StateFlow<Boolean> = _showRecorderDialog.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    // Modus-Verwaltung (Edit/Practice)
    private val _isEditMode = MutableStateFlow(true)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // Übungsmodus - Angeklickter Hotspot Text
    private val _selectedHotspotText = MutableStateFlow("")
    val selectedHotspotText: StateFlow<String> = _selectedHotspotText.asStateFlow()

    private val _selectedHotspotName = MutableStateFlow("")
    val selectedHotspotName: StateFlow<String> = _selectedHotspotName.asStateFlow()

    // Übungsmodus - Audio-Wiedergabe
    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private val _currentAudioUri = MutableStateFlow<String?>(null)
    val currentAudioUri: StateFlow<String?> = _currentAudioUri.asStateFlow()

    // Bildspezifische Radius-Speicherung
    private val imageRadiusMap = mutableMapOf<String?, Float>()

    init {
        val database = PointDatabase.getDatabase(application)
        repository = PointRepository(database.pointDao())

        println("DEBUG: ViewModel initialisiert - Arbeits-Array ist leer")
    }

    fun addPoint(x: Float, y: Float) {
        val currentPoints = _currentSessionPoints.value.toMutableList()

        // Nächste verfügbare Punktnummer im aktuellen Array
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

        println("DEBUG: Punkt $pointName im Array erstellt - Radius: ${_pointRadius.value} - Array-Größe: ${currentPoints.size}")

        _message.value = "Punkt $pointName erstellt (${x.toInt()}, ${y.toInt()}) - ${_pointRadius.value.toInt()}px [im Arbeitsspeicher]"

        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _message.value = ""
        }
    }

    fun deletePoint(pointId: Int, pointName: String) {
        val currentPoints = _currentSessionPoints.value.toMutableList()

        val pointToRemove = currentPoints.find { it.name == pointName }
        if (pointToRemove != null) {
            currentPoints.remove(pointToRemove)

            val renumberedPoints = currentPoints.mapIndexed { index, point ->
                point.copy(name = "P${index + 1}")
            }

            _currentSessionPoints.value = renumberedPoints

            println("DEBUG: Punkt $pointName aus Array entfernt und neu nummeriert - Array-Größe: ${renumberedPoints.size}")

            _message.value = "Punkt $pointName gelöscht und Array neu nummeriert"
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
                    _message.value = "Bild geladen - ${savedPoints.size} gespeicherte Punkte wiederhergestellt"
                    println("DEBUG: Bild $newImageUri geladen - ${savedPoints.size} Punkte aus DB ins Array geladen")
                } else {
                    _currentSessionPoints.value = emptyList()
                    _message.value = "Neues Bild geladen - Array ist leer"
                    println("DEBUG: Neues Bild $newImageUri geladen - Array geleert")
                }

                val savedRadius = imageRadiusMap[newImageUri] ?: 50f
                _pointRadius.value = savedRadius

            } else {
                _currentSessionPoints.value = emptyList()
                _message.value = "Kein Bild - Array geleert"
                println("DEBUG: Kein Bild - Array geleert")
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

                    println("DEBUG: ${currentPoints.size} Punkte für Bild $currentImageUri in DB gespeichert")
                    _message.value = "${currentPoints.size} Punkte für Bild gespeichert"

                } catch (e: Exception) {
                    println("DEBUG: Fehler beim Speichern: ${e.message}")
                    _message.value = "Fehler beim Speichern: ${e.message}"
                }
            } else {
                _message.value = "Keine Daten zum Speichern"
                println("DEBUG: Keine Daten zum Speichern - Bild: $currentImageUri, Punkte: ${currentPoints.size}")
            }

            imageRadiusMap[currentImageUri] = _pointRadius.value

            _currentSessionPoints.value = emptyList()
            _backgroundImageUri.value = null

            println("DEBUG: Array geleert und Bild entfernt")

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

            println("DEBUG: Radius erhöht auf $newRadius für aktuelle Sitzung")
        }
    }

    fun decreasePointRadius() {
        val currentRadius = _pointRadius.value
        if (currentRadius > 10f) {
            val newRadius = currentRadius - 20f
            _pointRadius.value = newRadius

            val currentImageUri = _backgroundImageUri.value?.toString()
            imageRadiusMap[currentImageUri] = newRadius

            println("DEBUG: Radius verringert auf $newRadius für aktuelle Sitzung")
        }
    }

    // Text-Dialog-Funktionen
    fun openTextDialog(point: DrawPoint) {
        _selectedPointForText.value = point
        _textInput.value = point.text ?: ""
        _showTextDialog.value = true
        println("DEBUG: Text-Dialog für Punkt ${point.name} geöffnet - Aktueller Text: '${point.text}'")
    }

    fun closeTextDialog() {
        _showTextDialog.value = false
        _selectedPointForText.value = null
        _textInput.value = ""
        println("DEBUG: Text-Dialog geschlossen")
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

                println("DEBUG: Text für Punkt ${selectedPoint.name} gespeichert: '$inputText'")
                _message.value = "Text für ${selectedPoint.name} gespeichert"

                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _message.value = ""
                }
            }
        }

        closeTextDialog()
    }

    // Audio-Dialog-Funktionen
    fun openAudioDialog(point: DrawPoint) {
        _selectedPointForAudio.value = point
        _showAudioDialog.value = true
        println("DEBUG: Audio-Dialog für Punkt ${point.name} geöffnet - Aktuelles Audio: '${point.audioUri}'")
    }

    fun closeAudioDialog() {
        _showAudioDialog.value = false
        _selectedPointForAudio.value = null
        println("DEBUG: Audio-Dialog geschlossen")
    }

    fun saveAudioToPoint(audioUri: String) {
        val selectedPoint = _selectedPointForAudio.value

        if (selectedPoint != null) {
            val currentPoints = _currentSessionPoints.value.toMutableList()
            val pointIndex = currentPoints.indexOfFirst { it.name == selectedPoint.name }

            if (pointIndex != -1) {
                val updatedPoint = currentPoints[pointIndex].copy(
                    audioUri = audioUri
                )
                currentPoints[pointIndex] = updatedPoint
                _currentSessionPoints.value = currentPoints

                println("DEBUG: Audio für Punkt ${selectedPoint.name} gespeichert: '$audioUri'")
                _message.value = "Audio für ${selectedPoint.name} zugeordnet"

                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _message.value = ""
                }
            }
        }

        closeAudioDialog()
    }

    fun removeAudioFromPoint() {
        val selectedPoint = _selectedPointForAudio.value

        if (selectedPoint != null) {
            val currentPoints = _currentSessionPoints.value.toMutableList()
            val pointIndex = currentPoints.indexOfFirst { it.name == selectedPoint.name }

            if (pointIndex != -1) {
                val updatedPoint = currentPoints[pointIndex].copy(
                    audioUri = null
                )
                currentPoints[pointIndex] = updatedPoint
                _currentSessionPoints.value = currentPoints

                println("DEBUG: Audio für Punkt ${selectedPoint.name} entfernt")
                _message.value = "Audio für ${selectedPoint.name} entfernt"

                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _message.value = ""
                }
            }
        }

        closeAudioDialog()
    }

    // Audio-Recorder Funktionen
    fun openRecorderDialog() {
        _showAudioDialog.value = false  // Audio-Dialog schließen
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
        println("DEBUG: Aufnahme gestoppt - Dauer: ${_recordingDuration.value}s")
    }

    fun updateRecordingDuration(seconds: Int) {
        _recordingDuration.value = seconds
    }

    // Modus-Umschaltung
    fun toggleToEditMode() {
        if (!_isEditMode.value) {
            _isEditMode.value = true

            closeTextDialog()
            closeAudioDialog()
            closeRecorderDialog()
            _selectedHotspotText.value = ""
            _selectedHotspotName.value = ""
            stopAudio()

            _message.value = "Editiermodus aktiviert - Long-Click erkannt"

            println("DEBUG: Long-Click - Umschaltung zu Editiermodus")

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

            _message.value = "Übungsmodus aktiviert"

            println("DEBUG: Umschaltung zu Übungsmodus")

            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _message.value = ""
            }
        }
    }

    // Übungsmodus - Hotspot-Text anzeigen
    fun showHotspotText(point: DrawPoint) {
        if (!_isEditMode.value) {
            _selectedHotspotName.value = point.name

            if (point.text != null && point.text.isNotEmpty()) {
                _selectedHotspotText.value = point.text
                println("DEBUG: Übungsmodus - Text für ${point.name} angezeigt: '${point.text}'")
            } else {
                _selectedHotspotText.value = "Kein Text für diesen Hotspot verfügbar."
                println("DEBUG: Übungsmodus - Kein Text für ${point.name}")
            }
        }
    }

    // Übungsmodus - Audio abspielen
    fun playAudio(audioUri: String) {
        _currentAudioUri.value = audioUri
        _isPlayingAudio.value = true
        println("DEBUG: Audio-Wiedergabe gestartet: $audioUri")
    }

    fun stopAudio() {
        _isPlayingAudio.value = false
        _currentAudioUri.value = null
        println("DEBUG: Audio-Wiedergabe gestoppt")
    }

    fun clearHotspotText() {
        _selectedHotspotText.value = ""
        _selectedHotspotName.value = ""
        stopAudio()
    }
}