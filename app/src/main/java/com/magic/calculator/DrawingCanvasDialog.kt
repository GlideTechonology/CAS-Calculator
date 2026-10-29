package com.magic.calculator

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DrawingCanvasDialog(
    onDismiss: () -> Unit,
    onDone: (Bitmap) -> Unit
) {
    var paths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(Translator.tr("Ekrana Çizerek Formül Girin"), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                },
                                onDrag = { change, _ ->
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    // Trigger recomposition
                                    currentPath = Path().apply { addPath(currentPath!!) }
                                },
                                onDragEnd = {
                                    currentPath?.let { paths = paths + it }
                                    currentPath = null
                                }
                            )
                        }
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize(),
                        onDraw = {
                            canvasSize = size
                            val stroke = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            paths.forEach { path ->
                                drawPath(path, color = Color.Black, style = stroke)
                            }
                            currentPath?.let {
                                drawPath(it, color = Color.Black, style = stroke)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { paths = listOf() }) {
                        Text(Translator.tr("Temizle"), color = MaterialTheme.colorScheme.error)
                    }
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text(Translator.tr("İptal"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (paths.isEmpty() && currentPath == null) {
                                onDismiss()
                                return@Button
                            }
                            // Create bitmap
                            val bitmap = Bitmap.createBitmap(
                                canvasSize.width.toInt().takeIf { it > 0 } ?: 1,
                                canvasSize.height.toInt().takeIf { it > 0 } ?: 1,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 8f
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                isAntiAlias = true
                            }
                            
                            val androidPaths = paths.map { it.asAndroidPath() }
                            androidPaths.forEach { canvas.drawPath(it, paint) }
                            
                            currentPath?.asAndroidPath()?.let { canvas.drawPath(it, paint) }
                            
                            onDone(bitmap)
                        }) {
                            Text(Translator.tr("Okut"))
                        }
                    }
                }
            }
        }
    }
}
