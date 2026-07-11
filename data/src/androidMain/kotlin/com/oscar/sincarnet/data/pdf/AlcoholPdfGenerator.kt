package com.oscar.sincarnet.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.oscar.sincarnet.domain.model.ComplementarioAlcoholData
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object AlcoholPdfGenerator {

    private const val A4_WIDTH_PT = 595f
    private const val A4_HEIGHT_PT = 842f

    private fun mmToPt(mm: Float): Float = mm * 72f / 25.4f

    fun generateAlcoholPdf(
        context: Context,
        data: ComplementarioAlcoholData,
        agenteSignature: Bitmap? = null,
        operadorSignature: Bitmap? = null,
        personaSignature: Bitmap? = null
    ): AtestadoPdfResult {
        val pdfDocument = PdfDocument()
        val page = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(A4_WIDTH_PT.toInt(), A4_HEIGHT_PT.toInt(), 1).create()
        )
        val canvas = page.canvas

        val marginLeft = mmToPt(6f)
        val marginRight = mmToPt(6f)
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

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val shadedCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(236, 236, 236)
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.3f)
            typeface = regularTypeface
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(1.95f)
            typeface = regularTypeface
        }
        val tinyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(1.55f)
            typeface = regularTypeface
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.35f)
            typeface = boldTypeface
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.8f)
            typeface = boldTypeface
        }

        fun drawRect(l: Float, t: Float, r: Float, b: Float) = canvas.drawRect(l, t, r, b, linePaint)
        fun fillRect(l: Float, t: Float, r: Float, b: Float) = canvas.drawRect(l, t, r, b, shadedCellPaint)
        fun drawHLine(x1: Float, x2: Float, yy: Float) = canvas.drawLine(x1, yy, x2, yy, linePaint)
        fun drawVLine(xx: Float, y1: Float, y2: Float) = canvas.drawLine(xx, y1, xx, y2, linePaint)
        fun boxText(
            txt: String,
            l: Float,
            t: Float,
            r: Float,
            b: Float,
            paint: Paint = textPaint,
            leftPadMm: Float = 1.8f
        ) {
            val baseline = t + (b - t - (paint.descent() + paint.ascent())) / 2f
            canvas.drawText(txt, l + mmToPt(leftPadMm), baseline, paint)
        }
        fun drawFitSingleLineText(
            txt: String,
            left: Float,
            right: Float,
            yTop: Float,
            yBottom: Float,
            basePaint: Paint,
            minSizePt: Float = mmToPt(1.7f)
        ) {
            val p = Paint(basePaint)
            val maxWidth = (right - left) - mmToPt(4f)
            while (p.textSize > minSizePt && p.measureText(txt) > maxWidth) {
                p.textSize -= 0.2f
            }
            val baseline = yTop + (yBottom - yTop - (p.descent() + p.ascent())) / 2f
            val x = left + ((right - left) - p.measureText(txt)) / 2f
            canvas.drawText(txt, x, baseline, p)
        }
        fun boxMultiText(
            txt: String,
            l: Float,
            t: Float,
            r: Float,
            b: Float,
            paint: Paint = textPaint,
            leftPadMm: Float = 1.8f,
            topPadMm: Float = 1.2f,
            lineGapMm: Float = 0.5f
        ) {
            if (r <= l) return
            val lines = txt.split('\n')
            var yy = t + paint.textSize + mmToPt(topPadMm)
            val step = paint.textSize + mmToPt(lineGapMm)
            lines.forEach { line ->
                if (yy <= b - mmToPt(0.5f)) {
                    canvas.drawText(line, l + mmToPt(leftPadMm), yy, paint)
                }
                yy += step
            }
        }
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
        fun drawWrappedInCell(
            txt: String,
            l: Float,
            t: Float,
            r: Float,
            b: Float,
            paint: Paint = smallPaint,
            lineGapMm: Float = 0.45f
        ) {
            val maxWidth = (r - l) - mmToPt(3f)
            val lines = wrapTextToLines(txt, maxWidth, paint)
            var yy = t + paint.textSize + mmToPt(0.8f)
            val step = paint.textSize + mmToPt(lineGapMm)
            lines.forEach { line ->
                if (yy > b - mmToPt(0.6f)) return@forEach
                canvas.drawText(line, l + mmToPt(1.4f), yy, paint)
                yy += step
            }
        }
        fun drawCheck(isChecked: Boolean): String = if (isChecked) "☒" else "☐"
        fun drawSignatureOnLine(
            signature: Bitmap?,
            left: Float,
            right: Float,
            baselineY: Float,
            alignRight: Boolean = false,
            maxHeightMm: Float = 14f
        ) {
            if (signature == null) return
            val maxW = right - left
            val maxH = mmToPt(maxHeightMm)
            val ratio = signature.width.toFloat() / signature.height.toFloat().coerceAtLeast(1f)
            var targetW = maxW
            var targetH = targetW / ratio
            if (targetH > maxH) {
                targetH = maxH
                targetW = targetH * ratio
            }
            val x = if (alignRight) right - targetW else left + (maxW - targetW) / 2f
            val yTop = baselineY - targetH - mmToPt(1f)
            canvas.drawBitmap(signature, null, RectF(x, yTop, x + targetW, yTop + targetH), null)
        }
        fun checkedCase(caseCode: String): Boolean {
            val value = data.caso.lowercase(Locale.getDefault())
            return when (caseCode) {
                "A" -> value.contains("conductorvehiculos") || value.contains("conductor_vehiculos")
                "B" -> value.contains("usuarioaccidente") || value.contains("usuario_accidente")
                "C" -> value.contains("mercancias")
                "D" -> value.contains("novato")
                "E" -> value.contains("otros")
                else -> false
            }
        }

        // CABECERA INSTITUCIONAL
        val headerTop = y
        val headerHeight = mmToPt(25f)
        val headerBottom = headerTop + headerHeight

        val leftShieldX = contentLeft + mmToPt(7f)
        val leftShieldY = headerTop + mmToPt(4f)
        val leftShieldH = mmToPt(12.5f)
        var leftShieldW = mmToPt(10f)
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
        canvas.drawText("MINISTERIO", leftHeaderX, leftHeaderY, smallPaint)
        canvas.drawText("DEL", leftHeaderX, leftHeaderY + mmToPt(2.9f), smallPaint)
        canvas.drawText("INTERIOR", leftHeaderX, leftHeaderY + mmToPt(5.8f), smallPaint)

        val rightShieldH = mmToPt(12f)
        val rightShieldY = headerTop + mmToPt(4.6f)
        val rightBlockRight = contentRight - mmToPt(15f)
        val rightTitle = "GUARDIA CIVIL"
        val rightSubTitle = "DIRECCION GENERAL"
        val rightLine1 = "Mando de Operaciones"
        val rightLine2 = "Agrupación de Tráfico"
        val rightLine3 = "Área de Operaciones"
        val rightBlockTextWidth = maxOf(
            boldPaint.measureText(rightTitle),
            smallPaint.measureText(rightSubTitle),
            tinyPaint.measureText(rightLine1),
            tinyPaint.measureText(rightLine2),
            tinyPaint.measureText(rightLine3)
        )
        val rightX = rightBlockRight - rightBlockTextWidth
        var rightShieldW = mmToPt(10f)
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
        canvas.drawText(rightTitle, rightX, rightTop, boldPaint)
        canvas.drawText(rightSubTitle, rightX, rightTop + mmToPt(3f), smallPaint)
        val mandoTop = rightTop + mmToPt(6.4f)
        canvas.drawText(rightLine1, rightX, mandoTop, tinyPaint)
        canvas.drawText(rightLine2, rightX, mandoTop + mmToPt(2.7f), tinyPaint)
        canvas.drawText(rightLine3, rightX, mandoTop + mmToPt(5.3f), tinyPaint)

        y = headerBottom + mmToPt(2f)
        val annex = "ANEXO IV"
        val annexX = contentLeft + (contentWidth - sectionPaint.measureText(annex)) / 2f
        canvas.drawText(annex, annexX, y + sectionPaint.textSize, sectionPaint)
        y += mmToPt(9f)

        // TÍTULO
        val titleTop = y
        val titleBottom = titleTop + mmToPt(8.2f)
        fillRect(contentLeft, titleTop, contentRight, titleBottom)
        drawRect(contentLeft, titleTop, contentRight, titleBottom)
        val title =
            "ACTA DE REQUERIMIENTO PARA LA REALIZACIÓN DE PRUEBAS DE ALCOHOL O DROGAS A PETICIÓN DE OTROS CUERPOS POLICIALES, UNIDADES, U OTROS ORGANISMOS."
        drawFitSingleLineText(title, contentLeft, contentRight, titleTop, titleBottom, boldPaint)
        y = titleBottom + mmToPt(6f)

        // PÁRRAFO INTRODUCTORIO
        val intro =
            "Se procede a levantar el presente acta, al objeto de dejar constancia de las circunstancias que concurrieron en la realización de las pruebas de:"
        val introLines = wrapTextToLines(intro, contentWidth - mmToPt(3f), textPaint)
        introLines.forEach { line ->
            canvas.drawText(line, contentLeft + mmToPt(2f), y, textPaint)
            y += textPaint.textSize + mmToPt(0.5f)
        }
        y += mmToPt(2f)

        // PRUEBAS REALIZADAS
        val pruebaTop = y
        val pruebaBottom = pruebaTop + mmToPt(10f)
        drawRect(contentLeft, pruebaTop, contentRight, pruebaBottom)
        boxText("Pruebas realizadas", contentLeft, pruebaTop, contentLeft + mmToPt(45f), pruebaBottom, boldPaint)
        boxText(
            "${drawCheck(data.pruebasAlcohol)} Verificación del grado de impregnación alcohólica.",
            contentLeft + mmToPt(45f),
            pruebaTop,
            contentRight - mmToPt(80f),
            pruebaBottom
        )
        boxText(
            "${drawCheck(data.pruebasDrogas)} Presencia de drogas.",
            contentRight - mmToPt(80f),
            pruebaTop,
            contentRight,
            pruebaBottom
        )
        y = pruebaBottom + mmToPt(4f)

        // BLOQUE CENTRAL: DATOS, MOTIVO, IDENTIDAD, CALIDAD
        val middleTop = y
        val row1H = mmToPt(5.8f)
        val row2H = mmToPt(5.8f)
        val row3H = mmToPt(5.8f)
        val row4H = mmToPt(5.8f)
        val row5H = mmToPt(10.4f)
        val row6H = mmToPt(11.2f)
        val row7H = mmToPt(13f)
        val middleBottom = middleTop + row1H + row2H + row3H + row4H + row5H + row6H + row7H
        drawRect(contentLeft, middleTop, contentRight, middleBottom)

        var rowTop = middleTop

        // Fila fecha/hora/lugar
        val colDate = contentLeft + contentWidth * 0.21f
        val colHour = contentLeft + contentWidth * 0.36f
        val row1Bottom = rowTop + row1H
        drawHLine(contentLeft, contentRight, row1Bottom)
        drawVLine(colDate, rowTop, row1Bottom)
        drawVLine(colHour, rowTop, row1Bottom)
        boxText("Fecha: ${data.fecha}", contentLeft, rowTop, colDate, row1Bottom, boldPaint)
        boxText("Hora: ${data.hora}", colDate, rowTop, colHour, row1Bottom, boldPaint)
        boxText("Lugar: ${data.lugar}", colHour, rowTop, contentRight, row1Bottom, boldPaint)
        rowTop = row1Bottom

        // Fila término municipal / partido judicial
        val row2Bottom = rowTop + row2H
        val colTerm = contentLeft + contentWidth * 0.53f
        drawHLine(contentLeft, contentRight, row2Bottom)
        drawVLine(colTerm, rowTop, row2Bottom)
        boxText("Termino municipal: ${data.terminoMunicipal}", contentLeft, rowTop, colTerm, row2Bottom, boldPaint)
        boxText("Partido Judicial: ${data.partidoJudicial}", colTerm, rowTop, contentRight, row2Bottom, boldPaint)
        rowTop = row2Bottom

        // Fila unidad apoyo
        val row3Bottom = rowTop + row3H
        drawHLine(contentLeft, contentRight, row3Bottom)
        val tipoUnidad = data.tipoUnidad.lowercase(Locale.getDefault())
        val unidadLinea = buildString {
            append("Unidad que presta el apoyo   ")
            append("${drawCheck(tipoUnidad.contains("sector") && !tipoUnidad.contains("subsector"))}Sector   ")
            append("${drawCheck(tipoUnidad.contains("subsector"))}Subsector   ")
            append("${drawCheck(tipoUnidad.contains("destacamento"))}Destacamento: ${data.unidadNombre}")
        }
        boxText(unidadLinea, contentLeft, rowTop, contentRight, row3Bottom, boldPaint)
        rowTop = row3Bottom

        // Fila unidad/cuerpo + diligencias
        val row4Bottom = rowTop + row4H
        val colUnidad = contentLeft + contentWidth * 0.58f
        drawHLine(contentLeft, contentRight, row4Bottom)
        drawVLine(colUnidad, rowTop, row4Bottom)
        boxText("Unidad, cuerpo, organismo solicitante: ${data.cuerpoSolicitante}", contentLeft, rowTop, colUnidad, row4Bottom, boldPaint)
        boxText("Diligencias/expediente n°: ${data.diligenciasExpediente}", colUnidad, rowTop, contentRight, row4Bottom, boldPaint)
        rowTop = row4Bottom

        // Fila motivo (2 subfilas)
        val row5Bottom = rowTop + row5H
        drawHLine(contentLeft, contentRight, row5Bottom)
        val motivoLabelW = mmToPt(44f)
        val motivoX = contentLeft + motivoLabelW
        drawVLine(motivoX, rowTop, row5Bottom)
        val motivoMid = rowTop + row5H / 2f
        drawHLine(motivoX, contentRight, motivoMid)
        val mCol1 = motivoX + (contentRight - motivoX) / 3f
        val mCol2 = motivoX + (contentRight - motivoX) * 2f / 3f
        drawVLine(mCol1, rowTop, row5Bottom)
        drawVLine(mCol2, rowTop, row5Bottom)
        fillRect(contentLeft, rowTop, motivoX, row5Bottom)
        drawRect(contentLeft, rowTop, motivoX, row5Bottom)
        boxText("Motivo requerimiento", contentLeft, rowTop, motivoX, row5Bottom, boldPaint)
        boxText("${drawCheck(data.motivoSiniestroVial)} Siniestro vial", motivoX, rowTop, mCol1, motivoMid)
        boxText("${drawCheck(data.motivoSignosEvidentes)} Signos evidentes", mCol1, rowTop, mCol2, motivoMid)
        boxText("${drawCheck(data.motivoRequerimientoJudicial)} Requerimiento judicial", mCol2, rowTop, contentRight, motivoMid)
        boxText("${drawCheck(data.motivoInfraccionTrafico)} Infracción tráfico", motivoX, motivoMid, mCol1, row5Bottom)
        boxText("${drawCheck(data.motivoControlPreventivo)} Control preventivo", mCol1, motivoMid, mCol2, row5Bottom)
        boxText("${drawCheck(data.motivoOtros)} Otras: ${data.motivoOtrosDetalle}", mCol2, motivoMid, contentRight, row5Bottom)
        rowTop = row5Bottom

        // Fila identidad
        val row6Bottom = rowTop + row6H
        drawHLine(contentLeft, contentRight, row6Bottom)
        val idLabelW = mmToPt(44f)
        val idX = contentLeft + idLabelW
        drawVLine(idX, rowTop, row6Bottom)
        val idMid = rowTop + row6H / 2f
        drawHLine(idX, contentRight, idMid)
        val idDniX = idX + (contentRight - idX) * 0.62f
        val idNacX = idX + (contentRight - idX) * 0.31f
        val idTelX = idX + (contentRight - idX) * 0.86f
        drawVLine(idDniX, rowTop, idMid)
        drawVLine(idNacX, idMid, row6Bottom)
        drawVLine(idTelX, idMid, row6Bottom)
        fillRect(contentLeft, rowTop, idX, row6Bottom)
        drawRect(contentLeft, rowTop, idX, row6Bottom)
        boxMultiText("Identidad persona\nsometida", contentLeft, rowTop, idX, row6Bottom, boldPaint)
        boxText("Nombre/apellidos: ${data.personaNombre}", idX, rowTop, idDniX, idMid)
        boxText("Dni/Nie: ${data.personaDni}", idDniX, rowTop, contentRight, idMid)
        boxText(
            "Nacido el: ${data.personaFechaNacimiento.ifBlank { data.personaFechaExpedicion }}",
            idX,
            idMid,
            idNacX,
            row6Bottom
        )
        boxText("Domicilio: ${data.personaDomicilio}", idNacX, idMid, idTelX, row6Bottom)
        boxText("Tlf: ${data.personaTelefono}", idTelX, idMid, contentRight, row6Bottom)
        rowTop = row6Bottom

        // Fila en calidad de
        val row7Bottom = rowTop + row7H
        drawHLine(contentLeft, contentRight, row7Bottom)
        val calLabelW = mmToPt(44f)
        val calX = contentLeft + calLabelW
        drawVLine(calX, rowTop, row7Bottom)
        val calMid = rowTop + row7H * 0.7f
        drawHLine(calX, contentRight, calMid)
        val calRightMid = calX + (contentRight - calX) * 0.58f
        drawVLine(calRightMid, calMid, row7Bottom)
        fillRect(contentLeft, rowTop, calX, row7Bottom)
        drawRect(contentLeft, rowTop, calX, row7Bottom)
        val enCalidad = data.enCalidadDe.lowercase(Locale.getDefault())
        val isConductor = enCalidad.contains("conductor")
        val isOtrosUsuarios = enCalidad.contains("otrosusuarios") || enCalidad.contains("usuarios")
        val isOtros = enCalidad.contains("otros")
        boxText("En calidad de", contentLeft, rowTop, calX, row7Bottom, boldPaint)
        boxMultiText(
            "${drawCheck(isConductor)} Conductor vehículo a motor, ciclomotor o bicicleta.\nMarca/modelo/matrícula: ${data.vehiculoMarca}/${data.vehiculoModelo}/${data.vehiculoMatricula}",
            calX,
            rowTop,
            contentRight,
            calMid,
            textPaint,
            1.8f,
            1f,
            0.6f
        )
        boxText("${drawCheck(isOtrosUsuarios)} Otros usuarios de la vía:", calX, calMid, calRightMid, row7Bottom)
        boxText("${drawCheck(isOtros)} Otros: ${data.enCalidadOtros}", calRightMid, calMid, contentRight, row7Bottom)

        y = middleBottom + mmToPt(4f)

        // TABLA TASAS MÁXIMAS
        val tasasTop = y
        val tasasTitleH = mmToPt(6.2f)
        val tasasHeaderH = mmToPt(6.5f)
        val tasasRows = listOf(mmToPt(6.6f), mmToPt(6.2f), mmToPt(13.4f), mmToPt(9.1f), mmToPt(6f))
        val tasasBottom = tasasTop + tasasTitleH + tasasHeaderH + tasasRows.sum()
        drawRect(contentLeft, tasasTop, contentRight, tasasBottom)

        val tasasTitleBottom = tasasTop + tasasTitleH
        drawHLine(contentLeft, contentRight, tasasTitleBottom)
        canvas.drawText(
            "Tasas máximas permitidas:",
            contentLeft + mmToPt(2f),
            tasasTop + mmToPt(4.4f),
            boldPaint
        )
        canvas.drawText(
            "Real Decreto 1428/2003, de 21 de noviembre, por el que se aprueba el Reglamento General de Circulación.",
            contentLeft + mmToPt(42f),
            tasasTop + mmToPt(4.2f),
            tinyPaint
        )

        val caseWidth = mmToPt(14f)
        val equalAlcoholDrugWidth = mmToPt(56f)
        val c1 = contentLeft + caseWidth
        val c3 = contentRight - equalAlcoholDrugWidth
        val c2 = c3 - equalAlcoholDrugWidth
        val tasasHeaderBottom = tasasTitleBottom + tasasHeaderH
        drawHLine(contentLeft, contentRight, tasasHeaderBottom)
        drawVLine(c1, tasasTitleBottom, tasasBottom)
        drawVLine(c2, tasasTitleBottom, tasasBottom)
        drawVLine(c3, tasasTitleBottom, tasasBottom)
        fillRect(contentLeft, tasasTitleBottom, c1, tasasHeaderBottom)
        fillRect(c1, tasasTitleBottom, c2, tasasHeaderBottom)
        fillRect(c2, tasasTitleBottom, c3, tasasHeaderBottom)
        fillRect(c3, tasasTitleBottom, contentRight, tasasHeaderBottom)
        drawRect(contentLeft, tasasTitleBottom, c1, tasasHeaderBottom)
        drawRect(c1, tasasTitleBottom, c2, tasasHeaderBottom)
        drawRect(c2, tasasTitleBottom, c3, tasasHeaderBottom)
        drawRect(c3, tasasTitleBottom, contentRight, tasasHeaderBottom)
        boxText("CASO", contentLeft, tasasTitleBottom, c1, tasasHeaderBottom, boldPaint, 1.2f)
        boxText("PERSONA SOMETIDA A PRUEBA", c1, tasasTitleBottom, c2, tasasHeaderBottom, smallPaint, 1f)
        boxText("ALCOHOL EN AIRE ESPIRADO", c2, tasasTitleBottom, c3, tasasHeaderBottom, smallPaint, 1f)
        boxText("DROGAS EN FLUIDO ORAL", c3, tasasTitleBottom, contentRight, tasasHeaderBottom, smallPaint, 1f)

        val tasaRows = listOf(
            Triple(
                "A",
                "Conductor de vehículos y bicicletas.",
                Pair("0,25 mg/l", "Mera presencia.")
            ),
            Triple(
                "B",
                "Usuario de la vía implicado en accidente o comisión infracción.",
                Pair("Persona obligada a someterse, sin tasa específica", "")
            ),
            Triple(
                "C",
                "Conductor de vehículos destinados a transporte de mercancías con PMA. Superior a 3.500 kg, vehículos destinados al transporte de viajeros de más de 9 plazas o de servicio público, escolar o de menores, mercancías peligrosas, de servicio de urgencias o transportes especiales.",
                Pair("0,15 mg/l", "Mera presencia")
            ),
            Triple(
                "D",
                "Conductores de cualquier vehículo, durante los dos años siguientes a la obtención del permiso o licencia que les habilita para conducir.",
                Pair("0,15 mg/l", "Mera presencia")
            ),
            Triple(
                "E",
                "Otras en casos ajenos a la seguridad vial.",
                Pair("Sin tasa específica, en su caso normativa aplicable particular", "")
            )
        )

        var tasaY = tasasHeaderBottom
        tasaRows.forEachIndexed { index, row ->
            val h = tasasRows[index]
            val next = tasaY + h
            drawHLine(contentLeft, contentRight, next)
            boxText("${drawCheck(checkedCase(row.first))}", contentLeft, tasaY, c1, next, boldPaint, 7f)
            drawWrappedInCell(row.second, c1, tasaY, c2, next, tinyPaint)
            drawWrappedInCell(row.third.first, c2, tasaY, c3, next, smallPaint)
            drawWrappedInCell(row.third.second, c3, tasaY, contentRight, next, smallPaint)
            tasaY = next
        }

        y = tasasBottom + mmToPt(4f)

        // TABLA INSTRUMENTOS + RESULTADOS
        val instTop = y
        val instLabelW = mmToPt(36f)
        val instBodyX = contentLeft + instLabelW
        val instMidX = instBodyX + (contentRight - instBodyX) / 2f
        val instHeaderH = mmToPt(7f)
        val instRowH = mmToPt(6.2f)
        val instRowsCount = 8
        val instBottom = instTop + instHeaderH + instRowH * instRowsCount

        drawRect(contentLeft, instTop, contentRight, instBottom)
        drawVLine(instBodyX, instTop, instBottom)
        drawVLine(instMidX, instTop, instBottom)
        drawHLine(instBodyX, contentRight, instTop + instHeaderH)
        fillRect(contentLeft, instTop, instBodyX, instTop + instHeaderH + instRowH * 5f)
        drawRect(contentLeft, instTop, instBodyX, instTop + instHeaderH + instRowH * 5f)
        boxMultiText("INSTRUMENTO\nUTILIZADO", contentLeft, instTop, instBodyX, instTop + instHeaderH + instRowH * 5f, boldPaint)
        boxText("Etilómetro evidencial", instBodyX, instTop, instMidX, instTop + instHeaderH, boldPaint)
        boxText("Lector de drogas", instMidX, instTop, contentRight, instTop + instHeaderH, boldPaint)

        var instY = instTop + instHeaderH
        repeat(5) {
            instY += instRowH
            drawHLine(instBodyX, contentRight, instY)
        }

        val fechaHora = if (data.lectorDrogasHoraVerificacion.isNotBlank()) data.lectorDrogasHoraVerificacion else ""
        val instrumentRows = listOf(
            Pair("Marca: ${data.etilometroMarca}", "Marca: ${data.lectorDrogasMarca}"),
            Pair("Modelo: ${data.etilometroModelo}", "Modelo: ${data.lectorDrogasModelo}"),
            Pair("N° serie: ${data.etilometroNumeroSerie}", "N° serie: ${data.lectorDrogasNumeroSerie}"),
            Pair("Fecha verificación periódica: ${data.etilometroHoraVerificacion}", "Hora: $fechaHora"),
            Pair("Agente operador TIP: ${data.etilometroAgente}", "Agente operador TIP: ${data.lectorDrogasAgente}")
        )
        var rowY = instTop + instHeaderH
        instrumentRows.forEach { (left, right) ->
            boxText(left, instBodyX, rowY, instMidX, rowY + instRowH)
            boxText(right, instMidX, rowY, contentRight, rowY + instRowH)
            rowY += instRowH
        }

        val resultsTop = instTop + instHeaderH + instRowH * 5f
        drawHLine(contentLeft, contentRight, resultsTop)
        fillRect(contentLeft, resultsTop, instBodyX, instBottom)
        drawRect(contentLeft, resultsTop, instBodyX, instBottom)
        boxMultiText("RESULTADOS\nOBTENIDOS", contentLeft, resultsTop, instBodyX, instBottom, boldPaint)

        val result1Bottom = resultsTop + instRowH
        val result2Bottom = result1Bottom + instRowH
        val result3Bottom = result2Bottom + instRowH
        drawHLine(instBodyX, contentRight, result1Bottom)
        drawHLine(instBodyX, contentRight, result2Bottom)
        drawHLine(instBodyX, contentRight, result3Bottom)

        val alcoholHour1 = data.primeraPruebaHora.ifBlank { " " }
        val alcoholHour2 = data.segundaPruebaHora.ifBlank { " " }
        val alcoholRes1 = data.primeraPruebaResultado.ifBlank { " " }
        val alcoholRes2 = data.segundaPruebaResultado.ifBlank { " " }
        val contrasteAlcohol = data.pruebaContrasteAlcohol.lowercase(Locale.getDefault())
        val alcSi = contrasteAlcohol.contains("si")
        val alcNo = contrasteAlcohol.contains("no")

        boxText("1ª prueba: $alcoholRes1 mg/l    Hora: $alcoholHour1", instBodyX, resultsTop, instMidX, result1Bottom)
        boxText("2ª prueba: $alcoholRes2 mg/l    Hora: $alcoholHour2", instBodyX, result1Bottom, instMidX, result2Bottom)
        boxText("Desea prueba de contraste:   ${drawCheck(alcSi)} Sí   ${drawCheck(alcNo)} No", instBodyX, result2Bottom, instMidX, result3Bottom)

        val drogasRes = data.pruebaDrogasResultado.lowercase(Locale.getDefault())
        val neg = drogasRes.contains("neg")
        val pos = drogasRes.contains("pos")
        val drugType = data.pruebaDrogasTipo.uppercase(Locale.getDefault())
        val hasTHC = drugType.contains("THC")
        val hasOPI = drugType.contains("OPI")
        val hasCOC = drugType.contains("COC")
        val hasAMP = drugType.contains("AMP") && !drugType.contains("MAMP")
        val hasMAMP = drugType.contains("MAMP")
        val contrasteDrug = data.pruebaDrogasContraste.lowercase(Locale.getDefault())
        val dSi = contrasteDrug.contains("si")
        val dNo = contrasteDrug.contains("no")

        boxText("${drawCheck(neg)} Negativa", instMidX, resultsTop, contentRight, result1Bottom)
        drawWrappedInCell(
            "${drawCheck(pos)} Positiva: ${drawCheck(hasTHC)} THC  ${drawCheck(hasOPI)} OPI  ${drawCheck(hasCOC)} COC  ${drawCheck(hasAMP)} AMP  ${drawCheck(hasMAMP)} MAMP",
            instMidX,
            result1Bottom,
            contentRight,
            result2Bottom,
            smallPaint,
            0.25f
        )
        boxText("Desea prueba de contraste:   ${drawCheck(dSi)} Sí   ${drawCheck(dNo)} No", instMidX, result2Bottom, contentRight, result3Bottom)

        y = instBottom + mmToPt(4f)

        // TEXTO FINAL
        val finalText =
            "Finalizada la actuación de los agentes que realizan las pruebas, se entregan a la autoridad/agente solicitante los “tiques” impresos aportados por el instrumento utilizado con el resultado de las mismas. Asimismo, se le informa que en el caso de que la prueba de detección del consumo de drogas haya resultado positiva, se tomará otra muestra para análisis confirmatorio en laboratorio, garantizando la cadena de custodia. El informe emitido por el mismo será enviado a la autoridad, cuerpo u organismo solicitante."
        val finalLines = wrapTextToLines(finalText, contentWidth - mmToPt(12f), textPaint)
        finalLines.forEach { line ->
            canvas.drawText(line, contentLeft + mmToPt(6f), y, textPaint)
            y += textPaint.textSize + mmToPt(0.35f)
        }
        y += mmToPt(5f)

        // FIRMAS
        val line1Start = contentLeft + mmToPt(8f)
        val line1End = contentLeft + mmToPt(62f)
        val line2Start = contentLeft + mmToPt(70f)
        val line2End = contentLeft + mmToPt(124f)
        val line3Start = contentLeft + mmToPt(132f)
        val line3End = contentRight - mmToPt(8f)
        canvas.drawText("Firma de la autoridad o agente solicitante", line1Start, y, boldPaint)
        canvas.drawText("Firma del agente operador", line2Start, y, boldPaint)
        canvas.drawText("Firma de la persona sometida", line3Start, y, boldPaint)
        y += mmToPt(13f)
        drawHLine(line1Start, line1End, y)
        drawHLine(line2Start, line2End, y)
        drawHLine(line3Start, line3End, y)
        drawSignatureOnLine(agenteSignature, line1Start, line1End, y, alignRight = true, maxHeightMm = 8f)
        drawSignatureOnLine(operadorSignature, line2Start, line2End, y, alignRight = true, maxHeightMm = 8f)
        drawSignatureOnLine(personaSignature, line3Start, line3End, y, alignRight = true, maxHeightMm = 8f)

        y += mmToPt(8f)
        val legal1 =
            "ACTA DE REQUERIMIENTO PARA LA REALIZACIÓN DE PRUEBAS DE ALCOHOL O DROGAS A PETICIÓN DE OTROS CUERPOS POLICIALES, UNIDADES, U OTROS ORGANISMOS, según Procedimiento Operativo 2017."
        val legal2 =
            "De uso obligatorio en todas las unidades de la Agrupación de Tráfico de la Guardia Civil."
        val legal3 = "No está permitida la modificación de formatos o de cualquier de sus apartados."
        canvas.drawText(legal1, contentLeft + mmToPt(1f), y, tinyPaint)
        canvas.drawText(legal2, contentLeft + mmToPt(1f), y + mmToPt(2.3f), tinyPaint)
        canvas.drawText(legal3, contentLeft + mmToPt(1f), y + mmToPt(4.6f), tinyPaint)

        pdfDocument.finishPage(page)

        val directory = File(context.filesDir, "atestados").apply { mkdirs() }
        val file = File(directory, "ActaColaboracion.pdf")
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
            context.assets.open(path).use { input -> BitmapFactory.decodeStream(input) }
        } catch (_: Exception) {
            null
        }
    }
}
