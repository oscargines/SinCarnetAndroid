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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

private const val A4_WIDTH_PT = 595f
private const val A4_HEIGHT_PT = 842f
private const val CONTENT_TOP_MM = 17f
private const val BOTTOM_MARGIN_MM = 15f
private const val MIN_SIGNATURE_SECTION_HEIGHT_PT = 170f

internal fun generateAtestadoContinuousPdf(
    context: Context,
    courtData: JuzgadoAtestadoData,
    personData: PersonaInvestigadaData,
    ocurrenciaData: OcurrenciaDelitData,
    vehicleData: VehiculoData,
    manifestacionData: ManifestacionData,
    signatures: Map<PdfSignatureSlot, PdfSignatureContent<ImageBitmap>> = emptyMap(),
    investigatedNoSignText: String = NO_DESEA_FIRMAR_TEXT,
    instructorTip: String,
    secretaryTip: String,
    instructorUnit: String
): AtestadoPdfResult {
    val now = System.currentTimeMillis()
    val pdfDocument = PdfDocument()

    val orderedDocuments = mutableListOf<CitacionDocument>()
    val staticFiles = listOf(
        "01inicio.json",
        "02derechos.json",
        "03letradogratis.json",
        "04manifestacion.json",
        "05inmovilizacion.json"
    )
    staticFiles.forEach { fileName ->
        orderedDocuments += loadAtestadoTemplateAsCitacion(context, fileName)
    }
    orderedDocuments += loadCitacionDocument(context, courtData.tipoJuicio)

    val calibriRegular = loadTypefaceFromAssets(context, "fonts/calibri-regular.ttf")
    val calibriBold = loadTypefaceFromAssets(context, "fonts/calibri-bold.ttf") ?: calibriRegular

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 12f
        typeface = calibriBold
    }
    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 11f
        typeface = calibriBold
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 11f
        typeface = calibriRegular
    }
    val textPaintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9f
        typeface = calibriRegular
    }
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }

    var pageNumber = 1
    orderedDocuments.forEach { document ->
        var pageState = startContinuousPage(
            pdfDocument = pdfDocument,
            context = context,
            pageNumber = pageNumber,
            courtData = courtData,
            textPaintSmall = textPaintSmall,
            boxPaint = boxPaint
        )
        pageNumber += 1

        val maxContentY = A4_HEIGHT_PT - mmToPt(BOTTOM_MARGIN_MM)

        fun drawBlock(text: String, paint: Paint, lineHeight: Float, bottomSpacing: Float = 0f) {
            if (text.isBlank()) return
            val lines = wrapTextLines(text, pageState.right - pageState.left, paint)
            lines.forEach { line ->
                if (pageState.cursorY + lineHeight > maxContentY) {
                    pdfDocument.finishPage(pageState.page)
                    pageState = startContinuousPage(
                        pdfDocument = pdfDocument,
                        context = context,
                        pageNumber = pageNumber,
                        courtData = courtData,
                        textPaintSmall = textPaintSmall,
                        boxPaint = boxPaint
                    )
                    pageNumber += 1
                }
                if (line.isNotEmpty()) {
                    drawTextEmbedded(pageState.canvas, line, pageState.left, pageState.cursorY, paint)
                }
                pageState.cursorY += lineHeight
            }
            pageState.cursorY += bottomSpacing
        }

        val title = replaceCitacionPlaceholders(
            text = document.titulo,
            courtData = courtData,
            personData = personData,
            ocurrenciaData = ocurrenciaData,
            instructorTip = instructorTip,
            secretaryTip = secretaryTip,
            instructorUnit = instructorUnit,
            vehicleData = vehicleData,
            manifestacionData = manifestacionData
        )
        drawBlock(title, titlePaint, 14f, 10f)

        val body = replaceCitacionPlaceholders(
            text = document.cuerpoDescripcion,
            courtData = courtData,
            personData = personData,
            ocurrenciaData = ocurrenciaData,
            instructorTip = instructorTip,
            secretaryTip = secretaryTip,
            instructorUnit = instructorUnit,
            vehicleData = vehicleData,
            manifestacionData = manifestacionData
        )
        drawBlock(body, textPaint, 13f, 10f)

        document.secciones.forEach { section ->
            val sectionTitle = replaceCitacionPlaceholders(
                text = section.titulo,
                courtData = courtData,
                personData = personData,
                ocurrenciaData = ocurrenciaData,
                instructorTip = instructorTip,
                secretaryTip = secretaryTip,
                instructorUnit = instructorUnit,
                vehicleData = vehicleData,
                manifestacionData = manifestacionData
            )
            drawBlock(sectionTitle, headingPaint, 13f, 4f)

            val sectionContent = replaceCitacionPlaceholders(
                text = section.contenido,
                courtData = courtData,
                personData = personData,
                ocurrenciaData = ocurrenciaData,
                instructorTip = instructorTip,
                secretaryTip = secretaryTip,
                instructorUnit = instructorUnit,
                vehicleData = vehicleData,
                manifestacionData = manifestacionData
            )
            drawBlock(sectionContent, textPaint, 13f, 6f)

            section.items.forEach { item ->
                val itemText = replaceCitacionPlaceholders(
                    text = item.descripcion,
                    courtData = courtData,
                    personData = personData,
                    ocurrenciaData = ocurrenciaData,
                    instructorTip = instructorTip,
                    secretaryTip = secretaryTip,
                    instructorUnit = instructorUnit,
                    vehicleData = vehicleData,
                    manifestacionData = manifestacionData
                )
                if (itemText.isNotBlank()) {
                    val itemLines = wrapTextLines("- $itemText", (pageState.right - pageState.left) - 12f, textPaint)
                    itemLines.forEach { line ->
                        if (pageState.cursorY + 13f > maxContentY) {
                            pdfDocument.finishPage(pageState.page)
                            pageState = startContinuousPage(
                                pdfDocument = pdfDocument,
                                context = context,
                                pageNumber = pageNumber,
                                courtData = courtData,
                                textPaintSmall = textPaintSmall,
                                boxPaint = boxPaint
                            )
                            pageNumber += 1
                        }
                        if (line.isNotEmpty()) {
                            drawTextEmbedded(pageState.canvas, line, pageState.left + 12f, pageState.cursorY, textPaint)
                        }
                        pageState.cursorY += 13f
                    }
                    pageState.cursorY += 4f
                }
            }

            section.opciones.forEach { option ->
                val optionText = replaceCitacionPlaceholders(
                    text = option.descripcion,
                    courtData = courtData,
                    personData = personData,
                    ocurrenciaData = ocurrenciaData,
                    instructorTip = instructorTip,
                    secretaryTip = secretaryTip,
                    instructorUnit = instructorUnit,
                    vehicleData = vehicleData,
                    manifestacionData = manifestacionData
                )
                if (optionText.isNotBlank()) {
                    val optionLines = wrapTextLines("- $optionText", (pageState.right - pageState.left) - 12f, textPaint)
                    optionLines.forEach { line ->
                        if (pageState.cursorY + 13f > maxContentY) {
                            pdfDocument.finishPage(pageState.page)
                            pageState = startContinuousPage(
                                pdfDocument = pdfDocument,
                                context = context,
                                pageNumber = pageNumber,
                                courtData = courtData,
                                textPaintSmall = textPaintSmall,
                                boxPaint = boxPaint
                            )
                            pageNumber += 1
                        }
                        if (line.isNotEmpty()) {
                            drawTextEmbedded(pageState.canvas, line, pageState.left + 12f, pageState.cursorY, textPaint)
                        }
                        pageState.cursorY += 13f
                    }
                    pageState.cursorY += 4f
                }
            }

            val extraInfo = replaceCitacionPlaceholders(
                text = section.informacionAdicional,
                courtData = courtData,
                personData = personData,
                ocurrenciaData = ocurrenciaData,
                instructorTip = instructorTip,
                secretaryTip = secretaryTip,
                instructorUnit = instructorUnit,
                vehicleData = vehicleData,
                manifestacionData = manifestacionData
            )
            drawBlock(extraInfo, textPaint, 13f, 8f)
        }

        val closeText = replaceCitacionPlaceholders(
            text = document.cierre,
            courtData = courtData,
            personData = personData,
            ocurrenciaData = ocurrenciaData,
            instructorTip = instructorTip,
            secretaryTip = secretaryTip,
            instructorUnit = instructorUnit,
            vehicleData = vehicleData,
            manifestacionData = manifestacionData
        )
        drawBlock(closeText, textPaint, 13f, 8f)

        val enteradoTitle = replaceCitacionPlaceholders(
            text = document.enteradoTitulo,
            courtData = courtData,
            personData = personData,
            ocurrenciaData = ocurrenciaData,
            instructorTip = instructorTip,
            secretaryTip = secretaryTip,
            instructorUnit = instructorUnit,
            vehicleData = vehicleData,
            manifestacionData = manifestacionData
        )
        drawBlock(enteradoTitle, headingPaint, 13f, 4f)

        val enteradoText = replaceCitacionPlaceholders(
            text = document.enteradoTexto,
            courtData = courtData,
            personData = personData,
            ocurrenciaData = ocurrenciaData,
            instructorTip = instructorTip,
            secretaryTip = secretaryTip,
            instructorUnit = instructorUnit,
            vehicleData = vehicleData,
            manifestacionData = manifestacionData
        )
        drawBlock(enteradoText, textPaint, 13f, 0f)

        if (pageState.cursorY + MIN_SIGNATURE_SECTION_HEIGHT_PT > maxContentY) {
            pdfDocument.finishPage(pageState.page)
            pageState = startContinuousPage(
                pdfDocument = pdfDocument,
                context = context,
                pageNumber = pageNumber,
                courtData = courtData,
                textPaintSmall = textPaintSmall,
                boxPaint = boxPaint
            )
            pageNumber += 1
        }

        drawCitacionSignatures(
            canvas = pageState.canvas,
            signatures = signatures,
            startY = pageState.cursorY + 6f,
            leftX = pageState.left,
            rightX = pageState.right,
            pageHeight = A4_HEIGHT_PT,
            bottomMargin = mmToPt(BOTTOM_MARGIN_MM),
            instructorTip = instructorTip,
            secretaryTip = secretaryTip,
            investigatedNoSignText = investigatedNoSignText,
            textPaint = textPaint,
            tipPaint = textPaintSmall
        )

        pdfDocument.finishPage(pageState.page)
    }

    val directory = File(context.filesDir, "atestados").apply { mkdirs() }
    val fileName = buildAtestadoPdfFileName(courtData.numeroDiligencias)
    val file = File(directory, fileName)

    FileOutputStream(file).use { output ->
        pdfDocument.writeTo(output)
    }
    pdfDocument.close()

    return AtestadoPdfResult(file = file, createdAtMillis = now)
}

