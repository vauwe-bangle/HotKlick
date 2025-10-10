// DialogComponents.kt
package de.softopus.hotklick

import android.content.ContentValues
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.viewmodel.DrawingViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// TEXT-DIALOG
@Composable
fun TextDialog(
    showDialog: Boolean,
    selectedPoint: DrawPoint?,
    textInput: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog && selectedPoint != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Text für ${selectedPoint.name} eingeben")
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
                        onValueChange = onTextChange,
                        label = { Text("Hotspot-Text") },
                        placeholder = { Text("Text eingeben...") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (selectedPoint.text != null) {
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
                TextButton(onClick = onSave) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

// AUDIO-DIALOG
@Composable
fun AudioDialog(
    showDialog: Boolean,
    selectedPoint: DrawPoint?,
    onLoadAudio: () -> Unit,
    onRecordAudio: () -> Unit,
    onRemoveAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog && selectedPoint != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Audio für ${selectedPoint.name}")
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

                    if (selectedPoint.audioUri != null) {
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
                        onClick = onLoadAudio,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("MP3-Datei laden")
                    }

                    Button(
                        onClick = onRecordAudio,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("🎤 Audio aufnehmen")
                    }

                    HorizontalDivider()

                    if (selectedPoint.audioUri != null) {
                        OutlinedButton(
                            onClick = onRemoveAudio,
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
                TextButton(onClick = onDismiss) {
                    Text("Schließen")
                }
            }
        )
    }
}

// RECORDER-DIALOG
@Composable
fun RecorderDialog(
    showDialog: Boolean,
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
    if (showDialog) {
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
                                                val audioUri = Uri.withAppendedPath(
                                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                    id.toString()
                                                )
                                                resultUri = audioUri.toString()
                                            }
                                        }
                                        resultUri
                                    } else {
                                        println("DEBUG: Verwende direkten Dateizugriff")
                                        recordingFile?.let { Uri.fromFile(it).toString() }
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
}

// AUFGABENANZAHL-DIALOG
@Composable
fun TaskCountDialog(
    showDialog: Boolean,
    selectedType: String?,
    onStart: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog && selectedType != null) {
        var taskCount by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = when (selectedType) {
                        "text" -> "Zufall-Text Modus"
                        "audio" -> "Zufall-Audio Modus"
                        "both" -> "Text + Audio Modus"
                        else -> "Vertiefungsmodus"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Wie viele Aufgaben möchten Sie üben?",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = taskCount,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 3) {
                                taskCount = it
                            }
                        },
                        label = { Text("Anzahl Aufgaben") },
                        placeholder = { Text("z.B. 10") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Empfohlen: 5-20 Aufgaben",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = taskCount.toIntOrNull() ?: 5
                        onStart(count, selectedType)
                    },
                    enabled = taskCount.toIntOrNull() != null &&
                            taskCount.toIntOrNull()!! > 0 &&
                            taskCount.toIntOrNull()!! <= 100
                ) {
                    Text("Starten")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun ExerciseNameDialog(
    showDialog: Boolean,
    exerciseNameInput: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Übungsname bearbeiten")
            },
            text = {
                Column {
                    Text(
                        text = "Geben Sie einen Namen für diese Übung ein:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = exerciseNameInput,
                        onValueChange = onNameChange,
                        label = { Text("Übungsname") },
                        placeholder = { Text("z.B. Anatomie Herz") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dieser Name wird über dem Canvas angezeigt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onSave) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        )
    }
}