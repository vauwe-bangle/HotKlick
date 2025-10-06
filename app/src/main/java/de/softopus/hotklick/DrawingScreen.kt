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

    val canvasWidthDp = 1024.dp
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
                                        // Prüfe ob Infobild geladen ist
                                        val isInfoImage = backgroundImageUri?.toString()?.contains("info_edit") == true ||
                                                backgroundImageUri?.toString()?.contains("info_practice") == true

                                        if (backgroundImageUri == null || isInfoImage) {
                                            // Kein Bild oder Infobild: Neues Bild laden
                                            editImagePickerLauncher.launch("image/*")
                                        } else {
                                            // Arbeitsbild geladen: Hotspot-Logik
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