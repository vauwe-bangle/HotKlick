// MainActivity.kt - Hauptaktivität mit Navigation
package de.softopus.hotklick

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import de.softopus.hotklick.data.ExerciseManager
import de.softopus.hotklick.ui.theme.DrawPointTheme
import de.softopus.hotklick.viewmodel.DrawingViewModel
import kotlinx.coroutines.launch

import androidx.activity.compose.rememberLauncherForActivityResult

class MainActivity : ComponentActivity() {

    private lateinit var exerciseManager: ExerciseManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Optional: Zeige Info-Dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exerciseManager = ExerciseManager(this)

        // Prüfe Audio-Permission
        checkAudioPermission()

        setContent {
            DrawPointTheme {
                HotKlickApp(exerciseManager = exerciseManager)
            }
        }
    }

    private fun checkAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

/**
 * Hauptkomponente der App mit Navigation
 */
@Composable
fun HotKlickApp(
    exerciseManager: ExerciseManager,
    viewModel: DrawingViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var exercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importExercise(it)
            currentScreen = Screen.Drawing
        }
    }

    // Lade Übungen beim Start
    LaunchedEffect(Unit) {
        exercises = exerciseManager.loadExercises()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    exercises = exercises,
                    onExerciseClick = { exercise: Exercise ->
                        // Lade Übung ins ViewModel
                        exercise.imageUri?.let { uri: Uri ->
                            coroutineScope.launch {
                                viewModel.setBackgroundImage(uri)
                                // Warte bis Punkte geladen sind, dann setze Namen
                                kotlinx.coroutines.delay(100)
                                viewModel.updateExerciseNameInput(exercise.name)
                                println("DEBUG: Übung '${exercise.name}' geladen")
                            }
                        }
                        currentScreen = Screen.Drawing
                    },
                    onNewExerciseClick = { imageUri: Uri?, name: String ->
                        // Erstelle neue Übung
                        coroutineScope.launch {
                            val newExercise = exerciseManager.saveExercise(
                                name = name,
                                imageUri = imageUri
                            )
                            exercises = exerciseManager.loadExercises()

                            // Öffne Drawing Screen
                            imageUri?.let { uri: Uri ->
                                viewModel.setBackgroundImage(uri)
                                viewModel.updateExerciseNameInput(name)
                                viewModel.saveExerciseName()  // Setze _exerciseName
                            }
                            currentScreen = Screen.Drawing
                        }
                    },
                    onDeleteClick = { exercise: Exercise ->
                        coroutineScope.launch {
                            exerciseManager.deleteExercise(exercise.id)
                            exercises = exerciseManager.loadExercises()
                        }
                    },
                    onImportClick = {
                        importLauncher.launch("application/zip")
                    }
                )
            }
            is Screen.Drawing -> {
                // Rufe direkt die DrawingScreen aus DrawingScreen.kt auf
                de.softopus.hotklick.DrawingScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        // Speichere Übung vor dem Zurückgehen
                        coroutineScope.launch {
                            val currentImageUri = viewModel.backgroundImageUri.value
                            val currentName = viewModel.exerciseNameInput.value.ifEmpty { "Übung" }
                            val pointCount = viewModel.points.value.size

                            if (currentImageUri != null) {
                                // Speichere Übung und erhalte persistente URI
                                val savedExercise = exerciseManager.saveExercise(
                                    name = currentName,
                                    imageUri = currentImageUri,
                                    hotspotCount = pointCount
                                )

                                // Update imageUri im ViewModel auf persistente URI
                                if (savedExercise.imageUri != currentImageUri) {
                                    viewModel.setBackgroundImage(savedExercise.imageUri)
                                }

                                // Jetzt Hotspots in DB speichern
                                viewModel.saveDataAndClearImage()
                            }

                            // Aktualisiere Übungsliste
                            exercises = exerciseManager.loadExercises()
                        }
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}

/**
 * Screen-Navigation
 */
sealed class Screen {
    object Home : Screen()
    object Drawing : Screen()
}