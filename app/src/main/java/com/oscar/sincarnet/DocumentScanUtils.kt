package com.oscar.sincarnet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Utilidades para el escáner de documentos:
 *  – lectura de imagen con corrección EXIF
 *  – transformación de perspectiva (sin OpenCV)
 *  – mejora de imagen (efecto escáner)
 *  – exportación a PDF A4
 */
internal object DocumentScanUtils {

    private data class BrightBox(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
        val area: Int
    )

    /**
     * Detecta automaticamente el rectangulo de un documento claro sobre fondo oscuro.
     * Devuelve las 4 esquinas en coordenadas del bitmap original (TL, TR, BR, BL).
     * Si no hay confianza suficiente, devuelve null para usar ajuste manual.
     */
    fun detectDocumentCornersOnDarkBackground(bitmap: Bitmap): Array<PointF>? {
        val srcW = bitmap.width
        val srcH = bitmap.height
        if (srcW < 64 || srcH < 64) return null

        // Reducimos para acelerar calculo de luminancia.
        val maxSide = 512
        val ratio = min(maxSide.toFloat() / srcW, maxSide.toFloat() / srcH).coerceAtMost(1f)
        val smallW = max(32, (srcW * ratio).toInt())
        val smallH = max(32, (srcH * ratio).toInt())
        val small = if (ratio < 1f) {
            Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        } else {
            bitmap
        }

        val pixels = IntArray(small.width * small.height)
        small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)

