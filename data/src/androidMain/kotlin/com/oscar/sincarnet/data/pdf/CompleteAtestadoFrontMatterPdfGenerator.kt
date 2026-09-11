package com.oscar.sincarnet.data.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.oscar.sincarnet.domain.model.AtestadoInicioModalData
import com.oscar.sincarnet.domain.model.JuzgadoAtestadoData
import com.oscar.sincarnet.domain.model.OcurrenciaDelitData
import com.oscar.sincarnet.domain.model.PersonaInvestigadaData
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PAGE_WIDTH = 595f
private const val PAGE_HEIGHT = 842f
private const val SIDE_MARGIN = 57f
private const val TOP_MARGIN = 35f
private const val MARGIN = SIDE_MARGIN

/** Genera las páginas iniciales del atestado completo. */
fun generateCompleteAtestadoFrontMatterPdf(
    context: Context,
    courtData: JuzgadoAtestadoData,
    personData: PersonaInvestigadaData,
    occurrenceData: OcurrenciaDelitData,
    inicioData: AtestadoInicioModalData,
    instructorTip: String,
    secretaryTip: String,
    instructorUnit: String,
    includeAnnexCover: Boolean = false,
    onlyAnnexCover: Boolean = false
): File {
    val document = PdfDocument()
    var pageNumber = 1
    val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10f
        typeface = Typeface.create("Arial", Typeface.NORMAL)
    }
    val bold = Paint(regular).apply { typeface = Typeface.create("Arial", Typeface.BOLD) }
    val small = Paint(regular).apply { textSize = 8f }
    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    fun page(): PdfDocument.Page = document.startPage(
        PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber++).create()
    )

    if (!onlyAnnexCover) {
        drawPortada(
            page = page(), context = context, regular = regular, bold = bold, line = line,
            courtData = courtData, occurrenceData = occurrenceData, inicioData = inicioData, instructorTip = instructorTip,
            secretaryTip = secretaryTip, instructorUnit = instructorUnit
        ).also { document.finishPage(it) }

        drawResumen(
            page = page(), context = context, regular = regular, bold = bold, small = small, line = line,
            courtData = courtData, personData = personData, occurrenceData = occurrenceData,
            inicioData = inicioData, instructorTip = instructorTip, secretaryTip = secretaryTip,
            instructorUnit = instructorUnit
        ).also { document.finishPage(it) }
    }

    if (includeAnnexCover || onlyAnnexCover) {
        drawPortadaAnexo(page = page(), context = context, bold = bold).also { document.finishPage(it) }
    }

    val output = File(context.cacheDir, "atestado_front_matter_${System.currentTimeMillis()}.pdf")
    FileOutputStream(output).use { document.writeTo(it) }
    document.close()
    return output
}

