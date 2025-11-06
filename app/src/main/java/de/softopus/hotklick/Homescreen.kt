// HomeScreen.kt - Startbildschirm mit Übungsliste (Web-App Design)
package de.softopus.hotklick

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.softopus.hotklick.ui.theme.HotKlickColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class für gespeicherte Übungen
 */
data class Exercise(
    val id: String,
    val name: String,
    val imageUri: Uri?,
    val hotspotCount: Int,
    val lastModified: Date
)

/**
 * HomeScreen - Übersichtsseite mit Übungsliste
 * Analog zur Web-App mit Grid-Layout und FAB
 */
@Composable
fun HomeScreen(
    exercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
    onNewExerciseClick: (Uri?, String) -> Unit,
    onDeleteClick: (Exercise) -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewExerciseDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HotKlickColors.Light)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Titel
            Text(
                text = "Meine Übungen",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = HotKlickColors.Dark,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Übungsliste oder Empty State
            if (exercises.isEmpty()) {
                EmptyState()
            } else {
                ExerciseGrid(
                    exercises = exercises,
                    onExerciseClick = onExerciseClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }

        // FAB (Floating Action Button)
        FloatingActionButton(
            onClick = { showNewExerciseDialog = true },
            containerColor = HotKlickColors.Secondary,
            contentColor = HotKlickColors.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                ),
            shape = CircleShape
        ) {
            Text(
                text = "+",
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = HotKlickColors.White
            )
        }
    }

    // Dialog für neue Übung
    if (showNewExerciseDialog) {
        NewExerciseDialog(
            onDismiss = { showNewExerciseDialog = false },
            onCreate = { imageUri, name ->
                onNewExerciseClick(imageUri, name)
                showNewExerciseDialog = false
            },
            onImport = onImportClick
        )
    }
}

/**
 * Empty State - Wenn keine Übungen vorhanden sind
 */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(60.dp, 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Text(
            text = "📚",
            fontSize = 64.sp,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .graphicsLayer(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        // Haupttext
        Text(
            text = "Noch keine Übungen vorhanden",
            fontSize = 16.sp,
            color = HotKlickColors.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Hinweistext
        Text(
            text = "Erstelle deine erste Übung mit dem + Button",
            fontSize = 14.sp,
            color = HotKlickColors.Gray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Grid mit Übungskarten
 */
@Composable
private fun ExerciseGrid(
    exercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
    onDeleteClick: (Exercise) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // Platz für FAB
    ) {
        items(exercises) { exercise ->
            ExerciseCard(
                exercise = exercise,
                onClick = { onExerciseClick(exercise) },
                onDelete = { onDeleteClick(exercise) }
            )
        }
    }
}

/**
 * Einzelne Übungskarte
 */
@Composable
private fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .graphicsLayer(
                translationY = if (isHovered) -2.dp.value else 0f
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = HotKlickColors.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHovered) 8.dp else 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HotKlickColors.Light)
                        .border(1.dp, HotKlickColors.Border, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (exercise.imageUri != null) {
                        AsyncImage(
                            model = exercise.imageUri,
                            contentDescription = exercise.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "📷",
                            fontSize = 48.sp,
                            color = HotKlickColors.Gray.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Übungsname
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = HotKlickColors.Dark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Metadaten
                Text(
                    text = formatExerciseMeta(exercise),
                    fontSize = 14.sp,
                    color = HotKlickColors.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Delete Button (rechts oben)
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Text("🗑️", fontSize = 16.sp)
            }
        }
    }

    // Delete Bestätigung
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Übung löschen?") },
            text = { Text("Möchtest du \"${exercise.name}\" wirklich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HotKlickColors.Danger
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Dialog für neue Übung erstellen
 */
@Composable
private fun NewExerciseDialog(
    onDismiss: () -> Unit,
    onCreate: (Uri?, String) -> Unit,
    onImport: () -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HotKlickColors.White,
        shape = RoundedCornerShape(8.dp),
        title = {
            Text(
                text = "Neue Übung erstellen",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = HotKlickColors.Dark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Übungsname
                Text(
                    text = "Übungsname",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HotKlickColors.Dark,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    placeholder = { Text("z.B. Anatomie Herz") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HotKlickColors.Primary,
                        unfocusedBorderColor = HotKlickColors.Border,
                        focusedContainerColor = HotKlickColors.White,
                        unfocusedContainerColor = HotKlickColors.White
                    ),
                    singleLine = true
                )

                // Bild auswählen
                Text(
                    text = "Bild auswählen",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HotKlickColors.Dark,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HotKlickColors.Light,
                        contentColor = HotKlickColors.Dark
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedImageUri != null) "✓ Bild ausgewählt" else "Bild auswählen",
                        fontSize = 14.sp
                    )
                }

                // Trennlinie
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = HotKlickColors.Border)
                Spacer(modifier = Modifier.height(8.dp))

                // Import Button
                Button(
                    onClick = {
                        onImport()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HotKlickColors.Secondary
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📥 ÜBUNG IMPORTIEREN",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(selectedImageUri, exerciseName.ifEmpty { "Übung" }) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HotKlickColors.Primary
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "ERSTELLEN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HotKlickColors.Gray
                )
            ) {
                Text(
                    text = "ABBRECHEN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

/**
 * Helper: Formatiert Metadaten für Übungskarte
 */
private fun formatExerciseMeta(exercise: Exercise): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    val dateStr = dateFormat.format(exercise.lastModified)
    return "$dateStr"
}

// graphicsLayer wird direkt von Compose UI bereitgestellt