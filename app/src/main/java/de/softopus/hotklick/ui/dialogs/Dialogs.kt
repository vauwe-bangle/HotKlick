// Dialogs.kt - Modernisierte Dialoge im Web-App Design
package de.softopus.hotklick.ui.dialogs

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.viewmodel.DrawingViewModel
import de.softopus.hotklick.ui.theme.HotKlickColors
import kotlinx.coroutines.delay
import java.io.File

// === TEXT DIALOG (Web-App Style) ===
@OptIn(ExperimentalMaterial3Api::class)
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
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HotKlickColors.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Text(
                        text = "Hotspot bearbeiten",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = HotKlickColors.Dark
                    )

                    // Label Info
                    Text(
                        text = "Label: ${selectedPoint.name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = HotKlickColors.Gray
                    )

                    // Text Input
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = onTextChange,
                        label = { Text("Text") },
                        placeholder = { Text("Beschreibung des Hotspots...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HotKlickColors.Primary,
                            focusedLabelColor = HotKlickColors.Primary,
                            cursorColor = HotKlickColors.Primary
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HotKlickColors.Gray
                            )
                        ) {
                            Text("ABBRECHEN", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HotKlickColors.Primary
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("SPEICHERN", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// === AUDIO DIALOG (Web-App Style) ===
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
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HotKlickColors.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Text(
                        text = "Audio hinzufügen",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = HotKlickColors.Dark
                    )

                    // Label Info
                    Text(
                        text = "Hotspot: ${selectedPoint.name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = HotKlickColors.Gray
                    )

                    Divider(color = HotKlickColors.Border)

                    // Audio Options
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onRecordAudio,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HotKlickColors.Primary
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text(
                                text = "🎤",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AUDIO AUFNEHMEN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        OutlinedButton(
                            onClick = onLoadAudio,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HotKlickColors.Primary
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text(
                                text = "📁",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AUDIO-DATEI HOCHLADEN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (selectedPoint.audioUri != null) {
                            Button(
                                onClick = onRemoveAudio,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HotKlickColors.Danger
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Text(
                                    text = "🗑️",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "AUDIO ENTFERNEN",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Divider(color = HotKlickColors.Border)

                    // Close Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HotKlickColors.Gray
                        )
                    ) {
                        Text("ABBRECHEN", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// === RECORDER DIALOG (Web-App Style) ===
@Composable
fun RecorderDialog(
    showDialog: Boolean,
    isRecording: Boolean,
    recordingDuration: Int,
    context: Context,
    mediaRecorder: MediaRecorder?,
    recordingFile: File?,
    onMediaRecorderChange: (MediaRecorder?) -> Unit,
    onRecordingFileChange: (File?) -> Unit,
    viewModel: DrawingViewModel,
    selectedPointName: String
) {
    if (showDialog) {
        Dialog(onDismissRequest = {
            if (!isRecording) viewModel.closeRecorderDialog()
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HotKlickColors.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Text(
                        text = "Audio aufnehmen",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = HotKlickColors.Dark
                    )

                    // Point Info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = HotKlickColors.Light
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Hotspot: $selectedPointName",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = HotKlickColors.Primary
                        )
                    }

                    // Recording Time Display
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRecording)
                                HotKlickColors.Danger.copy(alpha = 0.1f)
                            else HotKlickColors.Light
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isRecording) {
                                Text(
                                    text = "⚫",
                                    fontSize = 32.sp,
                                    color = HotKlickColors.Danger
                                )
                            } else {
                                Text(
                                    text = "🎤",
                                    fontSize = 32.sp,
                                    color = HotKlickColors.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) HotKlickColors.Danger else HotKlickColors.Dark
                            )

                            if (isRecording) {
                                Text(
                                    text = "Aufnahme läuft...",
                                    fontSize = 13.sp,
                                    color = HotKlickColors.Danger,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Control Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isRecording) {
                            Button(
                                onClick = {
                                    val file = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
                                    onRecordingFileChange(file)

                                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        MediaRecorder(context)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        MediaRecorder()
                                    }

                                    recorder.apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                        setOutputFile(file.absolutePath)
                                        prepare()
                                        start()
                                    }

                                    onMediaRecorderChange(recorder)
                                    viewModel.startRecording()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HotKlickColors.Primary
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Text(
                                    text = "🎤",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "AUFNAHME STARTEN",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    mediaRecorder?.apply {
                                        stop()
                                        release()
                                    }
                                    onMediaRecorderChange(null)
                                    viewModel.stopRecording()

                                    recordingFile?.let { file ->
                                        viewModel.saveAudioToPointById(
                                            file.toURI().toString(),
                                            selectedPointName ?: ""
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HotKlickColors.Danger
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Text(
                                    text = "⏹️",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "STOPPEN & SPEICHERN",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                mediaRecorder?.apply {
                                    if (isRecording) {
                                        stop()
                                    }
                                    release()
                                }
                                onMediaRecorderChange(null)
                                viewModel.closeRecorderDialog()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HotKlickColors.Gray
                            )
                        ) {
                            Text("ABBRECHEN", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// === TASK COUNT DIALOG (Web-App Style) ===
@Composable
fun TaskCountDialog(
    showDialog: Boolean,
    selectedType: String,
    onStart: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var taskCount by remember { mutableStateOf(10) }

    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HotKlickColors.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = "Vertiefungsmodus starten",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = HotKlickColors.Dark
                    )

                    // Task Count Input
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Wie viele Aufgaben?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = HotKlickColors.Gray
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(
                                onClick = { if (taskCount > 1) taskCount-- },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = HotKlickColors.Primary
                                )
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HotKlickColors.White
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = HotKlickColors.Light
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = taskCount.toString(),
                                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HotKlickColors.Primary
                                )
                            }

                            IconButton(
                                onClick = { if (taskCount < 50) taskCount++ },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = HotKlickColors.Primary
                                )
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HotKlickColors.White
                                )
                            }
                        }
                    }

                    Divider(color = HotKlickColors.Border)

                    // Actions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onStart(taskCount, selectedType) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HotKlickColors.Primary
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text(
                                "STARTEN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HotKlickColors.Gray
                            )
                        ) {
                            Text("ABBRECHEN", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// === EXERCISE NAME DIALOG (Web-App Style) ===
@Composable
fun ExerciseNameDialog(
    showDialog: Boolean,
    exerciseNameInput: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HotKlickColors.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Text(
                        text = "Übungsname bearbeiten",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = HotKlickColors.Dark
                    )

                    // Input Field
                    OutlinedTextField(
                        value = exerciseNameInput,
                        onValueChange = onNameChange,
                        label = { Text("Übungsname") },
                        placeholder = { Text("z.B. Anatomie Herz") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HotKlickColors.Primary,
                            focusedLabelColor = HotKlickColors.Primary,
                            cursorColor = HotKlickColors.Primary
                        ),
                        shape = RoundedCornerShape(4.dp),
                        singleLine = true
                    )

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HotKlickColors.Gray
                            )
                        ) {
                            Text("ABBRECHEN", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HotKlickColors.Primary
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("SPEICHERN", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}