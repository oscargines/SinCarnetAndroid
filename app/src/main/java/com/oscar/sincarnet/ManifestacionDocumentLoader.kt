package com.oscar.sincarnet

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader

internal data class ManifestacionDocument(
    val titulo: String = "",
    val cuerpoDescripcion: String = "",
    val opcionesDescripcion: String = "",
    val opciones: List<ManifestacionOption> = emptyList(),
    val documentacionDescripcion: String = "",
    val anexos: List<ManifestacionAnexo> = emptyList(),
    val preguntas: List<ManifestacionQuestion> = emptyList(),
    val cierreTexto: String = ""
)

internal data class ManifestacionOption(
    val id: Int,
    val texto: String
)

internal data class ManifestacionAnexo(
    val id: String,
    val texto: String
)

internal data class ManifestacionQuestion(
    val id: Int,
    val pregunta: String,
    val campoVariable: String = ""
)

internal fun loadManifestacionDocument(context: Context): ManifestacionDocument {
    return runCatching {
        val inputStream = context.assets.open("docs/04manifestacion.json")
        val content = InputStreamReader(inputStream).use { it.readText() }
        val jsonRoot = JSONObject(content)
        val documento = jsonRoot.getJSONObject("documento")
        val cuerpo = documento.getJSONObject("cuerpo")
        val opcionesInvestigado = documento.getJSONObject("opciones_investigado")
        val documentacion = documento.getJSONObject("documentacion")
        val cierre = documento.getJSONObject("cierre")

        val opciones = buildList {
            val opcionesArray = opcionesInvestigado.getJSONArray("opciones")
            for (index in 0 until opcionesArray.length()) {
                val option = opcionesArray.getJSONObject(index)
                add(
                    ManifestacionOption(
                        id = option.optInt("id", index + 1),
                        texto = option.optString("texto", "")
                    )
                )
            }
        }

        val anexos = buildList {
            val anexosArray = documentacion.getJSONArray("anexos")
            for (index in 0 until anexosArray.length()) {
                val anexo = anexosArray.getJSONObject(index)
                add(
                    ManifestacionAnexo(
                        id = anexo.optString("id", "Anexo ${index + 1}"),
                        texto = anexo.optString("texto", "")
                    )
                )
            }
        }

        val preguntas = buildList {
            val preguntasArray = documento.getJSONArray("preguntas")
            for (index in 0 until preguntasArray.length()) {
                val pregunta = preguntasArray.getJSONObject(index)
                add(
                    ManifestacionQuestion(
                        id = pregunta.optInt("id", index + 1),
                        pregunta = pregunta.optString("pregunta", ""),
                        campoVariable = pregunta.optString("campo_variable", "")
                    )
                )
            }
        }

        ManifestacionDocument(
            titulo = documento.optString("titulo", ""),
            cuerpoDescripcion = cuerpo.optString("descripcion", ""),
            opcionesDescripcion = opcionesInvestigado.optString("descripcion", ""),
            opciones = opciones,
            documentacionDescripcion = documentacion.optString("descripcion", ""),
            anexos = anexos,
            preguntas = preguntas,
            cierreTexto = cierre.optString("texto", "")
        )
    }.getOrElse {
        ManifestacionDocument(
            titulo = "Manifestación",
            cuerpoDescripcion = "No se pudo cargar el documento de manifestación."
        )
    }
}

internal fun replaceManifestacionPlaceholders(
    text: String,
    personData: PersonaInvestigadaData,
    courtData: JuzgadoAtestadoData,
    ocurrenciaData: OcurrenciaDelitData,
    vehicleData: VehiculoData
): String {
    val nombreCompleto = listOf(personData.firstName, personData.lastName1, personData.lastName2)
        .filter { it.isNotBlank() }
        .joinToString(" ")

    val horaFechaManifestacion = listOf(ocurrenciaData.hora, ocurrenciaData.fecha)
        .filter { it.isNotBlank() }
        .joinToString(" del día ")

    // Si otrosdocumentos está vacío, sustituir por línea
    val otrosDocumentos = if (personData.otrosDocumentos.isNullOrBlank()) "_______________" else personData.otrosDocumentos
    return text
        .replace("[[horafechamanifestacion]]", horaFechaManifestacion)
        .replace("[[terminomunicipal]]", ocurrenciaData.terminoMunicipal)
        .replace("[[partidojudicial]]", courtData.municipioNombre)
        .replace("[[nombrecompletoinvestigado]]", nombreCompleto)
        .replace("[[documentoidentificacion]]", "")
        .replace("[[matricula]]", vehicleData.plate)
        .replace("[[otrosdocumentos]]", otrosDocumentos)
        .replace("[[segundafechahora]]", horaFechaManifestacion)
}

