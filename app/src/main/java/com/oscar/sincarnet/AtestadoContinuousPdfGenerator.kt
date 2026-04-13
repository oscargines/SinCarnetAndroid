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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val A4_WIDTH_PT = 595f
private const val A4_HEIGHT_PT = 842f
private const val CONTENT_TOP_MM = 19f
private const val BOTTOM_MARGIN_MM = 15f
private const val MIN_SIGNATURE_SECTION_HEIGHT_PT = 170f

data class AtestadoInicioModalData(
    val motivo: String = "",
    val norma: String = "",
    val articulo: String = "",
    val dgtNoRecord: Boolean = false,
    val internationalNoRecord: Boolean = false,
    val existsRecord: Boolean = false,
    val vicisitudesOption: String = "",
    val jefaturaProvincial: String = "",
    val tiempoPrivacion: String = "",
    val juzgadoDecreta: String = ""
)

private enum class TextAlignment { LEFT, CENTER }
private data class AlignedLine(val text: String, val alignment: TextAlignment)

internal fun generateAtestadoContinuousPdf(
    context: Context,
    courtData: JuzgadoAtestadoData,
    personData: PersonaInvestigadaData,
    ocurrenciaData: OcurrenciaDelitData,
    vehicleData: VehiculoData,
    manifestacionData: ManifestacionData,
    hasSecondDriver: Boolean,
    signatures: Map<PdfSignatureSlot, PdfSignatureContent<ImageBitmap>> = emptyMap(),
    investigatedNoSignText: String = NO_DESEA_FIRMAR_TEXT,
    instructorTip: String,
    secretaryTip: String,
    instructorUnit: String,
    inicioModalData: AtestadoInicioModalData = AtestadoInicioModalData()
): AtestadoPdfResult {

    val prefs = context.getSharedPreferences("segundo_conductor", Context.MODE_PRIVATE)
    val segundoConductorNombre = prefs.getString("nombre", "")?.trim() ?: ""
    val segundoConductorDocumento = prefs.getString("documento", "")?.trim() ?: ""
    android.util.Log.d("PDF", "Segundo conductor: nombre='$segundoConductorNombre', documento='$segundoConductorDocumento'")

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
        android.util.Log.d("ATEN", "load static template: $fileName")
        val doc = loadAtestadoTemplateAsCitacion(
            context = context,
            fileName = fileName,
            inicioModalData = inicioModalData,
            personData = personData,
            courtData = courtData,
            ocurrenciaData = ocurrenciaData,
            vehicleData = vehicleData,
            hasSecondDriver = hasSecondDriver,
            manifestacionData = manifestacionData,
            segundoConductorNombre = segundoConductorNombre,
            segundoConductorDocumento = segundoConductorDocumento
        )
        android.util.Log.d("ATEN", "  loaded template title='${doc.titulo}' for file=$fileName")
        orderedDocuments += doc
    }
    // Si courtData.tipoJuicio está vacío, inferir juicio rápido por fecha/hora disponibles
    val tipoJuicioForTemplate = when {
        courtData.tipoJuicio.isNotBlank() -> courtData.tipoJuicio
        courtData.fechaJuicioRapido.isNotBlank() || courtData.horaJuicioRapido.isNotBlank() -> "rapido"
        else -> ""
    }
    orderedDocuments += loadCitacionDocument(context, tipoJuicioForTemplate)

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
    orderedDocuments.forEachIndexed { documentIndex, document ->
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

        fun replaceForDocument(text: String): String = replaceCitacionPlaceholders(
            text = text,
            courtData = courtData,
            personData = personData,
            ocurrenciaData = ocurrenciaData,
            instructorTip = instructorTip,
            secretaryTip = secretaryTip,
            instructorUnit = instructorUnit,
            vehicleData = vehicleData,
            manifestacionData = manifestacionData,
            inicioModalData = inicioModalData,
            segundoConductorNombre = segundoConductorNombre,
            segundoConductorDocumento = segundoConductorDocumento,
            documentSequenceIndex = documentIndex
        )

        fun drawBlock(text: String, paint: Paint, lineHeight: Float, bottomSpacing: Float = 0f) {
            // Ignorar si el texto es nulo, vacío o solo saltos de línea/espacios
            if (text.isBlank() || text.trim().isEmpty() || text.trim() == "\n") return
            // Ignorar si el texto es un bloque JSON vacío o irrelevante
            val trimmed = text.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.length < 10) return

            val alignedLines = wrapTextLines(text, pageState.right - pageState.left, paint)
            // Si todas las líneas están vacías, no pintar nada
            if (alignedLines.all { it.text.isBlank() }) return

            alignedLines.forEach { alignedLine ->
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
                if (alignedLine.text.isNotEmpty()) {
                    val x = when (alignedLine.alignment) {
                        TextAlignment.LEFT -> pageState.left
                        TextAlignment.CENTER -> {
                            val textWidth = paint.measureText(alignedLine.text)
                            pageState.left + (pageState.right - pageState.left - textWidth) / 2f
                        }
                    }
                    drawTextEmbedded(pageState.canvas, alignedLine.text, x, pageState.cursorY, paint)
                }
                pageState.cursorY += lineHeight
            }
            pageState.cursorY += bottomSpacing
        }

        val title = replaceForDocument(document.titulo)
        drawBlock(title, titlePaint, 14f, 10f)

        val body = replaceForDocument(document.cuerpoDescripcion)
        drawBlock(body, textPaint, 13f, 10f)

        document.secciones.forEach { section ->
            val sectionTitle = replaceForDocument(section.titulo)
            drawBlock(sectionTitle, headingPaint, 13f, 4f)

            val sectionContent = replaceForDocument(section.contenido)
            drawBlock(sectionContent, textPaint, 13f, 6f)

            section.items.forEach { item ->
                val itemText = replaceForDocument(item.descripcion)
                if (itemText.isNotBlank()) {
                    val itemLines = wrapTextLines("- $itemText", (pageState.right - pageState.left) - 12f, textPaint)
                    itemLines.forEach { alignedLine ->
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
                        if (alignedLine.text.isNotEmpty()) {
                            val x = when (alignedLine.alignment) {
                                TextAlignment.LEFT -> pageState.left + 12f
                                TextAlignment.CENTER -> {
                                    val textWidth = textPaint.measureText(alignedLine.text)
                                    pageState.left + (pageState.right - pageState.left - textWidth) / 2f
                                }
                            }
                            drawTextEmbedded(pageState.canvas, alignedLine.text, x, pageState.cursorY, textPaint)
                        }
                        pageState.cursorY += 13f
                    }
                    pageState.cursorY += 4f
                }
            }
            section.opciones.forEach { option ->
                val optionText = replaceForDocument(option.descripcion)
                if (optionText.isNotBlank()) {
                    val optionLines = wrapTextLines("- $optionText", (pageState.right - pageState.left) - 12f, textPaint)
                    optionLines.forEach { alignedLine ->
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
                        if (alignedLine.text.isNotEmpty()) {
                            val x = when (alignedLine.alignment) {
                                TextAlignment.LEFT -> pageState.left + 12f
                                TextAlignment.CENTER -> {
                                    val textWidth = textPaint.measureText(alignedLine.text)
                                    pageState.left + (pageState.right - pageState.left - textWidth) / 2f
                                }
                            }
                            drawTextEmbedded(pageState.canvas, alignedLine.text, x, pageState.cursorY, textPaint)
                        }
                        pageState.cursorY += 13f
                    }
                    pageState.cursorY += 4f
                }
            }

            val extraInfo = replaceForDocument(section.informacionAdicional)
            drawBlock(extraInfo, textPaint, 13f, 8f)
        }

        val closeText = replaceForDocument(document.cierre)
        drawBlock(closeText, textPaint, 13f, 8f)

        val enteradoTitle = replaceForDocument(document.enteradoTitulo)
        val enteradoText = replaceForDocument(document.enteradoTexto)
        drawBlock(enteradoTitle, headingPaint, 13f, 4f)
        drawBlock(enteradoText, textPaint, 13f, 8f)

        // Las firmas se dibujarán más abajo (una sola vez):
        // - si el documento es 03letradogratis.json sólo se dibuja la firma del investigado con 'Enterado'
        // - en caso contrario se dibuja la disposición completa de firmas

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

        // Loguear título del documento para depuración: queremos garantizar que
        // drawSoloInvestigadoSignature sólo se invoque para el documento correcto.
        android.util.Log.d("PDF_SIG", "Signing decision for document.titulo='${document.titulo.trim()}' hasSecondDriver=$hasSecondDriver")
        // Si el documento fue marcado como 'onlyEnterado' (03letradogratis.json), solo dibujar la firma del investigado con el texto 'Enterado'
        if (document.onlyEnterado) {
            drawSoloInvestigadoSignature(
                canvas = pageState.canvas,
                signatures = signatures,
                startY = pageState.cursorY + 6f,
                leftX = pageState.left,
                rightX = pageState.right,
                pageHeight = A4_HEIGHT_PT,
                bottomMargin = mmToPt(BOTTOM_MARGIN_MM),
                textPaint = textPaint,
                tipPaint = textPaintSmall
            )
        } else {
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
                tipPaint = textPaintSmall,
                hasSecondDriver = hasSecondDriver,
                secondDriverName = if (segundoConductorNombre.isNotBlank() && segundoConductorDocumento.isNotBlank())
                    "$segundoConductorNombre ($segundoConductorDocumento)"
                else
                    segundoConductorNombre,
                allowSecondDriver = document.allowSecondDriver
            )
        }

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

