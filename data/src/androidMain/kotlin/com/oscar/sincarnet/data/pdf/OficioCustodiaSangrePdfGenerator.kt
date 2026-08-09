package com.oscar.sincarnet.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.oscar.sincarnet.domain.model.OficioCustodiaSangreData
import java.io.File
import java.io.FileOutputStream

object OficioCustodiaSangrePdfGenerator {

    private const val A4_WIDTH_PT = 595f
    private const val A4_HEIGHT_PT = 842f

    private fun mmToPt(mm: Float): Float = mm * 72f / 25.4f

    private val spanishMonths = mapOf(
        "01" to "enero", "02" to "febrero", "03" to "marzo", "04" to "abril",
        "05" to "mayo", "06" to "junio", "07" to "julio", "08" to "agosto",
        "09" to "septiembre", "10" to "octubre", "11" to "noviembre", "12" to "diciembre"
    )

    private fun monthNumberToSpanish(month: String): String =
        spanishMonths[month] ?: month

    fun generatePdf(
        context: Context,
        data: OficioCustodiaSangreData,
        agenteSignature: Bitmap? = null
    ): AtestadoPdfResult {
        val pdfDocument = PdfDocument()
        val page = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(A4_WIDTH_PT.toInt(), A4_HEIGHT_PT.toInt(), 1).create()
        )
        val canvas = page.canvas

        val marginLeft = mmToPt(20f)
        val marginRight = mmToPt(18f)
        val topMargin = mmToPt(4f)
        val contentLeft = marginLeft
        val contentRight = A4_WIDTH_PT - marginRight
        val contentWidth = contentRight - contentLeft
        var y = topMargin