private fun drawPortada(
    page: PdfDocument.Page,
    context: Context,
    regular: Paint,
    bold: Paint,
    line: Paint,
    courtData: JuzgadoAtestadoData,
    occurrenceData: OcurrenciaDelitData,
    inicioData: AtestadoInicioModalData,
    instructorTip: String,
    secretaryTip: String,
    instructorUnit: String
): PdfDocument.Page {
    val canvas = page.canvas
    canvas.drawColor(Color.WHITE)
    val towerWidth = drawFullHeightTower(context, canvas)

    val headerLeft = maxOf(SIDE_MARGIN + 24f, towerWidth + 24f)
    drawLogoWithHeight(
        context = context,
        canvas = canvas,
        path = "images/EscEspana_bw.png",
        x = headerLeft,
        y = TOP_MARGIN + 8f,
        height = 2.4f * 72f / 2.54f
    )
    bold.textSize = 11f
    canvas.drawText("MINISTERIO", headerLeft + 68f, TOP_MARGIN + 27f, bold)
    canvas.drawText("DEL INTERIOR", headerLeft + 68f, TOP_MARGIN + 42f, bold)

    val rightHeaderImageLeft = PAGE_WIDTH - SIDE_MARGIN - 170f
    val aspaWidth = 1.008f * 72f / 2.54f
    drawLogo(context, canvas, "images/EscAspas_bw.png", rightHeaderImageLeft, TOP_MARGIN + 18f, aspaWidth)
    bold.textSize = 10f
    val rightTextLeft = rightHeaderImageLeft + aspaWidth + 10f
    val guardiaCivilText = "GUARDIA CIVIL"
    canvas.drawText(guardiaCivilText, rightTextLeft, TOP_MARGIN + 30f, bold)
    line.strokeWidth = 0.75f
    canvas.drawLine(
        rightTextLeft,
        TOP_MARGIN + 35f,
        rightTextLeft + bold.measureText(guardiaCivilText),
        TOP_MARGIN + 35f,
        line
    )
    bold.textSize = 6f
    canvas.drawText("DIRECCION GENERAL", rightTextLeft, TOP_MARGIN + 44f, bold)
    bold.textSize = 9f
    canvas.drawText("Dirección Adjunta Operativa", rightHeaderImageLeft, TOP_MARGIN + 86f, bold)
    canvas.drawText("Agrupación de Tráfico", rightHeaderImageLeft, TOP_MARGIN + 101f, bold)

    val (sector, destacamento) = splitActingUnit(instructorUnit)
    val contentLeft = maxOf(SIDE_MARGIN, towerWidth + 12f)
    val contentRight = PAGE_WIDTH - SIDE_MARGIN
    val labelX = contentLeft
    val valueLeft = contentLeft + 210f
    var y = 145f
    bold.textSize = 11f
    regular.textSize = 11f

    val unitLabel = "Unidad ACTUANTE:"
    canvas.drawText(unitLabel, valueLeft - bold.measureText(unitLabel) - 8f, y + 17f, bold)
    canvas.drawRect(valueLeft, y, contentRight, y + 42f, line)
    canvas.drawText(sector, valueLeft + 8f, y + 16f, bold)
    canvas.drawText(destacamento, valueLeft + 8f, y + 33f, bold)
    y += 55f

    drawLogo(context, canvas, "images/escudo_bw.png", (PAGE_WIDTH - 70f) / 2f, y, 70f, 95f)
    y += 112f

    val courtLabel = "JUZGADO"
    canvas.drawText(courtLabel, valueLeft - bold.measureText(courtLabel) - 8f, y + 17f, bold)
    val courtName = listOf(
        courtData.sedeNombre.ifBlank { courtData.municipioNombre },
        courtData.municipioNombre.takeIf { it.isNotBlank() && it != courtData.sedeNombre }
    ).filterNotNull().filter { it.isNotBlank() }.joinToString(", ")
    drawWrapped(
        canvas,
        courtName,
        RectF(valueLeft, y, contentRight, y + 42f),
        bold,
        center = false
    )
    y += 52f

    drawLabeledValueBox(canvas, "ATESTADO NÚMERO", courtData.numeroDiligencias, y, valueLeft, contentRight, bold, line)
    y += 48f
    drawLabeledValueBox(canvas, "INSTRUCTOR T.I.P NÚM.:", instructorTip, y, valueLeft, contentRight, bold, line, centeredValue = true)
    y += 48f
    drawLabeledValueBox(canvas, "SECRETARIO T.I.P NÚM.:", secretaryTip, y, valueLeft, contentRight, bold, line, centeredValue = true)
    y += 68f

    val relato = "ATESTADO INSTRUIDO POR UN PRESUNTO DELITO CONTRA LA SEGURIDAD VIAL CONSISTENTE EN CONDUCIR UN VEHÍCULO NO HABIENDO OBTENIDO PERMISO O LICENCIA DE CONDUCCIÓN QUE LE HABILITE A ELLO."
    bold.textSize = 16f
    drawWrapped(canvas, relato, RectF(contentLeft, y, contentRight, y + 125f), bold, center = false)
    y += 135f

    bold.textSize = 10f
    drawLabeledValueBox(canvas, "FECHA:", formatPortadaDate(occurrenceData.fecha), y, valueLeft, contentRight, bold, line)
    return page
}