private data class OdtImageAsset(
    val path: String,
    val mediaType: String,
    val bytes: ByteArray,
    val widthCm: String,
    val heightCm: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OdtImageAsset) return false
        return path == other.path &&
            mediaType == other.mediaType &&
            bytes.contentEquals(other.bytes) &&
            widthCm == other.widthCm &&
            heightCm == other.heightCm
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + mediaType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + widthCm.hashCode()
        result = 31 * result + heightCm.hashCode()
        return result
    }
}

internal fun generateAtestadoOdt(
    context: Context,
    courtData: JuzgadoAtestadoData,
    personData: PersonaInvestigadaData,
    ocurrenciaData: OcurrenciaDelitData,
    vehicleData: VehiculoData,
    manifestacionData: ManifestacionData,
    hasSecondDriver: Boolean,
    signatures: Map<PdfSignatureSlot, PdfSignatureContent<ImageBitmap>> = emptyMap(),
    investigatedNoSignText: String = NO_DESEA_FIRMAR_TEXT,
    instructorTip: String,
    secretaryTip: String,
    instructorUnit: String,
    inicioModalData: AtestadoInicioModalData = AtestadoInicioModalData()
): File {
    val prefs = context.getSharedPreferences("segundo_conductor", Context.MODE_PRIVATE)
    val segundoConductorNombre = prefs.getString("nombre", "")?.trim() ?: ""
    val segundoConductorDocumento = prefs.getString("documento", "")?.trim() ?: ""

    val orderedDocuments = mutableListOf<CitacionDocument>()
    val staticFiles = listOf(
        "01inicio.json",
        "02derechos.json",
        "03letradogratis.json",
        "04manifestacion.json",
        "05inmovilizacion.json"
    )
    staticFiles.forEach { fileName ->
        orderedDocuments += loadAtestadoTemplateAsCitacion(
            context = context,
            fileName = fileName,
            inicioModalData = inicioModalData,
            personData = personData,
            courtData = courtData,
            ocurrenciaData = ocurrenciaData,
            vehicleData = vehicleData,
            hasSecondDriver = hasSecondDriver,
            manifestacionData = manifestacionData,
            segundoConductorNombre = segundoConductorNombre,
            segundoConductorDocumento = segundoConductorDocumento
        )
    }
    val tipoJuicioForTemplate = when {
        courtData.tipoJuicio.isNotBlank() -> courtData.tipoJuicio
        courtData.fechaJuicioRapido.isNotBlank() || courtData.horaJuicioRapido.isNotBlank() -> "rapido"
        else -> ""
    }
    orderedDocuments += loadCitacionDocument(context, tipoJuicioForTemplate)

    val secondDriverDisplayName = if (segundoConductorNombre.isNotBlank() && segundoConductorDocumento.isNotBlank()) {
        "$segundoConductorNombre ($segundoConductorDocumento)"
    } else {
        segundoConductorNombre
    }

    val signatureAssets = buildOdtSignatureAssets(signatures)
    val contentXml = buildAtestadoOdtContentXml(
        orderedDocuments = orderedDocuments,
        signatureAssets = signatureAssets,
        courtData = courtData,
        personData = personData,
        ocurrenciaData = ocurrenciaData,
        vehicleData = vehicleData,
        manifestacionData = manifestacionData,
        inicioModalData = inicioModalData,
        instructorTip = instructorTip,
        secretaryTip = secretaryTip,
        instructorUnit = instructorUnit,
        investigatedNoSignText = investigatedNoSignText,
        secondDriverName = secondDriverDisplayName,
        segundoConductorNombre = segundoConductorNombre,
        segundoConductorDocumento = segundoConductorDocumento
    )

    val dir = File(context.filesDir, "atestados").apply { mkdirs() }
    val file = File(dir, buildAtestadoOdtFileName(courtData.numeroDiligencias))
    writeAtestadoOdtPackage(context, file, contentXml, signatureAssets.values.toList())
    return file
}