        val regularTypeface =
            loadTypefaceFromAssets(context, "fonts/calibri-regular.ttf")
                ?: Typeface.create("arial", Typeface.NORMAL)
                ?: Typeface.SANS_SERIF
        val boldTypeface =
            loadTypefaceFromAssets(context, "fonts/calibri-bold.ttf")
                ?: Typeface.create("arial", Typeface.BOLD)
                ?: Typeface.create(regularTypeface, Typeface.BOLD)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = regularTypeface
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = regularTypeface
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = boldTypeface
        }
        val headerMinisterioPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
            typeface = regularTypeface
        }
        val headerSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = regularTypeface
        }
        val headerBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = boldTypeface
        }
        val tinyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8f
            typeface = regularTypeface
        }
        val spacedTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = boldTypeface
            letterSpacing = 0.35f
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val decorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val tableTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
            typeface = regularTypeface
        }
        val tableBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
            typeface = boldTypeface
        }

        fun drawHLine(x1: Float, x2: Float, yy: Float) = canvas.drawLine(x1, yy, x2, yy, linePaint)

        fun wrapTextToLines(text: String, maxWidth: Float, paint: Paint): List<String> {
            if (text.isBlank()) return emptyList()
            val words = text.trim().split(Regex("\\s+"))
            val lines = mutableListOf<String>()
            var current = ""
            words.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    if (current.isNotBlank()) lines += current
                    current = word
                }
            }
            if (current.isNotBlank()) lines += current
            return lines
        }

        fun drawWrappedText(
            txt: String,
            left: Float,
            right: Float,
            startY: Float,
            paint: Paint = textPaint,
            lineGapPt: Float = 2f
        ): Float {
            val maxWidth = right - left
            val lines = wrapTextToLines(txt, maxWidth, paint)
            var yy = startY
            lines.forEach { line ->
                canvas.drawText(line, left, yy, paint)
                yy += paint.textSize + lineGapPt
            }
            return yy
        }

        fun drawSignatureOnLine(
            signature: Bitmap?,
            left: Float,
            right: Float,
            topY: Float,
            maxHeightPt: Float = 40f
        ): Float {
            if (signature == null) return topY
            val maxW = right - left
            val maxH = maxHeightPt
            val ratio = signature.width.toFloat() / signature.height.toFloat().coerceAtLeast(1f)
            var targetW = maxW
            var targetH = targetW / ratio
            if (targetH > maxH) {
                targetH = maxH
                targetW = targetH * ratio
            }
            val x = left + (maxW - targetW) / 2f
            canvas.drawBitmap(signature, null, RectF(x, topY, x + targetW, topY + targetH), null)
            return topY + targetH
        }

        // ═══════════════════════════════════════════════════════════════════
        // CABECERA INSTITUCIONAL
        // ═══════════════════════════════════════════════════════════════════
        val headerTop = y
        val headerHeight = mmToPt(30f)
        val headerBottom = headerTop + headerHeight

        // Escudo de España (izquierda) - 20% más grande
        val leftShieldX = contentLeft
        val leftShieldY = headerTop + mmToPt(4f)
        val leftShieldH = mmToPt(18f)
        var leftShieldW = mmToPt(14.4f)
        loadBitmapFromAssets(context, "images/EscEspana.png")?.let { bmp ->
            leftShieldW = leftShieldH * (bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f))
            canvas.drawBitmap(
                bmp,
                null,
                RectF(leftShieldX, leftShieldY, leftShieldX + leftShieldW, leftShieldY + leftShieldH),
                null
            )
        }
        val leftHeaderX = leftShieldX + leftShieldW + mmToPt(3f)
        val leftHeaderY = headerTop + mmToPt(9.5f)
        canvas.drawText("MINISTERIO", leftHeaderX, leftHeaderY, headerMinisterioPaint)
        canvas.drawText("DEL", leftHeaderX, leftHeaderY + mmToPt(3.5f), headerMinisterioPaint)
        canvas.drawText("INTERIOR", leftHeaderX, leftHeaderY + mmToPt(7f), headerMinisterioPaint)

        // Escudo Guardia Civil (derecha) - 20% más grande
        val rightShieldH = mmToPt(14.4f)
        val rightShieldY = headerTop + mmToPt(4.6f)
        val rightBlockRight = contentRight
        val rightTitle = "GUARDIA CIVIL"
        val rightSubTitle = "DIRECCION GENERAL"
        val rightLine1 = "Mando de Operaciones"
        val rightLine2 = "Agrupación de Tráfico"
        val rightLine3 = data.unidad.ifBlank { "\"Unidad\"" }
        val rightBlockTextWidth = maxOf(
            headerBoldPaint.measureText(rightTitle),
            headerSmallPaint.measureText(rightSubTitle),
            tinyPaint.measureText(rightLine1),
            tinyPaint.measureText(rightLine2),
            tinyPaint.measureText(rightLine3)
        )
        val rightX = rightBlockRight - rightBlockTextWidth
        var rightShieldW = mmToPt(12f)
        loadBitmapFromAssets(context, "images/EscGuardiaCivil.png")?.let { bmp ->
            rightShieldW = rightShieldH * (bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f))
            canvas.drawBitmap(
                bmp,
                null,
                RectF(rightX - mmToPt(3f) - rightShieldW, rightShieldY, rightX - mmToPt(3f), rightShieldY + rightShieldH),
                null
            )
        }
        val rightTop = headerTop + mmToPt(7.3f)
        canvas.drawText(rightTitle, rightX, rightTop, headerBoldPaint)
        canvas.drawText(rightSubTitle, rightX, rightTop + mmToPt(3f), headerSmallPaint)
        val mandoTop = rightTop + mmToPt(6.4f)
        canvas.drawText(rightLine1, rightX, mandoTop, tinyPaint)
        canvas.drawText(rightLine2, rightX, mandoTop + mmToPt(2.7f), tinyPaint)
        canvas.drawText(rightLine3, rightX, mandoTop + mmToPt(5.3f), tinyPaint)

        y = headerBottom + mmToPt(4f)

        // ═══════════════════════════════════════════════════════════════════
        // TÍTULO "O  F  I  C  I  O" (justificado a la izquierda)
        // ═══════════════════════════════════════════════════════════════════
        val oficioTitle = "O  F  I  C  I  O"
        val oficioTitleY = y + spacedTitlePaint.textSize
        canvas.drawText(oficioTitle, contentLeft, oficioTitleY, spacedTitlePaint)
        val yOficioCenter = oficioTitleY - spacedTitlePaint.textSize / 2f
        y += mmToPt(10f)

        // ═══════════════════════════════════════════════════════════════════
        // LÍNEAS DECORATIVAS (bracket izquierdo)
        // ═══════════════════════════════════════════════════════════════════
        val xLine = mmToPt(10f)
        val hLineLen = mmToPt(5f)
        val vLineLen = mmToPt(45f)
        // Horizontal superior: desde xLine hacia la derecha 5mm
        canvas.drawLine(xLine, yOficioCenter, xLine + hLineLen, yOficioCenter, decorLinePaint)
        // Vertical: desde xLine hacia abajo 4.5cm
        canvas.drawLine(xLine, yOficioCenter, xLine, yOficioCenter + vLineLen, decorLinePaint)
        // Horizontal inferior: desde xLine hacia la izquierda 5mm
        canvas.drawLine(xLine - hLineLen, yOficioCenter + vLineLen, xLine, yOficioCenter + vLineLen, decorLinePaint)

        // ═══════════════════════════════════════════════════════════════════
        // CAMPOS DE REFERENCIA
        // ═══════════════════════════════════════════════════════════════════
        canvas.drawText("S/REF:", contentLeft, y + textPaint.textSize, textPaint)
        y += mmToPt(4f)

        canvas.drawText("N/REF:          Atestado Núm. ${data.numeroDiligencia.ifBlank { "________" }}", contentLeft, y + textPaint.textSize, textPaint)
        y += mmToPt(5f)

        val asuntoText = "ASUNTO:   Solicitud de custodia y conservación de las muestras de sangre obtenidas con fines terapéuticos en ese Centro Sanitario."
        y = drawWrappedText(asuntoText, contentLeft, contentRight, y + textPaint.textSize)

        canvas.drawText("FECHA:", contentLeft, y + textPaint.textSize, textPaint)
        val fechaText = data.fechaHoraSolicitud.ifBlank { "________" }
        canvas.drawText(fechaText, contentLeft + mmToPt(14f), y + textPaint.textSize, textPaint)
        y += mmToPt(6f)

        // ═══════════════════════════════════════════════════════════════════
        // DESTINATARIO
        // ═══════════════════════════════════════════════════════════════════
        canvas.drawText("DESTINATARIO:", contentLeft, y + boldPaint.textSize, boldPaint)
        val destinatarioLabel = "SR./SRA. DIRECTOR/A DEL CENTRO"
        canvas.drawText(destinatarioLabel, contentLeft + mmToPt(28f), y + boldPaint.textSize, boldPaint)
        val centroText = data.centroDestinatario.ifBlank { "________" }
        val centroX = contentLeft + mmToPt(28f) + boldPaint.measureText(destinatarioLabel) + mmToPt(3f)
        canvas.drawText(centroText, centroX, y + boldPaint.textSize, boldPaint)
        y += mmToPt(20f)

        // ═══════════════════════════════════════════════════════════════════
        // PÁRRAFO 1 - CUERPO DEL OFICIO
        // ═══════════════════════════════════════════════════════════════════
        val splitParts = data.fechaHoraSolicitud.split(" ", limit = 2)
        val fechaPart = splitParts.getOrElse(0) { "" }
        val horaPart = splitParts.getOrElse(1) { "" }
        val fechaSplit = fechaPart.split("-")
        val dia = fechaSplit.getOrElse(0) { "___" }
        val mes = fechaSplit.getOrElse(1) { "___" }
        val anio = fechaSplit.getOrElse(2) { "____" }

        val paragraph1 = "A las ${horaPart.ifBlank { "___" }} horas del día ${dia.ifBlank { "___" }} de ${monthNumberToSpanish(mes)} del ${anio.ifBlank { "____" }}, " +
            "por el Equipo de Atestados de la Agrupación de Tráfico de la Guardia Civil de ${data.unidad.ifBlank { "________" }}," +
            " formado por los componentes con T.I.P. núm. ${data.tipSolicitante.ifBlank { "________" }} y ${data.empleoSolicitante.ifBlank { "________" }} ," +
            " en virtud de lo preceptuado en el artículo 796.1.7ª de la Ley de Enjuiciamiento Criminal, en relación con el artículo" +
            " 14 de la Ley sobre Tráfico, Circulación de Vehículos a Motor y Seguridad Vial, así como en los Capítulos IV y V del" +
            " Reglamento General de Circulación, aprobado por Real Decreto 1428/2003, de 21 de noviembre, se SOLICITA de" +
            " ese Centro Sanitario la custodia y conservación en condiciones idóneas, de las muestras de sangre extraídas con" +
            " fines terapéuticos a:"
        y = drawWrappedText(paragraph1, contentLeft, contentRight, y)
        y += mmToPt(4f)

        // ═══════════════════════════════════════════════════════════════════
        // TABLA: NOMBRE Y APELLIDOS / D.N.I.
        // ═══════════════════════════════════════════════════════════════════
        val tableLeft = contentLeft + mmToPt(5f)
        val tableRight = contentRight - mmToPt(5f)
        val tableWidth = tableRight - tableLeft
        val col1Width = tableWidth * 0.72f
        val col2Width = tableWidth - col1Width
        val col1Right = tableLeft + col1Width
        val rowHeight = mmToPt(8f)
        val tableTop = y

        // Header row
        canvas.drawRect(tableLeft, tableTop, tableRight, tableTop + rowHeight, linePaint)
        canvas.drawLine(col1Right, tableTop, col1Right, tableTop + rowHeight, linePaint)

        val headerTextY = tableTop + (rowHeight - tableBoldPaint.textSize) / 2f + tableBoldPaint.textSize
        val hdr1 = "NOMBRE Y APELLIDOS DEL CONDUCTOR"
        val hdr1W = tableBoldPaint.measureText(hdr1)
        canvas.drawText(hdr1, tableLeft + (col1Width - hdr1W) / 2f, headerTextY, tableBoldPaint)
        val hdr2 = "D.N.I."
        val hdr2W = tableBoldPaint.measureText(hdr2)
        canvas.drawText(hdr2, col1Right + (col2Width - hdr2W) / 2f, headerTextY, tableBoldPaint)

        // Data row
        val dataRowTop = tableTop + rowHeight
        canvas.drawRect(tableLeft, dataRowTop, tableRight, dataRowTop + rowHeight, linePaint)
        canvas.drawLine(col1Right, dataRowTop, col1Right, dataRowTop + rowHeight, linePaint)

        val dataTextY = dataRowTop + (rowHeight - tableTextPaint.textSize) / 2f + tableTextPaint.textSize
        canvas.drawText("D./Dña. ${data.nombreApellidos.ifBlank { "________" }}", tableLeft + mmToPt(2f), dataTextY, tableTextPaint)
        canvas.drawText(data.numeroDocumento.ifBlank { "________" }, col1Right + mmToPt(2f), dataTextY, tableTextPaint)

        y = dataRowTop + rowHeight + mmToPt(5f)

        // ═══════════════════════════════════════════════════════════════════
        // PÁRRAFO 2 - SINIESTRO VIAL
        // ═══════════════════════════════════════════════════════════════════
        val siniestroParts = data.fechaHoraSiniestro.split(" ", limit = 2)
        val siniestroFecha = siniestroParts.getOrElse(0) { "" }
        val siniestroHora = siniestroParts.getOrElse(1) { "" }
        val siniestroFechaSplit = siniestroFecha.split("-")
        val sinDia = siniestroFechaSplit.getOrElse(0) { "___" }
        val sinMes = siniestroFechaSplit.getOrElse(1) { "___" }
        val sinAnio = siniestroFechaSplit.getOrElse(2) { "____" }

        val paragraph2 = "La persona anterionmente identificada, se encuentra implicada en una investigación en curso sobre un" +
            " siniestro vial ocurrido a las ${siniestroHora.ifBlank { "___" }} horas del día ${sinDia.ifBlank { "____" }} de ${monthNumberToSpanish(sinMes)} del ${sinAnio.ifBlank { "____" }}" +
            " a la altura del punto kilométrico ${data.puntoKilometrico.ifBlank { "_______" }} , de la carretera ${data.carretera.ifBlank { "_______" }} ," +
            " término municipal de ${data.municipioOcurrencia.ifBlank { "_______" }} y Partido Judicial de ${data.partidoJudicial.ifBlank { "_______" }}" +
            " con el resultado de ${data.catalogacionHecho.ifBlank { "________________________________" }}"
        y = drawWrappedText(paragraph2, contentLeft, contentRight, y)
        y += mmToPt(3f)

        val paragraph3 = "por lo que se va a solicitar mandamiento judicial al Ilmo. Sr. Magistrado-Juez de la Sección de Instrucción del Tribunal de Instancia ${data.juzgadoCompetente.ifBlank { "________" }} ," +
            " para que ordene la práctica de pruebas analíticas sobre dichas muestras, al objeto de la determinación de la tasa" +
            " de alcohol en sangre y la cantidad de drogas tóxicas, estupefacientes y psicotrópicos presentes en su organismo," +
            " lo cual podría ser determinante como fuente de prueba para demostrar una posible imprudencia grave constitutiva" +
            " de delito y/o la comisión de un delito contra la seguridad vial"
        y = drawWrappedText(paragraph3, contentLeft, contentRight, y)
        y += mmToPt(3f)

        val paragraph4 = "Lo que participo a V., para su conocimiento y a los efectos pertinentes hasta tanto se pronuncie la Autoridad Judicial."
        y = drawWrappedText(paragraph4, contentLeft, contentRight, y)
        y += mmToPt(8f)

        // ═══════════════════════════════════════════════════════════════════
        // FIRMA DEL AGENTE INSTRUCTOR
        // ═══════════════════════════════════════════════════════════════════
        val firmaCenterX = contentLeft + contentWidth / 2f
        val empleoText = data.empleoSolicitante.ifBlank { "______________" }
        val firmaLabel = "EL $empleoText  INSTRUCTOR"
        val firmaLabelW = boldPaint.measureText(firmaLabel)
        canvas.drawText(firmaLabel, firmaCenterX - firmaLabelW / 2f, y + boldPaint.textSize, boldPaint)
        y += mmToPt(20f)

        // Firma del agente (sin líneas)
        val sigLeft = firmaCenterX - mmToPt(30f)
        val sigRight = firmaCenterX + mmToPt(30f)
        val sigBottom = drawSignatureOnLine(agenteSignature, sigLeft, sigRight, y)
        y = sigBottom + mmToPt(4f)

        // TIP núm
        canvas.drawText("TIP núm.: ${data.tipSolicitante.ifBlank { "________" }}", firmaCenterX - mmToPt(15f), y + boldPaint.textSize, boldPaint)

        // ═══════════════════════════════════════════════════════════════════
        // FINALIZAR
        // ═══════════════════════════════════════════════════════════════════
        pdfDocument.finishPage(page)

        val directory = File(context.filesDir, "atestados").apply { mkdirs() }
        val file = File(directory, "OficioCustodiaSangre.pdf")
        FileOutputStream(file).use { output -> pdfDocument.writeTo(output) }
        pdfDocument.close()

        return AtestadoPdfResult(file = file, createdAtMillis = System.currentTimeMillis())
    }

    private fun loadTypefaceFromAssets(context: Context, path: String): Typeface? {
        return try {
            Typeface.createFromAsset(context.assets, path)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadBitmapFromAssets(context: Context, path: String): Bitmap? {
        return try {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }
}