private fun drawResumen(
    page: PdfDocument.Page,
    context: Context,
    regular: Paint,
    bold: Paint,
    small: Paint,
    line: Paint,
    courtData: JuzgadoAtestadoData,
    personData: PersonaInvestigadaData,
    occurrenceData: OcurrenciaDelitData,
    inicioData: AtestadoInicioModalData,
    instructorTip: String,
    secretaryTip: String,
    instructorUnit: String
): PdfDocument.Page {
    val canvas = page.canvas
    canvas.drawColor(Color.WHITE)
    val left = 80f
    val right = 541f
    val smallBold = Paint(bold).apply { textSize = 8f }
    val body = Paint(regular).apply { textSize = 8.5f }
    val bodyBold = Paint(bold).apply { textSize = 8.5f }
    val headerPaint = Paint(bold).apply { textSize = 8f }

    // Reuse the exact header used by normal diligences.
    drawCitacionHeader(
        canvas = canvas,
        context = context,
        courtData = courtData,
        textPaintSmall = headerPaint,
        boxPaint = line,
        centerColLeftX = pointsFromMm(15f) + pointsFromMm(10f),
        centerColRightX = 538f,
        contentLeft = pointsFromMm(5f),
        topMargin = pointsFromMm(5.5f)
    )
    canvas.drawText("INSTRUCTOR TIP:", 226f, 58f, headerPaint)
    canvas.drawText(instructorTip, 286f, 58f, headerPaint)
    canvas.drawText("SECRETARIO TIP:", 393f, 58f, headerPaint)
    canvas.drawText(secretaryTip, 474f, 58f, headerPaint)

    val (sector, destacamento) = splitActingUnit(instructorUnit)
    canvas.drawRect(left, 80f, right, 107f, line)
    canvas.drawRect(left, 80f, 280f, 107f, line)
    canvas.drawText(sector.ifBlank { "SECTOR" }.uppercase(), 87f, 92f, smallBold)
    canvas.drawText(destacamento.ifBlank { "DESTACAMENTO" }.uppercase(), 87f, 103f, smallBold)
    canvas.drawText("EQUIPO DE ATESTADOS DE ${destacamento.ifBlank { instructorTip }}", 295f, 97f, smallBold)

    val boxTop = 120f
    val boxBottom = 770f
    canvas.drawRect(left, boxTop, right, boxBottom, line)
    canvas.drawRect(left, boxTop, right, boxTop + 21f, line)
    centerTextIn(canvas, "DOCUMENTO- RESUMEN DEL ATESTADO INSTRUIDO", left, right, boxTop + 15f, bodyBold)

    val quick = courtData.tipoJuicio.contains("rapido", ignoreCase = true)
    val detained = courtData.tipoJuicio.contains("detenido", ignoreCase = true)
    val options = listOf(
        "JUICIO RÁPIDO POR DELITO CON DETENIDO (J.R.D.)",
        "JUICIO RÁPIDO POR DELITO SIN DETENIDO (J.R.S.D.)",
        "JUICIO POR DELITO CON DETENIDO",
        "JUICIO POR DELITO SIN DETENIDO"
    )
    val selected = when {
        quick && detained -> 0
        quick -> 1
        detained -> 2
        else -> 3
    }
    options.forEachIndexed { index, option ->
        val optionY = boxTop + 55f + index * 38f
        canvas.drawRect(148f, optionY, 181f, optionY + 22f, line)
        if (index == selected) canvas.drawText("X", 159f, optionY + 16f, bodyBold)
        canvas.drawText(option, 185f, optionY + 15f, body)
    }

    val separatorY = 315f
    canvas.drawLine(left, separatorY, right, separatorY, line)
    val fullName = listOf(personData.firstName, personData.lastName1, personData.lastName2)
        .filter { it.isNotBlank() }.joinToString(" ")
    val address = listOf(occurrenceData.carretera, occurrenceData.pk, occurrenceData.localidad)
        .filter { it.isNotBlank() }.joinToString(", ")
    summaryField(canvas, "ATESTADO Nº", courtData.numeroDiligencias, left + 8f, separatorY + 25f, body, bodyBold)
    summaryField(canvas, "DE FECHA:", summaryDate(occurrenceData.fecha), left + 8f, separatorY + 59f, body, bodyBold)
    summaryField(canvas, "TIPO DE INFRACCIÓN PENAL:", "Presunto delito contra la Seguridad Vial, conducir careciendo de permiso.", left + 8f, separatorY + 93f, body, bodyBold, 190f)
    summaryField(canvas, "FECHA Y LUGAR DE COMISIÓN DEL HECHO:", "$address, ${occurrenceData.hora} ${occurrenceData.fecha}", left + 8f, separatorY + 135f, body, bodyBold, 205f)
    canvas.drawText("OBSERVACIONES DE TRÁMITE:", left + 8f, separatorY + 183f, body)

    canvas.drawText("CITACIONES :", left + 8f, separatorY + 220f, bodyBold)
    val trialName = if (quick) "JUICIO RÁPIDO" else "JUICIO ORDINARIO"
    canvas.drawText(trialName, left + 220f, separatorY + 220f, bodyBold)
    canvas.drawText("ENCARTADOS", left + 22f, separatorY + 255f, bodyBold)
    canvas.drawText("DÍA / HORA", left + 195f, separatorY + 255f, bodyBold)
    canvas.drawText("SITUACIÓN-CIRCUNSTANCIA", left + 295f, separatorY + 255f, bodyBold)
    canvas.drawText(fullName, left + 20f, separatorY + 282f, bodyBold)
    canvas.drawText(personData.documentIdentification, left + 50f, separatorY + 295f, bodyBold)
    canvas.drawText(courtData.fechaJuicioRapido.ifBlank { occurrenceData.fecha }, left + 195f, separatorY + 282f, bodyBold)
    canvas.drawText(courtData.horaJuicioRapido.ifBlank { occurrenceData.hora }, left + 215f, separatorY + 295f, bodyBold)
    canvas.drawText("INVESTIGADO", left + 330f, separatorY + 282f, bodyBold)

    canvas.drawText("Otros intervinientes:", left + 5f, boxBottom - 57f, body)
    canvas.drawText("GUARDIA CIVIL INSTRUCTOR: $instructorTip, GUARDIA CIVIL SECRETARIO: $secretaryTip", left + 5f, boxBottom - 43f, body)
    canvas.drawText("OBSERVACIONES:", left + 5f, boxBottom - 22f, body)
    canvas.drawText("Teléfono contacto investigado: ${personData.phone}. Email: ${personData.email}", left + 5f, boxBottom - 8f, body)
    return page
}