private fun buildAtestadoOdtFileName(numeroDiligencias: String): String {
    val safeNumber = numeroDiligencias
        .trim()
        .ifBlank { "" }
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
    return if (safeNumber.isBlank()) "Atestado.odt" else "Atestado${safeNumber}.odt"
}

private fun buildAtestadoOdtContentXml(
    orderedDocuments: List<CitacionDocument>,
    signatureAssets: Map<PdfSignatureSlot, OdtImageAsset>,
    courtData: JuzgadoAtestadoData,
    personData: PersonaInvestigadaData,
    ocurrenciaData: OcurrenciaDelitData,
    vehicleData: VehiculoData,
    manifestacionData: ManifestacionData,
    inicioModalData: AtestadoInicioModalData,
    instructorTip: String,
    secretaryTip: String,
    instructorUnit: String,
    investigatedNoSignText: String,
    secondDriverName: String,
    segundoConductorNombre: String,
    segundoConductorDocumento: String
): String {
    val body = StringBuilder()

    fun replaceForDocument(text: String, index: Int): String = replaceCitacionPlaceholders(
        text = text,
        courtData = courtData,
        personData = personData,
        ocurrenciaData = ocurrenciaData,
        instructorTip = instructorTip,
        secretaryTip = secretaryTip,
        instructorUnit = instructorUnit,
        vehicleData = vehicleData,
        manifestacionData = manifestacionData,
        inicioModalData = inicioModalData,
        segundoConductorNombre = segundoConductorNombre,
        segundoConductorDocumento = segundoConductorDocumento,
        documentSequenceIndex = index
    )

    fun appendParagraphs(rawText: String, styleName: String = "P_BODY") {
        rawText
            .split("\n")
            .map { it.trim() }
            .forEach { line ->
                if (line.isBlank()) {
                    body.append("<text:p text:style-name=\"P_BODY\"/>")
                } else {
                    val isCentered = line.startsWith("[[CENTER]]")
                    val clean = if (isCentered) line.removePrefix("[[CENTER]]").trim() else line
                    val style = if (isCentered) "P_CENTER" else styleName
                    body.append("<text:p text:style-name=\"")
                        .append(style)
                        .append("\">")
                        .append(escapeOdtXml(clean))
                        .append("</text:p>")
                }
            }
    }

    fun appendSignatureBlock(title: String, asset: OdtImageAsset?, tip: String?, fallback: String) {
        body.append("<text:p text:style-name=\"P_HEADING\">")
            .append(escapeOdtXml(title))
            .append("</text:p>")
        tip?.takeIf { it.isNotBlank() }?.let {
            body.append("<text:p text:style-name=\"P_BODY\">TIP: ")
                .append(escapeOdtXml(it))
                .append("</text:p>")
        }
        if (asset != null) {
            body.append("<text:p text:style-name=\"P_BODY\"><draw:frame draw:name=\"")
                .append(escapeOdtXml(title))
                .append("\" text:anchor-type=\"paragraph\" svg:width=\"")
                .append(asset.widthCm)
                .append("\" svg:height=\"")
                .append(asset.heightCm)
                .append("\"><draw:image xlink:href=\"")
                .append(asset.path)
                .append("\" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/></draw:frame></text:p>")
        } else {
            body.append("<text:p text:style-name=\"P_BODY\">")
                .append(escapeOdtXml(fallback))
                .append("</text:p>")
        }
    }

    orderedDocuments.forEachIndexed { index, document ->
        appendParagraphs(replaceForDocument(document.titulo, index), "P_TITLE")
        appendParagraphs(replaceForDocument(document.cuerpoDescripcion, index))

        document.secciones.forEach { section ->
            appendParagraphs(replaceForDocument(section.titulo, index), "P_HEADING")
            appendParagraphs(replaceForDocument(section.contenido, index))
            section.items.forEach { item ->
                appendParagraphs("- ${replaceForDocument(item.descripcion, index)}", "P_LIST")
            }
            section.opciones.forEach { option ->
                appendParagraphs("- ${replaceForDocument(option.descripcion, index)}", "P_LIST")
            }
            appendParagraphs(replaceForDocument(section.informacionAdicional, index))
        }

        appendParagraphs(replaceForDocument(document.cierre, index))
        appendParagraphs(replaceForDocument(document.enteradoTitulo, index), "P_HEADING")
        appendParagraphs(replaceForDocument(document.enteradoTexto, index))

        body.append("<text:p text:style-name=\"P_HEADING\">Firmas</text:p>")
        if (document.onlyEnterado) {
            appendSignatureBlock(
                title = "Enterado",
                asset = signatureAssets[PdfSignatureSlot.INVESTIGATED],
                tip = null,
                fallback = investigatedNoSignText
            )
        } else {
            appendSignatureBlock(
                title = "Instructor",
                asset = signatureAssets[PdfSignatureSlot.INSTRUCTOR],
                tip = instructorTip,
                fallback = "Sin firma"
            )
            appendSignatureBlock(
                title = "Secretario",
                asset = signatureAssets[PdfSignatureSlot.SECRETARY],
                tip = secretaryTip,
                fallback = "Sin firma"
            )
            appendSignatureBlock(
                title = "Investigado",
                asset = signatureAssets[PdfSignatureSlot.INVESTIGATED],
                tip = null,
                fallback = investigatedNoSignText
            )
            if (document.allowSecondDriver) {
                appendSignatureBlock(
                    title = "Conductor habilitado",
                    asset = signatureAssets[PdfSignatureSlot.SECOND_DRIVER],
                    tip = secondDriverName,
                    fallback = if (secondDriverName.isBlank()) "Sin firma" else "Conductor: $secondDriverName"
                )
            }
        }

        if (index < orderedDocuments.lastIndex) {
            body.append("<text:p text:style-name=\"P_DOC_BREAK\"/>")
        }
    }

    val automaticStyles = """
        <office:automatic-styles>
            <style:style style:name="P_TITLE" style:family="paragraph" style:parent-style-name="Normal" style:master-page-name="MP0">
                <style:paragraph-properties fo:margin-top="0.25cm" fo:margin-bottom="0.2cm"/>
                <style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="11pt" fo:font-weight="bold"/>
            </style:style>
            <style:style style:name="P_HEADING" style:family="paragraph" style:parent-style-name="Normal">
                <style:paragraph-properties fo:margin-top="0.18cm" fo:margin-bottom="0.1cm"/>
                <style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="11pt" fo:font-weight="bold"/>
            </style:style>
            <style:style style:name="P_BODY" style:family="paragraph" style:parent-style-name="Normal">
                <style:paragraph-properties fo:margin-bottom="0.08cm"/>
                <style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="10pt"/>
            </style:style>
            <style:style style:name="P_LIST" style:family="paragraph" style:parent-style-name="Normal">
                <style:paragraph-properties fo:margin-bottom="0.06cm" fo:margin-left="0.7cm" fo:text-indent="-0.35cm"/>
                <style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="10pt"/>
            </style:style>
            <style:style style:name="P_CENTER" style:family="paragraph" style:parent-style-name="Normal">
                <style:paragraph-properties fo:text-align="center"/>
                <style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="10pt"/>
            </style:style>
            <style:style style:name="P_DOC_BREAK" style:family="paragraph" style:parent-style-name="Normal" style:master-page-name="MP0">
                <style:paragraph-properties fo:break-before="page"/>
            </style:style>
        </office:automatic-styles>
    """.trimIndent()

    return """<?xml version="1.0" encoding="UTF-8"?><office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0" xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0" xmlns:xlink="http://www.w3.org/1999/xlink" office:version="1.2">$automaticStyles<office:body><office:text>$body</office:text></office:body></office:document-content>"""
}

