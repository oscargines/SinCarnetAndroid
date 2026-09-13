package com.oscar.sincarnet.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import com.oscar.sincarnet.data.device.getDeviceIdentifierSuffix
import kotlin.math.cos
import kotlin.math.sin

/** Color monocromo del sello institucional (azul agrupación tráfico). */
private const val SEAL_COLOR = 0xFF393185.toInt()

/** Tamaño del sello al plasmarlo en el PDF, en milímetros. */
const val SEAL_SIZE_MM = 35f

/** Resolución por defecto del render (35 mm a ~600 dpi). */
private const val SEAL_DEFAULT_SIZE_PX = 826

/** Texto fijo del arco superior del sello. */
private const val SEAL_TOP_TEXT = "AGRUPACIÓN TRÁFICO GUARDIA CIVIL"

/** Barrido máximo admisible de cada arco de texto, en grados. */
private const val SEAL_MAX_TEXT_SWEEP_DEG = 165f

private const val SEAL_A4_HEIGHT_PT = 842f

/** Caché de sellos renderizados por unidad, identificador y tamaño. */
private val sealCache = LruCache<String, Bitmap>(6)

/**
 * Construye el texto variable del arco inferior del sello a partir de la
 * denominación de la unidad del instructor.
 *
 * Si la denominación contiene varias componentes (p. ej.
 * "Sector Tráfico Asturias / Destacamento de Ribadesella") se toma la más
 * específica. Sobre el resultado se aplica la abreviación por prefijo:
 * "Destacamento" → "Desto.", "Subsector" → "Sbtor.", "Sector" → "Sect.";
 * si no coincide con ninguno, se usa el nombre tal cual.
 *
 * @param unidad Denominación de la unidad del instructor.
 * @return Texto a dibujar en el arco inferior del sello.
 */
fun buildSealUnitText(unidad: String): String {
    val component = mostSpecificUnitComponent(unidad.trim())
    return when {
        component.startsWith("Destacamento", ignoreCase = true) ->
            "Desto." + component.substring("Destacamento".length)
        component.startsWith("Subsector", ignoreCase = true) ->
            "Sbtor." + component.substring("Subsector".length)
        component.startsWith("Sector", ignoreCase = true) ->
            "Sect." + component.substring("Sector".length)
        else -> component
    }.trim()
}

/**
 * Devuelve el bitmap del sello institucional con el texto de unidad indicado,
 * reutilizando copias en caché para renderizados repetidos.
 *
 * El texto se dibuja literalmente en el arco inferior, sin aplicar
 * abreviaciones; para sugerir un texto a partir de la denominación de la
 * unidad usar [buildSealUnitText].
 *
 * @param context Contexto para acceder a los assets (escudo y fuente).
 * @param sealText Texto a dibujar en el arco inferior del sello.
 * @param sizePx Tamaño del bitmap en píxeles.
 * @return Sello monocromo #393185 con el texto indicado en el arco inferior.
 */
fun getInstitutionalSealBitmap(
    context: Context,
    sealText: String,
    sizePx: Int = SEAL_DEFAULT_SIZE_PX
): Bitmap {
    val unitText = sealText.trim()
    val deviceId = getDeviceIdentifierSuffix(context)
    val cacheKey = "$sizePx|$unitText|$deviceId"
    sealCache.get(cacheKey)?.let { return it }
    val bitmap = renderInstitutionalSeal(context, unitText, deviceId, sizePx)
    sealCache.put(cacheKey, bitmap)
    return bitmap
}

/**
 * Indica si el texto indicado posiblemente no quepa en el arco inferior del
 * sello a tamaño nominal, y conviene acortarlo con acrónimos.
 *
 * @param context Contexto para cargar la tipografía del sello.
 * @param text Texto a validar.
 * @return True si el texto excede el barrido máximo del arco.
 */