private fun drawPortadaAnexo(page: PdfDocument.Page, context: Context, bold: Paint): PdfDocument.Page {
    val canvas = page.canvas
    canvas.drawColor(Color.WHITE)
    drawFullHeightTower(context, canvas)
    bold.textSize = 22f
    bold.typeface = Typeface.create("Calibri", Typeface.BOLD)
    centerText(canvas, "Anexos", PAGE_HEIGHT / 2f, bold)
    return page
}

private fun centerTextIn(canvas: Canvas, text: String, left: Float, right: Float, y: Float, paint: Paint) {
    canvas.drawText(text, left + (right - left - paint.measureText(text)) / 2f, y, paint)
}

private fun summaryField(
    canvas: Canvas,
    label: String,
    value: String,
    x: Float,
    y: Float,
    regular: Paint,
    bold: Paint,
    valueXOffset: Float = 150f
) {
    canvas.drawText(label, x, y, regular)
    drawWrapped(canvas, value, RectF(x + valueXOffset, y - 12f, 533f, y + 28f), bold, center = false)
}

private fun summaryDate(value: String): String {
    val parsed = runCatching {
        LocalDate.parse(value, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
    }.getOrNull() ?: return value
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    return parsed.format(formatter)
}

private fun tableRow(
    canvas: Canvas,
    top: Float,
    bottom: Float,
    label: String,
    value: String,
    regular: Paint,
    bold: Paint,
    line: Paint
) {
    val split = MARGIN + 175f
    canvas.drawRect(MARGIN, top, PAGE_WIDTH - MARGIN, bottom, line)
    canvas.drawLine(split, top, split, bottom, line)
    val labelX = split - 8f - bold.measureText(label)
    drawWrapped(canvas, label, RectF(labelX, top + 7f, split - 8f, bottom - 5f), bold, false)
    drawWrapped(canvas, value.ifBlank { " " }, RectF(split + 8f, top + 7f, PAGE_WIDTH - MARGIN - 8f, bottom - 5f), regular, false)
}

private fun drawWrapped(canvas: Canvas, value: String, bounds: RectF, paint: Paint, center: Boolean) {
    val words = value.replace("\n", " \n").split(" ")
    var line = StringBuilder()
    var y = bounds.top + paint.textSize
    words.forEach { word ->
        if (word == "\n") {
            drawLine(canvas, line.toString(), bounds, y, paint, center)
            line = StringBuilder(); y += paint.textSize + 3f
        } else {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > bounds.width() && line.isNotEmpty()) {
                drawLine(canvas, line.toString(), bounds, y, paint, center)
                line = StringBuilder(word); y += paint.textSize + 3f
            } else line = StringBuilder(candidate)
        }
    }
    if (line.isNotEmpty() && y <= bounds.bottom + paint.textSize) drawLine(canvas, line.toString(), bounds, y, paint, center)
}

