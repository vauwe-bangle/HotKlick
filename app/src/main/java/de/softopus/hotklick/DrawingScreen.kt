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
    val selectedHotspotName by viewModel.selectedHotspotName.collectAsState()
    val density = LocalDensity.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var recordingFile: File? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            mediaRecorder?.apply {
                try { stop() } catch (e: Exception) { }
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

    val canvasWidthDp = 800.dp
    val canvasHeightDp = 600.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(canvasWidthDp, canvasHeightDp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (backgroundImageUri != null) {
                    AsyncImage(
                        model = backgroundImageUri,
                        contentDescription = "Hintergrundbild",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points, pointRadius, isEditMode) {
                            detectTapGestures(
                                onTap = { offset: Offset ->
                                    val hitPoint = points.find { point ->
                                        val distance = kotlin.math.sqrt(
                                            (offset.x - point.x) * (offset.x - point.x) +
                                                    (offset.y - point.y) * (offset.y - point.y)
                                        )
                                        distance <= point.radius
                                    }

                                    if (isEditMode) {
                                        if (hitPoint != null) {
                                            viewModel.deletePoint(hitPoint.id, hitPoint.name)
                                        }
                                    } else {
                                        if (hitPoint != null) {
                                            if (hitPoint.text != null && hitPoint.text.isNotEmpty()) {
                                                viewModel.showHotspotText(hitPoint)

                                                mediaPlayer?.apply {
                                                    if (isPlaying()) {
                                                        stop()
                                                        reset()
                                                    }
                                                    release()
                                                }
                                                mediaPlayer = null
                                                isPlaying = false
                                            }
                                        } else {
                                            viewModel.clearHotspotText()

                                            mediaPlayer?.apply {
                                                if (isPlaying()) {
                                                    stop()
                                                    reset()
                                                }
                                                release()
                                            }
                                            mediaPlayer = null
                                            isPlaying = false
                                        }
                                    }
                                },
                                onLongPress = { offset: Offset ->
                                    if (isEditMode) {
                                        val hitPoint = points.find { point ->
                                            val distance = kotlin.math.sqrt(
                                                (offset.x - point.x) * (offset.x - point.x) +
                                                        (offset.y - point.y) * (offset.y - point.y)
                                            )
                                            distance <= point.radius
                                        }

                                        if (hitPoint != null) {
                                            viewModel.openAudioDialog(hitPoint)
                                        } else {
                                            viewModel.addPoint(offset.x, offset.y)
                                        }
                                    } else {
                                        practiceImagePickerLauncher.launch("image/*")
                                    }
                                },
                                onDoubleTap = { offset: Offset ->
                                    if (isEditMode) {
                                        val hitPoint = points.find { point ->
                                            val distance = kotlin.math.sqrt(
                                                (offset.x - point.x) * (offset.x - point.x) +
                                                        (offset.y - point.y) * (offset.y - point.y)
                                            )
                                            distance <= point.radius
                                        }

                                        hitPoint?.let { point ->
                                            viewModel.openTextDialog(point)
                                        }
                                    } else {
                                        val hitPoint = points.find { point ->
                                            val distance = kotlin.math.sqrt(
                                                (offset.x - point.x) * (offset.x - point.x) +
                                                        (offset.y - point.y) * (offset.y - point.y)
                                            )
                                            distance <= point.radius
                                        }

                                        if (hitPoint != null && hitPoint.audioUri != null) {
                                            if (hitPoint.text != null && hitPoint.text.isNotEmpty()) {
                                                viewModel.showHotspotText(hitPoint)
                                            }

                                            try {
                                                mediaPlayer?.apply {
                                                    if (isPlaying()) {
                                                        stop()
                                                    }
                                                    reset()
                                                    release()
                                                }

                                                mediaPlayer = MediaPlayer().apply {
                                                    setDataSource(context, AndroidUri.parse(hitPoint.audioUri))
                                                    prepare()
                                                    start()
                                                    isPlaying = true

                                                    setOnCompletionListener {
                                                        isPlaying = false
                                                        release()
                                                        mediaPlayer = null
                                                    }
                                                }

                                                viewModel.playAudio(hitPoint.audioUri)
                                            } catch (e: Exception) {
                                                println("DEBUG: Fehler beim Audio abspielen: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    drawPointsWithIndividualRadii(points, isEditMode)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .pointerInput(isEditMode) {
                        detectTapGestures(
                            onTap = {
                                if (isEditMode) {
                                    viewModel.toggleToPracticeMode()
                                }
                            },
                            onLongPress = { _: Offset ->
                                if (isEditMode) {
                                    viewModel.toggleToPracticeMode()
                                } else {
                                    viewModel.toggleToEditMode()
                                }
                            }
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    text = if (isEditMode) "Editiermodus (Click → Übung)" else "Übungsmodus (Long-Click → Edit)",
                    fontWeight = FontWeight.Bold,
                    color = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isEditMode) {
            Text(
                text = "Long-Click: Bild laden • 1x-Click: Text anzeigen • 2x-Click: Audio abspielen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isEditMode) {
            Text(
                text = "Long-Press leer: Hotspot erstellen • Long-Press Hotspot: Audio • 1x-Click: Löschen • 2x-Click: Text",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isEditMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { editImagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bild laden")
                }

                if (backgroundImageUri != null) {
                    Button(
                        onClick = { viewModel.saveDataAndClearImage() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Daten speichern & Bild entfernen")
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.decreasePointRadius() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${pointRadius.toInt()}px",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { viewModel.increasePointRadius() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (points.isNotEmpty()) {
            Text(
                text = "${points.size} Hotspots: ${points.count { it.text != null }} mit Text, ${points.count { it.audioUri != null }} mit Audio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (!isEditMode && selectedHotspotText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    ClickableText(
                        text = buildAnnotatedString {
                            parseTextWithLinks(selectedHotspotText)
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        onClick = { offset: Int ->
                            val annotatedString = buildAnnotatedString {
                                parseTextWithLinks(selectedHotspotText)
                            }

                            annotatedString.getStringAnnotations(
                                tag = "URL",
                                start = offset,
                                end = offset
                            ).firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { viewModel.clearHotspotText() }
                        ) {
                            Text("Schließen")
                        }
                    }
                }
            }
        }
    }

    if (isEditMode && showTextDialog && selectedPointForText != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeTextDialog() },
            title = {
                Text("Text für ${selectedPointForText!!.name} eingeben")
            },
            text = {
                Column {
                    Text(
                        text = "Geben Sie den Text für diesen Hotspot ein:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { viewModel.updateTextInput(it) },
                        label = { Text("Hotspot-Text") },
                        placeholder = { Text("Text eingeben...") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (selectedPointForText!!.text != null) {
                        Text(
                            text = "Bestehender Text wird überschrieben",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveTextToPoint() }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.closeTextDialog() }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (isEditMode && showAudioDialog && selectedPointForAudio != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAudioDialog() },
            title = {
                Text("Audio für ${selectedPointForAudio!!.name}")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Wählen Sie eine Option:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedPointForAudio!!.audioUri != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Audio zugeordnet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Noch kein Audio zugeordnet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    HorizontalDivider()

                    Button(
                        onClick = {
                            audioPickerLauncher.launch("audio/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("MP3-Datei laden")
                    }

                    Button(
                        onClick = {
                            viewModel.openRecorderDialog()
                            viewModel.closeAudioDialog()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("🎤 Audio aufnehmen")
                    }

                    HorizontalDivider()

                    if (selectedPointForAudio!!.audioUri != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.removeAudioFromPoint()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Audio entfernen")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.closeAudioDialog() }
                ) {
                    Text("Schließen")
                }
            }
        )
    }

    if (showRecorderDialog) {
        RecorderDialog(
            isRecording = isRecording,
            recordingDuration = recordingDuration,
            context = context,
            mediaRecorder = mediaRecorder,
            recordingFile = recordingFile,
            onMediaRecorderChange = { mediaRecorder = it },
            onRecordingFileChange = { recordingFile = it },
            viewModel = viewModel,
            selectedPointName = currentRecordingPointName  // GEÄNDERT - verwende State
        )
    }
}

@Composable
fun RecorderDialog(
    isRecording: Boolean,
    recordingDuration: Int,
    context: android.content.Context,
    mediaRecorder: MediaRecorder?,
    recordingFile: File?,
    onMediaRecorderChange: (MediaRecorder?) -> Unit,
    onRecordingFileChange: (File?) -> Unit,
    viewModel: DrawingViewModel,
    selectedPointName: String?
) {
    AlertDialog(
        onDismissRequest = {
            if (isRecording) {
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                    onMediaRecorderChange(null)
                    viewModel.stopRecording()
                } catch (e: Exception) {
                    println("DEBUG: Fehler beim Stoppen: ${e.message}")
                }
            }
            viewModel.closeRecorderDialog()
        },
        title = { Text("Audio aufnehmen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRecording) {
                    Text(
                        text = "Aufnahme läuft...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${recordingDuration}s",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    LaunchedEffect(isRecording) {
                        var duration = 0
                        while (isRecording) {
                            delay(1000)
                            duration++
                            viewModel.updateRecordingDuration(duration)
                        }
                    }
                } else {
                    Text(
                        text = "Bereit zur Aufnahme",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (!isRecording) {
                    Button(
                        onClick = {
                            try {
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val fileName = "HotKlick_$timeStamp.m4a"

                                val musicDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/m4a")
                                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/HotKlick")
                                    }
                                    val uri = context.contentResolver.insert(
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        contentValues
                                    )
                                    uri?.let { context.contentResolver.openFileDescriptor(it, "w")?.fileDescriptor }
                                } else {
                                    val musicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "HotKlick")
                                    if (!musicFolder.exists()) {
                                        musicFolder.mkdirs()
                                    }
                                    onRecordingFileChange(File(musicFolder, fileName))
                                    null
                                }

                                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MediaRecorder(context)
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaRecorder()
                                }.apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    setAudioEncodingBitRate(128000)
                                    setAudioSamplingRate(44100)

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && musicDir != null) {
                                        setOutputFile(musicDir)
                                    } else {
                                        setOutputFile(recordingFile?.absolutePath)
                                    }

                                    prepare()
                                    start()
                                }

                                onMediaRecorderChange(recorder)
                                viewModel.startRecording()
                            } catch (e: Exception) {
                                println("DEBUG: Fehler beim Starten: ${e.message}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aufnahme starten")
                    }
                } else {
                    Button(
                        onClick = {
                            println("DEBUG: === AUFNAHME BEENDEN BUTTON GEKLICKT ===")
                            try {
                                println("DEBUG: Stoppe MediaRecorder")
                                mediaRecorder?.apply {
                                    stop()
                                    println("DEBUG: Stop erfolgreich")
                                    release()
                                    println("DEBUG: Release erfolgreich")
                                }
                                onMediaRecorderChange(null)
                                viewModel.stopRecording()

                                println("DEBUG: Android Version: ${Build.VERSION.SDK_INT}")

                                val audioUriToSave = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    println("DEBUG: Verwende MediaStore Abfrage")
                                    val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
                                    val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
                                    val selectionArgs = arrayOf("HotKlick_%")
                                    val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

                                    var resultUri: String? = null
                                    context.contentResolver.query(
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        projection,
                                        selection,
                                        selectionArgs,
                                        sortOrder
                                    )?.use { cursor ->
                                        if (cursor.moveToFirst()) {
                                            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                                            val id = cursor.getLong(idColumn)
                                            val audioUri = AndroidUri.withAppendedPath(
                                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                id.toString()
                                            )
                                            resultUri = audioUri.toString()
                                        }
                                    }
                                    resultUri
                                } else {
                                    println("DEBUG: Verwende direkten Dateizugriff")
                                    recordingFile?.let { AndroidUri.fromFile(it).toString() }
                                }

                                viewModel.closeRecorderDialog()
                                println("DEBUG: audioUriToSave = $audioUriToSave")
                                println("DEBUG: selectedPointName = $selectedPointName")


                                audioUriToSave?.let { uri ->
                                    selectedPointName?.let { name ->
                                        println("DEBUG: Rufe saveAudioToPointById auf mit Name: $name")
                                        viewModel.saveAudioToPointById(uri, name)
                                    }
                                }
                            } catch (e: Exception) {
                                println("DEBUG: FEHLER beim Stoppen: ${e.message}")
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Aufnahme beenden")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isRecording) {
                        try {
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            onMediaRecorderChange(null)
                            viewModel.stopRecording()
                        } catch (e: Exception) { }
                    }
                    viewModel.closeRecorderDialog()
                }
            ) {
                Text("Abbrechen")
            }
        }
    )
}

private fun AnnotatedString.Builder.parseTextWithLinks(text: String) {
    val urlPattern = Pattern.compile(
        "(?i)\\b(?:https?://|www\\.)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    )

    val matcher = urlPattern.matcher(text)
    var lastEnd = 0

    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        val url = matcher.group()

        if (start > lastEnd) {
            append(text.substring(lastEnd, start))
        }

        withStyle(
            style = SpanStyle(
                color = androidx.compose.ui.graphics.Color.Blue,
                textDecoration = TextDecoration.Underline
            )
        ) {
            pushStringAnnotation(
                tag = "URL",
                annotation = if (url.startsWith("http")) url else "https://$url"
            )
            append(url)
            pop()
        }

        lastEnd = end
    }

    if (lastEnd < text.length) {
        append(text.substring(lastEnd))
    }
}

private fun DrawScope.drawPointsWithIndividualRadii(points: List<DrawPoint>, isEditMode: Boolean) {
    points.forEach { point ->
        if (isEditMode) {
            val pointColor = when {
                point.text != null && point.audioUri != null -> Color(0xFF4CAF50)
                point.text != null -> Color(0xFFFFC107)
                point.audioUri != null -> Color(0xFF2196F3)
                else -> Color(0xFFF44336)
            }

            drawCircle(
                color = pointColor,
                radius = point.radius,
                center = Offset(point.x, point.y)
            )

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = Color.Black.toArgb()
                    textSize = (point.radius * 0.6f).coerceIn(16f, 48f)
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }

                canvas.nativeCanvas.drawText(
                    point.name,
                    point.x,
                    point.y - point.radius - 12f,
                    paint
                )
            }

            drawCircle(
                color = Color.Black,
                radius = point.radius,
                center = Offset(point.x, point.y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )

            if (point.text != null) {
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(point.x - 8f, point.y)
                )
            }

            if (point.audioUri != null) {
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(point.x + 8f, point.y)
                )
            }
        }
    }
}