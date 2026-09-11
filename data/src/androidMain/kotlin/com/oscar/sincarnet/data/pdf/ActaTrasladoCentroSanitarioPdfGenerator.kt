package com.oscar.sincarnet.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.oscar.sincarnet.domain.model.ActaTrasladoCentroSanitarioData
import java.io.File
import java.io.FileOutputStream

object ActaTrasladoCentroSanitarioPdfGenerator {

    private const val A4_WIDTH_PT = 595f
    private const val A4_HEIGHT_PT = 842f

    private fun mmToPt(mm: Float): Float = mm * 72f / 25.4f

    fun generatePdf(
        context: Context,
        data: ActaTrasladoCentroSanitarioData,
        interesadoSignature: Bitmap? = null,
        facultativoSignature: Bitmap? = null,
        sanitarioSignature: Bitmap? = null,
        agenteSignature: Bitmap? = null
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
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.35f)
            typeface = boldTypeface
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.6f)
            typeface = boldTypeface
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(2.45f)
            typeface = boldTypeface
        }
        val tinyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = mmToPt(1.55f)
            typeface = regularTypeface
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
            lineGapMm: Float = 0.35f
        ): Float {
            val maxWidth = right - left - mmToPt(4f)
            val lines = wrapTextToLines(txt, maxWidth, paint)
            var yy = startY
            lines.forEach { line ->
                canvas.drawText(line, left + mmToPt(2f), yy, paint)
                yy += paint.textSize + mmToPt(lineGapMm)
            }
            return yy
        }

        fun drawCheck(isChecked: Boolean): String = if (isChecked) "☒" else "☐"

        fun drawSignatureOnLine(
            signature: Bitmap?,
            left: Float,
            right: Float,
            baselineY: Float,
            maxHeightMm: Float = 10f
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
            val x = left + (maxW - targetW) / 2f
            val yTop = baselineY - targetH - mmToPt(1f)
            canvas.drawBitmap(signature, null, RectF(x, yTop, x + targetW, yTop + targetH), null)
        }

        // ═══════════════════════════════════════════════════════════════════
        // CABECERA INSTITUCIONAL
        // ═══════════════════════════════════════════════════════════════════
        val headerTop = y
        val headerHeight = mmToPt(25f)
        val headerBottom = headerTop + headerHeight

        // Escudo de España (izquierda)
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

        // Escudo Guardia Civil (derecha)
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

        // Subtítulo del documento
        val tituloLinea1 = "Título II: ALCOHOL Y OTRAS DROGAS-VENCIÓN 2018.- ANEXO XIV"
        canvas.drawText(tituloLinea1, contentLeft + mmToPt(1f), y + smallPaint.textSize, smallPaint)
        y += mmToPt(4f)

        val tituloLinea2 = "AGRUPACIÓN DE TRÁFICO DE LA GUARDIA CIVIL"
        canvas.drawText(tituloLinea2, contentLeft + mmToPt(1f), y + boldPaint.textSize, boldPaint)
        y += mmToPt(4f)

        // Subsector y Unidad interviniente
        canvas.drawText("SUBSECTOR: ${data.subsector.ifBlank { "__________" }}", contentLeft + mmToPt(1f), y + textPaint.textSize, textPaint)
        y += mmToPt(3.5f)
        canvas.drawText("UNIDAD INTERVINIENTE: ${data.unidadInterviniente.ifBlank { "__________" }}", contentLeft + mmToPt(1f), y + textPaint.textSize, textPaint)
        y += mmToPt(3.5f)
        canvas.drawText("N.º DE EXPEDIENTE /DILIGENCIAS: ${data.numeroExpediente.ifBlank { "__________" }}", contentLeft + mmToPt(1f), y + textPaint.textSize, textPaint)
        y += mmToPt(5f)

        // TÍTULO PRINCIPAL
        val mainTitle = "ACTA REALIZACIÓN PRUEBAS DE DETECCIÓN DE ALCOHOL Y/O PRESENCIA DE DROGAS MEDIANTE RECONOCIMIENTO MÉDICO O ANÁLISIS CLÍNICOS POR RAZONES JUSTIFICADAS"
        val titleLines = wrapTextToLines(mainTitle, contentWidth - mmToPt(4f), titlePaint)
        titleLines.forEach { line ->
            val lineWidth = titlePaint.measureText(line)
            canvas.drawText(line, contentLeft + (contentWidth - lineWidth) / 2f, y + titlePaint.textSize, titlePaint)
            y += titlePaint.textSize + mmToPt(0.8f)
        }
        y += mmToPt(3f)

        // ═══════════════════════════════════════════════════════════════════
        // DATOS DEL LUGAR DE LA INTERVENCIÓN POLICIAL
        // ═══════════════════════════════════════════════════════════════════
        val lugarTop = y
        val lugarH1 = mmToPt(5f)
        val lugarH2 = mmToPt(5f)
        val lugarH3 = mmToPt(5f)
        val lugarBottom = lugarTop + lugarH1 + lugarH2 + lugarH3

        drawRect(contentLeft, lugarTop, contentRight, lugarBottom)
        drawRect(contentLeft, lugarTop, contentRight, lugarTop + lugarH1, )
        boxText("Datos del lugar de la intervención policial:", contentLeft, lugarTop, contentRight, lugarTop + lugarH1, boldPaint)

        drawHLine(contentLeft, contentRight, lugarTop + lugarH1)
        drawHLine(contentLeft, contentRight, lugarTop + lugarH1 + lugarH2)

        val campoLugarW = contentWidth * 0.55f
        val campoMotivoW = contentWidth * 0.25f
        val campoFechaW = contentWidth * 0.20f

        boxText(data.lugarIntervencion.ifBlank { " " }, contentLeft, lugarTop + lugarH1, contentLeft + campoLugarW, lugarTop + lugarH1 + lugarH2, textPaint)
        drawVLine(contentLeft + campoLugarW, lugarTop + lugarH1, lugarTop + lugarH1 + lugarH2)
        boxText("Motivo: ${data.motivo.ifBlank { " " }}", contentLeft + campoLugarW, lugarTop + lugarH1, contentLeft + campoLugarW + campoMotivoW, lugarTop + lugarH1 + lugarH2, textPaint)
        drawVLine(contentLeft + campoLugarW + campoMotivoW, lugarTop + lugarH1, lugarTop + lugarH1 + lugarH2)
        boxText("Fecha y hora: ${data.fechaHora.ifBlank { " " }}", contentLeft + campoLugarW + campoMotivoW, lugarTop + lugarH1, contentRight, lugarTop + lugarH1 + lugarH2, smallPaint)

        y = lugarBottom + mmToPt(3f)

        // ═══════════════════════════════════════════════════════════════════
        // TEXTO LEGAL 1)
        // ═══════════════════════════════════════════════════════════════════
        val legalText1 = "1) No puede circular por las vías objeto de la legislación sobre Tráfico, Circulación de Vehículos a Motor y Seguridad Vial, el conductor de cualquier vehículo con las tasas de alcohol en sangre superiores a las legalmente establecidos, tampoco puede hacerlo el conductor de cualquier vehículo con presencia de drogas en el organismo (Art. 14.1 LSV). El conductor de un vehículo está obligado a someterse a las pruebas para la detección de alcohol o de drogas en su organismo que en cada caso sean requeridas por los agentes de la autoridad competentes en materia de tráfico. (Art. 14.3 LSV). El personal sanitario que colabore con la autoridad podrá practicar las pruebas analíticas que tenga por convenientes en los supuestos que hagan infringir el articulo 14.1 (Art. 20.1.c). Igualmente quedan obligados los demás usuarios de la vía cuando se hallen implicados en un accidente de circulación (Art. 14.3 LSV)."
        y = drawWrappedText(legalText1, contentLeft, contentRight, y, smallPaint, 0.2f)
        y += mmToPt(2f)

        val legalText2 = "2) Las pruebas para la detección de alcohol consisten en la medición del grado de alcohol en el aire espirado mediante dispositivos autorizados. y/o para la detección de la presencia de drogas, en un principio solo mediante dispositivos autorizados o en un posterior análisis de una muestra salival en un centro sanitario. Las pruebas para la detección de drogas en el organismo serán las que determine el personal sanitario que se haga cargo de la muestra. No obstante, se podrán utilizar dispositivos homologados para la realización de los análisis preliminares, debiendo tenerse en cuenta que el resultado del mismo nunca podrá ser utilizado como medio de prueba, y su única finalidad será la de orientar a los agentes para determinar la existencia de un posible delito contra la seguridad vial. (Art. 14.3 LSV). La negativa a la realización de las pruebas será constitutiva de un delito contra la seguridad vial (Art. 383 del Código Penal). Se tendrá en cuenta lo dispuesto en la Orden del Ministerio del Interior de 8 de julio de 2004."
        y = drawWrappedText(legalText2, contentLeft, contentRight, y, smallPaint, 0.2f)
        y += mmToPt(2f)

        val legalText3 = "3) Se podrá realizar un análisis de sangre u otro análogo cuando exista la creencia fundada de que se ha producido un accidente de tráfico con consecuencias personales o que el conductor ha cometido una infracción grave. El personal sanitario que colabore con la autoridad podrá practicar las pruebas analíticas que tenga por convenientes en los supuestos que hagan infringir el articulo 14.1 (Art. 20.1.c)."
        y = drawWrappedText(legalText3, contentLeft, contentRight, y, smallPaint, 0.2f)
        y += mmToPt(3f)

        // ═══════════════════════════════════════════════════════════════════
        // DATOS DEL INTERESADO
        // ═══════════════════════════════════════════════════════════════════
        val personaTop = y
        val personaH = mmToPt(7f)
        drawRect(contentLeft, personaTop, contentRight, personaTop + personaH)
        drawRect(contentLeft, personaTop, contentRight, personaTop + mmToPt(4f), )
        boxText("Datos del interesado (conductor, otro usuario): Nombre y apellidos", contentLeft, personaTop, contentRight - mmToPt(50f), personaTop + mmToPt(4f), boldPaint)
        drawHLine(contentLeft, contentRight, personaTop + mmToPt(4f))
        boxText(data.nombreApellidos.ifBlank { " " }, contentLeft, personaTop + mmToPt(4f), contentRight - mmToPt(50f), personaTop + personaH, textPaint)
        drawVLine(contentRight - mmToPt(50f), personaTop, personaTop + personaH)
        boxText("DNI, NIE o NªPas nº:", contentRight - mmToPt(50f), personaTop, contentRight, personaTop + mmToPt(4f), boldPaint)
        boxText(data.dniNiePasaporte.ifBlank { " " }, contentRight - mmToPt(50f), personaTop + mmToPt(4f), contentRight, personaTop + personaH, textPaint)
        y = personaTop + personaH + mmToPt(2f)

        // ═══════════════════════════════════════════════════════════════════
        // FACULTATIVO
        // ═══════════════════════════════════════════════════════════════════
        val facText = "El facultativo cuya identificación se consigna, considera, que existen razones justificadas que impiden realizar las pruebas de alcoholemia y/o drogas por parte de los agentes de la autoridad encargados de la vigilancia del tráfico, dichas razones son:"
        y = drawWrappedText(facText, contentLeft, contentRight, y, smallPaint, 0.2f)
        y += mmToPt(1f)

        val facTop = y
        val facH = mmToPt(5f)
        drawRect(contentLeft, facTop, contentRight, facTop + facH)
        drawRect(contentLeft, facTop, contentRight - mmToPt(40f), facTop + facH, )
        boxText("Colegiado nº:", contentLeft, facTop, contentRight - mmToPt(40f), facTop + facH, boldPaint)
        boxText(data.facultativoColegiado.ifBlank { " " }, contentRight - mmToPt(40f), facTop, contentRight, facTop + facH, textPaint)
        y = facTop + facH + mmToPt(3f)

        // ═══════════════════════════════════════════════════════════════════
        // PRUEBAS O ANÁLISIS
        // ═══════════════════════════════════════════════════════════════════
        val pruebasTop = y
        val pruebasH = mmToPt(10f)
        drawRect(contentLeft, pruebasTop, contentRight, pruebasTop + pruebasH)
        drawRect(contentLeft, pruebasTop, contentRight, pruebasTop + mmToPt(4f), )
        boxText("Pruebas o análisis que el facultativo estima oportuno realizar:", contentLeft, pruebasTop, contentRight, pruebasTop + mmToPt(4f), boldPaint)
        drawHLine(contentLeft, contentRight, pruebasTop + mmToPt(4f))

        val sangreY = pruebasTop + mmToPt(4f) + (pruebasH - mmToPt(4f)) / 2f
        canvas.drawText("${drawCheck(data.pruebaSangre)}  La extracción para análisis de muestras de sangre", contentLeft + mmToPt(4f), sangreY, textPaint)
        canvas.drawText("${drawCheck(data.pruebaOtroTipo)}  Otro tipo de extracción (con detalle): ${data.pruebaOtroDetalle.ifBlank { " " }}", contentLeft + mmToPt(90f), sangreY, textPaint)
        y = pruebasTop + pruebasH + mmToPt(2f)

        // ═══════════════════════════════════════════════════════════════════
        // TEXTO LEGAL 2-4)
        // ═══════════════════════════════════════════════════════════════════
        val legalText4 = "4) Tras informar al interesado de las causas que impiden la realización de las pruebas mediante los métodos habituales, motivos del requerimiento y posibles consecuencias que puede derivarse para la persona en caso de resultado positivo, se procederá a realizar las pruebas que determine el facultativo, y cuyo resultado se consignará en el presente documento."
        y = drawWrappedText(legalText4, contentLeft, contentRight, y, smallPaint, 0.2f)
        y += mmToPt(1f)

        val legalText5 = "5) El personal sanitario vendrá colegiado, en todo caso, y procederá a la obtención de muestras y remitirlas al laboratorio correspondiente, y a dar cuenta, de resultados de las pruebas realizadas, a la autoridad judicial, o al organismo autónomo Jefatura Central de Tráfico en el plazo de un mes, salvo que se requiera un plazo mayor por circunstancias debidamente justificadas."
        y = drawWrappedText(legalText5, contentLeft, contentRight, y, smallPaint, 0.2f)
        y += mmToPt(1f)

        val legalText6 = "6) Las muestras extraídas serán de sangre, perfirada en dos tubos de 5 ml, al menos uno con fluoruro sódico como conservante y citrato potásico como anticoagulante. En caso de que el volumen de sangre no sea suficiente, se obtendrán las muestras que se consideren oportunas. Para la detección de alcoholemia en sangre, se llevará a cabo bajo condiciones deseables, no empaquetar ni alcohol o desinfectantes con fracciones volátiles en la zona de la extracción del tubo. En caso de que no se obtengan muestras de sangre se procederá a la obtención de muestras de orina."
        y = drawWrappedText(legalText6, contentLeft, contentRight, y, smallPaint, 0.2f)
        y += mmToPt(3f)

        // ═══════════════════════════════════════════════════════════════════
        // CENTRO SANITARIO
        // ═══════════════════════════════════════════════════════════════════
        val centroTop = y
        val centroH1 = mmToPt(5f)
        val centroH2 = mmToPt(5f)
        val centroH3 = mmToPt(5f)
        val centroBottom = centroTop + centroH1 + centroH2 + centroH3

        drawRect(contentLeft, centroTop, contentRight, centroBottom)
        drawRect(contentLeft, centroTop, contentRight, centroTop + centroH1, )
        boxText("Centro sanitario donde se realiza la toma de muestras", contentLeft, centroTop, contentRight, centroTop + centroH1, boldPaint)

        drawHLine(contentLeft, contentRight, centroTop + centroH1)
        drawHLine(contentLeft, contentRight, centroTop + centroH1 + centroH2)

        boxText(data.centroSanitario.ifBlank { " " }, contentLeft, centroTop + centroH1, contentRight, centroTop + centroH1 + centroH2, textPaint)

        val extraccionCampoW = contentWidth * 0.60f
        boxText("Extracción realizada por colegiado nº:", contentLeft, centroTop + centroH1 + centroH2, contentLeft + extraccionCampoW, centroBottom, boldPaint)
        drawVLine(contentLeft + extraccionCampoW, centroTop + centroH1 + centroH2, centroBottom)
        boxText(data.extraccionColegiado.ifBlank { " " }, contentLeft + extraccionCampoW, centroTop + centroH1 + centroH2, contentRight, centroBottom, textPaint)
        y = centroBottom + mmToPt(3f)

        // ═══════════════════════════════════════════════════════════════════
        // MUESTRAS
        // ═══════════════════════════════════════════════════════════════════
        val muestrasTop = y
        val muestrasH1 = mmToPt(5f)
        val muestrasH2 = mmToPt(5f)
        val muestrasBottom = muestrasTop + muestrasH1 + muestrasH2

        drawRect(contentLeft, muestrasTop, contentRight, muestrasBottom)
        drawRect(contentLeft, muestrasTop, contentRight, muestrasTop + muestrasH1, )
        boxText("Una vez obtenidas las muestras, se procede a su sellado y precintado, dado/s el/los correspondiente proceso de cadena de custodia.", contentLeft, muestrasTop, contentRight, muestrasTop + muestrasH1, boldPaint)

        drawHLine(contentLeft, contentRight, muestrasTop + muestrasH1)

        val precintosW = contentWidth * 0.55f
        boxText("Para garantizar inalterabilidad de las muestras se utilizan: ${data.precintosSeguridad.ifBlank { "PRECINTOS DE SEGURIDAD" }}", contentLeft, muestrasTop + muestrasH1, contentLeft + precintosW, muestrasBottom, textPaint)
        drawVLine(contentLeft + precintosW, muestrasTop + muestrasH1, muestrasBottom)
        boxText("Dato/s dado/s a la muestra: ${data.datoMuestra.ifBlank { " " }}", contentLeft + precintosW, muestrasTop + muestrasH1, contentRight, muestrasBottom, textPaint)
        y = muestrasBottom + mmToPt(3f)

        // ═══════════════════════════════════════════════════════════════════
        // JUZGADO U ORGANISMO COMPETENTE
        // ═══════════════════════════════════════════════════════════════════
        val juzgadoTop = y
        val juzgadoH = mmToPt(8f)
        drawRect(contentLeft, juzgadoTop, contentRight, juzgadoTop + juzgadoH)
        drawRect(contentLeft, juzgadoTop, contentRight, juzgadoTop + mmToPt(4f), )
        boxText("Juzgado u organismo competente para la investigación de los hechos y forma remite informe:", contentLeft, juzgadoTop, contentRight, juzgadoTop + mmToPt(4f), boldPaint)
        drawHLine(contentLeft, contentRight, juzgadoTop + mmToPt(4f))
        boxText(data.juzgadoOrganismo.ifBlank { " " }, contentLeft, juzgadoTop + mmToPt(4f), contentRight, juzgadoTop + juzgadoH, textPaint)
        y = juzgadoTop + juzgadoH + mmToPt(3f)

        // ═══════════════════════════════════════════════════════════════════
        // OTRAS OBSERVACIONES
        // ═══════════════════════════════════════════════════════════════════
        val obsTop = y
        val obsH = mmToPt(10f)
        drawRect(contentLeft, obsTop, contentRight, obsTop + obsH)
        drawRect(contentLeft, obsTop, contentRight, obsTop + mmToPt(4f), )
        boxText("OTRAS OBSERVACIONES", contentLeft, obsTop, contentRight, obsTop + mmToPt(4f), boldPaint)
        drawHLine(contentLeft, contentRight, obsTop + mmToPt(4f))
        boxText(data.otrasObservaciones.ifBlank { " " }, contentLeft, obsTop + mmToPt(4f), contentRight, obsTop + obsH, textPaint)
        y = obsTop + obsH + mmToPt(3f)

        // ═══════════════════════════════════════════════════════════════════
        // TEXTO FINAL
        // ═══════════════════════════════════════════════════════════════════
        val finalText = "La presente acta se hace constar, por si acaso, a los efectos de democunetación, y se entrega una copia la misma (sujeto sometido a las pruebas, al principally o a raíz de ser trasladado a Juzgado, junto con el facultativo, personal sanitario y agentes intervinientes."
        y = drawWrappedText(finalText, contentLeft, contentRight, y, smallPaint, 0.2f)
        y += mmToPt(4f)

        // ═══════════════════════════════════════════════════════════════════
        // FIRMAS
        // ═══════════════════════════════════════════════════════════════════
        val firmasTop = y
        val sigWidth = contentWidth / 4f
        val gap = mmToPt(2f)

        val sig1Left = contentLeft
        val sig1Right = contentLeft + sigWidth - gap
        val sig2Left = contentLeft + sigWidth
        val sig2Right = contentLeft + sigWidth * 2 - gap
        val sig3Left = contentLeft + sigWidth * 2
        val sig3Right = contentLeft + sigWidth * 3 - gap
        val sig4Left = contentLeft + sigWidth * 3
        val sig4Right = contentRight

        canvas.drawText("Firma de interesado", sig1Left + mmToPt(2f), firmasTop, smallPaint)
        canvas.drawText("Firma de facultativo", sig2Left + mmToPt(2f), firmasTop, smallPaint)
        canvas.drawText("Firma sanitario realiza extracción", sig3Left + mmToPt(2f), firmasTop, smallPaint)
        canvas.drawText("Firmas agentes intervinientes", sig4Left + mmToPt(2f), firmasTop, smallPaint)

        y = firmasTop + mmToPt(12f)
        drawHLine(sig1Left + mmToPt(4f), sig1Right - mmToPt(4f), y)
        drawHLine(sig2Left + mmToPt(4f), sig2Right - mmToPt(4f), y)
        drawHLine(sig3Left + mmToPt(4f), sig3Right - mmToPt(4f), y)
        drawHLine(sig4Left + mmToPt(4f), sig4Right - mmToPt(4f), y)

        drawSignatureOnLine(interesadoSignature, sig1Left + mmToPt(4f), sig1Right - mmToPt(4f), y)
        drawSignatureOnLine(facultativoSignature, sig2Left + mmToPt(4f), sig2Right - mmToPt(4f), y)
        drawSignatureOnLine(sanitarioSignature, sig3Left + mmToPt(4f), sig3Right - mmToPt(4f), y)
        drawSignatureOnLine(agenteSignature, sig4Left + mmToPt(4f), sig4Right - mmToPt(4f), y)

        y += mmToPt(6f)
        canvas.drawText("N.º TIP: ${data.numTip.ifBlank { " " }}", contentLeft + mmToPt(1f), y, boldPaint)

        pdfDocument.finishPage(page)

        val directory = File(context.filesDir, "atestados").apply { mkdirs() }
        val file = File(directory, "ActaTrasladoCentroSanitario.pdf")
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
