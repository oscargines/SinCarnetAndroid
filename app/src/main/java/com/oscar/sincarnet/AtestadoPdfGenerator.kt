package com.oscar.sincarnet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import java.io.FileOutputStream

/** Resultado de generación PDF con archivo y timestamp de creación. */
internal data class AtestadoPdfResult(
    val file: File,
    val createdAtMillis: Long
)

private const val A4_WIDTH_PT = 595f
private const val A4_HEIGHT_PT = 842f

private const val SIDE_MARGIN_MM = 5f
private const val TOP_MARGIN_MM = 5.5f
private const val BOTTOM_MARGIN_MM = 15f

private const val LEFT_COL_MM = 10f
private const val CENTER_COL_MM = 175f

private const val ESC_ESPANA_WIDTH_MM = 8f
private const val ESC_GC_WIDTH_MM = 7f
private const val HEADER_HEIGHT_MM = 22f
private const val TITLE_TOP_MM = 15f
private const val TITLE_BODY_GAP_PT = 18f

/** Convierte milímetros a puntos tipográficos (PDF). */
private fun mmToPt(mm: Float): Float = mm * 72f / 25.4f

/**
 * Genera PDF de atestado con contenido principal y bloque de firmas.
 *
 * Se usa como variante de salida PDF compacta frente al generador continuo.
 */