private fun buildAtestadoPdfFileName(numeroDiligencias: String): String {
    val safeNumber = numeroDiligencias
        .trim()
        .ifBlank { "" }
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
    return if (safeNumber.isBlank()) "Atestado.pdf" else "Atestado${safeNumber}.pdf"
}

private data class ContinuousPageState(
    val page: PdfDocument.Page,
    val canvas: android.graphics.Canvas,
    val left: Float,
    val right: Float,
    var cursorY: Float
)

private fun startContinuousPage(
    pdfDocument: PdfDocument,
    context: Context,
    pageNumber: Int,
    courtData: JuzgadoAtestadoData,
    textPaintSmall: Paint,
    boxPaint: Paint
): ContinuousPageState {
    val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT.toInt(), A4_HEIGHT_PT.toInt(), pageNumber).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val leftMargin = mmToPt(5f)
    val rightMargin = mmToPt(5f)
    val contentLeft = leftMargin
    val contentRight = A4_WIDTH_PT - rightMargin
    val leftColWidth = mmToPt(10f)
    val centerColWidth = mmToPt(175f)
    val centerColLeftX = contentLeft + leftColWidth
    val centerColRightX = (centerColLeftX + centerColWidth).coerceAtMost(contentRight)

    drawCitacionHeader(
        canvas = canvas,
        context = context,
        courtData = courtData,
        textPaintSmall = textPaintSmall,
        boxPaint = boxPaint,
        centerColLeftX = centerColLeftX,
        centerColRightX = centerColRightX,
        contentLeft = contentLeft,
        topMargin = mmToPt(5.5f)
    )

    return ContinuousPageState(
        page = page,
        canvas = canvas,
        left = centerColLeftX + 6f,
        right = centerColRightX - 6f,
        cursorY = mmToPt(CONTENT_TOP_MM)
    )
}

