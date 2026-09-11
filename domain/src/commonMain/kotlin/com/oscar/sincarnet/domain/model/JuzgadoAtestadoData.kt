package com.oscar.sincarnet.domain.model

data class JuzgadoAtestadoData(
    val ccaaId: Int? = null,
    val ccaaNombre: String = "",
    val provinciaId: Int? = null,
    val provinciaNombre: String = "",
    val municipioNombre: String = "",
    val sedeId: Int? = null,
    val sedeNombre: String = "",
    val sedeDireccion: String = "",
    val sedeTelefono: String = "",
    val sedeCodigoPostal: String = "",
    val numeroDiligencias: String = "",
    val tipoJuicio: String = "",
    val fechaJuicioRapido: String = "",
    val horaJuicioRapido: String = ""
)

fun JuzgadoAtestadoData.isJuicioRapido(): Boolean {
    val tipoNormalized = java.text.Normalizer.normalize(this.tipoJuicio, java.text.Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
        .trim()

    if (tipoNormalized.contains("rapido")) return true
    if (this.fechaJuicioRapido.isNotBlank() || this.horaJuicioRapido.isNotBlank()) return true
    return false
}