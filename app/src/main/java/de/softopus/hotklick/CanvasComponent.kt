// CanvasComponent.kt
// KORRIGIERT: Mit View → Bitmap Koordinaten-Konvertierung
package de.softopus.hotklick

import android.graphics.RectF
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.viewmodel.DrawingViewModel
import androidx.compose.ui.unit.IntSize

@Composable
fun HotspotCanvas(
    backgroundImageUri: Uri?,
    points: List<DrawPoint>,
    pointRadius: Float,
    isEditMode: Boolean,
    isDeepLearningMode: Boolean,
    viewModel: DrawingViewModel,
    mediaPlayer: MediaPlayer?,
    isPlaying: Boolean,
    onMediaPlayerChange: (MediaPlayer?) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    editImagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    practiceImagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    context: android.content.Context
) {
    // Speichere View-Größe und Bild-Größe für Koordinaten-Konvertierung
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    // Lade Original-Bildgröße direkt aus URI (WICHTIG!)
    LaunchedEffect(backgroundImageUri) {
        backgroundImageUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                    imageSize = IntSize(options.outWidth, options.outHeight)
                    println("DEBUG: Original image size from URI: ${imageSize.width}x${imageSize.height}")
                }
            } catch (e: Exception) {
                println("DEBUG: Error loading image size: ${e.message}")
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coordinates ->
            viewSize = coordinates.size
            println("DEBUG: View size: ${viewSize.width}x${viewSize.height}")
        }
    ) {
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
                .pointerInput(points, pointRadius, isEditMode, isDeepLearningMode, backgroundImageUri) {
                    detectTapGestures(
                        onTap = { offset: Offset ->
                            // Konvertiere View → Bitmap Koordinaten für Hit-Test
                            val bitmapOffset = viewToBitmapCoordinates(
                                offset, viewSize, imageSize
                            )
                            val hitPoint = findHitPoint(points, bitmapOffset)

                            if (isEditMode) {
                                hitPoint?.let {
                                    viewModel.deletePoint(it.id, it.name)
                                }
                            } else {
                                if (isDeepLearningMode) {
                                    if (hitPoint != null) {
                                        viewModel.checkDeepLearningAnswer(hitPoint)
                                    } else {
                                        val dummyPoint = DrawPoint(
                                            id = -1,
                                            name = "FEHLER",
                                            x = 0f,
                                            y = 0f,
                                            radius = 0f,
                                            imageUri = null,
                                            text = null,
                                            audioUri = null
                                        )
                                        viewModel.checkDeepLearningAnswer(dummyPoint)
                                    }
                                } else {
                                    if (hitPoint != null) {
                                        if (hitPoint.text != null && hitPoint.text.isNotEmpty()) {
                                            viewModel.showHotspotText(hitPoint)
                                            stopMediaPlayer(mediaPlayer, onMediaPlayerChange, onIsPlayingChange)
                                        }
                                    } else {
                                        viewModel.clearHotspotText()
                                        stopMediaPlayer(mediaPlayer, onMediaPlayerChange, onIsPlayingChange)
                                    }
                                }
                            }
                        },
                        onLongPress = { offset: Offset ->
                            if (isEditMode) {
                                val isInfoImage = backgroundImageUri?.toString()?.contains("info_") == true
                                val hasNoRealImage = backgroundImageUri == null || isInfoImage
                                if (hasNoRealImage) {
                                    editImagePickerLauncher.launch("image/*")
                                } else {
                                    // Konvertiere View → Bitmap Koordinaten
                                    val bitmapOffset = viewToBitmapCoordinates(
                                        offset, viewSize, imageSize
                                    )

                                    println("DEBUG LongPress: View(${offset.x}, ${offset.y}) → Bitmap(${bitmapOffset.x}, ${bitmapOffset.y})")

                                    val hitPoint = findHitPoint(points, bitmapOffset)
                                    if (hitPoint != null) {
                                        viewModel.openAudioDialog(hitPoint)
                                    } else {
                                        // WICHTIG: Speichere BITMAP-Koordinaten!
                                        viewModel.addPoint(bitmapOffset.x, bitmapOffset.y)
                                    }
                                }
                            } else {
                                practiceImagePickerLauncher.launch("image/*")
                            }
                        },
                        onDoubleTap = { offset: Offset ->
                            if (isEditMode) {
                                // Konvertiere View → Bitmap Koordinaten
                                val bitmapOffset = viewToBitmapCoordinates(
                                    offset, viewSize, imageSize
                                )
                                val hitPoint = findHitPoint(points, bitmapOffset)
                                hitPoint?.let { point ->
                                    viewModel.openTextDialog(point)
                                }
                            } else {
                                // Konvertiere View → Bitmap Koordinaten
                                val bitmapOffset = viewToBitmapCoordinates(
                                    offset, viewSize, imageSize
                                )
                                val hitPoint = findHitPoint(points, bitmapOffset)
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

                                        val newPlayer = MediaPlayer().apply {
                                            setDataSource(context, Uri.parse(hitPoint.audioUri))
                                            prepare()
                                            start()

                                            setOnCompletionListener {
                                                onIsPlayingChange(false)
                                                release()
                                                onMediaPlayerChange(null)
                                            }
                                        }

                                        onMediaPlayerChange(newPlayer)
                                        onIsPlayingChange(true)
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
            // WICHTIG: Zeichne Hotspots auch mit Koordinaten-Konvertierung!
            drawHotspots(points, isEditMode, viewSize, imageSize)
        }
    }
}

/**
 * KRITISCH: Konvertiert View-Koordinaten zu Bitmap-Koordinaten
 *
 * Berücksichtigt:
 * - ContentScale.Fit (Bild wird skaliert aber Aspect Ratio beibehalten)
 * - Zentrierung des Bildes in der View
 *
 * @param viewOffset Touch-Koordinaten aus dem Canvas (View)
 * @param viewSize Größe der View/Canvas
 * @param imageSize Originalgröße des Bildes (Bitmap)
 * @return Koordinaten im Bitmap-Koordinatensystem
 */
private fun viewToBitmapCoordinates(
    viewOffset: Offset,
    viewSize: IntSize,
    imageSize: IntSize
): Offset {
    if (imageSize.width == 0 || imageSize.height == 0) {
        // Bild noch nicht geladen
        return viewOffset
    }

    // Berechne Skalierungsfaktor für ContentScale.Fit
    val viewAspect = viewSize.width.toFloat() / viewSize.height.toFloat()
    val imageAspect = imageSize.width.toFloat() / imageSize.height.toFloat()

    val scale: Float
    val scaledWidth: Float
    val scaledHeight: Float
    val offsetX: Float
    val offsetY: Float

    if (viewAspect > imageAspect) {
        // View ist breiter → Bild wird an Höhe angepasst
        scale = viewSize.height.toFloat() / imageSize.height.toFloat()
        scaledWidth = imageSize.width * scale
        scaledHeight = viewSize.height.toFloat()
        offsetX = (viewSize.width - scaledWidth) / 2f
        offsetY = 0f
    } else {
        // View ist höher → Bild wird an Breite angepasst
        scale = viewSize.width.toFloat() / imageSize.width.toFloat()
        scaledWidth = viewSize.width.toFloat()
        scaledHeight = imageSize.height * scale
        offsetX = 0f
        offsetY = (viewSize.height - scaledHeight) / 2f
    }

    // Konvertiere View-Koordinaten zu Bitmap-Koordinaten
    val bitmapX = (viewOffset.x - offsetX) / scale
    val bitmapY = (viewOffset.y - offsetY) / scale

    return Offset(bitmapX, bitmapY)
}

/**
 * KRITISCH: Konvertiert Bitmap-Koordinaten zu View-Koordinaten (für Darstellung)
 */
private fun bitmapToViewCoordinates(
    bitmapOffset: Offset,
    viewSize: IntSize,
    imageSize: IntSize
): Offset {
    if (imageSize.width == 0 || imageSize.height == 0) {
        return bitmapOffset
    }

    val viewAspect = viewSize.width.toFloat() / viewSize.height.toFloat()
    val imageAspect = imageSize.width.toFloat() / imageSize.height.toFloat()

    val scale: Float
    val scaledWidth: Float
    val scaledHeight: Float
    val offsetX: Float
    val offsetY: Float

    if (viewAspect > imageAspect) {
        scale = viewSize.height.toFloat() / imageSize.height.toFloat()
        scaledWidth = imageSize.width * scale
        scaledHeight = viewSize.height.toFloat()
        offsetX = (viewSize.width - scaledWidth) / 2f
        offsetY = 0f
    } else {
        scale = viewSize.width.toFloat() / imageSize.width.toFloat()
        scaledWidth = viewSize.width.toFloat()
        scaledHeight = imageSize.height * scale
        offsetX = 0f
        offsetY = (viewSize.height - scaledHeight) / 2f
    }

    // Konvertiere Bitmap-Koordinaten zu View-Koordinaten
    val viewX = (bitmapOffset.x * scale) + offsetX
    val viewY = (bitmapOffset.y * scale) + offsetY

    return Offset(viewX, viewY)
}

private fun findHitPoint(points: List<DrawPoint>, offset: Offset): DrawPoint? {
    return points.find { point ->
        val distance = kotlin.math.sqrt(
            (offset.x - point.x) * (offset.x - point.x) +
                    (offset.y - point.y) * (offset.y - point.y)
        )
        distance <= point.radius
    }
}

private fun stopMediaPlayer(
    mediaPlayer: MediaPlayer?,
    onMediaPlayerChange: (MediaPlayer?) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit
) {
    mediaPlayer?.apply {
        if (isPlaying()) {
            stop()
            reset()
        }
        release()
    }
    onMediaPlayerChange(null)
    onIsPlayingChange(false)
}

private fun DrawScope.drawHotspots(
    points: List<DrawPoint>,
    isEditMode: Boolean,
    viewSize: IntSize,
    imageSize: IntSize
) {
    points.forEach { point ->
        // Konvertiere Bitmap-Koordinaten (gespeichert) zu View-Koordinaten (Darstellung)
        val bitmapOffset = Offset(point.x, point.y)
        val viewOffset = bitmapToViewCoordinates(bitmapOffset, viewSize, imageSize)

        // Skaliere auch den Radius
        val scale = if (imageSize.width > 0) {
            val viewAspect = viewSize.width.toFloat() / viewSize.height.toFloat()
            val imageAspect = imageSize.width.toFloat() / imageSize.height.toFloat()
            if (viewAspect > imageAspect) {
                viewSize.height.toFloat() / imageSize.height.toFloat()
            } else {
                viewSize.width.toFloat() / imageSize.width.toFloat()
            }
        } else {
            1f
        }
        val viewRadius = point.radius * scale

        if (isEditMode) {
            val pointColor = when {
                point.text != null && point.audioUri != null -> Color(0xFF4CAF50)
                point.text != null -> Color(0xFFFFC107)
                point.audioUri != null -> Color(0xFF2196F3)
                else -> Color(0xFFF44336)
            }

            drawCircle(
                color = pointColor,
                radius = viewRadius,
                center = viewOffset
            )

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = Color.Black.toArgb()
                    textSize = (viewRadius * 0.6f).coerceIn(16f, 48f)
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }

                canvas.nativeCanvas.drawText(
                    point.name,
                    viewOffset.x,
                    viewOffset.y - viewRadius - 12f,
                    paint
                )
            }

            drawCircle(
                color = Color.Black,
                radius = viewRadius,
                center = viewOffset,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )

            if (point.text != null) {
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(viewOffset.x - 8f, viewOffset.y)
                )
            }

            if (point.audioUri != null) {
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(viewOffset.x + 8f, viewOffset.y)
                )
            }
        }
    }
}