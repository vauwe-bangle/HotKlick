// ExportImportComponents.kt - Version mit Text statt Icons
package de.softopus.hotklick

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Export/Import Buttons für den Editiermodus
 */
@Composable
fun ExportImportButtons(
    hasImage: Boolean,
    hasPoints: Boolean,
    isExporting: Boolean,
    isImporting: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Export / Import",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Export Button
            Button(
                onClick = onExportClick,
                modifier = Modifier.weight(1f),
                enabled = hasImage && hasPoints && !isExporting && !isImporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isExporting) "Exportiere..." else "📤 Exportieren",
                    fontWeight = FontWeight.Bold
                )
            }

            // Import Button
            Button(
                onClick = onImportClick,
                modifier = Modifier.weight(1f),
                enabled = !isExporting && !isImporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isImporting) "Importiere..." else "📥 Importieren",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Hinweistext
        if (!hasImage || !hasPoints) {
            Text(
                text = "Export benötigt ein Bild mit Hotspots",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}