internal fun generateAtestadoSignaturesPdf(
    context: Context,
    signatures: Map<PdfSignatureSlot, PdfSignatureContent<ImageBitmap>>,
    investigatedNoSignText: String = NO_DESEA_FIRMAR_TEXT,
    courtData: JuzgadoAtestadoData? = null,
    personData: PersonaInvestigadaData? = null,
    ocurrenciaData: OcurrenciaDelitData? = null,
    citacionDocument: CitacionDocument? = null,
    instructorTip: String = "",
    secretaryTip: String = "",
    instructorUnit: String = ""
): AtestadoPdfResult {
    val now = System.currentTimeMillis()
    val pdfDocument = PdfDocument()

    val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT.toInt(), A4_HEIGHT_PT.toInt(), 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val calibriRegular = loadTypefaceFromAssets(context, "fonts/calibri-regular.ttf")
    val calibriBold = loadTypefaceFromAssets(context, "fonts/calibri-bold.ttf") ?: calibriRegular

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 14f
        typeface = calibriBold
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 11f
        typeface = calibriRegular
    }
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }

    val leftMargin = mmToPt(SIDE_MARGIN_MM)
    val rightMargin = mmToPt(SIDE_MARGIN_MM)
    val topMargin = mmToPt(TOP_MARGIN_MM)
    val bottomMargin = mmToPt(BOTTOM_MARGIN_MM)

    val contentLeft = leftMargin
    val contentRight = A4_WIDTH_PT - rightMargin

    val leftColWidth = mmToPt(LEFT_COL_MM)
    val leftColRightX = contentLeft + leftColWidth
    val centerColWidth = mmToPt(CENTER_COL_MM)
    val centerColLeftX = leftColRightX
    val centerColRightX = (centerColLeftX + centerColWidth).coerceAtMost(contentRight)

    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 1: Caja "ATESTADO NÚMERO:" (arriba a la izquierda de zona derecha)
    // ════════════════════════════════════════════════════════════════════════════
    val atestadoBoxTop = mmToPt(5.6f)
    // Distancia TOP al borde superior: 5.6 mm
    val atestadoBoxLeft = centerColLeftX + mmToPt(5f)
    // Distancia LEFT al inicio de columna derecha: 5 mm
    val atestadoBoxWidth = mmToPt(75f)
    val atestadoBoxHeight = mmToPt(6f)
    val atestadoBoxRect = RectF(
        atestadoBoxLeft,
        atestadoBoxTop,
        atestadoBoxLeft + atestadoBoxWidth,
        atestadoBoxTop + atestadoBoxHeight
    )
    // Dimensiones: 75 mm ancho x 6 mm alto
    
    // Dibujar caja
    canvas.drawRect(atestadoBoxRect, boxPaint)
    
    // Dibujar texto centrado verticalmente dentro de la caja
    val textPaintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9f
        typeface = calibriRegular
    }
    val numeroDiligencias = courtData?.numeroDiligencias.orEmpty().trim()
    val atestadoNumeroText = if (numeroDiligencias.isBlank()) {
        "ATESTADO NUMERO:"
    } else {
        "ATESTADO NUMERO:   $numeroDiligencias"
    }
    val textX = atestadoBoxLeft + 4f
    // Texto interior con padding: 4 mm desde el borde izquierdo de la caja
    val textY = atestadoBoxTop + (atestadoBoxHeight + textPaintSmall.textSize) / 2f
    drawTextEmbedded(canvas, atestadoNumeroText, textX, textY, textPaintSmall)

    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 2: Caja "FOLIO Nº" (arriba a la derecha)
    // ════════════════════════════════════════════════════════════════════════════
    val folioBoxTop = mmToPt(5.6f)
    // Distancia TOP al borde superior: 5.6 mm (ALINEADO CON ATESTADO)
    val folioBoxWidth = mmToPt(25f)
    val folioBoxHeight = mmToPt(6f)
    val folioBoxLeft = centerColRightX - mmToPt(3f) - folioBoxWidth
    // Distancia RIGHT al borde derecho: 3 mm
    val folioBoxRect = RectF(
        folioBoxLeft,
        folioBoxTop,
        folioBoxLeft + folioBoxWidth,
        folioBoxTop + folioBoxHeight
    )
    // Dimensiones: 25 mm ancho x 6 mm alto
    
    // Dibujar caja
    canvas.drawRect(folioBoxRect, boxPaint)
    
    // Dibujar texto centrado dentro de la caja
    val folioTextX = folioBoxLeft + 2f
    // Texto interior con padding: 2 mm desde el borde izquierdo de la caja
    val folioTextY = folioBoxTop + (folioBoxHeight + textPaintSmall.textSize) / 2f
    drawTextEmbedded(canvas, "FOLIO Nº", folioTextX, folioTextY, textPaintSmall)

    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 3: Líneas guía laterales (dividisoras de columnas)
    // ════════════════════════════════════════════════════════════════════════════
    val headerTop = topMargin
    val headerBottom = headerTop + mmToPt(HEADER_HEIGHT_MM)

    val guideTopY = mmToPt(5.6f)
    // Inicio de línea: TOP 5.6 mm
    val guideBottomY = A4_HEIGHT_PT - mmToPt(15f)
    // Fin de línea: BOTTOM 15 mm (desde abajo)
    val guideLeftX = mmToPt(15f)
    // Línea IZQUIERDA: distancia LEFT 20 mm
    val guideRightX = A4_WIDTH_PT - mmToPt(20f)
    // Línea DERECHA: distancia RIGHT 15 mm
    // Longitud vertical: ${(guideBottomY - guideTopY).toInt()} pt

    canvas.drawLine(guideLeftX, guideTopY, guideLeftX, guideBottomY, boxPaint)
    canvas.drawLine(guideRightX, guideTopY, guideRightX, guideBottomY, boxPaint)

    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 4: Escudo de España (esquina superior izquierda)
    // ════════════════════════════════════════════════════════════════════════════
    // Posición: esquina superior izquierda de columna izquierda
    // Distancia TOP: $headerTop pt (margent superior: 5.5 mm)
    // Distancia LEFT: $contentLeft pt (margen izquierdo: 5 mm)
    // Dimensiones: 8 mm ancho x variable altura (proporcional)
    loadBitmapFromAssets(context, "images/EscEspana.png")?.let { bmp ->
        val aspect = bmp.height.toFloat() / bmp.width.toFloat().coerceAtLeast(1f)
        val baseWidth = mmToPt(ESC_ESPANA_WIDTH_MM)
        val baseHeight = baseWidth * aspect
        val targetHeight = baseHeight + mmToPt(1f)
        val targetWidth = (targetHeight / aspect).coerceAtLeast(1f)
        drawBitmapAlignedTopLeft(
            canvas = canvas,
            bitmap = bmp,
            left = contentLeft,
            top = headerTop,
            targetWidth = targetWidth
        )
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 5: Escudo Guardia Civil (esquina superior derecha)
    // ════════════════════════════════════════════════════════════════════════════
    // Posición: esquina superior derecha de columna derecha
    // Distancia TOP: $headerTop pt (margen superior: 5.5 mm)
    // Distancia RIGHT: 1 mm del borde derecho de columna derecha
    // Dimensiones: 7 mm ancho x variable altura (proporcional)
    loadBitmapFromAssets(context, "images/EscGuardiaCivil.png")?.let { bmp ->
        val aspect = bmp.height.toFloat() / bmp.width.toFloat().coerceAtLeast(1f)
        val baseWidth = mmToPt(ESC_GC_WIDTH_MM)
        val baseHeight = baseWidth * aspect
        val targetHeight = (baseHeight - mmToPt(1f)).coerceAtLeast(mmToPt(1f))
        val targetWidth = (targetHeight / aspect).coerceAtLeast(1f)
        drawBitmapAlignedTopRight(
            canvas = canvas,
            bitmap = bmp,
            right = centerColRightX + mmToPt(1f) + targetWidth,
            top = headerTop,
            targetWidth = targetWidth
        )
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 6: Área de contenido principal (Título + Cuerpo de citación)
    // ════════════════════════════════════════════════════════════════════════════
    val titleTopY = mmToPt(TITLE_TOP_MM)
    // TOP del título: 13 mm
    val titleBaselineY = titleTopY - titlePaint.fontMetrics.ascent
    var cursorY = titleBaselineY
    val bodyLeft = centerColLeftX + 6f
    // LEFT del contenido: columna derecha + 6 pt (margent interior)
    val bodyRight = centerColRightX - 6f
    // RIGHT del contenido: columna derecha - 6 pt (margen interior)

    val textWidth = (bodyRight - bodyLeft).coerceAtLeast(1f)
    // Ancho disponible para texto: $(textWidth.toInt()) pt
    
    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 7: Sección de citación (si está disponible)
    // ════════════════════════════════════════════════════════════════════════════
    // - Título de citación
    // - Cuerpo principal (con reemplazo de placeholders)
    // - Secciones (títulos + contenido + opciones + items + información adicional)
    // - Cierre
    // - Enterado (título + texto)
    // Distancia LEFT: $bodyLeft pt (6 pt margen interior)
    // Distancia RIGHT: $bodyRight pt (6 pt margen interior)
    // TOP: desde $titleTopY pt (después del encabezado)
    // BOTTOM: antes de $bottomMargin pt
    citacionDocument?.let { citacion ->
        // Título
        drawTextEmbedded(canvas, citacion.titulo, bodyLeft, cursorY, titlePaint)
        cursorY += titlePaint.fontMetrics.descent + TITLE_BODY_GAP_PT
        
        // Cuerpo principal
        val processedCuerpo = replaceCitacionPlaceholders(
            citacion.cuerpoDescripcion,
            courtData ?: JuzgadoAtestadoData(),
            personData ?: PersonaInvestigadaData(),
            ocurrenciaData ?: OcurrenciaDelitData(),
            instructorTip,
            secretaryTip,
            instructorUnit
        )
        cursorY = drawMultilineText(canvas, processedCuerpo, bodyLeft, cursorY, textWidth, textPaint, 12f) + 10f
        
        // Secciones
        citacion.secciones.forEach { seccion ->
            if (seccion.titulo.isNotBlank()) {
                drawTextEmbedded(canvas, seccion.titulo, bodyLeft, cursorY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 11f
                    typeface = calibriBold
                })
                cursorY += 12f
            }
            
            if (seccion.contenido.isNotBlank()) {
                cursorY = drawMultilineText(canvas, seccion.contenido, bodyLeft, cursorY, textWidth, textPaint, 12f) + 6f
            }
            
            seccion.opciones.forEach { opcion ->
                if (opcion.descripcion.isNotBlank()) {
                    cursorY = drawMultilineText(canvas, opcion.descripcion, bodyLeft + 8f, cursorY, textWidth - 8f, textPaint, 12f) + 6f
                }
            }
            
            seccion.items.forEach { item ->
                if (item.descripcion.isNotBlank()) {
                    cursorY = drawMultilineText(canvas, item.descripcion, bodyLeft + 8f, cursorY, textWidth - 8f, textPaint, 12f) + 6f
                }
            }
            
            if (seccion.informacionAdicional.isNotBlank()) {
                cursorY = drawMultilineText(canvas, seccion.informacionAdicional, bodyLeft, cursorY, textWidth, textPaint, 12f) + 8f
            }
        }
        
        // Cierre
        if (citacion.cierre.isNotBlank()) {
            cursorY = drawMultilineText(canvas, citacion.cierre, bodyLeft, cursorY, textWidth, textPaint, 12f) + 10f
        }
        
        // Enterado
        if (citacion.enteradoTitulo.isNotBlank()) {
            drawTextEmbedded(canvas, citacion.enteradoTitulo, bodyLeft, cursorY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 11f
                typeface = calibriBold
            })
            cursorY += 12f
        }
        if (citacion.enteradoTexto.isNotBlank()) {
            cursorY = drawMultilineText(canvas, citacion.enteradoTexto, bodyLeft, cursorY, textWidth, textPaint, 12f) + 12f
        }
    }

    cursorY += 6f

    // ════════════════════════════════════════════════════════════════════════════
    // SECCIÓN DE FIRMAS (Elementos 8, 9 y 10)
    // ════════════════════════════════════════════════════════════════════════════
    val availableHeight = (A4_HEIGHT_PT - bottomMargin) - cursorY
    // Altura disponible para firmas: ${availableHeight.toInt()} pt
    // BOTTOM MARGIN (distancia desde abajo): $bottomMargin pt = ${mmToPt(BOTTOM_MARGIN_MM).toInt()} pt
    
    val signatureGap = 10f
    // Espacio entre fila superior e inferior de firmas: $signatureGap pt
    val spaceBetweenSignatures = mmToPt(20f)
    // Espacio entre cajas de Instructor y Secretario: 20 mm
    val maxSignatureBoxWidth = mmToPt(50f)
    val maxSignatureBoxHeight = mmToPt(40f)
    val previousTopRowHeight = (availableHeight * 0.42f).coerceAtLeast(92f)
    val previousBottomRowHeight = (availableHeight - previousTopRowHeight - signatureGap).coerceAtLeast(92f)
    val topRowHeight = (previousTopRowHeight - mmToPt(1f))
        .coerceAtLeast(80f)
        .coerceAtMost(maxSignatureBoxHeight)
    // Altura de fila superior (Instructor + Secretario): +2 mm respecto al ajuste anterior
    val bottomRowHeight = (previousBottomRowHeight - mmToPt(3f))
        .coerceAtLeast(80f)
        .coerceAtMost(maxSignatureBoxHeight)
    // Altura de fila inferior (Investigado): reducida 3 mm
    
    val topRowWidth = centerColRightX - centerColLeftX
    // Ancho disponible fila superior: ${topRowWidth.toInt()} pt
    val topBoxWidth = ((topRowWidth - spaceBetweenSignatures) / 2f)
        .coerceAtLeast(40f)
        .coerceAtMost(maxSignatureBoxWidth)
    // Ancho de cada caja (Instructor y Secretario): calculado para dejar 20 mm entre ellas
    val investigatedBoxWidth = ((centerColRightX - centerColLeftX) / 3f)
        .coerceAtLeast(40f)
        .coerceAtMost(maxSignatureBoxWidth)
    // Ancho de caja investigado: ${investigatedBoxWidth.toInt()} pt

    // Posicionamiento centralizado: calcular offset para centrar las dos cajas
    val totalWidthNeeded = (2 * topBoxWidth) + spaceBetweenSignatures
    val leftPadding = (topRowWidth - totalWidthNeeded) / 2f

    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 8: Caja de firma INSTRUCTOR (superior izquierda)
    // ════════════════════════════════════════════════════════════════════════════
    val instructorRect = RectF(
        centerColLeftX + leftPadding,
        cursorY,
        centerColLeftX + leftPadding + topBoxWidth,
        cursorY + topRowHeight
    )
    // TOP: ${cursorY.toInt()} pt
    // LEFT: ${(centerColLeftX + leftPadding).toInt()} pt (centrada)
    // Altura: ajustada (+2 mm respecto al ajuste previo)
    // BOTTOM: ${(A4_HEIGHT_PT - (cursorY + topRowHeight)).toInt()} pt
    
    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 9: Caja de firma SECRETARIO (superior derecha)
    // ════════════════════════════════════════════════════════════════════════════
    val secretaryRect = RectF(
        centerColLeftX + leftPadding + topBoxWidth + spaceBetweenSignatures,
        cursorY,
        centerColLeftX + leftPadding + topBoxWidth + spaceBetweenSignatures + topBoxWidth,
        cursorY + topRowHeight
    )
    // TOP: ${cursorY.toInt()} pt
    // LEFT: ${(centerColLeftX + leftPadding + topBoxWidth + spaceBetweenSignatures).toInt()} pt
    // Espacio desde Instructor: 20 mm
    // Altura: ajustada (+2 mm respecto al ajuste previo)
    // BOTTOM: ${(A4_HEIGHT_PT - (cursorY + topRowHeight)).toInt()} pt

    drawSignatureBlock(
        canvas = canvas,
        rect = instructorRect,
        title = "Instructor",
        tip = instructorTip,
        content = signatures[PdfSignatureSlot.INSTRUCTOR],
        fallbackText = "Sin firma",
        textPaint = textPaint,
        tipPaint = textPaintSmall
    )
    drawSignatureBlock(
        canvas = canvas,
        rect = secretaryRect,
        title = "Secretario",
        tip = secretaryTip,
        content = signatures[PdfSignatureSlot.SECRETARY],
        fallbackText = "Sin firma",
        textPaint = textPaint,
        tipPaint = textPaintSmall
    )

    cursorY = instructorRect.bottom + signatureGap

    // ════════════════════════════════════════════════════════════════════════════
    // ELEMENTO 10: Caja de firma INVESTIGADO (inferior centrada)
    // ════════════════════════════════════════════════════════════════════════════
    val investigatedLeftPadding = (topRowWidth - investigatedBoxWidth) / 2f
    val investigatedRect = RectF(
        centerColLeftX + investigatedLeftPadding,
        cursorY,
        centerColLeftX + investigatedLeftPadding + investigatedBoxWidth,
        (cursorY + bottomRowHeight).coerceAtMost(A4_HEIGHT_PT - bottomMargin)
    )
    // TOP: ${cursorY.toInt()} pt (después de gap)
    // LEFT: ${(centerColLeftX + investigatedLeftPadding).toInt()} pt (centrada)
    // Ancho: ${investigatedBoxWidth.toInt()} pt
    // Altura: reducida 3 mm desde original
    // BOTTOM: ${(A4_HEIGHT_PT - investigatedRect.bottom).toInt()} pt (~15 mm)
    
    drawSignatureBlock(
        canvas = canvas,
        rect = investigatedRect,
        title = "Investigado",
        tip = null,
        content = signatures[PdfSignatureSlot.INVESTIGATED],
        fallbackText = investigatedNoSignText,
        textPaint = textPaint,
        tipPaint = textPaintSmall
    )


    pdfDocument.finishPage(page)

    val directory = File(context.filesDir, "atestados").apply { mkdirs() }
    directory.listFiles { candidate ->
        candidate.isFile && candidate.extension.equals("pdf", ignoreCase = true)
    }?.forEach { existingPdf ->
        if (existingPdf.name != GENERATED_ATESTADO_PDF_NAME) {
            existingPdf.delete()
        }
    }
    val file = File(directory, GENERATED_ATESTADO_PDF_NAME)

    FileOutputStream(file).use { output ->
        pdfDocument.writeTo(output)
    }
    pdfDocument.close()

    return AtestadoPdfResult(file = file, createdAtMillis = now)
}

