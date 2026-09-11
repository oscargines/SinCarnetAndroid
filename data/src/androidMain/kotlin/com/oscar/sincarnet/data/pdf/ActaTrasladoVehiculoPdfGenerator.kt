package com.oscar.sincarnet.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.oscar.sincarnet.domain.model.ActaTrasladoVehiculoData
import java.io.File
import java.io.FileOutputStream

object ActaTrasladoVehiculoPdfGenerator {
    fun buildPrintableText(data: ActaTrasladoVehiculoData): String = buildString {
        appendLine("ACTA TRASLADO VEHICULO")
        appendLine()
        appendLine("VEHICULO")
        appendLine("Tipo: ${data.vehiculoTipo}")
        appendLine("Marca: ${data.vehiculoMarca}")
        appendLine("Modelo: ${data.vehiculoModelo}")
        appendLine("Color: ${data.vehiculoColor}")
        appendLine("Matricula: ${data.vehiculoMatricula}")
        appendLine()
        appendLine("PERSONA")
        appendLine("Nombre: ${data.nombre} ${data.primerApellido} ${data.segundoApellido}".trim())
        appendLine("Documento: ${data.dniNie}")
        appendLine("Telefono: ${data.telefono}")
        appendLine()
        appendLine("INICIO DE LA ACTUACION")
        val inicioMotivo = when {
            data.inicioAuxilioAccidente -> "Auxilio accidente"
            data.inicioInfraccion -> "Infraccion"
            data.inicioOtroMotivo.isNotBlank() -> "Otro motivo: ${data.inicioOtroMotivo}"
            else -> "Pendiente"
        }
        appendLine("Motivo: $inicioMotivo")
        appendLine()
        appendLine("CIRCUNSTANCIAS QUE JUSTIFICAN EL TRASLADO")
        val circunstancias = buildList {
            if (data.circunstanciaFactoresAtmosfericos) add("Factores atmosfericos")
            if (data.circunstanciaMalaVisibilidad) add("Mala visibilidad")
            if (data.circunstanciaConfiguracionVia.isNotBlank()) add("Configuracion de la via: ${data.circunstanciaConfiguracionVia}")
            if (data.circunstanciaOtras.isNotBlank()) add("Otras: ${data.circunstanciaOtras}")
        }
        appendLine(if (circunstancias.isEmpty()) "Pendiente" else circunstancias.joinToString(" / "))
        appendLine()
        appendLine("TRASLADO")
        appendLine("Lugar inicio: ${data.inicioLugar}")
        appendLine("Fecha inicio: ${data.inicioFecha}")
        appendLine("Hora inicio: ${data.inicioHora}")
        appendLine("Lugar fin: ${data.finLugar}")
        appendLine("Fecha fin: ${data.finFecha}")
        appendLine("Hora fin: ${data.finHora}")
        appendLine("Unidad responsable: ${data.unidadResponsable}")
        appendLine("Telefono unidad: ${data.unidadTelefono}")
        appendLine("Agente TIP: ${data.agenteTip}")
        appendLine("Agente unidad: ${data.agenteUnidad}")
        appendLine()
        appendLine("CONSENTIMIENTO Y AUTORIZACION")
        appendLine("Consentimiento traslado: ${data.consentimientoTraslado?.let { if (it) "Si" else "No" } ?: "Pendiente"}")
        appendLine("Autoriza titular: ${if (data.autorizaTitular) "Si" else "No"}")
        appendLine("Autoriza conductor: ${if (data.autorizaConductor) "Si" else "No"}")
        appendLine("Firmante autoriza traslado: ${data.firmanteAutorizaTraslado?.let { if (it) "Si" else "No" } ?: "Pendiente"}")
        appendLine("Incidencia durante traslado: ${data.incidenciaDuranteTraslado?.let { if (it) "Si" else "No" } ?: "Pendiente"}")
        appendLine("Entrega llaves: ${data.entregaLlaves?.let { if (it) "Si" else "No" } ?: "Pendiente"}")
    }

