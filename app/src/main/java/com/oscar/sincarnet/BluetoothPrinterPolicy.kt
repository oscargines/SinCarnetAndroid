package com.oscar.sincarnet

import java.util.Locale

private val ALLOWED_ZEBRA_MODELS = setOf("RW420", "ZQ521")

/**
 * Normaliza el nombre de modelo de impresora Zebra a un formato estándar.
 *
 * Aplica las siguientes transformaciones:
 * - Convierte a mayúsculas (usando locale de raíz)
 * - Elimina espacios en blanco
 * - Elimina guiones
 * - Elimina guiones bajos
 *
 * Ejemplo: "RW 420", "rw-420", "rw_420" → "RW420"
 *
 * @param model Nombre del modelo a normalizar (p. ej. "RW 420", "ZQ521", "rw420").
 * @return Nombre normalizado en mayúsculas sin caracteres especiales.
 */
internal fun normalizeZebraModel(model: String): String =
    model
        .uppercase(Locale.ROOT)
        .replace(" ", "")
        .replace("-", "")
        .replace("_", "")

/**
 * Verifica si un modelo de impresora Zebra está en la lista de modelos permitidos.
 *
 * Primero normaliza el nombre del modelo y luego lo compara contra la lista
 * [ALLOWED_ZEBRA_MODELS]. Devuelve false si el modelo es nulo o está en blanco.
 *
 * @param model Nombre del modelo a validar, puede ser null o en blanco.
 * @return true si el modelo está permitido, false en caso contrario.
 *
 * @see normalizeZebraModel
 */
internal fun isAllowedZebraPrinterModel(model: String?): Boolean {
    if (model.isNullOrBlank()) return false
    return normalizeZebraModel(model) in ALLOWED_ZEBRA_MODELS
}

/**
 * Obtiene una cadena descriptiva de los modelos de impresoras Bluetooth soportados.
 *
 * Utilizada en la interfaz de usuario para informar al usuario sobre qué
 * modelos de impresoras son compatibles con la aplicación.
 *
 * @return Cadena con los nombres de los modelos soportados separados por comas (p. ej. "RW420, ZQ521").
 */
internal fun supportedBluetoothPrinterModelsText(): String = "RW420, ZQ521"