private fun drawLine(canvas: Canvas, text: String, bounds: RectF, y: Float, paint: Paint, center: Boolean) {
    val x = if (center) bounds.left + (bounds.width() - paint.measureText(text)) / 2f else bounds.left
    canvas.drawText(text, x, y, paint)
}

private fun centerText(canvas: Canvas, text: String, y: Float, paint: Paint) {
    canvas.drawText(text, (PAGE_WIDTH - paint.measureText(text)) / 2f, y, paint)
}

private fun drawLogo(context: Context, canvas: Canvas, path: String, x: Float, y: Float, width: Float, forcedHeight: Float? = null) {
    runCatching {
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)?.let { bitmap ->
                val height = forcedHeight ?: width * bitmap.height.toFloat() / bitmap.width.toFloat().coerceAtLeast(1f)
                canvas.drawBitmap(bitmap, null, RectF(x, y, x + width, y + height), null)
                bitmap.recycle()
            }
        }
    }
}

private fun drawLogoWithHeight(
    context: Context,
    canvas: Canvas,
    path: String,
    x: Float,
    y: Float,
    height: Float
) {
    runCatching {
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)?.let { bitmap ->
                val width = height * bitmap.width.toFloat() /
                    bitmap.height.toFloat().coerceAtLeast(1f)
                canvas.drawBitmap(bitmap, null, RectF(x, y, x + width, y + height), null)
                bitmap.recycle()
            }
        }
    }
}

private fun drawFullHeightTower(context: Context, canvas: Canvas): Float {
    var width = 0f
    runCatching {
        context.assets.open("images/torre.png").use { stream ->
            BitmapFactory.decodeStream(stream)?.let { bitmap ->
                val targetHeight = 858f
                width = targetHeight * bitmap.width.toFloat() /
                    bitmap.height.toFloat().coerceAtLeast(1f)
                canvas.drawBitmap(
                    bitmap,
                    null,
                    RectF(0f, -15f, width, -15f + targetHeight),
                    null
                )
                bitmap.recycle()
            }
        }
    }
    return width
}

private fun drawLabeledValueBox(
    canvas: Canvas,
    label: String,
    value: String,
    y: Float,
    valueLeft: Float,
    contentRight: Float,
    bold: Paint,
    line: Paint,
    centeredValue: Boolean = false
) {
    canvas.drawText(label, valueLeft - bold.measureText(label) - 8f, y + 17f, bold)
    canvas.drawRect(valueLeft, y, contentRight, y + 28f, line)
    val x = if (centeredValue) {
        valueLeft + (contentRight - valueLeft - bold.measureText(value)) / 2f
    } else {
        valueLeft + 8f
    }
    canvas.drawText(value, x, y + 18f, bold)
}

private fun splitActingUnit(value: String): Pair<String, String> {
    val normalized = value.trim()
    val marker = Regex("\\s+(?:/|-|;)\\s+|\\n+").find(normalized)
    if (marker != null) {
        return normalized.substring(0, marker.range.first).trim() to
            normalized.substring(marker.range.last + 1).trim()
    }
    val destacamentoIndex = normalized.uppercase().indexOf("DESTACAMENTO")
    if (destacamentoIndex > 0) {
        return normalized.substring(0, destacamentoIndex).trim() to normalized.substring(destacamentoIndex).trim()
    }
    return normalized to ""
}

private fun formatPortadaDate(value: String): String {
    val parts = value.split("-")
    return if (parts.size == 3 && parts[0].length == 2) {
        "${parts[0]}/${parts[1]}/${parts[2]}"
    } else value
}

private fun pointsFromMm(value: Float): Float = value * 72f / 25.4f
