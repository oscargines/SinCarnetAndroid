package com.oscar.sincarnet

/**
 * Línea de texto reconocida por OCR junto con su caja delimitadora en píxeles.
 */
internal data class CanTextLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

/**
 * Zona de la imagen, en coordenadas relativas (0..1), donde se espera el CAN.
 */
internal data class CanGuideArea(
    val leftFraction: Float,
    val topFraction: Float,
    val rightFraction: Float,
    val bottomFraction: Float
) {
    fun contains(centerX: Float, centerY: Float): Boolean =
        centerX in leftFraction..rightFraction && centerY in topFraction..bottomFraction
}

/**
 * Extrae el código CAN (6 dígitos) del texto reconocido por la cámara.
 *
 * El CAN está impreso en la esquina inferior derecha del anverso del DNIe, por
 * lo que se descartan números de 8 dígitos (DNI, fechas) y secuencias pegadas a
 * letras (número de soporte). Si hay varios candidatos se prioriza el que cae
 * dentro de [guideArea] y, en su defecto, el más cercano a la esquina inferior
 * derecha.
 */
internal object CanCodeDetector {

    private val candidateRegex =
        Regex("(?<![A-Za-z0-9])([0-9OoIlSsZzBbGg]{6})(?![A-Za-z0-9])")

    private val confusionMap = mapOf(
        'O' to '0', 'o' to '0',
        'I' to '1', 'l' to '1',
        'S' to '5', 's' to '5',
        'Z' to '2', 'z' to '2',
        'B' to '8', 'b' to '8',
        'G' to '6', 'g' to '6'
    )

    /**
     * Devuelve el CAN más probable o `null` si no hay candidatos válidos.
     */
    fun detect(
        lines: List<CanTextLine>,
        imageWidth: Int,
        imageHeight: Int,
        guideArea: CanGuideArea? = null
    ): String? {
        if (imageWidth <= 0 || imageHeight <= 0) return null

        val candidates = lines.flatMap { line ->
            candidateRegex.findAll(line.text).mapNotNull { match ->
                val code = normalize(match.value) ?: return@mapNotNull null
                val centerX = ((line.left + line.right) / 2f) / imageWidth
                val centerY = ((line.top + line.bottom) / 2f) / imageHeight
                CanCandidate(code = code, centerX = centerX, centerY = centerY)
            }
        }

        if (candidates.isEmpty()) return null

        val insideGuide = guideArea?.let { guide ->
            candidates.filter { guide.contains(it.centerX, it.centerY) }
        }.orEmpty()

        val pool = if (insideGuide.isNotEmpty()) insideGuide else candidates
        return pool.maxByOrNull { it.centerX + it.centerY }?.code
    }

    private fun normalize(token: String): String? {
        if (token.all { it.isDigit() }) return token
        if (token.count { it.isDigit() } < 2) return null

        val normalized = buildString(token.length) {
            token.forEach { char ->
                append(confusionMap[char] ?: char)
            }
        }

        return normalized.takeIf { it.length == 6 && it.all { digit -> digit.isDigit() } }
    }

    private data class CanCandidate(
        val code: String,
        val centerX: Float,
        val centerY: Float
    )
}