private fun mmToPt(mm: Float): Float = mm * 72f / 25.4f

private fun drawCitacionHeader(
    canvas: android.graphics.Canvas,
    context: Context,
    courtData: JuzgadoAtestadoData,
    textPaintSmall: Paint,
    boxPaint: Paint,
    centerColLeftX: Float,
    centerColRightX: Float,
    contentLeft: Float,
    topMargin: Float
) {
    val atestadoBoxTop = mmToPt(5.6f)
    val atestadoBoxLeft = centerColLeftX + mmToPt(5f)
    val atestadoBoxWidth = mmToPt(75f)
    val atestadoBoxHeight = mmToPt(6f)
    val atestadoBoxRect = RectF(
        atestadoBoxLeft,
        atestadoBoxTop,
        atestadoBoxLeft + atestadoBoxWidth,
        atestadoBoxTop + atestadoBoxHeight
    )
    canvas.drawRect(atestadoBoxRect, boxPaint)
    val numeroDiligencias = courtData.numeroDiligencias.trim()
    val atestadoNumeroText = if (numeroDiligencias.isBlank()) {
        "ATESTADO NUMERO:"
    } else {
        "ATESTADO NUMERO:   $numeroDiligencias"
    }
    val textY = atestadoBoxTop + (atestadoBoxHeight + textPaintSmall.textSize) / 2f
    drawTextEmbedded(canvas, atestadoNumeroText, atestadoBoxLeft + 4f, textY, textPaintSmall)

    val folioBoxTop = mmToPt(5.6f)
    val folioBoxWidth = mmToPt(25f)
    val folioBoxHeight = mmToPt(6f)
    val folioBoxLeft = centerColRightX - mmToPt(3f) - folioBoxWidth
    val folioBoxRect = RectF(
        folioBoxLeft,
        folioBoxTop,
        folioBoxLeft + folioBoxWidth,
        folioBoxTop + folioBoxHeight
    )
    canvas.drawRect(folioBoxRect, boxPaint)
    val folioTextY = folioBoxTop + (folioBoxHeight + textPaintSmall.textSize) / 2f
    drawTextEmbedded(canvas, "FOLIO Nº", folioBoxLeft + 2f, folioTextY, textPaintSmall)

    val guideTopY = mmToPt(5.6f)
    val guideBottomY = 842f - mmToPt(15f)
    val guideLeftX = mmToPt(15f)
    val guideRightX = 595f - mmToPt(20f)
    canvas.drawLine(guideLeftX, guideTopY, guideLeftX, guideBottomY, boxPaint)
    canvas.drawLine(guideRightX, guideTopY, guideRightX, guideBottomY, boxPaint)

    loadBitmapFromAssets(context, "images/EscEspana.png")?.let { bmp ->
        val aspect = bmp.height.toFloat() / bmp.width.toFloat().coerceAtLeast(1f)
        val baseWidth = mmToPt(8f)
        val baseHeight = baseWidth * aspect
        val targetHeight = baseHeight + mmToPt(1f)
        val targetWidth = (targetHeight / aspect).coerceAtLeast(1f)
        drawBitmapAlignedTopLeft(canvas, bmp, contentLeft, topMargin, targetWidth)
    }
    loadBitmapFromAssets(context, "images/EscGuardiaCivil.png")?.let { bmp ->
        val aspect = bmp.height.toFloat() / bmp.width.toFloat().coerceAtLeast(1f)
        val baseWidth = mmToPt(7f)
        val baseHeight = baseWidth * aspect
        val targetHeight = (baseHeight - mmToPt(1f)).coerceAtLeast(mmToPt(1f))
        val targetWidth = (targetHeight / aspect).coerceAtLeast(1f)
        drawBitmapAlignedTopRight(canvas, bmp, centerColRightX + mmToPt(1f) + targetWidth, topMargin, targetWidth)
    }
}