    private const val A4_WIDTH_PT = 595f
    private const val A4_HEIGHT_PT = 842f
    private fun mmToPt(mm: Float): Float = mm * 72f / 25.4f
    private const val GENERATED_FILE_NAME = "acta_traslado_vehiculo.pdf"

    fun generatePdf(
        context: Context,
        data: ActaTrasladoVehiculoData,
        agenteSignature: Bitmap? = null,
        titularConductorSignature: Bitmap? = null
    ): AtestadoPdfResult {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT.toInt(), A4_HEIGHT_PT.toInt(), 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        Log.d("ActaTrasladoPdf", "Iniciando render PDF")

        val left = mmToPt(9f)
        val right = A4_WIDTH_PT - mmToPt(9f)
        val width = right - left
        var y = mmToPt(10f)

        val regularTypeface = Typeface.create("sans-serif", Typeface.NORMAL) ?: Typeface.SANS_SERIF
        val boldTypeface = Typeface.create("sans-serif-medium", Typeface.BOLD) ?: Typeface.DEFAULT_BOLD

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(3.8f)
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.45f)
            typeface = boldTypeface
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.55f)
            typeface = boldTypeface
        }
        val cellHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.35f)
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        val cellValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.35f)
            typeface = regularTypeface
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.25f)
            typeface = regularTypeface
        }
        val bodyItalicPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.18f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC) ?: regularTypeface
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.05f)
            typeface = regularTypeface
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(230, 230, 230)
            style = Paint.Style.FILL
        }
        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.15f)
            typeface = regularTypeface
        }

        fun textTop(paint: Paint): Float {
            val fm = paint.fontMetrics
            return -fm.ascent
        }

        fun lineHeight(paint: Paint, factor: Float = 1.15f): Float {
            val fm = paint.fontMetrics
            return (fm.descent - fm.ascent) * factor
        }

        fun drawCenteredText(text: String, boxLeft: Float, boxTop: Float, boxRight: Float, boxBottom: Float, paint: Paint) {
            val fm = paint.fontMetrics
            val baseline = boxTop + ((boxBottom - boxTop) - (fm.descent - fm.ascent)) / 2f - fm.ascent
            val oldAlign = paint.textAlign
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, (boxLeft + boxRight) / 2f, baseline, paint)
            paint.textAlign = oldAlign
        }

        fun drawLeftText(text: String, x: Float, baseline: Float, paint: Paint = bodyPaint) {
            canvas.drawText(text, x, baseline, paint)
        }

        fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
            canvas.drawLine(x1, y1, x2, y2, borderPaint)
        }

        fun drawRect(left: Float, top: Float, right: Float, bottom: Float, fill: Boolean = false) {
            if (fill) canvas.drawRect(left, top, right, bottom, fillPaint)
            canvas.drawRect(left, top, right, bottom, borderPaint)
        }

        fun drawCheckBox(x: Float, y: Float, size: Float, checked: Boolean) {
            canvas.drawRect(x, y, x + size, y + size, borderPaint)
            if (checked) {
                canvas.drawLine(x + size * 0.18f, y + size * 0.55f, x + size * 0.42f, y + size * 0.8f, borderPaint)
                canvas.drawLine(x + size * 0.42f, y + size * 0.8f, x + size * 0.82f, y + size * 0.22f, borderPaint)
            }
        }

        fun drawCheckboxLabel(label: String, x: Float, baseline: Float, checked: Boolean) {
            val size = mmToPt(2.8f)
            drawCheckBox(x, baseline - size + mmToPt(0.6f), size, checked)
            drawLeftText(label, x + size + mmToPt(1.4f), baseline, bodyPaint)
        }

        fun drawWrapped(text: String, x: Float, width: Float, top: Float, paint: Paint = bodyPaint): Float {
            val lines = wrapText(text, width, paint)
            var currentY = top + textTop(paint)
            lines.forEach {
                drawLeftText(it, x, currentY, paint)
                currentY += lineHeight(paint, 1.0f)
            }
            return currentY
        }

        fun drawSectionHeader(text: String): Float {
            val baseline = y + textTop(sectionPaint)
            drawLeftText(text, left, baseline, sectionPaint)
            y = baseline + mmToPt(1.1f)
            drawLine(left, y, right, y)
            y += mmToPt(2.2f)
            return y
        }

        fun drawLabelValueLine(label: String, value: String, splitRatio: Float = 0.33f, paint: Paint = bodyPaint): Float {
            val baseline = y + textTop(paint)
            val lineX = left + width * splitRatio
            drawLeftText(label, left, baseline, paint)
            drawLine(lineX, baseline + mmToPt(0.5f), right, baseline + mmToPt(0.5f))
            if (value.isNotBlank()) {
                drawLeftText(value, lineX + mmToPt(2f), baseline, paint)
            }
            y = baseline + lineHeight(paint, 1.15f)
            return y
        }

        fun drawRadioLine(label: String, selected: Boolean, x: Float): Float {
            val baseline = y + textTop(bodyPaint)
            drawCheckboxLabel(label, x, baseline, selected)
            return x
        }

        fun yesNoText(value: Boolean?): String = when (value) {
            true -> "Sí ☒   No ☐"
            false -> "Sí ☐   No ☒"
            null -> "Sí ☐   No ☐"
        }

        fun optionBox(label: String, checked: Boolean, x: Float, top: Float): Float {
            val boxSize = mmToPt(2.9f)
            drawCheckBox(x, top, boxSize, checked)
            drawLeftText(label, x + boxSize + mmToPt(1.2f), top + textTop(bodyPaint) - mmToPt(0.4f), bodyPaint)
            return x
        }

        val headerLine1 = y + textTop(headerPaint)
        drawLeftText("AGRUPACIÓN DE TRÁFICO DE LA GUARDIA CIVIL", left, headerLine1, headerPaint)
        val sectorText = if (data.sector.isBlank()) "____________________" else data.sector
        drawLeftText("SECTOR DE $sectorText", left + width * 0.62f, headerLine1, headerPaint)
        y += mmToPt(4.5f)
        val headerLine2 = y + textTop(headerPaint)
        val subsectorText = if (data.subsector.isBlank()) "____________________" else data.subsector
        val destacamentoText = if (data.destacamento.isBlank()) "____________________" else data.destacamento
        drawLeftText("SUBSECTOR DE TRÁFICO $subsectorText", left, headerLine2, headerPaint)
        drawLeftText("DESTACAMENTO DE $destacamentoText", left + width * 0.57f, headerLine2, headerPaint)
        y += mmToPt(7.5f)
        drawCenteredText(
            "ACTA AUTORIZACIÓN TRASLADO VEHÍCULO POR MOTIVOS DE SEGURIDAD VIAL",
            left,
            y,
            right,
            y + mmToPt(4.5f),
            titlePaint
        )
        y += mmToPt(6f)

        val vehicleHeaderH = mmToPt(5.7f)
        val vehicleColH = mmToPt(5.2f)
        val vehicleValueH = mmToPt(8.5f)
        val vehicleBottom = y + vehicleHeaderH + vehicleColH + vehicleValueH
        drawRect(left, y, right, vehicleBottom)
        drawRect(left, y, right, y + vehicleHeaderH, fill = true)
        drawCenteredText("V E H Í C U L O", left, y, right, y + vehicleHeaderH, cellHeaderPaint)
        val vehicleWidths = floatArrayOf(width * 0.19f, width * 0.19f, width * 0.18f, width * 0.14f, width * 0.30f)
        val vehicleTitles = listOf("TIPO", "MARCA", "MODELO", "COLOR", "MATRÍCULA")
        var x = left
        val headerTop = y + vehicleHeaderH
        val valueTop = headerTop + vehicleColH
        drawRect(left, headerTop, right, valueTop, fill = true)
        for (i in vehicleTitles.indices) {
            val w = vehicleWidths[i]
            if (i > 0) {
                drawLine(x, headerTop, x, vehicleBottom)
            }
            drawCenteredText(vehicleTitles[i], x, headerTop, x + w, headerTop + vehicleColH, cellHeaderPaint)
            drawCenteredText(
                listOf(data.vehiculoTipo, data.vehiculoMarca, data.vehiculoModelo, data.vehiculoColor, data.vehiculoMatricula)[i],
                x + mmToPt(1f),
                valueTop,
                x + w - mmToPt(1f),
                vehicleBottom,
                cellValuePaint
            )
            x += w
        }
        drawLine(left, headerTop, right, headerTop)
        drawLine(left, valueTop, right, valueTop)
        y = vehicleBottom + mmToPt(2f)

        val userHeaderH = mmToPt(5.7f)
        val userColH = mmToPt(5.2f)
        val userValueH = mmToPt(8.3f)
        val userBottom = y + userHeaderH + userColH + userValueH
        drawRect(left, y, right, userBottom)
        drawRect(left, y, right, y + userHeaderH, fill = true)
        val userHeaderFm = headerPaint.fontMetrics
        val userHeaderBaseline = y + (userHeaderH - (userHeaderFm.descent - userHeaderFm.ascent)) / 2f - userHeaderFm.ascent
        drawLeftText("DATOS DEL:", left + mmToPt(1.2f), userHeaderBaseline, headerPaint)
        val checkboxSize = mmToPt(2.8f)
        val checkboxY = y + (userHeaderH - checkboxSize) / 2f
        val titularCheckX = left + width * 0.40f
        drawCheckBox(titularCheckX, checkboxY, checkboxSize, data.esTitular)
        drawLeftText("TITULAR", titularCheckX + checkboxSize + mmToPt(1.3f), userHeaderBaseline, headerPaint)
        val conductorCheckX = left + width * 0.67f
        drawCheckBox(conductorCheckX, checkboxY, checkboxSize, data.esConductor)
        drawLeftText("CONDUCTOR", conductorCheckX + checkboxSize + mmToPt(1.3f), userHeaderBaseline, headerPaint)
        val userWidths = floatArrayOf(width * 0.24f, width * 0.22f, width * 0.22f, width * 0.16f, width * 0.16f)
        val userTitles = listOf("NOMBRE", "1ER. APELLIDO", "2º APELLIDO", "D.N.I / N.I.E", "TELÉFONO")
        x = left
        val userHeaderRowTop = y + userHeaderH
        val userValueRowTop = userHeaderRowTop + userColH
        drawRect(left, userHeaderRowTop, right, userValueRowTop, fill = true)
        for (i in userTitles.indices) {
            val w = userWidths[i]
            if (i > 0) drawLine(x, userHeaderRowTop, x, userBottom)
            drawCenteredText(userTitles[i], x, userHeaderRowTop, x + w, userHeaderRowTop + userColH, cellHeaderPaint)
            drawCenteredText(
                listOf(data.nombre, data.primerApellido, data.segundoApellido, data.dniNie, data.telefono)[i],
                x + mmToPt(1f),
                userValueRowTop,
                x + w - mmToPt(1f),
                userBottom,
                cellValuePaint
            )
            x += w
        }
        drawLine(left, userHeaderRowTop, right, userHeaderRowTop)
        drawLine(left, userValueRowTop, right, userValueRowTop)
        y = userBottom + mmToPt(2f)

        drawSectionHeader("LA ACTUACIÓN DE LOS AGENTES CON EL USUARIO SE INICIA")
        val startBaseline = y + textTop(bodyPaint)
        drawCheckboxLabel("AUXILIO ACCIDENTE", left, startBaseline, data.inicioAuxilioAccidente)
        drawCheckboxLabel("INFRACCIÓN", left + width * 0.39f, startBaseline, data.inicioInfraccion)
        y += lineHeight(bodyPaint, 1.0f)
        val otherBaseline = y + textTop(bodyPaint)
        drawCheckboxLabel("Otro motivo (Indicar):", left, otherBaseline, data.inicioOtroMotivo.isNotBlank())
        drawLine(left + mmToPt(48f), otherBaseline + mmToPt(0.8f), right, otherBaseline + mmToPt(0.8f))
        if (data.inicioOtroMotivo.isNotBlank()) {
            drawLeftText(data.inicioOtroMotivo, left + mmToPt(49f), otherBaseline, bodyPaint)
        }
        y += lineHeight(bodyPaint, 1.15f)

        drawSectionHeader("CIRCUNSTANCIAS QUE JUSTIFICAN EL TRASLADO")
        val circBaseline = y + textTop(bodyPaint)
        drawCheckboxLabel("FACTORES ATMOSFÉRICOS", left, circBaseline, data.circunstanciaFactoresAtmosfericos)
        drawCheckboxLabel("MALA VISIBILIDAD", left + width * 0.37f, circBaseline, data.circunstanciaMalaVisibilidad)
        y += lineHeight(bodyPaint, 1.0f)
        val configBaseline = y + textTop(bodyPaint)
        drawCheckboxLabel("CONFIGURACIÓN DE LA VÍA (Indicar)", left, configBaseline, data.circunstanciaConfiguracionVia.isNotBlank())
        drawLine(left + mmToPt(60f), configBaseline + mmToPt(0.8f), right, configBaseline + mmToPt(0.8f))
        if (data.circunstanciaConfiguracionVia.isNotBlank()) {
            drawLeftText(data.circunstanciaConfiguracionVia, left + mmToPt(61f), configBaseline, bodyPaint)
        }
        y += lineHeight(bodyPaint, 1.0f)
        val otherCircBaseline = y + textTop(bodyPaint)
        drawCheckboxLabel("Otras (Indicar)", left, otherCircBaseline, data.circunstanciaOtras.isNotBlank())
        drawLine(left + mmToPt(34f), otherCircBaseline + mmToPt(0.8f), right, otherCircBaseline + mmToPt(0.8f))
        if (data.circunstanciaOtras.isNotBlank()) {
            drawLeftText(data.circunstanciaOtras, left + mmToPt(35f), otherCircBaseline, bodyPaint)
        }
        y += lineHeight(bodyPaint, 1.25f)

        drawSectionHeader("INFORMACIÓN AL USUARIO SOBRE TRASLADO DE SU VEHÍCULO")
        val infoWidth = width - mmToPt(2f)
        val infoTextTop = y
        y = drawWrapped(
            "El conductor o propietario del vehículo, podrá dar su consentimiento expreso para que, por parte de cualquiera de los Agentes, por motivos de seguridad, efectúen el traslado de su vehículo a una zona próxima donde quede inmovilizado conforme al art 104 y 105 LSV. A falta de su consentimiento, se procederá a dar aviso al servicio de grúa, trasladándose su vehículo para la inmovilización y siendo el importe satisfecho por el usuario.",
            left,
            infoWidth,
            infoTextTop,
            bodyItalicPaint
        ) + mmToPt(1.2f)
        y = drawWrapped(
            "En caso de que Vd., no abone el pago del servicio de grúa, se retirará igualmente el vehículo y se solicitará factura a nombre de la Dirección General de Tráfico, cuyo importe será repercutido, con posterioridad, por este organismo sobre él.",
            left,
            infoWidth,
            y,
            bodyItalicPaint
        ) + mmToPt(1.0f)
        y = drawWrapped(
            "En el caso de que el conductor no fuera el propietario del vehículo, como responsable temporal del mismo, se compromete a participar esta actuación al propietario de la forma más rápida posible.",
            left,
            infoWidth,
            y,
            bodyItalicPaint
        ) + mmToPt(1.4f)

        drawSectionHeader("INICIO / FIN DEL TRASLADO")
        drawLabelValueLine("INICIO DEL TRASLADO: Lugar:", data.inicioLugar)
        drawLabelValueLine("Fecha y hora:", "${data.inicioFecha}  ${data.inicioHora}".trim())
        drawLabelValueLine("FIN DEL TRASLADO: Lugar:", data.finLugar)
        drawLabelValueLine("Fecha y hora:", "${data.finFecha}  ${data.finHora}".trim())
        drawLabelValueLine("UNIDAD RESPONSABLE:", data.unidadResponsable)
        drawLabelValueLine("TELÉFONO:", data.unidadTelefono)
        drawLabelValueLine("AGENTE QUE EFECTÚA TRASLADO: T.I.P.", data.agenteTip)
        drawLabelValueLine("UNIDAD:", data.agenteUnidad)

        drawSectionHeader("CONSENTIMIENTO Y AUTORIZACIÓN")
        drawLabelValueLine(
            "DECLARO MI CONSENTIMIENTO EXPRESO PARA EL TRASLADO DEL VEHÍCULO:",
            yesNoText(data.consentimientoTraslado),
            splitRatio = 0.76f
        )
        drawLabelValueLine(
            "PERSONA QUE AUTORIZA:",
            "TITULAR ${if (data.autorizaTitular) "☒" else "☐"}   CONDUCTOR ${if (data.autorizaConductor) "☒" else "☐"}",
            splitRatio = 0.54f
        )
        drawLabelValueLine(
            "El abajo firmante autoriza el traslado de vehículo:",
            yesNoText(data.firmanteAutorizaTraslado),
            splitRatio = 0.76f
        )
        drawLabelValueLine(
            "Se ha producido alguna incidencia o daño en el vehículo durante el traslado:",
            yesNoText(data.incidenciaDuranteTraslado),
            splitRatio = 0.76f
        )
        drawLabelValueLine(
            "El conductor/propietario se hace nuevamente cargo de las llaves del mismo:",
            yesNoText(data.entregaLlaves),
            splitRatio = 0.76f
        )

        y += mmToPt(4f)
        val sigTop = y
        val sigMid = left + width * 0.52f
        val sigLineY = sigTop + mmToPt(12f)
        drawLine(left, sigLineY, sigMid - mmToPt(5f), sigLineY)
        drawLine(sigMid + mmToPt(5f), sigLineY, right, sigLineY)
        drawLeftText("Firma Agente que ha trasladado", left + mmToPt(12f), sigLineY + mmToPt(4.5f), smallPaint)
        drawLeftText("Firma Titular/Conductor", sigMid + mmToPt(10f), sigLineY + mmToPt(4.5f), smallPaint)
        drawSignature(canvas, agenteSignature, left, sigMid - mmToPt(5f), sigLineY)
        drawSignature(canvas, titularConductorSignature, sigMid + mmToPt(5f), right, sigLineY)

        pdfDocument.finishPage(page)
        Log.d("ActaTrasladoPdf", "Página finalizada")

        val directory = File(context.filesDir, "atestados").apply { mkdirs() }
        Log.d("ActaTrasladoPdf", "Directorio de salida: ${directory.absolutePath}")
        directory.listFiles { candidate ->
            candidate.isFile && candidate.extension.equals("pdf", ignoreCase = true)
        }?.forEach { existingPdf ->
            if (existingPdf.name != GENERATED_FILE_NAME) {
                existingPdf.delete()
            }
        }
        val outputFile = File(directory, GENERATED_FILE_NAME)
        Log.d("ActaTrasladoPdf", "Escribiendo archivo: ${outputFile.absolutePath}")
        FileOutputStream(outputFile).use { output ->
            pdfDocument.writeTo(output)
        }
        pdfDocument.close()
        Log.d("ActaTrasladoPdf", "PDF escrito correctamente")

        return AtestadoPdfResult(file = outputFile, createdAtMillis = System.currentTimeMillis())
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
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

    private fun drawSignature(
        canvas: android.graphics.Canvas,
        signature: Bitmap?,
        left: Float,
        right: Float,
        baselineY: Float
    ) {
        if (signature == null) return
        val maxHeight = mmToPt(14f)
        val maxWidth = right - left
        val ratio = signature.width.toFloat() / signature.height.toFloat().coerceAtLeast(1f)
        var targetW = maxWidth
        var targetH = targetW / ratio
        if (targetH > maxHeight) {
            targetH = maxHeight
            targetW = targetH * ratio
        }
        val x = left + (maxWidth - targetW) / 2f
        val top = baselineY - targetH - mmToPt(1f)
        canvas.drawBitmap(signature, null, RectF(x, top, x + targetW, top + targetH), null)
    }
}
