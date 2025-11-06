// DrawingScreen.kt - Modernisiert nach Web-App Design
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.viewmodel.DrawingViewModel
import de.softopus.hotklick.ui.theme.HotKlickColors
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
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    viewModel: DrawingViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null
) {
    // States
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
    val currentRecordingPointName by viewModel.currentRecordingPointName.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedHotspotText by viewModel.selectedHotspotText.collectAsState()
    val showDeepLearningResult by viewModel.showDeepLearningResult.collectAsState()
    val showExerciseNameDialog by viewModel.showExerciseNameDialog.collectAsState()
    val exerciseNameInput by viewModel.exerciseNameInput.collectAsState()
    val density = LocalDensity.current
    val selectedHotspotName by viewModel.selectedHotspotName.collectAsState()
    val showDeepLearningButtons by viewModel.showDeepLearningButtons.collectAsState()
    val isDeepLearningMode by viewModel.isDeepLearningMode.collectAsState()
    val showTaskCountDialog by viewModel.showTaskCountDialog.collectAsState()
    val selectedDeepLearningType by viewModel.selectedDeepLearningType.collectAsState()
    val deepLearningTasksCurrent by viewModel.deepLearningTasksCurrent.collectAsState()
    val deepLearningTasksTotal by viewModel.deepLearningTasksTotal.collectAsState()
    val deepLearningCorrect by viewModel.deepLearningCorrect.collectAsState()
    val deepLearningWrong by viewModel.deepLearningWrong.collectAsState()
    val deepLearningFeedback by viewModel.deepLearningFeedback.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var recordingFile: File? by remember { mutableStateOf(null) }
    val currentAudio by viewModel.currentAudioUri.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    // Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            println("DEBUG Export: Ziel-URI = $uri")
            viewModel.exportExercise(it)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            println("DEBUG Import: Quell-URI = $uri")
            viewModel.importExercise(it)
        }
    }

    // Audio-Launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            println("DEBUG AudioPicker: URI empfangen = $uri")
            viewModel.saveAudioToPoint(it.toString())
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openRecorderDialog()
        }
    }

    // Image-Launcher
    val editImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        println("DEBUG ImagePicker: URI empfangen = $uri")
        viewModel.setBackgroundImage(uri)
        println("DEBUG ImagePicker: setBackgroundImage aufgerufen")
    }

    val practiceImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        println("DEBUG PracticeImagePicker: URI empfangen = $uri")
        viewModel.setBackgroundImage(uri)
        println("DEBUG PracticeImagePicker: setBackgroundImage aufgerufen")
    }

    // Audio-Wiedergabe
    LaunchedEffect(currentAudio, isDeepLearningMode, deepLearningTasksCurrent) {
        println("DEBUG LaunchedEffect: currentAudio=$currentAudio, isDeepLearningMode=$isDeepLearningMode")
        if (isDeepLearningMode && currentAudio != null) {
            println("DEBUG: Bedingung erfüllt - starte Audio")
            try {
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

    // Canvas-Dimensionen
    val canvasWidthDp = 1024.dp / density.density
    val canvasHeightDp = 600.dp / density.density

    // === MAIN UI ===
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HotKlickColors.Light)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar (Web-App Style)
        ModernToolbar(
            isEditMode = isEditMode,
            showDeepLearningButtons = showDeepLearningButtons,
            isDeepLearningMode = isDeepLearningMode,
            tasksCurrent = deepLearningTasksCurrent,
            tasksTotal = deepLearningTasksTotal,
            correct = deepLearningCorrect,
            backgroundImageUri = backgroundImageUri?.toString(),
            exerciseNameInput = exerciseNameInput,
            pointRadius = pointRadius,
            viewModel = viewModel,
            onExerciseNameClick = { viewModel.openExerciseNameDialog() },
            onBackClick = onBackClick,
            onExportClick = {
                val exerciseName = exerciseNameInput.ifEmpty { "Übung" }
                val fileName = exerciseName.replace(" ", "_") + "_" +
                        System.currentTimeMillis() + ".zip"
                exportLauncher.launch(fileName)
            },
            onImportClick = {
                importLauncher.launch("application/zip")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Canvas Card
        Card(
            modifier = Modifier
                .width(canvasWidthDp)
                .height(canvasHeightDp),
            colors = CardDefaults.cardColors(
                containerColor = HotKlickColors.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            HotspotCanvas(
                backgroundImageUri = backgroundImageUri,
                points = points,
                pointRadius = pointRadius,
                isEditMode = isEditMode,
                isDeepLearningMode = isDeepLearningMode,
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

        // Practice Mode Hints
        if (!isEditMode && !isDeepLearningMode) {
            PracticeModeHints()
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Ergebnis-Anzeige
        if (showDeepLearningResult) {
            DeepLearningResult(
                tasksTotal = deepLearningTasksTotal,
                correct = deepLearningCorrect,
                wrong = deepLearningWrong,
                onBack = { viewModel.backToOverview() }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Hotspot Stats
        HotspotStats(points = points)

        // Hotspot Text Display (immer sichtbar)
        HotspotTextDisplay(
            selectedHotspotText = selectedHotspotText,
            uriHandler = uriHandler,
            viewModel = viewModel
        )

        // === DIALOGE ===
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
                viewModel.closeAudioDialog()
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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

        ExerciseNameDialog(
            showDialog = showExerciseNameDialog,
            exerciseNameInput = exerciseNameInput,
            onNameChange = { viewModel.updateExerciseNameInput(it) },
            onSave = { viewModel.saveExerciseName() },
            onDismiss = { viewModel.closeExerciseNameDialog() }
        )
    }
}

// === MODERNISIERTE TOOLBAR (Web-App Exakt) ===
@Composable
fun ModernToolbar(
    isEditMode: Boolean,
    showDeepLearningButtons: Boolean,
    isDeepLearningMode: Boolean,
    tasksCurrent: Int,
    tasksTotal: Int,
    correct: Int,
    backgroundImageUri: String?,
    exerciseNameInput: String,
    pointRadius: Float,
    viewModel: DrawingViewModel,
    onExerciseNameClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    onExportClick: (() -> Unit)? = null,
    onImportClick: (() -> Unit)? = null
) {
    // Haupt-Toolbar
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HotKlickColors.Light,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Toggle Buttons
            Row(
                modifier = Modifier
                    .background(HotKlickColors.White, RoundedCornerShape(4.dp))
                    .border(1.dp, HotKlickColors.Border, RoundedCornerShape(4.dp)),
                horizontalArrangement = Arrangement.Start
            ) {
                // Übungsmodus
                Button(
                    onClick = {
                        viewModel.toggleToPracticeMode()
                        viewModel.hideDeepLearningButtons()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isEditMode && !showDeepLearningButtons)
                            HotKlickColors.Primary else Color.Transparent,
                        contentColor = if (!isEditMode && !showDeepLearningButtons)
                            HotKlickColors.White else HotKlickColors.Dark
                    ),
                    shape = RoundedCornerShape(4.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Übungsmodus",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Vertiefungsmodus
                Button(
                    onClick = {
                        viewModel.toggleToPracticeMode()
                        if (!showDeepLearningButtons) {
                            viewModel.toggleDeepLearningButtons()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showDeepLearningButtons || isDeepLearningMode)
                            HotKlickColors.Primary else Color.Transparent,
                        contentColor = if (showDeepLearningButtons || isDeepLearningMode)
                            HotKlickColors.White else HotKlickColors.Dark
                    ),
                    shape = RoundedCornerShape(4.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Vertiefungsmodus",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Editiermodus
                Button(
                    onClick = {
                        viewModel.toggleToEditMode()
                        viewModel.hideDeepLearningButtons()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditMode)
                            HotKlickColors.Success else Color.Transparent,
                        contentColor = if (isEditMode)
                            HotKlickColors.White else HotKlickColors.Dark
                    ),
                    shape = RoundedCornerShape(4.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Editiermodus",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // ← ZURÜCK Button
            TextButton(
                onClick = { onBackClick?.invoke() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HotKlickColors.Dark
                ),
                modifier = Modifier.height(36.dp),
                enabled = onBackClick != null
            ) {
                Text(
                    text = "← ZURÜCK",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Export/Import Buttons (immer sichtbar)
            Button(
                onClick = { onExportClick?.invoke() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HotKlickColors.Secondary
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("📦", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "EXPORT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = { onImportClick?.invoke() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HotKlickColors.Primary
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("📥", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "IMPORT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Radius Controls (nur im Editiermodus)
            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .background(HotKlickColors.White, RoundedCornerShape(4.dp))
                        .border(1.dp, HotKlickColors.Border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.decreasePointRadius() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = HotKlickColors.Primary
                        ),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Text(
                        text = "${pointRadius.toInt()}px",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(45.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = { viewModel.increasePointRadius() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = HotKlickColors.Primary
                        ),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Deepening Controls (nur im Vertiefungsmodus)
            if (showDeepLearningButtons && !isDeepLearningMode) {
                Row(
                    modifier = Modifier
                        .background(HotKlickColors.White, RoundedCornerShape(4.dp))
                        .border(1.dp, HotKlickColors.Border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.openTaskCountDialog("text") },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = HotKlickColors.Primary
                        ),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("📝", fontSize = 14.sp)
                    }

                    IconButton(
                        onClick = { viewModel.openTaskCountDialog("audio") },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = HotKlickColors.Primary
                        ),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("🎤", fontSize = 14.sp)
                    }

                    IconButton(
                        onClick = { viewModel.openTaskCountDialog("both") },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = HotKlickColors.Primary
                        ),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("🎤📝", fontSize = 10.sp)
                    }

                    IconButton(
                        onClick = { viewModel.hideDeepLearningButtons() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = HotKlickColors.Primary
                        ),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("←", fontSize = 14.sp, color = Color.White)
                    }
                }
            }

            // Deepening Stats (während Aufgaben)
            if (isDeepLearningMode) {
                Row(
                    modifier = Modifier
                        .background(HotKlickColors.White, RoundedCornerShape(4.dp))
                        .border(1.dp, HotKlickColors.Border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aufg. $tasksCurrent/$tasksTotal",
                        color = HotKlickColors.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "✅ $correct",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Spacer um Platz zu füllen
            Spacer(modifier = Modifier.weight(1f))

            // Übungsname (wenn Bild geladen)
            if (backgroundImageUri != null) {
                OutlinedTextField(
                    value = exerciseNameInput.ifEmpty { "Übung" },
                    onValueChange = { viewModel.updateExerciseNameInput(it) },
                    modifier = Modifier.width(200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HotKlickColors.Primary,
                        unfocusedBorderColor = HotKlickColors.Border,
                        focusedContainerColor = HotKlickColors.White,
                        unfocusedContainerColor = HotKlickColors.White
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.sp
                    ),
                    singleLine = true
                )
            }

            // WICHTIG - DATEN SPEICHERN Button
            Button(
                onClick = {
                    viewModel.saveExerciseName()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HotKlickColors.Danger
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("⚠️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "WICHTIG - DATEN SPEICHERN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper-Funktion (bleibt unverändert)
private fun getFileNameFromUri(context: android.content.Context, uriString: String): String {
    return try {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "content") {
            var displayName = ""
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex =
                        cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            }
            if (displayName.isNotEmpty()) {
                val nameWithoutExtension = displayName.substringBeforeLast(".")
                return nameWithoutExtension
            }
            return "Unbekannte Übung"
        } else if (uri.scheme == "file") {
            val path = uri.path ?: ""
            val fileName = path.substringAfterLast("/")
            return fileName.substringBeforeLast(".")
        } else {
            return ""
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return "Fehler"
    }
}