fun isSealTextTooLong(context: Context, text: String): Boolean {
    if (text.isBlank()) return false
    val sizePx = SEAL_DEFAULT_SIZE_PX
    val R = sizePx / 2f
    val bottomTextRadius = R * ((0.985f + 0.77f) / 2f)
    val botPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = loadSealTypeface(context)
        textSize = sizePx * 0.068f
    }
    return arcTextSweepDeg(text, botPaint, bottomTextRadius) > SEAL_MAX_TEXT_SWEEP_DEG
}

/**
 * Dibuja el sello sobre el canvas del PDF ajustado al rectángulo destino.
 *
 * @param canvas Canvas del documento PDF.
 * @param seal Bitmap del sello.
 * @param target Rectángulo destino en puntos.
 */
fun drawSeal(canvas: Canvas, seal: Bitmap, target: RectF) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(seal, null, target, paint)
}

/**
 * Estampa el sello en la esquina inferior izquierda del A4, respetando un
 * margen de 20 mm respecto a los bordes físicos de la hoja.
 *
 * @param canvas Canvas del documento PDF.
 * @param seal Bitmap del sello.
 */
fun drawSealBottomLeft(canvas: Canvas, seal: Bitmap) {
    drawSeal(canvas, seal, sealRectBottomLeft())
}

/**
 * Rectángulo (en puntos) del sello estampado en la esquina inferior izquierda
 * del A4, con margen de 20 mm respecto a los bordes físicos de la hoja.
 *
 * @return Rectángulo destino del sello.
 */
fun sealRectBottomLeft(): RectF {
    val size = sealMmToPt(SEAL_SIZE_MM)
    val left = sealMmToPt(10f)
    val bottom = SEAL_A4_HEIGHT_PT - sealMmToPt(10f)
    return RectF(left, bottom - size, left + size, bottom)
}

/**
 * Rectángulo (en puntos) del sello estampado a la izquierda de un bloque de
 * firma, centrado verticalmente con él y limitado al margen izquierdo de la
 * página.
 *
 * @param rect Rectángulo del bloque de firma de referencia.
 * @param gapMm Separación con el bloque de firma, en milímetros.
 * @return Rectángulo destino del sello.
 */
fun sealRectLeftOf(rect: RectF, gapMm: Float = 2f): RectF {
    val size = sealMmToPt(SEAL_SIZE_MM)
    val left = (rect.left - sealMmToPt(gapMm) - size).coerceAtLeast(sealMmToPt(3f))
    val top = (rect.centerY() - size / 2f).coerceIn(sealMmToPt(4f), SEAL_A4_HEIGHT_PT - sealMmToPt(4f) - size)
    return RectF(left, top, left + size, top + size)
}

/**
 * Rectángulo (en puntos) del sello estampado centrado sobre el hueco de un
 * bloque de firma existente (sustitución del recuadro).
 *
 * @param rect Rectángulo del bloque que el sello sustituye.
 * @return Rectángulo destino del sello.
 */
fun sealRectReplacing(rect: RectF): RectF {
    val size = sealMmToPt(SEAL_SIZE_MM)
    val left = rect.centerX() - size / 2f
    val top = rect.centerY() - size / 2f
    return RectF(left, top, left + size, top + size)
}

/** Conversión de milímetros a puntos PDF. */
private fun sealMmToPt(mm: Float): Float = mm * 72f / 25.4f

/**
 * Extrae la componente más específica de una denominación de unidad, siguiendo
 * el mismo criterio que la portada del atestado completo.
 */
private fun mostSpecificUnitComponent(value: String): String {
    val marker = Regex("\\s+(?:/|-|;)\\s+|\\n+").find(value)
    if (marker != null) {
        val after = value.substring(marker.range.last + 1).trim()
        if (after.isNotBlank()) return after
    }
    val destacamentoIndex = value.uppercase().indexOf("DESTACAMENTO")
    if (destacamentoIndex > 0) {
        return value.substring(destacamentoIndex).trim()
    }
    return value
}