private fun buildOdtSignatureAssets(
    signatures: Map<PdfSignatureSlot, PdfSignatureContent<ImageBitmap>>
): Map<PdfSignatureSlot, OdtImageAsset> {
    return buildMap {
        signatures.forEach { (slot, content) ->
            if (content is PdfSignatureContent.Image) {
                put(slot, createOdtImageAsset(slot, content.value.asAndroidBitmap()))
            }
        }
    }
}

private fun createOdtImageAsset(slot: PdfSignatureSlot, bitmap: Bitmap): OdtImageAsset {
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    val aspectRatio = bitmap.width.toDouble().coerceAtLeast(1.0) / bitmap.height.toDouble().coerceAtLeast(1.0)
    var widthCm = 8.0
    var heightCm = widthCm / aspectRatio
    if (heightCm > 3.0) {
        heightCm = 3.0
        widthCm = heightCm * aspectRatio
    }
    return OdtImageAsset(
        path = "Pictures/${slot.name.lowercase()}.png",
        mediaType = "image/png",
        bytes = output.toByteArray(),
        widthCm = String.format(java.util.Locale.US, "%.2fcm", widthCm),
        heightCm = String.format(java.util.Locale.US, "%.2fcm", heightCm)
    )
}

private fun writeAtestadoOdtPackage(
    context: Context,
    outputFile: File,
    contentXml: String,
    images: List<OdtImageAsset>
) {
    val mimetype = "application/vnd.oasis.opendocument.text"

    val fallbackStylesXml = """<?xml version="1.0" encoding="UTF-8"?><office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" office:version="1.2"><office:styles><style:style style:name="P_TITLE" style:family="paragraph"><style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="11pt" fo:font-weight="bold"/></style:style><style:style style:name="P_HEADING" style:family="paragraph"><style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="11pt" fo:font-weight="bold"/></style:style><style:style style:name="P_BODY" style:family="paragraph"><style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="10pt"/></style:style><style:style style:name="P_LIST" style:family="paragraph"><style:paragraph-properties fo:margin-left="0.7cm" fo:text-indent="-0.35cm"/><style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="10pt"/></style:style><style:style style:name="P_CENTER" style:family="paragraph"><style:paragraph-properties fo:text-align="center"/><style:text-properties style:font-name="Calibri" fo:font-family="Calibri" fo:font-size="10pt"/></style:style></office:styles></office:document-styles>"""

    val metaXml = """<?xml version="1.0" encoding="UTF-8"?><office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.2"><office:meta><meta:creation-date>${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(java.util.Date())}</meta:creation-date></office:meta></office:document-meta>"""

    val settingsXml = """<?xml version="1.0" encoding="UTF-8"?><office:document-settings xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" office:version="1.2"><office:settings/></office:document-settings>"""

    val entries = linkedMapOf<String, ByteArray>()

    // Cargar plantilla visual si está disponible en assets.
    // IMPORTANTE: Solo cargar archivos esenciales de la plantilla, ignorar meta.xml, settings.xml, etc.
    runCatching {
        context.assets.open("docs/formatodocumento.odt").use { templateStream ->
            ZipInputStream(templateStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && 
                        !entry.name.equals("mimetype", ignoreCase = true) &&
                        !entry.name.equals("meta.xml", ignoreCase = true) &&
                        !entry.name.equals("settings.xml", ignoreCase = true) &&
                        !entry.name.equals("content.xml", ignoreCase = true)) {
                        val out = ByteArrayOutputStream()
                        zipIn.copyTo(out)
                        entries[entry.name] = out.toByteArray()
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }
    }

    entries["content.xml"] = contentXml.toByteArray(Charsets.UTF_8)
    android.util.Log.d("ODT_GEN", "content.xml size: ${contentXml.length} bytes, starts with: ${contentXml.take(100)}")
    
    if (!entries.containsKey("styles.xml")) {
        entries["styles.xml"] = fallbackStylesXml.toByteArray(Charsets.UTF_8)
    }
    if (!entries.containsKey("meta.xml")) {
        entries["meta.xml"] = metaXml.toByteArray(Charsets.UTF_8)
    }
    if (!entries.containsKey("settings.xml")) {
        entries["settings.xml"] = settingsXml.toByteArray(Charsets.UTF_8)
    }

    images.forEach { image -> entries[image.path] = image.bytes }

    fun mediaTypeForPath(path: String): String = when {
        path.equals("content.xml", ignoreCase = true) -> "text/xml"
        path.equals("styles.xml", ignoreCase = true) -> "text/xml"
        path.equals("settings.xml", ignoreCase = true) -> "text/xml"
        path.equals("meta.xml", ignoreCase = true) -> "text/xml"
        path.endsWith(".xml", ignoreCase = true) -> "text/xml"
        path.endsWith(".png", ignoreCase = true) -> "image/png"
        path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
        else -> ""
    }

    val manifestEntries = StringBuilder().apply {
        append("<manifest:file-entry manifest:full-path=\"/\" manifest:media-type=\"").append(mimetype).append("\"/>")
        entries.keys.filter { !it.equals("META-INF/manifest.xml", ignoreCase = true) }.sorted().forEach { path ->
            append("<manifest:file-entry manifest:full-path=\"").append(path).append("\" manifest:media-type=\"").append(mediaTypeForPath(path)).append("\"/>")
        }
    }.toString()

    val manifestXml = """<?xml version="1.0" encoding="UTF-8"?><manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">$manifestEntries</manifest:manifest>"""
    entries["META-INF/manifest.xml"] = manifestXml.toByteArray(Charsets.UTF_8)

    ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
        // PRIMERO: Escribir mimetype sin compresión (requisito ODF)
        val mimeBytes = mimetype.toByteArray(Charsets.UTF_8)
        val crc = CRC32().apply { update(mimeBytes) }
        val mimeEntry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimeBytes.size.toLong()
            compressedSize = mimeBytes.size.toLong()
            this.crc = crc.value
            time = System.currentTimeMillis()
        }
        zip.putNextEntry(mimeEntry)
        zip.write(mimeBytes)
        zip.closeEntry()

        // SEGUNDO: Resto de archivos comprimidos
        fun addEntry(path: String, bytes: ByteArray) {
            val entry = ZipEntry(path).apply {
                method = ZipEntry.DEFLATED
                time = System.currentTimeMillis()
            }
            zip.putNextEntry(entry)
            zip.write(bytes)
            zip.closeEntry()
        }

        entries.keys.sorted().forEach { path ->
            addEntry(path, entries.getValue(path))
        }
    }
    
    android.util.Log.d("ODT_GEN", "ODT file created at: ${outputFile.absolutePath}, size: ${outputFile.length()} bytes")
}

