// DrawingScreen.kt
package de.softopus.drawpoint

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import java.util.regex.Pattern
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.softopus.drawpoint.data.DrawPoint
import de.softopus.drawpoint.viewmodel.DrawingViewModel


import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight



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
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedHotspotText by viewModel.selectedHotspotText.collectAsState()
    val selectedHotspotName by viewModel.selectedHotspotName.collectAsState()
    val density = LocalDensity.current
    val uriHandler = LocalUriHandler.current

    // Image Picker Launcher für Editiermodus
    val editImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setBackgroundImage(uri)
    }

    // Image Picker Launcher für Übungsmodus
    val practiceImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setBackgroundImage(uri)
    }

    // Konvertierung der gewünschten Pixel-Größe in dp
    val canvasWidthDp = 800.dp
    val canvasHeightDp = 600.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zeichenbereich mit Hintergrundbild (stabile Position)
        Card(
            modifier = Modifier.size(canvasWidthDp, canvasHeightDp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Hintergrundbild
                if (backgroundImageUri != null) {
                    AsyncImage(
                        model = backgroundImageUri,
                        contentDescription = "Hintergrundbild",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Canvas für Punkte über dem Bild
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points, pointRadius, isEditMode) {
                            detectTapGestures(
                                onTap = { offset: Offset ->
                                    // Hit-Detection für Punkte mit individuellen Radien
                                    val hitPoint = points.find { point ->
                                        val distance = kotlin.math.sqrt(
                                            (offset.x - point.x) * (offset.x - point.x) +
                                                    (offset.y - point.y) * (offset.y - point.y)
                                        )
                                        distance <= point.radius
                                    }

                                    if (hitPoint != null) {
                                        if (isEditMode) {
                                            // Editiermodus: Punkt löschen
                                            viewModel.deletePoint(hitPoint.id, hitPoint.name)
                                        } else {
                                            // Übungsmodus: Text anzeigen (nur wenn Text vorhanden)
                                            if (hitPoint.text != null && hitPoint.text.isNotEmpty()) {
                                                viewModel.showHotspotText(hitPoint)
                                            }
                                        }
                                    } else {
                                        // Übungsmodus: Klick auf leeren Bereich schließt Textbox
                                        if (!isEditMode) {
                                            viewModel.clearHotspotText()
                                        }
                                    }
                                },
                                onLongPress = { offset: Offset ->
                                    if (isEditMode) {
                                        // Editiermodus: Neue Punkte erstellen
                                        viewModel.addPoint(offset.x, offset.y)
                                    } else {
                                        // Übungsmodus: Bild laden
                                        practiceImagePickerLauncher.launch("image/*")
                                    }
                                },
                                onDoubleTap = { offset: Offset ->
                                    if (isEditMode) {
                                        // Nur im Editiermodus Text-Eingabe
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

        // Modus-Toggle Button mit Long-Click für Edit-Modus
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
                                    // Einfacher Click: Edit → Practice
                                    viewModel.toggleToPracticeMode()
                                }
                            },
                            onLongPress = { _: Offset ->
                                // Long-Click: Umschaltung in beiden Richtungen
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
                    text = if (isEditMode) "🖊️ Editiermodus (Click → Übung)" else "🎯 Übungsmodus (Long-Click → Edit)",
                    fontWeight = FontWeight.Bold,
                    color = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Übungsmodus-Hinweise
        if (!isEditMode) {
            Text(
                text = "Long-Click auf Canvas: Bild laden • Click auf Hotspot: Text anzeigen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Alle Buttons in einer Reihe (nur im Editiermodus)
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

                // Vergrößerter Radius Stepper
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

        // Status-Anzeige der Punkte
        if (points.isNotEmpty()) {
            Text(
                text = "${points.size} Hotspots: ${points.count { it.text != null }} mit Text, ${points.count { it.text == null }} ohne Text",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Übungsmodus: Hotspot-Text Anzeige
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
                    // Text ohne Hotspot-Bezeichnung, größere Schrift
                    ClickableText(
                        text = buildAnnotatedString {
                            parseTextWithLinks(selectedHotspotText)
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        onClick = { offset: Int ->
                            // Link-Clicks behandeln
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

    // Text-Eingabe Dialog (nur im Editiermodus)
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
}

// Hilfsfunktion für Link-Erkennung
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

        // Text vor dem Link hinzufügen
        if (start > lastEnd) {
            append(text.substring(lastEnd, start))
        }

        // Link hinzufügen mit Styling und Annotation
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

    // Restlichen Text hinzufügen
    if (lastEnd < text.length) {
        append(text.substring(lastEnd))
    }
}

private fun DrawScope.drawPointsWithIndividualRadii(points: List<DrawPoint>, isEditMode: Boolean) {
    points.forEach { point ->
        if (isEditMode) {
            // Editiermodus: Normale Darstellung
            val pointColor = if (point.text != null) Color.Green else Color.Red

            // Punkt als gefüllter Kreis mit individuellem Radius
            drawCircle(
                color = pointColor,
                radius = point.radius,
                center = Offset(point.x, point.y)
            )

            // Punkt-Name über dem Punkt
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

            // Umrandung des Punktes
            drawCircle(
                color = Color.Black,
                radius = point.radius,
                center = Offset(point.x, point.y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )

            // Text-Indikator (kleiner Punkt) wenn Text vorhanden
            if (point.text != null) {
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(point.x, point.y)
                )
            }
        } else {
            // Übungsmodus: Hotspots komplett unsichtbar (0% Transparenz)
            // Aber weiterhin klickbar durch Hit-Detection
            // Nichts zeichnen - unsichtbare Hotspots!
        }
    }
}