private fun drawCitacionSignatures(
    canvas: android.graphics.Canvas,
    signatures: Map<PdfSignatureSlot, PdfSignatureContent<ImageBitmap>>,
    startY: Float,
    leftX: Float,
    rightX: Float,
    pageHeight: Float,
    bottomMargin: Float,
    instructorTip: String,
    secretaryTip: String,
    investigatedNoSignText: String,
    textPaint: Paint,
    tipPaint: Paint
) {
    var cursorY = startY
    val availableHeight = (pageHeight - bottomMargin) - cursorY
    val signatureGap = 10f
    val spaceBetweenSignatures = mmToPt(20f)
    val maxSignatureBoxWidth = mmToPt(50f)
    val maxSignatureBoxHeight = mmToPt(40f)
    val previousTopRowHeight = (availableHeight * 0.42f).coerceAtLeast(92f)
    val previousBottomRowHeight = (availableHeight - previousTopRowHeight - signatureGap).coerceAtLeast(92f)
    val topRowHeight = (previousTopRowHeight - mmToPt(1f))
        .coerceAtLeast(80f)
        .coerceAtMost(maxSignatureBoxHeight)
    val bottomRowHeight = (previousBottomRowHeight - mmToPt(3f))
        .coerceAtLeast(80f)
        .coerceAtMost(maxSignatureBoxHeight)

    val topRowWidth = rightX - leftX
    val topBoxWidth = ((topRowWidth - spaceBetweenSignatures) / 2f)
        .coerceAtLeast(40f)
        .coerceAtMost(maxSignatureBoxWidth)
    val investigatedBoxWidth = (topRowWidth / 3f)
        .coerceAtLeast(40f)
        .coerceAtMost(maxSignatureBoxWidth)

    val totalWidthNeeded = (2 * topBoxWidth) + spaceBetweenSignatures
    val leftPadding = (topRowWidth - totalWidthNeeded) / 2f

    val instructorRect = RectF(
        leftX + leftPadding,
        cursorY,
        leftX + leftPadding + topBoxWidth,
        cursorY + topRowHeight
    )
    val secretaryRect = RectF(
        leftX + leftPadding + topBoxWidth + spaceBetweenSignatures,
        cursorY,
        leftX + leftPadding + topBoxWidth + spaceBetweenSignatures + topBoxWidth,
        cursorY + topRowHeight
    )

    drawSignatureBlock(
        canvas = canvas,
        rect = instructorRect,
        title = "Instructor",
        tip = instructorTip,
        content = signatures[PdfSignatureSlot.INSTRUCTOR],
        fallbackText = "Sin firma",
        textPaint = textPaint,
        tipPaint = tipPaint
    )
    drawSignatureBlock(
        canvas = canvas,
        rect = secretaryRect,
        title = "Secretario",
        tip = secretaryTip,
        content = signatures[PdfSignatureSlot.SECRETARY],
        fallbackText = "Sin firma",
        textPaint = textPaint,
        tipPaint = tipPaint
    )

    cursorY = instructorRect.bottom + signatureGap
    val investigatedLeftPadding = (topRowWidth - investigatedBoxWidth) / 2f
    val investigatedRect = RectF(
        leftX + investigatedLeftPadding,
        cursorY,
        leftX + investigatedLeftPadding + investigatedBoxWidth,
        (cursorY + bottomRowHeight).coerceAtMost(pageHeight - bottomMargin)
    )
    drawSignatureBlock(
        canvas = canvas,
        rect = investigatedRect,
        title = "Investigado",
        tip = null,
        content = signatures[PdfSignatureSlot.INVESTIGATED],
        fallbackText = investigatedNoSignText,
        textPaint = textPaint,
        tipPaint = tipPaint
    )
}

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