private fun escapeOdtXml(value: String): String {
    // Escape in correct order to avoid double-escaping
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
        // Remove any control characters that might break XML
        .replace(Regex("[\\x00-\\x08\\x0B-\\x0C\\x0E-\\x1F\\x7F]"), "")
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
    tipPaint: Paint,
    hasSecondDriver: Boolean = false,  // ← AÑADIR parámetro
    secondDriverName: String = "",    // ← AÑADIR parámetro
    allowSecondDriver: Boolean = false // ← nuevo: si el documento permite segundo conductor
) {
    android.util.Log.d("PDF_SIG", "drawCitacionSignatures called: hasSecondDriver=$hasSecondDriver, secondDriverName='$secondDriverName'")
    signatures.forEach { (k, v) ->
        android.util.Log.d("PDF_SIG", "  slot=$k, type=${v.let { it::class.simpleName }}${if (v is PdfSignatureContent.Image) " id=${System.identityHashCode(v.value)}" else ""}")
    }
    var cursorY = startY
    val availableHeight = (pageHeight - bottomMargin) - cursorY
    val signatureGap = 10f
    val spaceBetweenSignatures = mmToPt(20f)
    val maxSignatureBoxWidth = mmToPt(50f)
    val maxSignatureBoxHeight = mmToPt(40f)

    // ← MODIFICAR: Calcular filas según haya segundo conductor. Solo si el documento permite mostrarlo.
    // para cubrir el caso en que la flag externa no esté sincronizada pero la firma sí existe.
    val effectiveHasSecondDriver = allowSecondDriver && (hasSecondDriver || (signatures[PdfSignatureSlot.SECOND_DRIVER] != null))

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

    // Fila 1: Instructor y Secretario
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

    // ← MODIFICAR: Fila 2 - Investigado y Segundo Conductor (si existe)
    if (effectiveHasSecondDriver) {
        val secondDriverBoxWidth = ((topRowWidth - spaceBetweenSignatures) / 2f)
            .coerceAtLeast(40f)
            .coerceAtMost(maxSignatureBoxWidth)

        val investigatedRect = RectF(
            leftX + leftPadding,
            cursorY,
            leftX + leftPadding + secondDriverBoxWidth,
            (cursorY + bottomRowHeight).coerceAtMost(pageHeight - bottomMargin)
        )
        val secondDriverRect = RectF(
            leftX + leftPadding + secondDriverBoxWidth + spaceBetweenSignatures,
            cursorY,
            leftX + leftPadding + secondDriverBoxWidth + spaceBetweenSignatures + secondDriverBoxWidth,
            (cursorY + bottomRowHeight).coerceAtMost(pageHeight - bottomMargin)
        )
        val investigatedContent = signatures[PdfSignatureSlot.INVESTIGATED]
        val secondDriverContent = signatures[PdfSignatureSlot.SECOND_DRIVER]

        drawSignatureBlock(
            canvas = canvas,
            rect = investigatedRect,
            title = "Investigado",
            tip = null,
            content = investigatedContent,
            fallbackText = investigatedNoSignText,
            textPaint = textPaint,
            tipPaint = tipPaint
        )
        drawSignatureBlock(
            canvas = canvas,
            rect = secondDriverRect,
            title = "Conductor Habilitado",
            tip = secondDriverName.takeIf { it.isNotBlank() },
            content = secondDriverContent,
            fallbackText = "Sin firma",
            textPaint = textPaint,
            tipPaint = tipPaint
        )
    } else {
        // Comportamiento original sin segundo conductor
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
    android.util.Log.d("PDF_SIG", "drawSignatureBlock: title='$title', content=${content?.let { it::class.simpleName } ?: "null"}")
    if (content is PdfSignatureContent.Image) {
        try {
            android.util.Log.d("PDF_SIG", "  image identity=${System.identityHashCode(content.value)}")
        } catch (_: Throwable) { }
    }
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

/**
 * Dibuja solo la firma del investigado con el texto 'Enterado' centrado
 */
private fun drawSoloInvestigadoSignature(
    canvas: android.graphics.Canvas,
    signatures: Map<PdfSignatureSlot, PdfSignatureContent<ImageBitmap>>,
    startY: Float,
    leftX: Float,
    rightX: Float,
    pageHeight: Float,
    bottomMargin: Float,
    textPaint: Paint,
    tipPaint: Paint
) {
    android.util.Log.d("PDF_SIG", "drawSoloInvestigadoSignature called")
    signatures.forEach { (k, v) ->
        android.util.Log.d("PDF_SIG", "  slot=$k, type=${v.let { it::class.simpleName }}${if (v is PdfSignatureContent.Image) " id=${System.identityHashCode(v.value)}" else ""}")
    }
    val availableHeight = (pageHeight - bottomMargin) - startY
    val signatureBoxWidth = ((rightX - leftX) / 2f).coerceAtLeast(40f)
    val signatureBoxHeight = (availableHeight * 0.42f).coerceAtLeast(80f).coerceAtMost(mmToPt(40f))
    val leftPadding = ((rightX - leftX) - signatureBoxWidth) / 2f
    val rect = RectF(
        leftX + leftPadding,
        startY,
        leftX + leftPadding + signatureBoxWidth,
        (startY + signatureBoxHeight).coerceAtMost(pageHeight - bottomMargin)
    )
    drawSignatureBlock(
        canvas = canvas,
        rect = rect,
        title = "Enterado",
        tip = null,
        content = signatures[PdfSignatureSlot.INVESTIGATED],
        fallbackText = NO_DESEA_FIRMAR_TEXT,
        textPaint = textPaint,
        tipPaint = tipPaint
    )
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
private fun buildManifestacionPreguntasText(
    value: Any?,
    manifestacionData: ManifestacionData
): String {
    val array = value as? JSONArray ?: return ""
    val lines = mutableListOf<String>()

    val placeholderMap = mapOf(
        1 to "primerapregunta",
        2 to "segundapregunta",
        3 to "tercerapregunta",
        4 to "cuartapregunta",
        5 to "quintapregunta",
        6 to "sextapregunta",
        7 to "septimapregunta",
        8 to "octavapregunta"
    )

    for (i in 0 until array.length()) {
        val pregunta = array.optJSONObject(i) ?: continue
        val id = pregunta.optInt("id", i + 1)
        val textoPregunta = pregunta.optString("pregunta", "").trim()
        val campoVariable = pregunta.optString("campo_variable", "").trim()

        if (textoPregunta.isNotBlank()) lines += textoPregunta

        if (campoVariable.isNotBlank()) {
            val placeholderName = placeholderMap[id]
            val respuesta = if (placeholderName != null) {
                manifestacionData.respuestasPreguntas[id].orEmpty().ifBlank { "_______________" }
            } else {
                "_______________"
            }
            // Sustituir el placeholder dentro del campo_variable
            val campoResuelto = if (placeholderName != null) {
                campoVariable.replace("[[$placeholderName]]", respuesta)
            } else {
                campoVariable
            }
            lines += campoResuelto
        }
    }

    return lines.joinToString("\n\n")
}

private fun loadAtestadoTemplateAsCitacion(
    context: Context,
    fileName: String,
    inicioModalData: AtestadoInicioModalData,
    personData: PersonaInvestigadaData,
    courtData: JuzgadoAtestadoData,
    ocurrenciaData: OcurrenciaDelitData,
    vehicleData: VehiculoData,
    hasSecondDriver: Boolean,
    manifestacionData: ManifestacionData = ManifestacionData(),
    segundoConductorNombre: String = "",
    segundoConductorDocumento: String = ""
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

        val sections = if (fileName == "01inicio.json") {
            buildInicioSections(root, inicioModalData)
        } else sectionKeys.mapNotNull { key ->
            if (!root.has(key)) return@mapNotNull null
            val value = root.get(key)
            val contentText = when {
                fileName == "02derechos.json" && key == "hechos_investigacion" ->
                    buildDerechosHechosInvestigacionText(value)
                fileName == "02derechos.json" && key == "derechos_articulo_520" ->
                    buildDerechosArticulo520Text(value)
                fileName == "02derechos.json" && key == "manifestacion_investigado" ->
                    buildDerechosManifestacionInvestigadoText(value, personData, courtData, ocurrenciaData, vehicleData)
                fileName == "02derechos.json" && key == "momento_informacion_derechos" ->          // ← NUEVO
                    buildDerechosMomentoInformacionText(value, ocurrenciaData.derechosInformacionMomento) // ← NUEVO
                fileName == "04manifestacion.json" && key == "documentacion" ->
                    collectOrderedTextManifestacionAnexos(value, personData, courtData, ocurrenciaData, vehicleData, manifestacionData)
                fileName == "04manifestacion.json" && key == "preguntas" ->    // ← AÑADIR
                    buildManifestacionPreguntasText(value, manifestacionData)  // ← AÑADIR
                fileName == "05inmovilizacion.json" && key == "manifestaciones" ->
                    // Combinar nombre y documento del segundo conductor cuando ambos estén presentes
                    run {
                        val segundoDatos = if (segundoConductorNombre.isNotBlank() && segundoConductorDocumento.isNotBlank())
                            "$segundoConductorNombre ($segundoConductorDocumento)"
                        else
                            segundoConductorNombre
                        collectOrderedTextInmovilizacionManifestaciones(value, hasSecondDriver, segundoDatos)
                    }
                else -> collectOrderedText(value)
            }
            CitacionSeccion(
                titulo = key.replace('_', ' ').replaceFirstChar { it.uppercaseChar() },
                contenido = contentText
            )
        }

        CitacionDocument(
            titulo = title,
            cuerpoDescripcion = body,
            secciones = sections,
            cierre = root.optJSONObject("cierre")?.optString("texto", "")
                ?: root.optString("cierre", ""),
            onlyEnterado = fileName == "03letradogratis.json"
            ,
            allowSecondDriver = fileName == "05inmovilizacion.json"
        )
    }.getOrElse {
        CitacionDocument(
            titulo = fileName,
            cuerpoDescripcion = "No se pudo cargar el documento $fileName"
        )
    }
}
private fun buildDerechosMomentoInformacionText(value: Any?, selectedMomento: String): String {
    val obj = value as? JSONObject ?: return collectOrderedText(value)
    val lines = mutableListOf<String>()

    obj.optString("descripcion", "").takeIf { it.isNotBlank() }?.let { lines += it }

    val opciones = obj.optJSONArray("opciones") ?: return lines.joinToString("\n\n")

    // id 1 → "mismo_momento", id 2 → "inmediato"
    val targetId = when (selectedMomento) {
        "mismo_momento" -> 1
        "inmediato"     -> 2
        else            -> null   // si no hay selección, no pinta ninguna
    }

    for (i in 0 until opciones.length()) {
        val opcion = opciones.optJSONObject(i) ?: continue
        val id = opcion.optInt("id", -1)
        if (targetId != null && id != targetId) continue   // filtra la no seleccionada

        val texto = opcion.optString("texto", "").trim()
        if (texto.isNotBlank()) lines += texto
    }

    return lines.joinToString("\n\n")
}

private fun buildInicioSections(root: JSONObject, inicioModalData: AtestadoInicioModalData): List<CitacionSeccion> {
    val sections = mutableListOf<CitacionSeccion>()

    root.optJSONObject("vehiculo")?.let { vehiculoObj ->
        sections += CitacionSeccion(
            titulo = "Vehiculo",
            contenido = vehiculoObj.optString("descripcion", "")
        )
    }

    root.optJSONObject("motivo_identificacion")?.let { motivoObj ->
        val motivoDescripcion = motivoObj.optString("descripcion", "")
        val motivoOpciones = motivoObj.optJSONArray("opciones") ?: JSONArray()
        val motivoId = when (inicioModalData.motivo) {
            "Siniestro Vial" -> 1
            "Control preventivo" -> 2
            "Cometer infracción" -> 3
            else -> null
        }
        val selectedMotivoText = motivoId?.let { id ->
            findOptionTextById(motivoOpciones, id)
        }.orEmpty()

        sections += CitacionSeccion(
            titulo = "Motivo identificacion",
            contenido = motivoDescripcion,
            opciones = selectedMotivoText.takeIf { it.isNotBlank() }?.let {
                listOf(CitacionOpcion(descripcion = it))
            } ?: emptyList()
        )
    }

    root.optJSONObject("constatacion_carencia_autorizacion")?.let { constObj ->
        val constDescripcion = constObj.optString("descripcion", "")
        val situaciones = constObj.optJSONArray("situaciones") ?: JSONArray()
        val opciones = mutableListOf<CitacionOpcion>()

        if (inicioModalData.dgtNoRecord) {
            findOptionTextById(situaciones, 1)?.let { opciones += CitacionOpcion(descripcion = it) }
        }
        if (inicioModalData.internationalNoRecord) {
            findOptionTextById(situaciones, 2)?.let { opciones += CitacionOpcion(descripcion = it) }
        }
        if (inicioModalData.existsRecord) {
            val situacion3 = findOptionObjectById(situaciones, 3)
            situacion3?.optString("texto", "")?.takeIf { it.isNotBlank() }?.let {
                opciones += CitacionOpcion(descripcion = it)
            }

            val subOptionId = when (inicioModalData.vicisitudesOption) {
                "No ha obtenido nunca" -> "3a"
                "Pérdida de vigencia por pérdida de puntos" -> "3b"
                "Condena firme en vigor" -> "3c"
                "No consta realización y superación de cursos" -> "3d"
                else -> null
            }
            val subOpciones = situacion3?.optJSONArray("subopciones") ?: JSONArray()
            subOptionId?.let { id ->
                findOptionTextById(subOpciones, id)?.let { opciones += CitacionOpcion(descripcion = it) }
            }
        }

        sections += CitacionSeccion(
            titulo = "Constatacion carencia autorizacion",
            contenido = constDescripcion,
            opciones = opciones
        )
    }

    return sections
}

private fun buildDerechosHechosInvestigacionText(value: Any?): String {
    val obj = value as? JSONObject ?: return collectOrderedText(value)
    val lines = mutableListOf<String>()

    obj.optString("descripcion", "").takeIf { it.isNotBlank() }?.let { lines += it }

    val puntos = obj.optJSONArray("puntos") ?: return lines.joinToString("\n\n")
    for (i in 0 until puntos.length()) {
        val punto = puntos.optJSONObject(i) ?: continue
        val id = punto.optString("id", (i + 1).toString())
        val titulo = punto.optString("titulo", "")
        val campo = punto.optString("campo_variable", "")
        val texto = punto.optString("texto", "")

        val line = "$id. $titulo"
        if (line.isNotBlank()) lines += line

        if (campo.isNotBlank()) {
            lines += "[[CENTER]]$campo"
        }
        if (texto.isNotBlank()) {
            lines += "[[CENTER]]${texto.uppercase()}"
        }
    }

    return lines.joinToString("\n\n")
}

private fun buildDerechosManifestacionInvestigadoText(
    value: Any?,
    personData: PersonaInvestigadaData,
    courtData: JuzgadoAtestadoData,
    ocurrenciaData: OcurrenciaDelitData,
    vehicleData: VehiculoData
): String {
    val obj = value as? JSONObject ?: return collectOrderedText(value)
    val lines = mutableListOf<String>()

    obj.optString("descripcion", "").takeIf { it.isNotBlank() }?.let { lines += it }

    val opciones = obj.optJSONArray("opciones") ?: return lines.joinToString("\n\n")
    for (i in 0 until opciones.length()) {
        val option = opciones.optJSONObject(i) ?: continue
        val id = option.optInt("id", i + 1)
        var texto = option.optString("texto", "")
        val nota = option.optString("nota", "")
        val campoVariable = option.optString("campo_variable", "")
        val answerPlaceholder = when (id) {
            1 -> "[[right_declaracion]]"
            2 -> "[[right_renuncia_letrada]]"
            3 -> "[[right_letrado_particular]]"
            4 -> "[[right_letrado_oficio]]"
            5 -> "[[right_acceso_elementos]]"
            6 -> "[[right_interprete]]"
            else -> ""
        }

        // Interpolar placeholders en el texto
        texto = replaceManifestacionPlaceholders(
            texto,
            personData,
            courtData,
            ocurrenciaData,
            vehicleData
        )

        val optionLine = buildString {
            append("- ")
            append(texto)
            if (campoVariable.isNotBlank()) {
                append(" ")
                append(campoVariable)
            }
            if (answerPlaceholder.isNotBlank()) {
                append(": ")
                append(answerPlaceholder)
            }
        }
        if (optionLine.isNotBlank()) lines += optionLine
        if (nota.isNotBlank()) lines += "  Nota: $nota"
    }

    return lines.joinToString("\n\n")
}

private fun buildDerechosArticulo520Text(value: Any?): String {
    val obj = value as? JSONObject ?: return collectOrderedText(value)
    val lines = mutableListOf<String>()

    obj.optString("descripcion", "").takeIf { it.isNotBlank() }?.let { lines += it }

    val derechos = obj.optJSONArray("derechos") ?: return lines.joinToString("\n\n")
    for (i in 0 until derechos.length()) {
        val derecho = derechos.optJSONObject(i) ?: continue
        val id = derecho.optString("id", "").trim().uppercase()
        val texto = derecho.optString("texto", "").trim()
        if (id.isNotBlank() && texto.isNotBlank()) {
            lines += "$id) $texto"
        } else if (texto.isNotBlank()) {
            lines += texto
        }
    }

    return lines.joinToString("\n\n")
}

private fun findOptionObjectById(array: JSONArray, id: Any): JSONObject? {
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val currentId = obj.opt("id")
        if (currentId == id) return obj
    }
    return null
}

private fun findOptionTextById(array: JSONArray, id: Any): String? =
    findOptionObjectById(array, id)
        ?.optString("texto", "")
        ?.takeIf { it.isNotBlank() }

private fun collectOrderedText(value: Any?): String {
    val lines = mutableListOf<String>()

    fun collect(current: Any?) {
        when (current) {
            is JSONObject -> {
                val id = current.opt("id")?.toString()?.trim().orEmpty()
                val texto = current.optString("texto", "").trim()
                val pregunta = current.optString("pregunta", "").trim()
                val referencia = current.optString("referencia", "").trim()
                val principalText = when {
                    texto.isNotBlank() -> texto
                    pregunta.isNotBlank() -> pregunta
                    referencia.isNotBlank() -> referencia
                    else -> ""
                }

                // Si el objeto solo tiene campos irrelevantes y texto vacío, no añadir nada
                val keysSet = current.keys().asSequence().toSet()
                val soloCamposIrrelevantes = keysSet.all { it == "id" || it == "campos_variables" || it == "campo_variable" || it == "respuesta" || it == "texto" }
                if (soloCamposIrrelevantes && texto.isBlank()) {
                    return
                }

                if (id.isNotBlank() && principalText.isNotBlank()) {
                    lines += formatIdTextLine(id, principalText)

                    val nota = current.optString("nota", "").trim()
                    if (nota.isNotBlank()) {
                        lines += "Nota: $nota"
                    }

                    val keys = current.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (
                            key == "id" ||
                            key == "texto" ||
                            key == "pregunta" ||
                            key == "referencia" ||
                            key == "nota" ||
                            key == "campos_variables" ||
                            key == "campo_variable" ||
                            key == "respuesta"
                        ) continue
                        collect(current.opt(key))
                    }
                    return
                }

                val keys = current.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (
                        key == "id" ||
                        key == "campos_variables" ||
                        key == "campo_variable" ||
                        key == "respuesta"
                    ) continue
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

private fun formatIdTextLine(idRaw: String, text: String): String {
    val id = idRaw.trim()
    return if (id.startsWith("artículo", ignoreCase = true)) {
        "$id. $text"
    } else {
        "${id.uppercase()}) $text"
    }
}

private fun wrapTextLines(
    text: String,
    maxWidth: Float,
    paint: Paint
): List<AlignedLine> {
    val result = mutableListOf<AlignedLine>()
    text.split("\n").forEach { paragraph ->
        var p = paragraph.trim()
        var alignment = TextAlignment.LEFT
        if (p.startsWith("[[CENTER]]")) {
            p = p.substring(10).trim()
            alignment = TextAlignment.CENTER
        }

        if (p.isEmpty()) {
            result.add(AlignedLine("", alignment))
            return@forEach
        }

        val words = p.split(" ")
        var currentLineText = StringBuilder()
        for (word in words) {
            val test = if (currentLineText.isEmpty()) word else "$currentLineText $word"
            if (paint.measureText(test) <= maxWidth) {
                if (currentLineText.isEmpty()) currentLineText.append(word) else {
                    currentLineText.append(' ')
                    currentLineText.append(word)
                }
            } else {
                if (currentLineText.isNotEmpty()) result.add(AlignedLine(currentLineText.toString(), alignment))
                currentLineText = StringBuilder()
                if (paint.measureText(word) > maxWidth) {
                    var start = 0
                    while (start < word.length) {
                        var end = word.length
                        while (end > start && paint.measureText(word.substring(start, end)) > maxWidth) {
                            end--
                        }
                        if (end == start) end = (start + 1).coerceAtMost(word.length)
                        result.add(AlignedLine(word.substring(start, end) + if (end < word.length) "-" else "", alignment))
                        start = end
                    }
                } else {
                    currentLineText.append(word)
                }
            }
        }
        if (currentLineText.isNotEmpty()) result.add(AlignedLine(currentLineText.toString(), alignment))
    }
    return result
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

private fun collectOrderedTextManifestacionAnexos(
    value: Any?,
    personData: PersonaInvestigadaData,
    courtData: JuzgadoAtestadoData,
    ocurrenciaData: OcurrenciaDelitData,
    vehicleData: VehiculoData,
    manifestacionData: ManifestacionData = ManifestacionData()
): String {
    val lines = mutableListOf<String>()
    if (value is JSONObject && value.has("anexos")) {
        val anexosArray = value.getJSONArray("anexos")
        for (i in 0 until anexosArray.length()) {
            val anexo = anexosArray.getJSONObject(i)
            val id = anexo.optString("id", "Anexo ${i + 1}")
            val texto = anexo.optString("texto", "")
            if (texto.isNotBlank()) {
                val interpolated = replaceManifestacionPlaceholders(
                    texto,
                    personData,
                    courtData,
                    ocurrenciaData,
                    vehicleData,
                    manifestacionData
                )
                lines += "$id) $interpolated"
            }
        }
    }
    return lines.joinToString("\n\n")
}

private fun collectOrderedTextInmovilizacionManifestaciones(
    value: Any?,
    hasSecondDriver: Boolean,
    personasehacecargo: String = " "
): String {
    val lines = mutableListOf<String>()
    val array = when (value) {
        is JSONArray -> value
        is JSONObject -> JSONArray().apply { put(value) }
        else -> return " "
    }
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val id = obj.optInt("id", i + 1)
        if (hasSecondDriver) {
            if (id != 1) continue
        } else {
            if (id == 1) continue
        }
        var texto = obj.optString("texto", " ").trim()

        // ← MODIFICAR: Usar personasehacecargo si está disponible
        if (personasehacecargo.isNotBlank()) {
            texto = texto.replace("[[personasehacecargo]]", personasehacecargo)
        } else {
            // Fallback: usar placeholder vacío si no hay datos
            texto = texto.replace("[[personasehacecargo]]", "_______________")
        }

        if (texto.isNotBlank()) {
            lines += "- $texto"
        }

        // Si tiene datos_conductor_habilitado y es el id=1 y hasSecondDriver, mostrar los datos
        obj.optJSONObject("datos_conductor_habilitado")?.let { datos ->
                val desc = datos.optString("descripcion", " ").trim()
                val campoVar = datos.optString("campo_variable", "").trim()
                val condiciones = datos.optString("condiciones", " ").trim()

                if (desc.isNotBlank()) lines += "  $desc"

                // Si hay un campo_variable, intentamos sustituirlo por personasehacecargo
                if (campoVar.isNotBlank()) {
                    val resolved = if (personasehacecargo.isNotBlank()) personasehacecargo else "_______________"
                    // campoVar puede contener placeholders como [[datosconductorhabilitado]]
                    val campoResuelto = campoVar.replace("[[datosconductorhabilitado]]", resolved)
                        .replace("[[personasehacecargo]]", resolved)
                    if (campoResuelto.isNotBlank()) {
                        // Usar indicador [[CENTER]] para que el renderizado centre horizontalmente esta línea
                        lines += "[[CENTER]]$campoResuelto"
                    }
                }

                if (condiciones.isNotBlank()) lines += "  $condiciones"
        }

        obj.optString("informacion_levantamiento", " ").takeIf { it.isNotBlank() }?.let {
            lines += "  $it"
        }
    }
    return lines.joinToString("\n\n")
}
