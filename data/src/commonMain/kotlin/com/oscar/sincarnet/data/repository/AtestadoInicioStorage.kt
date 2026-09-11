package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.AtestadoInicioModalData

class AtestadoInicioStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): AtestadoInicioModalData = AtestadoInicioModalData(
        motivo = storage.getString(KEY_MOTIVO, ""),
        norma = storage.getString(KEY_NORMA, ""),
        articulo = storage.getString(KEY_ARTICULO, ""),
        dgtNoRecord = storage.getBoolean(KEY_DGT_NO_RECORD, false),
        internationalNoRecord = storage.getBoolean(KEY_INTERNATIONAL_NO_RECORD, false),
        existsRecord = storage.getBoolean(KEY_EXISTS_RECORD, false),
        vicisitudesOption = storage.getString(KEY_VICISITUDES_OPTION, ""),
        jefaturaProvincial = storage.getString(KEY_JEFATURA_PROVINCIAL, ""),
        numeroBoletin = storage.getString(KEY_NUMERO_BOLETIN, ""),
        tieneAntecedentes = storage.getBoolean(KEY_TIENE_ANTECEDENTES, false),
        antecedentesGuardiaCivil = storage.getBoolean(KEY_ANTECEDENTES_GUARDIA_CIVIL, false),
        antecedentesSenalamientosNacionales = storage.getBoolean(KEY_ANTECEDENTES_SENALES, false),
        antecedentesDgt = storage.getBoolean(KEY_ANTECEDENTES_DGT, false),
        antecedentesOtrosCuerpos = storage.getBoolean(KEY_ANTECEDENTES_OTROS_CUERPOS, false),
        requisitoriasJudiciales = storage.getBoolean(KEY_REQUISITORIAS_JUDICIALES, false),
        enviarPorLexnet = storage.getBoolean(KEY_ENVIAR_POR_LEXNET, true),
        modoEnvio = storage.getString(KEY_MODO_ENVIO, ""),
        atestadoPdfPath = storage.getString(KEY_ATESTADO_PDF_PATH, ""),
        documentosEscaneadosPdfPath = storage.getString(KEY_DOCUMENTOS_ESCANEADOS_PDF_PATH, ""),
        atestadoCompletoPdfPath = storage.getString(KEY_ATESTADO_COMPLETO_PDF_PATH, ""),
        tiempoPrivacion = storage.getString(KEY_TIEMPO_PRIVACION, ""),
        juzgadoDecreta = storage.getString(KEY_JUZGADO_DECRETA, "")
    )

    fun saveCurrent(data: AtestadoInicioModalData) {
        storage.putString(KEY_MOTIVO, data.motivo)
        storage.putString(KEY_NORMA, data.norma)
        storage.putString(KEY_ARTICULO, data.articulo)
        storage.putBoolean(KEY_DGT_NO_RECORD, data.dgtNoRecord)
        storage.putBoolean(KEY_INTERNATIONAL_NO_RECORD, data.internationalNoRecord)
        storage.putBoolean(KEY_EXISTS_RECORD, data.existsRecord)
        storage.putString(KEY_VICISITUDES_OPTION, data.vicisitudesOption)
        storage.putString(KEY_JEFATURA_PROVINCIAL, data.jefaturaProvincial)
        storage.putString(KEY_NUMERO_BOLETIN, data.numeroBoletin)
        storage.putBoolean(KEY_TIENE_ANTECEDENTES, data.tieneAntecedentes)
        storage.putBoolean(KEY_ANTECEDENTES_GUARDIA_CIVIL, data.antecedentesGuardiaCivil)
        storage.putBoolean(KEY_ANTECEDENTES_SENALES, data.antecedentesSenalamientosNacionales)
        storage.putBoolean(KEY_ANTECEDENTES_DGT, data.antecedentesDgt)
        storage.putBoolean(KEY_ANTECEDENTES_OTROS_CUERPOS, data.antecedentesOtrosCuerpos)
        storage.putBoolean(KEY_REQUISITORIAS_JUDICIALES, data.requisitoriasJudiciales)
        storage.putBoolean(KEY_ENVIAR_POR_LEXNET, data.enviarPorLexnet)
        storage.putString(KEY_MODO_ENVIO, data.modoEnvio)
        storage.putString(KEY_ATESTADO_PDF_PATH, data.atestadoPdfPath)
        storage.putString(KEY_DOCUMENTOS_ESCANEADOS_PDF_PATH, data.documentosEscaneadosPdfPath)
        storage.putString(KEY_ATESTADO_COMPLETO_PDF_PATH, data.atestadoCompletoPdfPath)
        storage.putString(KEY_TIEMPO_PRIVACION, data.tiempoPrivacion)
        storage.putString(KEY_JUZGADO_DECRETA, data.juzgadoDecreta)
    }

    fun clearCurrent() {
        storage.remove(KEY_MOTIVO)
        storage.remove(KEY_NORMA)
        storage.remove(KEY_ARTICULO)
        storage.remove(KEY_DGT_NO_RECORD)
        storage.remove(KEY_INTERNATIONAL_NO_RECORD)
        storage.remove(KEY_EXISTS_RECORD)
        storage.remove(KEY_VICISITUDES_OPTION)
        storage.remove(KEY_JEFATURA_PROVINCIAL)
        storage.remove(KEY_NUMERO_BOLETIN)
        storage.remove(KEY_TIENE_ANTECEDENTES)
        storage.remove(KEY_ANTECEDENTES_GUARDIA_CIVIL)
        storage.remove(KEY_ANTECEDENTES_SENALES)
        storage.remove(KEY_ANTECEDENTES_DGT)
        storage.remove(KEY_ANTECEDENTES_OTROS_CUERPOS)
        storage.remove(KEY_REQUISITORIAS_JUDICIALES)
        storage.remove(KEY_ENVIAR_POR_LEXNET)
        storage.remove(KEY_MODO_ENVIO)
        storage.remove(KEY_ATESTADO_PDF_PATH)
        storage.remove(KEY_DOCUMENTOS_ESCANEADOS_PDF_PATH)
        storage.remove(KEY_ATESTADO_COMPLETO_PDF_PATH)
        storage.remove(KEY_TIEMPO_PRIVACION)
        storage.remove(KEY_JUZGADO_DECRETA)
    }

    private companion object {
        const val KEY_MOTIVO = "motivo"
        const val KEY_NORMA = "norma"
        const val KEY_ARTICULO = "articulo"
        const val KEY_DGT_NO_RECORD = "dgt_no_record"
        const val KEY_INTERNATIONAL_NO_RECORD = "international_no_record"
        const val KEY_EXISTS_RECORD = "exists_record"
        const val KEY_VICISITUDES_OPTION = "vicisitudes_option"
        const val KEY_JEFATURA_PROVINCIAL = "jefatura_provincial"
        const val KEY_NUMERO_BOLETIN = "numero_boletin"
        const val KEY_TIENE_ANTECEDENTES = "tiene_antecedentes"
        const val KEY_ANTECEDENTES_GUARDIA_CIVIL = "antecedentes_guardia_civil"
        const val KEY_ANTECEDENTES_SENALES = "antecedentes_senalamientos_nacionales"
        const val KEY_ANTECEDENTES_DGT = "antecedentes_dgt"
        const val KEY_ANTECEDENTES_OTROS_CUERPOS = "antecedentes_otros_cuerpos"
        const val KEY_REQUISITORIAS_JUDICIALES = "requisitorias_judiciales"
        const val KEY_ENVIAR_POR_LEXNET = "enviar_por_lexnet"
        const val KEY_MODO_ENVIO = "modo_envio"
        const val KEY_ATESTADO_PDF_PATH = "atestado_pdf_path"
        const val KEY_DOCUMENTOS_ESCANEADOS_PDF_PATH = "documentos_escaneados_pdf_path"
        const val KEY_ATESTADO_COMPLETO_PDF_PATH = "atestado_completo_pdf_path"
        const val KEY_TIEMPO_PRIVACION = "tiempo_privacion"
        const val KEY_JUZGADO_DECRETA = "juzgado_decreta"
    }
}