private fun loadTypefaceFromAssets(context: Context, path: String): Typeface? =
    runCatching { Typeface.createFromAsset(context.assets, path) }.getOrNull()

private fun loadBitmapFromAssets(context: Context, path: String): Bitmap? =
    runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it) } }.getOrNull()

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

private fun loadAtestadoTemplateAsCitacion(
    context: Context,
    fileName: String
): CitacionDocument {
    return runCatching {
        val inputStream = context.assets.open("docs/$fileName")
        val content = InputStreamReader(inputStream).use { it.readText() }
        val root = JSONObject(content).getJSONObject("documento")

        val title = root.optString("titulo", "")
        val body = root.optJSONObject("cuerpo")?.optString("descripcion", "")
            ?: root.optString("introduccion", "")

        val sectionKeys = when (fileName) {
            "01inicio.json" -> listOf("vehiculo", "motivo_identificacion", "constatacion_carencia_autorizacion")
            "02derechos.json" -> listOf(
                "momento_informacion_derechos",
                "hechos_investigacion",
                "derechos_articulo_520",
                "manifestacion_investigado",
                "elementos_esenciales_impugnacion"
            )
            "03letradogratis.json" -> listOf("articulos")
            "04manifestacion.json" -> listOf("opciones_investigado", "documentacion", "preguntas")
            "05inmovilizacion.json" -> listOf("normativa_aplicable", "manifestaciones", "responsabilidades_penales")
            else -> emptyList()
        }

        val sections = sectionKeys.mapNotNull { key ->
            if (!root.has(key)) return@mapNotNull null
            val value = root.get(key)
            val contentText = collectOrderedText(value)
            CitacionSeccion(
                titulo = key.replace('_', ' ').replaceFirstChar { it.uppercaseChar() },
                contenido = contentText
            )
        }

        CitacionDocument(
            titulo = title,
            cuerpoDescripcion = body,
            secciones = sections,
            cierre = root.optString("cierre", "")
        )
    }.getOrElse {
        CitacionDocument(
            titulo = fileName,
            cuerpoDescripcion = "No se pudo cargar el documento $fileName"
        )
    }
}