private const val GENERATED_ATESTADO_PDF_NAME = "citaciónindividual.pdf"

/** Carga tipografía desde assets (retorna null si falla). */
private fun loadTypefaceFromAssets(context: Context, path: String): Typeface? =
    runCatching { Typeface.createFromAsset(context.assets, path) }.getOrNull()

/** Carga bitmap desde assets (retorna null si falla). */
private fun loadBitmapFromAssets(context: Context, path: String): Bitmap? =
    runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it) } }.getOrNull()

/** Dibuja bitmap escalado alineado arriba-izquierda en coordenada base. */
private fun drawBitmapAlignedTopLeft(
    canvas: android.graphics.Canvas,
    bitmap: Bitmap,
    left: Float,
    top: Float,
    targetWidth: Float
) {
    val aspect = bitmap.height.toFloat() / bitmap.width.toFloat().coerceAtLeast(1f)
    val dst = RectF(left, top, left + targetWidth, top + targetWidth * aspect)
    canvas.drawBitmap(bitmap, null, dst, null)
}

/** Dibuja bitmap escalado alineado arriba-derecha en coordenada base. */
private fun drawBitmapAlignedTopRight(
    canvas: android.graphics.Canvas,
    bitmap: Bitmap,
    right: Float,
    top: Float,
    targetWidth: Float
) {
    val aspect = bitmap.height.toFloat() / bitmap.width.toFloat().coerceAtLeast(1f)
    val dst = RectF(right - targetWidth, top, right, top + targetWidth * aspect)
    canvas.drawBitmap(bitmap, null, dst, null)
}

