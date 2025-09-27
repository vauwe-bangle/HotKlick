// viewmodel/DrawingViewModel.kt
package de.softopus.drawpoint.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.softopus.drawpoint.data.DrawPoint
import de.softopus.drawpoint.data.PointDatabase
import de.softopus.drawpoint.repository.PointRepository
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

    // Modus-Verwaltung (Edit/Practice)
    private val _isEditMode = MutableStateFlow(true)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // Übungsmodus - Angeklickter Hotspot Text
    private val _selectedHotspotText = MutableStateFlow("")
    val selectedHotspotText: StateFlow<String> = _selectedHotspotText.asStateFlow()

    private val _selectedHotspotName = MutableStateFlow("")
    val selectedHotspotName: StateFlow<String> = _selectedHotspotName.asStateFlow()

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
            id = 0, // Temporäre ID für Array
            name = pointName,
            x = x,
            y = y,
            radius = _pointRadius.value,
            imageUri = _backgroundImageUri.value?.toString(),
            text = null // Noch kein Text zugeordnet
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

        // Punkt aus Array entfernen (nach Name suchen, da ID = 0)
        val pointToRemove = currentPoints.find { it.name == pointName }
        if (pointToRemove != null) {
            currentPoints.remove(pointToRemove)

            // Array neu nummerieren: P1, P2, P3...
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
            // Aktuellen Radius für das bisherige Bild speichern
            val previousImageUri = _backgroundImageUri.value?.toString()
            imageRadiusMap[previousImageUri] = _pointRadius.value

            _backgroundImageUri.value = uri
            val newImageUri = uri?.toString()

            if (uri != null) {
                // Neues Bild geladen - prüfen ob gespeicherte Punkte existieren
                val savedPoints = repository.getPointsForImageSync(newImageUri)

                if (savedPoints.isNotEmpty()) {
                    // Gespeicherte Punkte ins Array laden
                    _currentSessionPoints.value = savedPoints
                    _message.value = "Bild geladen - ${savedPoints.size} gespeicherte Punkte wiederhergestellt"
                    println("DEBUG: Bild $newImageUri geladen - ${savedPoints.size} Punkte aus DB ins Array geladen")
                } else {
                    // Neues Bild ohne gespeicherte Punkte
                    _currentSessionPoints.value = emptyList()
                    _message.value = "Neues Bild geladen - Array ist leer"
                    println("DEBUG: Neues Bild $newImageUri geladen - Array geleert")
                }

                // Bildspezifischen Radius wiederherstellen
                val savedRadius = imageRadiusMap[newImageUri] ?: 50f
                _pointRadius.value = savedRadius

            } else {
                // Kein Bild - Array leeren
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
                    // 1. Alte Punkte für dieses Bild löschen
                    repository.deleteAllPointsForImage(currentImageUri)

                    // 2. Array-Punkte in Datenbank speichern
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

            // 3. Radius für dieses Bild speichern
            imageRadiusMap[currentImageUri] = _pointRadius.value

            // 4. Array leeren und Bild entfernen
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

            // Radius für aktuelles Bild speichern
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

            // Radius für aktuelles Bild speichern
            val currentImageUri = _backgroundImageUri.value?.toString()
            imageRadiusMap[currentImageUri] = newRadius

            println("DEBUG: Radius verringert auf $newRadius für aktuelle Sitzung")
        }
    }

    // Text-Dialog-Funktionen
    fun openTextDialog(point: DrawPoint) {
        _selectedPointForText.value = point
        _textInput.value = point.text ?: "" // Bestehenden Text laden oder leer
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
                // Punkt im Array mit Text aktualisieren
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

    // Modus-Umschaltung (Long-Click erforderlich für Edit-Modus)
    fun toggleToEditMode() {
        if (!_isEditMode.value) {
            _isEditMode.value = true

            // Bei Umschaltung zu Edit alle Dialog-States zurücksetzen
            closeTextDialog()
            _selectedHotspotText.value = ""
            _selectedHotspotName.value = ""

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

            // Bei Umschaltung zu Practice alle Dialog-States zurücksetzen
            closeTextDialog()
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

    // Hotspot-Text löschen
    fun clearHotspotText() {
        _selectedHotspotText.value = ""
        _selectedHotspotName.value = ""
    }
}