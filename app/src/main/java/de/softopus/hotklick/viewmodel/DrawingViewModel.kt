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




    private val imageRadiusMap = mutableMapOf<String?, Float>()

    // Als StateFlow definieren (bei den anderen StateFlows)
    private val _currentRecordingPointName = MutableStateFlow<String?>(null)
    val currentRecordingPointName: StateFlow<String?> = _currentRecordingPointName.asStateFlow()


    // Vertiefungsmodus States
    private val _showDeepLearningButtons = MutableStateFlow(false)
    val showDeepLearningButtons: StateFlow<Boolean> = _showDeepLearningButtons.asStateFlow()



    init {
        val database = PointDatabase.getDatabase(application)
        repository = PointRepository(database.pointDao())

        // Lade Standard-Infobild beim Start (Übungsmodus)
        val infoImageUri = Uri.parse("android.resource://${application.packageName}/drawable/info_practice")
        _backgroundImageUri.value = infoImageUri

        println("DEBUG: ViewModel initialisiert mit Infobild")
    }


    fun loadInfoImage(isEditMode: Boolean) {
        val imageName = if (isEditMode) "info_edit" else "info_practice"
        val infoImageUri = Uri.parse("android.resource://${getApplication<Application>().packageName}/drawable/$imageName")
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
                    _message.value = "Bild geladen - ${savedPoints.size} Punkte"
                } else {
                    _currentSessionPoints.value = emptyList()
                    _message.value = "Neues Bild geladen"
                }
                val savedRadius = imageRadiusMap[newImageUri] ?: 50f
                _pointRadius.value = savedRadius
            } else {
                _currentSessionPoints.value = emptyList()
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
            val currentPoints = _currentSessionPoints.value.toMutableList()
            val pointIndex = currentPoints.indexOfFirst { it.name == selectedPoint.name }

            if (pointIndex != -1) {
                val updatedPoint = currentPoints[pointIndex].copy(audioUri = audioUri)
                currentPoints[pointIndex] = updatedPoint
                _currentSessionPoints.value = currentPoints
                println("DEBUG: Audio zugeordnet zu ${selectedPoint.name}")
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

        val currentPoints = _currentSessionPoints.value.toMutableList()
        val pointIndex = currentPoints.indexOfFirst { it.name == pointName }

        println("DEBUG: Gefundener Index: $pointIndex, Array-Größe: ${currentPoints.size}")

        if (pointIndex != -1) {
            val updatedPoint = currentPoints[pointIndex].copy(audioUri = audioUri)
            currentPoints[pointIndex] = updatedPoint
            _currentSessionPoints.value = currentPoints
            println("DEBUG: Audio erfolgreich zugeordnet zu ${updatedPoint.name}")
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
        if (!_isEditMode.value) {
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
}