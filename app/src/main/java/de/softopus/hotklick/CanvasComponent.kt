// CanvasComponent.kt
package de.softopus.hotklick

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import de.softopus.hotklick.data.DrawPoint
import de.softopus.hotklick.viewmodel.DrawingViewModel

@Composable
fun HotspotCanvas(
    backgroundImageUri: Uri?,
    points: List<DrawPoint>,
    pointRadius: Float,
    isEditMode: Boolean,
    isDeepLearningMode: Boolean,  // NEU
    viewModel: DrawingViewModel,
    mediaPlayer: MediaPlayer?,
    isPlaying: Boolean,
    onMediaPlayerChange: (MediaPlayer?) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    editImagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    practiceImagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    context: android.content.Context
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
                .pointerInput(points, pointRadius, isEditMode, isDeepLearningMode) {  // <-- HINZUGEFÜGT
                    detectTapGestures(
                        onTap = { offset: Offset ->
                            val hitPoint = findHitPoint(points, offset)

                            println("DEBUG Canvas: Klick erkannt, isDeepLearningMode=$isDeepLearningMode, hitPoint=${hitPoint?.name}")

                            if (isEditMode) {
                                hitPoint?.let {
                                    viewModel.deletePoint(it.id, it.name)
                                }
                            } else {
                                if (isDeepLearningMode) {
                                    println("DEBUG Canvas: Im Vertiefungsmodus, rufe checkDeepLearningAnswer auf")
                                    if (hitPoint != null) {
                                        viewModel.checkDeepLearningAnswer(hitPoint)
                                    } else {
                                        println("DEBUG Canvas: Kein Hotspot getroffen")
                                    }
                                } else {
                                    // Normaler Übungsmodus
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
                                val isInfoImage = backgroundImageUri?.toString()?.contains("info_edit") == true ||
                                        backgroundImageUri?.toString()?.contains("info_practice") == true

                                if (backgroundImageUri == null || isInfoImage) {
                                    editImagePickerLauncher.launch("image/*")
                                } else {
                                    val hitPoint = findHitPoint(points, offset)
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
                                val hitPoint = findHitPoint(points, offset)
                                hitPoint?.let { point ->
                                    viewModel.openTextDialog(point)
                                }
                            } else {
                                val hitPoint = findHitPoint(points, offset)
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
            drawHotspots(points, isEditMode)
        }
    }
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

private fun DrawScope.drawHotspots(points: List<DrawPoint>, isEditMode: Boolean) {
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