/** Renderiza el sello completo sobre un bitmap monocromo #393185. */
private fun renderInstitutionalSeal(
    context: Context,
    unitText: String,
    deviceId: String,
    sizePx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val radius = sizePx / 2f

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SEAL_COLOR
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.012f
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SEAL_COLOR }
    val typeface = loadSealTypeface(context)

    val topTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SEAL_COLOR
        this.typeface = typeface
        textSize = sizePx * 0.121f
        textAlign = Paint.Align.CENTER
    }
    val bottomTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SEAL_COLOR
        this.typeface = typeface
        textSize = sizePx * 0.068f
        textAlign = Paint.Align.CENTER
    }

    val outerCircleRadius = radius * 0.985f
    val innerCircleRadius = radius * 0.77f
    val textRingRadius = radius * ((0.985f + 0.77f) / 2f)

    canvas.drawCircle(cx, cy, outerCircleRadius, strokePaint)
    canvas.drawCircle(cx, cy, innerCircleRadius, strokePaint)

    val escudoMaxWidth = innerCircleRadius * 2f * 0.72f
    val shieldRect = drawSealEscudo(context, canvas, cx, cy, escudoMaxWidth)
    shieldRect?.let { rect ->
        val idPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SEAL_COLOR
            this.typeface = typeface
            textSize = 5f * sizePx / sealMmToPt(SEAL_SIZE_MM)
            textAlign = Paint.Align.CENTER
        }
        val gap = sizePx * 0.01f
        canvas.drawText("Id $deviceId", cx, rect.bottom + gap - idPaint.ascent(), idPaint)
    }

    val dotRadius = sizePx * 0.026f
    canvas.drawCircle(cx + textRingRadius * cos(Math.toRadians(20.0)).toFloat(), cy + textRingRadius * sin(Math.toRadians(20.0)).toFloat(), dotRadius, fillPaint)
    canvas.drawCircle(cx + textRingRadius * cos(Math.toRadians(160.0)).toFloat(), cy + textRingRadius * sin(Math.toRadians(160.0)).toFloat(), dotRadius, fillPaint)

    drawArcText(canvas, SEAL_TOP_TEXT, cx, cy, textRingRadius, topTextPaint, centerAngleDeg = 265f, flipped = false)
    drawArcText(canvas, unitText, cx, cy, textRingRadius, bottomTextPaint, centerAngleDeg = 90f, flipped = true)

    return bitmap
}

/** Dibuja el escudo de España tintado al color del sello, centrado en el interior. */
private fun drawSealEscudo(
    context: Context,
    canvas: Canvas,
    cx: Float,
    cy: Float,
    maxWidth: Float
): RectF? {
    val escudo = runCatching {
        context.assets.open("images/EscEspana_bw.png").use { BitmapFactory.decodeStream(it) }
    }.getOrNull() ?: return null
    val tinted = tintMonochromeToSeal(escudo)
    val aspect = tinted.height.toFloat() / tinted.width.toFloat().coerceAtLeast(1f)
    val targetWidth = maxWidth
    val targetHeight = targetWidth * aspect
    val left = cx - targetWidth / 2f
    val top = cy - targetHeight / 2f - targetHeight * 0.04f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val target = RectF(left, top, left + targetWidth, top + targetHeight)
    canvas.drawBitmap(tinted, null, target, paint)
    return target
}

/**
 * Convierte un bitmap en escala de grises (posiblemente opaco, negro sobre
 * blanco) en una máscara monocroma del color del sello: los píxeles oscuros se
 * vuelven opacos con el color del sello y los claros transparentes.
 */
private fun tintMonochromeToSeal(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val sealR = Color.red(SEAL_COLOR)
    val sealG = Color.green(SEAL_COLOR)
    val sealB = Color.blue(SEAL_COLOR)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha == 0) {
            pixels[i] = 0
            continue
        }
        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF
        val luminance = (r * 299 + g * 587 + b * 114) / 1000
        val newAlpha = alpha * (255 - luminance) / 255
        pixels[i] = (newAlpha shl 24) or (sealR shl 16) or (sealG shl 8) or sealB
    }
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
}

