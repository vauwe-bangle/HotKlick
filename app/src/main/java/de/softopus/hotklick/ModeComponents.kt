// ModeComponents.kt
package de.softopus.hotklick

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.viewmodel.DrawingViewModel
import java.util.regex.Pattern


// MODUS-BUTTON (MIT DOPPELKLICK)
@Composable
fun ModeToggleButton(
    isEditMode: Boolean,
    showDeepLearningButtons: Boolean,
    viewModel: DrawingViewModel
) {
    // Button nur zeigen wenn NICHT im Vertiefungsmodus
    if (!showDeepLearningButtons) {
        Card(
            modifier = Modifier
                .pointerInput(isEditMode) {
                    detectTapGestures(
                        onTap = {
                            if (isEditMode) {
                                viewModel.toggleToPracticeMode()
                            }
                        },
                        onDoubleTap = {
                            if (!isEditMode) {
                                viewModel.toggleDeepLearningButtons()
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
                containerColor = if (isEditMode)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(
                text = if (isEditMode)
                    "Editiermodus (Click → Übung)"
                else
                    "Übungsmodus (Doppel-Click → Vertiefung)",
                fontWeight = FontWeight.Bold,
                color = if (isEditMode)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

// VERTIEFUNGSMODUS-BUTTONS
@Composable
fun DeepLearningButtons(
    onTextClick: () -> Unit,
    onAudioClick: () -> Unit,
    onBothClick: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Vertiefungsmodus - Finde den richtigen Hotspot:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTextClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFFFC107)
                )
            ) {
                Text("Zufall-Text", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onAudioClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF2196F3)
                )
            ) {
                Text("Zufall-Audio", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onBothClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                )
            ) {
                Text("Beides", fontWeight = FontWeight.Bold)
            }
        }
    }
}


// EDITIERMODUS-CONTROLS
@Composable
fun EditModeControls(
    backgroundImageUri: Uri?,
    pointRadius: Float,
    editImagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    viewModel: DrawingViewModel
) {
    Column {
        Text(
            text = "Long-Press leer: Hotspot erstellen • Long-Press Hotspot: Audio • 1x-Click: Löschen • 2x-Click: Text",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
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
}

// ÜBUNGSMODUS-HINWEISE
@Composable
fun PracticeModeHints() {
    Text(
        text = "Long-Click: Bild laden • 1x-Click: Text anzeigen • 2x-Click: Audio abspielen",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

// HOTSPOT-STATISTIK
@Composable
fun HotspotStats(points: List<DrawPoint>) {
    if (points.isNotEmpty()) {
        Text(
            text = "${points.size} Hotspots: ${points.count { it.text != null }} mit Text, ${points.count { it.audioUri != null }} mit Audio",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// HOTSPOT-TEXT-ANZEIGE (Übungsmodus)
@Composable
fun HotspotTextDisplay(
    selectedHotspotText: String,
    uriHandler: UriHandler,
    viewModel: DrawingViewModel
) {
    if (selectedHotspotText.isNotEmpty()) {
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

// HILFSFUNKTION FÜR LINK-PARSING (aus DrawingScreen.kt verschoben)
private fun androidx.compose.ui.text.AnnotatedString.Builder.parseTextWithLinks(text: String) {
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
            style = androidx.compose.ui.text.SpanStyle(
                color = androidx.compose.ui.graphics.Color.Blue,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
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