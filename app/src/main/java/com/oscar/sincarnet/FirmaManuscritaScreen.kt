package com.oscar.sincarnet

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

private val SIGNATURE_COLOR = Color(0xFF474787)
private const val SIGNATURE_STROKE_WIDTH_DP = 10f
private const val SIGNATURE_DOT_RADIUS_DP = 3.5f
private const val SIGNATURE_STROKE_WIDTH_PX = 12f
private const val SIGNATURE_DOT_RADIUS_PX = 3.5f

@Composable
fun FirmaManuscritaScreen(
    modifier: Modifier = Modifier,
    signerName: String = "",
    onSignatureSaved: (ImageBitmap) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    val currentPath = remember { mutableStateListOf<Offset>() }
    var canvasWidthPx by remember { mutableStateOf(0) }
    var canvasHeightPx by remember { mutableStateOf(0) }

    val isEmpty = paths.isEmpty() && currentPath.isEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Título + área de firma centrada verticalmente en el espacio disponible
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.firma_screen_title, signerName),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Altura 220dp: proporción apaisada similar al box de impresión,
            // con espacio suficiente para firmar cómodamente.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                    .background(Color.White, MaterialTheme.shapes.medium)
            ) {
                if (isEmpty) {
                    Text(
                        text = stringResource(R.string.firma_hint),
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath.clear()
                                    currentPath.add(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPath.add(change.position)
                                },
                                onDragEnd = {
                                    if (currentPath.isNotEmpty()) {
                                        paths.add(currentPath.toList())
                                        currentPath.clear()
                                    }
                                },
                                onDragCancel = {
                                    currentPath.clear()
                                }
                            )
                        }
                ) {
                    canvasWidthPx = size.width.toInt()
                    canvasHeightPx = size.height.toInt()

                    val strokeStyle = Stroke(
                        width = SIGNATURE_STROKE_WIDTH_DP.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )

                    val allPaths = buildList {
                        addAll(paths)
                        if (currentPath.isNotEmpty()) add(currentPath.toList())
                    }

                    allPaths.forEach { pts ->
                        when {
                            pts.size >= 2 -> {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(path = path, color = SIGNATURE_COLOR, style = strokeStyle)
                            }
                            pts.size == 1 -> drawCircle(
                                color = SIGNATURE_COLOR,
                                radius = SIGNATURE_DOT_RADIUS_DP.dp.toPx(),
                                center = pts.first()
                            )
                        }
                    }
                }
            }
        }

        // Botones fijos en la parte inferior
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    paths.clear()
                    currentPath.clear()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF40407A),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(text = stringResource(R.string.firma_clear))
            }

            Button(
                onClick = {
                    val allPaths = buildList {
                        addAll(paths)
                        if (currentPath.isNotEmpty()) add(currentPath.toList())
                    }
                    onSignatureSaved(
                        renderSignatureToBitmap(allPaths, canvasWidthPx, canvasHeightPx)
                    )
                },
                enabled = !isEmpty,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF9A9AB8),
                    disabledContentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(text = stringResource(R.string.firma_save))
            }

            BackIconButton(onClick = onCancel)
        }
    }
}

private fun renderSignatureToBitmap(
    paths: List<List<Offset>>,
    width: Int,
    height: Int
): ImageBitmap {
    if (width <= 0 || height <= 0) return ImageBitmap(100, 100)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(44, 44, 84)
        strokeWidth = SIGNATURE_STROKE_WIDTH_PX
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }

    paths.forEach { pts ->
        when {
            pts.size >= 2 -> {
                val p = android.graphics.Path()
                p.moveTo(pts.first().x, pts.first().y)
                pts.drop(1).forEach { p.lineTo(it.x, it.y) }
                canvas.drawPath(p, paint)
            }
            pts.size == 1 -> canvas.drawCircle(pts.first().x, pts.first().y, SIGNATURE_DOT_RADIUS_PX, paint)
        }
    }

    return bitmap.asImageBitmap()
}

@Preview(showBackground = true)
@Composable
private fun FirmaManuscritaScreenPreview() {
    SinCarnetTheme {
        FirmaManuscritaScreen(signerName = "Instructor")
    }
}