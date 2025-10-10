// DrawingScreen.kt
package de.softopus.hotklick

import android.content.ContentValues
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.net.Uri as AndroidUri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.viewmodel.DrawingViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    viewModel: DrawingViewModel = viewModel()
) {
    val points by viewModel.points.collectAsState()
    val message by viewModel.message.collectAsState()
    val backgroundImageUri by viewModel.backgroundImageUri.collectAsState()
    val pointRadius by viewModel.pointRadius.collectAsState()
    val showTextDialog by viewModel.showTextDialog.collectAsState()
    val selectedPointForText by viewModel.selectedPointForText.collectAsState()
    val textInput by viewModel.textInput.collectAsState()
    val showAudioDialog by viewModel.showAudioDialog.collectAsState()
    val selectedPointForAudio by viewModel.selectedPointForAudio.collectAsState()
    val showRecorderDialog by viewModel.showRecorderDialog.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val currentRecordingPointName by viewModel.currentRecordingPointName.collectAsState()  // NEU
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedHotspotText by viewModel.selectedHotspotText.collectAsState()

    LaunchedEffect(selectedHotspotText) {
        println("DEBUG DrawingScreen: selectedHotspotText = '$selectedHotspotText'")
    }
    val selectedHotspotName by viewModel.selectedHotspotName.collectAsState()


// Audio-Wiedergabe für Vertiefungsmodus
    val showDeepLearningButtons by viewModel.showDeepLearningButtons.collectAsState()  // NEU
    val isDeepLearningMode by viewModel.isDeepLearningMode.collectAsState()
    val showTaskCountDialog by viewModel.showTaskCountDialog.collectAsState()
    val selectedDeepLearningType by viewModel.selectedDeepLearningType.collectAsState()
    val deepLearningTasksCurrent by viewModel.deepLearningTasksCurrent.collectAsState()
    val deepLearningTasksTotal by viewModel.deepLearningTasksTotal.collectAsState()
    val deepLearningCorrect by viewModel.deepLearningCorrect.collectAsState()
    val deepLearningWrong by viewModel.deepLearningWrong.collectAsState()
    val deepLearningFeedback by viewModel.deepLearningFeedback.collectAsState()
    val showDeepLearningResult by viewModel.showDeepLearningResult.collectAsState()
    val density = LocalDensity.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var recordingFile: File? by remember { mutableStateOf(null) }

    val currentAudio by viewModel.currentAudioUri.collectAsState()

    LaunchedEffect(currentAudio, isDeepLearningMode, deepLearningTasksCurrent) {  // Counter hinzufügen!
        println("DEBUG LaunchedEffect: currentAudio=$currentAudio, isDeepLearningMode=$isDeepLearningMode, task=$deepLearningTasksCurrent")

        if (isDeepLearningMode && currentAudio != null) {
            println("DEBUG: Bedingung erfüllt - starte Audio")
            try {
                println("DEBUG: Versuche Audio abzuspielen: $currentAudio")

                mediaPlayer?.apply {
                    if (isPlaying()) stop()
                    reset()
                    release()
                }

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(currentAudio))
                    prepare()
                    start()
                    isPlaying = true

                    setOnCompletionListener {
                        isPlaying = false
                        release()
                        mediaPlayer = null
                    }
                }

                println("DEBUG: Audio erfolgreich gestartet")
            } catch (e: Exception) {
                println("DEBUG: Fehler beim Audio abspielen: ${e.message}")
                e.printStackTrace()
            }
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                }
                release()
            }
            mediaRecorder = null
        }
    }

    val editImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setBackgroundImage(uri)
    }

    val practiceImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setBackgroundImage(uri)
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveAudioToPoint(it.toString())
        }
    }

    val canvasWidthDp = 1024.dp
    val canvasHeightDp = 600.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ÜBUNGSNAME
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (backgroundImageUri != null && !backgroundImageUri.toString().contains("info_")) {
                    val fileName = getFileNameFromUri(context, backgroundImageUri.toString())
                    if (fileName.isNotEmpty()) {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Keine Übung geladen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.size(canvasWidthDp, canvasHeightDp),            colors = CardDefaults.cardColors(containerColor = Color.White),            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            println("DEBUG DrawingScreen VOR Canvas: isDeepLearningMode=$isDeepLearningMode")

            HotspotCanvas(
                backgroundImageUri = backgroundImageUri,
                points = points,
                pointRadius = pointRadius,
                isEditMode = isEditMode,
                isDeepLearningMode = isDeepLearningMode,  // NEU
                viewModel = viewModel,
                mediaPlayer = mediaPlayer,
                isPlaying = isPlaying,
                onMediaPlayerChange = { mediaPlayer = it },
                onIsPlayingChange = { isPlaying = it },
                editImagePickerLauncher = editImagePickerLauncher,
                practiceImagePickerLauncher = practiceImagePickerLauncher,
                context = context
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ModeToggleButton(
                isEditMode = isEditMode,
                showDeepLearningButtons = showDeepLearningButtons,
                isDeepLearningMode = isDeepLearningMode,  // NEU
                tasksCurrent = deepLearningTasksCurrent,  // NEU
                tasksTotal = deepLearningTasksTotal,  // NEU
                correct = deepLearningCorrect,  // NEU
                viewModel = viewModel
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

// Vertiefungsmodus-Buttons

        if (showDeepLearningButtons) {
            DeepLearningButtons(
                onTextClick = { viewModel.openTaskCountDialog("text") },
                onAudioClick = { viewModel.openTaskCountDialog("audio") },
                onBothClick = { viewModel.openTaskCountDialog("both") }
            )
        }

        if (!isEditMode) {
            PracticeModeHints()
            Spacer(modifier = Modifier.height(8.dp))
        }

// Ergebnis-Anzeige
        if (showDeepLearningResult) {
            DeepLearningResult(
                tasksTotal = deepLearningTasksTotal,
                correct = deepLearningCorrect,
                wrong = deepLearningWrong,
                onBack = { viewModel.backToOverview() }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isEditMode) {
            EditModeControls(
                backgroundImageUri = backgroundImageUri,
                pointRadius = pointRadius,
                editImagePickerLauncher = editImagePickerLauncher,
                viewModel = viewModel
            )
        }

        HotspotStats(points = points)

        if (!isEditMode) {
            HotspotTextDisplay(
                selectedHotspotText = selectedHotspotText,
                uriHandler = uriHandler,
                viewModel = viewModel
            )
        }
// ========== DIALOGE ==========

        TextDialog(
            showDialog = isEditMode && showTextDialog,
            selectedPoint = selectedPointForText,
            textInput = textInput,
            onTextChange = { viewModel.updateTextInput(it) },
            onSave = { viewModel.saveTextToPoint() },
            onDismiss = { viewModel.closeTextDialog() }
        )

        AudioDialog(
            showDialog = isEditMode && showAudioDialog,
            selectedPoint = selectedPointForAudio,
            onLoadAudio = { audioPickerLauncher.launch("audio/*") },
            onRecordAudio = {
                viewModel.openRecorderDialog()
                viewModel.closeAudioDialog()
            },
            onRemoveAudio = { viewModel.removeAudioFromPoint() },
            onDismiss = { viewModel.closeAudioDialog() }
        )

        RecorderDialog(
            showDialog = showRecorderDialog,
            isRecording = isRecording,
            recordingDuration = recordingDuration,
            context = context,
            mediaRecorder = mediaRecorder,
            recordingFile = recordingFile,
            onMediaRecorderChange = { mediaRecorder = it },
            onRecordingFileChange = { recordingFile = it },
            viewModel = viewModel,
            selectedPointName = currentRecordingPointName
        )

        TaskCountDialog(
            showDialog = showTaskCountDialog,
            selectedType = selectedDeepLearningType,
            onStart = { count, type -> viewModel.startDeepLearning(count, type) },
            onDismiss = { viewModel.closeTaskCountDialog() }
        )
    }
}

private fun getFileNameFromUri(context: android.content.Context, uriString: String): String {
    return try {
        val uri = Uri.parse(uriString)
        println("DEBUG getFileName: URI = $uriString")
        println("DEBUG getFileName: Scheme = ${uri.scheme}")
        println("DEBUG getFileName: Authority = ${uri.authority}")

        if (uri.scheme == "content") {
            var displayName = ""

            println("DEBUG getFileName: Starte ContentResolver Query")
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                println("DEBUG getFileName: Cursor count = ${cursor.count}")
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    println("DEBUG getFileName: nameIndex = $nameIndex")
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                        println("DEBUG getFileName: DISPLAY_NAME = '$displayName'")
                    }
                }
            }

            if (displayName.isNotEmpty()) {
                val nameWithoutExtension = displayName.substringBeforeLast(".")
                println("DEBUG getFileName: Ergebnis = '$nameWithoutExtension'")
                return nameWithoutExtension
            }

            println("DEBUG getFileName: displayName ist leer")
            return "Unbekannte Übung"
        } else if (uri.scheme == "file") {
            val path = uri.path ?: ""
            val fileName = path.substringAfterLast("/")
            return fileName.substringBeforeLast(".")
        } else {
            println("DEBUG getFileName: Unbekanntes Scheme: ${uri.scheme}")
            return ""
        }
    } catch (e: Exception) {
        println("DEBUG getFileName: Exception = ${e.message}")
        e.printStackTrace()
        return "Fehler"
    }
}