private fun collectOrderedText(value: Any?): String {
    val lines = mutableListOf<String>()

    fun collect(current: Any?) {
        when (current) {
            is JSONObject -> {
                val keys = current.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "campos_variables" || key == "campo_variable" || key == "respuesta") continue
                    val child = current.opt(key)
                    when (key) {
                        "descripcion", "texto", "titulo", "introduccion", "referencia", "nota", "condiciones", "informacion_levantamiento" -> {
                            val text = child?.toString().orEmpty().trim()
                            if (text.isNotBlank()) lines += text
                        }
                        else -> collect(child)
                    }
                }
            }
            is JSONArray -> {
                for (index in 0 until current.length()) {
                    collect(current.opt(index))
                }
            }
            is String -> {
                val text = current.trim()
                if (text.isNotBlank()) lines += text
            }
        }
    }

    collect(value)
    return lines.joinToString("\n\n")
}

private fun wrapTextLines(
    text: String,
    maxWidth: Float,
    paint: Paint
): List<String> {
    val lines = mutableListOf<String>()
    text.split("\n").forEach { paragraph ->
        val trimmed = paragraph.trim()
        if (trimmed.isEmpty()) {
            lines += ""
            return@forEach
        }

        val words = trimmed.split(" ")
        val current = StringBuilder()
        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(test) <= maxWidth) {
                if (current.isEmpty()) current.append(word) else {
                    current.append(' ')
                    current.append(word)
                }
            } else {
                if (current.isNotEmpty()) lines += current.toString()
                current.clear()
                current.append(word)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
    }
    return lines
}

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