/**
 * Dibuja un texto siguiendo el arco de una circunferencia, con orientación
 * legible y auto-ajuste del cuerpo para no superar el barrido máximo.
 *
 * @param canvas Canvas destino.
 * @param text Texto a dibujar.
 * @param cx Coordenada X del centro del arco.
 * @param cy Coordenada Y del centro del arco.
 * @param textRingRadius Radio de la línea media del anillo de texto.
 * @param paint Pincel de texto (se ajusta su tamaño si es necesario).
 * @param centerAngleDeg Ángulo central del arco (270 = superior, 90 = inferior).
 * @param flipped True para el arco inferior (texto invertido legible).
 */
private fun drawArcText(
    canvas: Canvas,
    text: String,
    cx: Float,
    cy: Float,
    textRingRadius: Float,
    paint: Paint,
    centerAngleDeg: Float,
    flipped: Boolean
) {
    if (text.isBlank()) return
    fitArcTextSize(text, paint, textRingRadius)

    val widths = text.map { paint.measureText(it.toString()) }
    val totalWidth = widths.sum()
    val totalAngle = Math.toDegrees((totalWidth / textRingRadius).toDouble()).toFloat()
    val fontMetrics = paint.fontMetrics
    val baselineOffset = (-fontMetrics.ascent) / 2f
    val baselineRadius = if (flipped) textRingRadius + baselineOffset else textRingRadius - baselineOffset

    val step = if (flipped) -1f else 1f
    val firstCharAngle = Math.toDegrees((widths.first().toDouble() / baselineRadius)).toFloat()
    val lastCharAngle = Math.toDegrees((widths.last().toDouble() / baselineRadius)).toFloat()
    val symmetryOffset = if (flipped) {
        (firstCharAngle - lastCharAngle) / 4f
    } else {
        (lastCharAngle - firstCharAngle) / 4f
    }
    var currentAngle = if (flipped) {
        centerAngleDeg + totalAngle / 2f + symmetryOffset
    } else {
        centerAngleDeg - totalAngle / 2f + symmetryOffset
    }

    for (index in text.indices) {
        val charText = text.substring(index, index + 1)
        val charAngle = Math.toDegrees((widths[index] / baselineRadius).toDouble()).toFloat()
        val midAngle = currentAngle + step * charAngle / 2f
        val radians = Math.toRadians(midAngle.toDouble())
        val x = cx + baselineRadius * cos(radians).toFloat()
        val y = cy + baselineRadius * sin(radians).toFloat()
        val rotation = if (flipped) midAngle - 90f else midAngle - 270f
        canvas.save()
        canvas.rotate(rotation, x, y)
        canvas.drawText(charText, x, y, paint)
        canvas.restore()
        currentAngle += step * charAngle
    }
}

/** Reduce progresivamente el cuerpo del pincel hasta que el arco quepa. */
private fun fitArcTextSize(text: String, paint: Paint, textRingRadius: Float) {
    var guard = 0
    var sweep = arcTextSweepDeg(text, paint, textRingRadius)
    while (sweep > SEAL_MAX_TEXT_SWEEP_DEG && guard < 6 && paint.textSize > 1f) {
        paint.textSize *= (SEAL_MAX_TEXT_SWEEP_DEG / sweep) * 0.98f
        sweep = arcTextSweepDeg(text, paint, textRingRadius)
        guard++
    }
}

/** Calcula el barrido angular que ocuparía el texto con el pincel actual. */
private fun arcTextSweepDeg(text: String, paint: Paint, radius: Float): Float {
    var width = 0f
    for (char in text) width += paint.measureText(char.toString())
    if (radius <= 0f) return 0f
    return Math.toDegrees((width / radius).toDouble()).toFloat()
}

/** Carga la tipografía Calibri regular de los assets del proyecto. */
private fun loadSealTypeface(context: Context): Typeface? =
    runCatching { Typeface.createFromAsset(context.assets, "fonts/calibri-regular.ttf") }.getOrNull()