/** Renderiza una caja de firma con etiqueta y contenido (imagen o texto). */
private fun drawSignatureBlock(
    canvas: android.graphics.Canvas,
    rect: RectF,
    title: String,
    tip: String?,
    content: PdfSignatureContent<ImageBitmap>?,
    fallbackText: String,
    textPaint: Paint,
    tipPaint: Paint
) {
    drawTextEmbedded(canvas, title, rect.left + 8f, rect.top + 14f, textPaint)

    val tipText = tip?.takeIf { it.isNotBlank() }?.let { "TIP: $it" }
    val tipAreaHeight = if (tipText == null) 0f else 18f
    val targetTop = rect.top + 22f
    val targetBottom = rect.bottom - 8f - tipAreaHeight
    val targetLeft = rect.left + 8f
    val targetRight = rect.right - 8f

    when (content) {
        is PdfSignatureContent.Image -> {
            val bitmap = content.value.asAndroidBitmap()
            val targetWidth = (targetRight - targetLeft).coerceAtLeast(1f)
            val targetHeight = (targetBottom - targetTop).coerceAtLeast(1f)
            val srcWidth = bitmap.width.toFloat().coerceAtLeast(1f)
            val srcHeight = bitmap.height.toFloat().coerceAtLeast(1f)
            // Mantiene la proporcion de la imagen de firma.
            val scale = minOf(targetWidth / srcWidth, targetHeight / srcHeight)
            val drawWidth = srcWidth * scale
            val drawHeight = srcHeight * scale
            val dx = targetLeft + (targetWidth - drawWidth) / 2f
            val dy = targetTop + (targetHeight - drawHeight) / 2f
            canvas.drawBitmap(bitmap, null, RectF(dx, dy, dx + drawWidth, dy + drawHeight), null)
        }

        is PdfSignatureContent.Text -> {
            val text = content.value.ifBlank { fallbackText }
            drawTextEmbedded(canvas, text, rect.left + 12f, rect.centerY(), textPaint)
        }

        null -> {
            drawTextEmbedded(canvas, fallbackText, rect.left + 12f, rect.centerY(), textPaint)
        }
    }

    if (tipText != null) {
        val tipX = rect.left + ((rect.width() - tipPaint.measureText(tipText)) / 2f)
        val tipY = rect.bottom - 8f
        drawTextEmbedded(canvas, tipText, tipX, tipY, tipPaint)
    }
}

/** Dibuja texto multilínea con ajuste por ancho máximo. */
private fun drawMultilineText(
    canvas: android.graphics.Canvas,
    text: String,
    x: Float,
    y: Float,
    maxWidth: Float,
    paint: Paint,
    lineHeight: Float
): Float {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    val current = StringBuilder()
    for (word in words) {
        val test = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(test) <= maxWidth) {
            if (current.isEmpty()) current.append(word) else { current.append(' '); current.append(word) }
        } else {
            if (current.isNotEmpty()) lines.add(current.toString())
            current.clear(); current.append(word)
        }
    }
    if (current.isNotEmpty()) lines.add(current.toString())
    var cy = y
    for (line in lines) { drawTextEmbedded(canvas, line, x, cy, paint); cy += lineHeight }
    return cy
}

/** Dibuja texto embebido respetando codificación/normalización usada en PDF. */
private fun drawTextEmbedded(
    canvas: android.graphics.Canvas,
    text: String,
    x: Float,
    y: Float,
    paint: Paint
) {
    if (text.isEmpty()) return
    val path = Path()
    paint.getTextPath(text, 0, text.length, x, y, path)
    canvas.drawPath(path, paint)
}

