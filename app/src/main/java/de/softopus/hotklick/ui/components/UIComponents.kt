// UIComponents.kt - Modernisierte Komponenten im Web-App Design
package de.softopus.hotklick.ui.components

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.viewmodel.DrawingViewModel
import de.softopus.hotklick.ui.theme.HotKlickColors

// === EDIT MODE CONTROLS (Web-App Style) ===
@Composable
fun EditModeControls(
    backgroundImageUri: Uri?,
    pointRadius: Float,
    editImagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    viewModel: DrawingViewModel,
    isExporting: Boolean,
    isImporting: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HotKlickColors.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "EDITIERMODUS-STEUERUNG",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HotKlickColors.Success
            )

            // Bild-Steuerung
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { editImagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HotKlickColors.Primary
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BILD LADEN", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                if (backgroundImageUri != null) {
                    Button(
                        onClick = { viewModel.setBackgroundImage(null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HotKlickColors.Danger
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🗑️",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ENTFERNEN", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Divider(color = HotKlickColors.Border)

            // Radius-Steuerung
            Text(
                text = "Hotspot-Radius",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = HotKlickColors.Dark
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HotKlickColors.Light, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { viewModel.decreasePointRadius() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = HotKlickColors.Primary
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Text(
                    text = "${pointRadius.toInt()}px",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotKlickColors.Dark,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { viewModel.increasePointRadius() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = HotKlickColors.Primary
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Divider(color = HotKlickColors.Border)

            // Export/Import
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportClick,
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HotKlickColors.Secondary
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("📦 EXPORT", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Button(
                    onClick = onImportClick,
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HotKlickColors.Primary
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("📥 IMPORT", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// === PRACTICE MODE HINTS ===
@Composable
fun PracticeModeHints() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD) // Light Blue
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "💡 Hinweise",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HotKlickColors.Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Klicken Sie auf Hotspots um Informationen anzuzeigen\n" +
                        "• Wechseln Sie in den Vertiefungsmodus für Aufgaben\n" +
                        "• Im Editiermodus können Sie Hotspots bearbeiten",
                fontSize = 12.sp,
                color = HotKlickColors.Dark.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

// === HOTSPOT STATS ===
@Composable
fun HotspotStats(points: List<DrawPoint>) {
    if (points.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HotKlickColors.White
        ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem("📍", "Hotspots", points.size.toString())
            StatItem("📝", "Mit Text", points.count { !it.text.isNullOrEmpty() }.toString())
            StatItem("🎤", "Mit Audio", points.count { it.audioUri != null }.toString())
        }
    }
}

@Composable
private fun StatItem(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = HotKlickColors.Primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = HotKlickColors.Gray
        )
    }
}

// === HOTSPOT TEXT DISPLAY (Web-App Style Textbox) ===
@Composable
fun HotspotTextDisplay(
    selectedHotspotText: String,
    uriHandler: UriHandler,
    viewModel: DrawingViewModel
) {
    if (selectedHotspotText.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 250.dp),
            colors = CardDefaults.cardColors(
                containerColor = HotKlickColors.Light
            ),
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HotKlickColors.Border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Text Content
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = HotKlickColors.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, HotKlickColors.Primary)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = selectedHotspotText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = HotKlickColors.Dark,
                            lineHeight = 22.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 80.dp),
                            textAlign = TextAlign.Center
                        )

                        // Close Button
                        Button(
                            onClick = { viewModel.clearHotspotText() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HotKlickColors.Danger
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "Schließen",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// === DEEP LEARNING RESULT ===
@Composable
fun DeepLearningResult(
    tasksTotal: Int,
    correct: Int,
    wrong: Int,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HotKlickColors.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎯 Ergebnis",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HotKlickColors.Primary
            )

            Divider(color = HotKlickColors.Border)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultItem("✅", "Richtig", correct, HotKlickColors.Success)
                ResultItem("❌", "Falsch", wrong, HotKlickColors.Danger)
                ResultItem("📊", "Gesamt", tasksTotal, HotKlickColors.Primary)
            }

            val percentage = if (tasksTotal > 0) (correct * 100) / tasksTotal else 0

            LinearProgressIndicator(
                progress = percentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = when {
                    percentage >= 80 -> HotKlickColors.Success
                    percentage >= 50 -> HotKlickColors.Warning
                    else -> HotKlickColors.Danger
                },
                trackColor = HotKlickColors.Light
            )

            Text(
                text = "$percentage% korrekt",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    percentage >= 80 -> HotKlickColors.Success
                    percentage >= 50 -> HotKlickColors.Warning
                    else -> HotKlickColors.Danger
                }
            )

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HotKlickColors.Primary
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "← ZURÜCK ZUR ÜBERSICHT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ResultItem(icon: String, label: String, value: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = icon, fontSize = 24.sp)
        Text(
            text = value.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = HotKlickColors.Gray
        )
    }
}