        var sum = 0f
        var sumSq = 0f
        val luma = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val y = 0.299f * r + 0.587f * g + 0.114f * b
            luma[i] = y
            sum += y
            sumSq += y * y
        }

        val count = luma.size.toFloat()
        val avg = sum / count
        val variance = (sumSq / count) - (avg * avg)
        val std = sqrt(variance.coerceAtLeast(0f))

        // Estimamos luminancia de borde: normalmente el fondo oscuro está aquí.
        var borderSum = 0f
        var borderCount = 0
        val w = small.width
        val h = small.height
        for (x in 0 until w) {
            borderSum += luma[x]
            borderSum += luma[(h - 1) * w + x]
            borderCount += 2
        }
        for (y in 1 until (h - 1)) {
            borderSum += luma[y * w]
            borderSum += luma[y * w + (w - 1)]
            borderCount += 2
        }
        val borderMean = if (borderCount > 0) borderSum / borderCount else avg

        // Probamos varios umbrales para cubrir condiciones de luz distintas.
        val thresholds = listOf(
            (borderMean + 18f),
            (borderMean + 26f),
            (avg + std * 0.20f),
            (avg + std * 0.35f)
        ).map { it.coerceIn(50f, 235f) }

        var bestBox: BrightBox? = null
        var bestScore = -1f

        thresholds.forEach { threshold ->
            val mask = BooleanArray(w * h)
            for (i in luma.indices) {
                mask[i] = luma[i] >= threshold
            }

            val box = findLargestBrightComponent(mask, w, h) ?: return@forEach
            val boxW = (box.maxX - box.minX + 1).toFloat()
            val boxH = (box.maxY - box.minY + 1).toFloat()
            if (boxW <= 1f || boxH <= 1f) return@forEach

            val areaRatio = box.area.toFloat() / (w * h).toFloat()
            if (areaRatio < 0.05f || areaRatio > 0.92f) return@forEach

            val aspect = boxW / boxH
            if (aspect < 0.35f || aspect > 3.0f) return@forEach

            val touchesBorderCount =
                (if (box.minX <= 1) 1 else 0) +
                (if (box.minY <= 1) 1 else 0) +
                (if (box.maxX >= w - 2) 1 else 0) +
                (if (box.maxY >= h - 2) 1 else 0)

            // Preferimos área grande, pero penalizamos si "abraza" todo el borde.
            val score = areaRatio - (touchesBorderCount * 0.03f)
            if (score > bestScore) {
                bestScore = score
                bestBox = box
            }
        }

        val chosen = bestBox ?: return null

        val sx = srcW.toFloat() / small.width.toFloat()
        val sy = srcH.toFloat() / small.height.toFloat()

        val tl = PointF(chosen.minX * sx, chosen.minY * sy)
        val tr = PointF(chosen.maxX * sx, chosen.minY * sy)
        val br = PointF(chosen.maxX * sx, chosen.maxY * sy)
        val bl = PointF(chosen.minX * sx, chosen.maxY * sy)

        return arrayOf(tl, tr, br, bl)
    }

    private fun findLargestBrightComponent(
        mask: BooleanArray,
        width: Int,
        height: Int
    ): BrightBox? {
        val visited = BooleanArray(mask.size)
        val queue = IntArray(mask.size)
        var best: BrightBox? = null

        for (start in mask.indices) {
            if (!mask[start] || visited[start]) continue

            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true

            var minX = width
            var minY = height
            var maxX = -1
            var maxY = -1
            var area = 0

            while (head < tail) {
                val idx = queue[head++]
                val x = idx % width
                val y = idx / width

                area++
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y

                // 4-conectividad: suficiente para formas de documento y más estable.
                if (x > 0) {
                    val n = idx - 1
                    if (mask[n] && !visited[n]) {
                        visited[n] = true
                        queue[tail++] = n
                    }
                }
                if (x < width - 1) {
                    val n = idx + 1
                    if (mask[n] && !visited[n]) {
                        visited[n] = true
                        queue[tail++] = n
                    }
                }
                if (y > 0) {
                    val n = idx - width
                    if (mask[n] && !visited[n]) {
                        visited[n] = true
                        queue[tail++] = n
                    }
                }
                if (y < height - 1) {
                    val n = idx + width
                    if (mask[n] && !visited[n]) {
                        visited[n] = true
                        queue[tail++] = n
                    }
                }
            }

            if (best == null || area > best.area) {
                best = BrightBox(minX, minY, maxX, maxY, area)
            }
        }

        return best
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lectura de imagen con corrección de rotación EXIF
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Decodifica [file] como Bitmap y aplica la rotación que indica el EXIF.
     * Devuelve null si el archivo no es una imagen válida.
     */
    fun readBitmapWithExif(file: File): Bitmap? {
        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f,  1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL   -> matrix.preScale( 1f, -1f)
        }
        return if (matrix.isIdentity) raw
        else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ordenación de esquinas (TL, TR, BR, BL)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Ordena 4 puntos arbitrarios en el orden: Superior-Izquierda, Superior-Derecha,
     * Inferior-Derecha, Inferior-Izquierda.
     *
     * Algoritmo:
     *  - TL → menor suma (x+y)
     *  - BR → mayor suma (x+y)
     *  - TR → mayor diferencia (x-y)
     *  - BL → menor diferencia (x-y)
     */
    fun orderCorners(points: List<PointF>): Array<PointF> {
        val tl = points.minByOrNull { it.x + it.y }!!
        val br = points.maxByOrNull { it.x + it.y }!!
        val tr = points.maxByOrNull { it.x - it.y }!!
        val bl = points.minByOrNull { it.x - it.y }!!
        return arrayOf(tl, tr, br, bl)
    }

    /** Distancia euclidiana entre dos puntos. */
    fun distance(a: PointF, b: PointF): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt(dx * dx + dy * dy)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Transformación de perspectiva (corrección de ángulo)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Aplica una transformación de perspectiva (homografía) usando la clase
     * [Matrix.setPolyToPoly] de Android — sin librerías externas.
     *
     * @param source          Bitmap original (foto de cámara ya con EXIF corregido).
     * @param cornersInOrder  4 esquinas EN EL BITMAP en orden TL, TR, BR, BL.
     * @param outWidth        Ancho del bitmap de salida en píxeles.
     * @param outHeight       Alto del bitmap de salida en píxeles.
     * @return Bitmap "enderezado" con vista cenital del documento.
     */
    fun applyPerspectiveTransform(
        source: Bitmap,
        cornersInOrder: Array<PointF>,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val (tl, tr, br, bl) = cornersInOrder
        val matrix = Matrix()
        matrix.setPolyToPoly(
            // Puntos fuente (en bitmap original)
            floatArrayOf(tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y), 0,
            // Puntos destino (rectángulo de salida)
            floatArrayOf(
                0f,             0f,
                outWidth.toFloat(), 0f,
                outWidth.toFloat(), outHeight.toFloat(),
                0f,             outHeight.toFloat()
            ), 0,
            4
        )
        val result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(
            source, matrix,
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
        return result
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mejora de imagen (efecto fotocopiadora)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Convierte el bitmap a escala de grises y aumenta el contraste para
     * obtener un resultado similar al de un escáner de documentos.
     */
    fun enhanceScanBitmap(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1) Desaturar (escala de grises)
        val cm = ColorMatrix()
        cm.setSaturation(0f)

        // 2) Contraste + brillo (matrix 4x5)
        val contrast   = 1.35f
        val brightness = -28f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f,       0f,       0f, brightness,
                0f,       contrast, 0f,       0f, brightness,
                0f,       0f,       contrast, 0f, brightness,
                0f,       0f,       0f,       1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Exportación a PDF A4
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Guarda [bitmap] como una página A4 en PDF dentro del directorio
     * `filesDir/atestados/` (el mismo que usan los demás PDFs de la app).
     *
     * @param outputName Nombre del archivo PDF (p.ej. "scan_1712345678.pdf").
     * @return El [File] resultante.
     */
    fun saveScanToPdf(context: Context, bitmap: Bitmap, outputName: String): File {
        val pageW = 595
        val pageH = 842
        val margin = 18f

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val availW = pageW - 2 * margin
        val availH = pageH - 2 * margin
        val scale  = minOf(availW / bitmap.width, availH / bitmap.height)
        val drawW  = bitmap.width  * scale
        val drawH  = bitmap.height * scale
        val left   = margin + (availW - drawW) / 2f
        val top    = margin + (availH - drawH) / 2f

        canvas.drawBitmap(
            bitmap, null,
            RectF(left, top, left + drawW, top + drawH),
            null
        )
        pdfDoc.finishPage(page)

        val dir  = File(context.filesDir, "atestados").apply { mkdirs() }
        val file = File(dir, outputName)
        FileOutputStream(file).use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        return file
    }

    /**
     * Genera un PDF multipagina A4 con todas las imagenes escaneadas.
     * Cada bitmap se inserta en una pagina independiente.
     */
    fun saveScansToPdf(
        context: Context,
        scans: List<Bitmap>,
        outputName: String,
        numeroDiligencias: String = "",
        identityFront: Bitmap? = null,
        identityBack: Bitmap? = null,
        identityTitle: String = "Documento de identidad",
        drivingFront: Bitmap? = null,
        drivingBack: Bitmap? = null,
        drivingTitle: String = "Permiso de conducir",
        circulationBack: Bitmap? = null,
        circulationFront: Bitmap? = null,
        circulationTitle: String = "Permiso de circulación",
        frontLabel: String = "Anverso del documento",
        backLabel: String = "Reverso del documento"
    ): File {
        val hasIdentityFaces = identityFront != null && identityBack != null
        val hasDrivingFaces = drivingFront != null && drivingBack != null
        val hasCirculationFaces = circulationBack != null && circulationFront != null
        require(scans.isNotEmpty() || hasIdentityFaces || hasDrivingFaces || hasCirculationFaces) {
            "Debe existir al menos una imagen escaneada"
        }

        val pageW = 595
        val pageH = 842
        val margin = 18f
        val pdfDoc = PdfDocument()
        var pageNumber = 1

        if (hasIdentityFaces) {
            pageNumber = drawAnnexTwoFacesPage(
                pdfDoc = pdfDoc,
                context = context,
                pageNumber = pageNumber,
                numeroDiligencias = numeroDiligencias,
                title = identityTitle,
                front = identityFront,
                back = identityBack,
                frontLabel = frontLabel,
                backLabel = backLabel
            )
        }

        if (hasDrivingFaces) {
            pageNumber = drawAnnexTwoFacesPage(
                pdfDoc = pdfDoc,
                context = context,
                pageNumber = pageNumber,
                numeroDiligencias = numeroDiligencias,
                title = drivingTitle,
                front = drivingFront,
                back = drivingBack,
                frontLabel = frontLabel,
                backLabel = backLabel
            )
        }

        if (hasCirculationFaces) {
            pageNumber = drawCirculationPermitPages(
                pdfDoc = pdfDoc,
                context = context,
                pageNumber = pageNumber,
                numeroDiligencias = numeroDiligencias,
                title = circulationTitle,
                back = circulationBack,
                front = circulationFront,
                frontLabel = frontLabel,
                backLabel = backLabel
            )
        }

        scans.forEach { bitmap ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber++).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            drawScanStyledHeader(
                canvas = canvas,
                context = context,
                numeroDiligencias = numeroDiligencias
            )

            val leftLimit = mmToPt(15f) + 6f
            val rightLimit = (pageW - mmToPt(20f)) - 6f
            val topLimit = mmToPt(24f)
            val bottomLimit = pageH - mmToPt(15f) - 8f
            val availW = (rightLimit - leftLimit).coerceAtLeast(1f)
            val availH = (bottomLimit - topLimit).coerceAtLeast(1f)
            val scale = minOf(availW / bitmap.width, availH / bitmap.height)
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val left = leftLimit + (availW - drawW) / 2f
            val top = topLimit + (availH - drawH) / 2f

            canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), null)
            pdfDoc.finishPage(page)
        }

        val dir = File(context.filesDir, "atestados").apply { mkdirs() }
        val file = File(dir, outputName)
        FileOutputStream(file).use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        return file
    }

    private fun drawCirculationPermitPages(
        pdfDoc: PdfDocument,
        context: Context,
        pageNumber: Int,
        numeroDiligencias: String,
        title: String,
        back: Bitmap,
        front: Bitmap,
        frontLabel: String,
        backLabel: String
    ): Int {
        val rotatedBack = rotateBitmap90(back)
        val rotatedFront = rotateBitmap90(front)

        drawCirculationPermitSinglePage(
            pdfDoc = pdfDoc,
            context = context,
            pageNumber = pageNumber,
            numeroDiligencias = numeroDiligencias,
            title = title,
            bitmap = rotatedBack,
            faceLabel = backLabel
        )
        drawCirculationPermitSinglePage(
            pdfDoc = pdfDoc,
            context = context,
            pageNumber = pageNumber + 1,
            numeroDiligencias = numeroDiligencias,
            title = null,
            bitmap = rotatedFront,
            faceLabel = frontLabel
        )
        return pageNumber + 2
    }

    private fun drawCirculationPermitSinglePage(
        pdfDoc: PdfDocument,
        context: Context,
        pageNumber: Int,
        numeroDiligencias: String,
        title: String?,
        bitmap: Bitmap,
        faceLabel: String
    ) {
        val pageW = 595
        val pageH = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        drawScanStyledHeader(canvas, context, numeroDiligencias)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        val contentLeft = mmToPt(15f) + 6f
        val contentRight = (595f - mmToPt(20f)) - 6f
        val contentWidth = (contentRight - contentLeft).coerceAtLeast(1f)

        var boxTop = mmToPt(24f)
        title?.let {
            val titleY = mmToPt(20f)
            val titleX = contentLeft + (contentWidth - titlePaint.measureText(it)) / 2f
            canvas.drawText(it, titleX, titleY, titlePaint)
            boxTop = mmToPt(34f)
        }

        // Permiso de circulación: 148 x 210 mm (proporción 0.70476)
        val targetAspect = 148f / 210f
        val maxBoxWidth = (contentWidth * 0.92f).coerceAtLeast(1f)
        val maxBoxHeight = (pageH - boxTop - mmToPt(35f)).coerceAtLeast(1f)
        val boxWidth = minOf(maxBoxWidth, maxBoxHeight * targetAspect)
        val boxHeight = (boxWidth / targetAspect).coerceAtLeast(1f)
        val left = contentLeft + (contentWidth - boxWidth) / 2f
        val rect = RectF(left, boxTop, left + boxWidth, boxTop + boxHeight)

        canvas.drawRect(rect, boxPaint)
        drawBitmapInsideRect(canvas, bitmap, rect)

        val labelX = contentLeft + (contentWidth - labelPaint.measureText(faceLabel)) / 2f
        canvas.drawText(faceLabel, labelX, rect.bottom + 16f, labelPaint)

        pdfDoc.finishPage(page)
    }

    private fun rotateBitmap90(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun drawAnnexTwoFacesPage(
        pdfDoc: PdfDocument,
        context: Context,
        pageNumber: Int,
        numeroDiligencias: String,
        title: String,
        front: Bitmap,
        back: Bitmap,
        frontLabel: String,
        backLabel: String
    ): Int {
        val pageW = 595
        val pageH = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        drawScanStyledHeader(canvas, context, numeroDiligencias)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        val contentLeft = mmToPt(15f) + 6f
        val contentRight = (595f - mmToPt(20f)) - 6f
        val contentWidth = (contentRight - contentLeft).coerceAtLeast(1f)

        val titleY = mmToPt(20f)
        val titleX = contentLeft + (contentWidth - titlePaint.measureText(title)) / 2f
        canvas.drawText(title, titleX, titleY, titlePaint)

        val boxWidth = (contentWidth * 0.86f).coerceAtMost(360f)
        val boxHeight = (boxWidth / 1.58f).coerceAtLeast(110f)
        val left = contentLeft + (contentWidth - boxWidth) / 2f
        val firstTop = mmToPt(34f)
        val labelGap = 16f
        val secondTop = firstTop + boxHeight + labelGap + 26f

        val rectFront = RectF(left, firstTop, left + boxWidth, firstTop + boxHeight)
        val rectBack = RectF(left, secondTop, left + boxWidth, secondTop + boxHeight)
        canvas.drawRect(rectFront, boxPaint)
        canvas.drawRect(rectBack, boxPaint)
        drawBitmapInsideRect(canvas, front, rectFront)
        drawBitmapInsideRect(canvas, back, rectBack)

        val frontLabelX = contentLeft + (contentWidth - labelPaint.measureText(frontLabel)) / 2f
        val backLabelX = contentLeft + (contentWidth - labelPaint.measureText(backLabel)) / 2f
        canvas.drawText(frontLabel, frontLabelX, rectFront.bottom + labelGap, labelPaint)
        canvas.drawText(backLabel, backLabelX, rectBack.bottom + labelGap, labelPaint)

        pdfDoc.finishPage(page)
        return pageNumber + 1
    }

    private fun drawScanStyledHeader(
        canvas: android.graphics.Canvas,
        context: Context,
        numeroDiligencias: String
    ) {
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val textPaintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val contentLeft = mmToPt(5f)
        val centerColLeftX = contentLeft + mmToPt(10f)
        val centerColRightX = (centerColLeftX + mmToPt(175f)).coerceAtMost(595f - mmToPt(5f))

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
        val numero = numeroDiligencias.trim()
        val atestadoNumeroText = if (numero.isBlank()) {
            "ATESTADO NUMERO:"
        } else {
            "ATESTADO NUMERO:   $numero"
        }
        val atestadoTextY = atestadoBoxTop + (atestadoBoxHeight + textPaintSmall.textSize) / 2f
        canvas.drawText(atestadoNumeroText, atestadoBoxLeft + 4f, atestadoTextY, textPaintSmall)

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
        canvas.drawText("FOLIO N.", folioBoxLeft + 2f, folioTextY, textPaintSmall)

        val guideTopY = mmToPt(5.6f)
        val guideBottomY = 842f - mmToPt(15f)
        val guideLeftX = mmToPt(15f)
        val guideRightX = 595f - mmToPt(20f)
        canvas.drawLine(guideLeftX, guideTopY, guideLeftX, guideBottomY, boxPaint)
        canvas.drawLine(guideRightX, guideTopY, guideRightX, guideBottomY, boxPaint)

        val topMargin = mmToPt(5.5f)

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

    private fun mmToPt(mm: Float): Float = mm * 72f / 25.4f

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

    private fun drawBitmapInsideRect(
        canvas: android.graphics.Canvas,
        bitmap: Bitmap,
        rect: RectF
    ) {
        val padding = 7f
        val dstLeft = rect.left + padding
        val dstTop = rect.top + padding
        val dstRight = rect.right - padding
        val dstBottom = rect.bottom - padding

        val targetW = (dstRight - dstLeft).coerceAtLeast(1f)
        val targetH = (dstBottom - dstTop).coerceAtLeast(1f)
        val srcW = bitmap.width.toFloat().coerceAtLeast(1f)
        val srcH = bitmap.height.toFloat().coerceAtLeast(1f)
        val scale = minOf(targetW / srcW, targetH / srcH)
        val drawW = srcW * scale
        val drawH = srcH * scale
        val dx = dstLeft + (targetW - drawW) / 2f
        val dy = dstTop + (targetH - drawH) / 2f

        canvas.drawBitmap(bitmap, null, RectF(dx, dy, dx + drawW, dy + drawH), null)
    }
}

