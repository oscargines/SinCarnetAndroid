package com.oscar.sincarnet

import java.util.Locale

private val ALLOWED_ZEBRA_MODELS = setOf("RW420", "ZQ521")

internal fun normalizeZebraModel(model: String): String =
    model
        .uppercase(Locale.ROOT)
        .replace(" ", "")
        .replace("-", "")
        .replace("_", "")

internal fun isAllowedZebraPrinterModel(model: String?): Boolean {
    if (model.isNullOrBlank()) return false
    return normalizeZebraModel(model) in ALLOWED_ZEBRA_MODELS
}

internal fun supportedBluetoothPrinterModelsText(): String = "RW420, ZQ521"

