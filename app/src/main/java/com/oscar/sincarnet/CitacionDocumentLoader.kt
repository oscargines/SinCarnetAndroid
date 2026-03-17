package com.oscar.sincarnet

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class CitacionDocument(
    val titulo: String = "",
    val cuerpoDescripcion: String = "",
    val secciones: List<CitacionSeccion> = emptyList(),
    val cierre: String = "",
    val enteradoTitulo: String = "",
    val enteradoTexto: String = ""
)

internal data class CitacionSeccion(
    val titulo: String = "",
    val contenido: String = "",
    val opciones: List<CitacionOpcion> = emptyList(),
    val informacionAdicional: String = "",
    val items: List<CitacionItem> = emptyList()
)

internal data class CitacionOpcion(
    val afectado: String = "",
    val descripcion: String = ""
)

internal data class CitacionItem(
    val afectado: String = "",
    val descripcion: String = ""
)

internal fun loadCitacionDocument(
    context: Context,
    tipoJuicio: String
): CitacionDocument {
    val fileName = when (tipoJuicio) {
        "rapido" -> "citacionjuiciorapido.json"
        else -> "citacionjuicio.json"
    }
    
    return runCatching {
        val inputStream = context.assets.open("docs/$fileName")
        val content = InputStreamReader(inputStream).use { it.readText() }
        val jsonRoot = JSONObject(content)
        val documento = jsonRoot.getJSONObject("documento")
        
        val titulo = documento.optString("titulo", "")
        val cuerpo = documento.getJSONObject("cuerpo")
        val cuerpoDescripcion = cuerpo.optString("descripcion", "")
        
        val seccionesArray = documento.getJSONArray("secciones")
        val secciones = mutableListOf<CitacionSeccion>()
        
        for (i in 0 until seccionesArray.length()) {
            val secObj = seccionesArray.getJSONObject(i)
            val secTitulo = secObj.optString("titulo", "")
            val secContenido = secObj.optString("contenido", "")
            val secInfoAdicional = secObj.optString("informacion_adicional", "")
            
            val opciones = mutableListOf<CitacionOpcion>()
            if (secObj.has("opciones")) {
                val opcionesArray = secObj.getJSONArray("opciones")
                for (j in 0 until opcionesArray.length()) {
                    val optObj = opcionesArray.getJSONObject(j)
                    opciones.add(
                        CitacionOpcion(
                            afectado = optObj.optString("afectado", ""),
                            descripcion = optObj.optString("descripcion", "")
                        )
                    )
                }
            }
            
            val items = mutableListOf<CitacionItem>()
            if (secObj.has("items")) {
                val itemsArray = secObj.getJSONArray("items")
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    items.add(
                        CitacionItem(
                            afectado = itemObj.optString("afectado", ""),
                            descripcion = itemObj.optString("descripcion", "")
                        )
                    )
                }
            }
            
            secciones.add(
                CitacionSeccion(
                    titulo = secTitulo,
                    contenido = secContenido,
                    opciones = opciones,
                    informacionAdicional = secInfoAdicional,
                    items = items
                )
            )
        }
        
        val cierre = documento.optString("cierre", "")
        val enterado = documento.getJSONObject("enterado")
        val enteradoTitulo = enterado.optString("titulo", "")
        val enteradoTexto = enterado.optString("texto", "")
        
        CitacionDocument(
            titulo = titulo,
            cuerpoDescripcion = cuerpoDescripcion,
            secciones = secciones,
            cierre = cierre,
            enteradoTitulo = enteradoTitulo,
            enteradoTexto = enteradoTexto
        )
    }.getOrNull() ?: CitacionDocument()
}

internal fun replaceCitacionPlaceholders(
    text: String,
    courtData: JuzgadoAtestadoData,
    personData: PersonaInvestigadaData,
    ocurrenciaData: OcurrenciaDelitData,
    instructorTip: String,
    secretaryTip: String,
    instructorUnit: String
): String {
    var result = text

    // Lugar y fecha de la diligencia (desde OcurrenciaDelitStorage)
    result = result.replace("[[lugar]]", ocurrenciaData.localidad)
    result = result.replace("[[terminomunicipal]]", ocurrenciaData.terminoMunicipal)
    result = result.replace("[[hora]]", ocurrenciaData.hora)

    // Fecha con formato legible: "17 de marzo de 2026"
    val fechaFormateada = runCatching {
        val parsed = LocalDate.parse(ocurrenciaData.fecha, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        parsed.format(formatter)
    }.getOrElse { ocurrenciaData.fecha }
    result = result.replace("[[fechacompleta]]", fechaFormateada)

    // Partido judicial = municipio del juzgado
    result = result.replace("[[partidojudicial]]", courtData.municipioNombre)

    // Datos de actuantes
    result = result.replace("[[instructor]]", instructorTip)
    result = result.replace("[[secretario]]", secretaryTip)
    result = result.replace("[[unidadinferior]]", instructorUnit)

    // Datos del investigado
    val nombreCompleto = listOf(personData.firstName, personData.lastName1, personData.lastName2)
        .filter { it.isNotBlank() }.joinToString(" ")
    result = result.replace("[[nombrecompletoinvestigado]]", nombreCompleto)
    result = result.replace("[[documentoidentificacion]]", "")

    // Datos del juzgado (campos individuales)
    result = result.replace("[[nombrejuzgado]]", courtData.sedeNombre)
    result = result.replace("[[direccionjuzgado]]", courtData.sedeDireccion)
    result = result.replace("[[telefonojuzgado]]", courtData.sedeTelefono)
    result = result.replace("[[codigopostaljuzgado]]", courtData.sedeCodigoPostal)

    // Datos del juzgado (composición completa)
    val datosJuzgado = listOf(
        courtData.sedeNombre,
        courtData.sedeDireccion,
        courtData.sedeTelefono,
        courtData.sedeCodigoPostal
    ).map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { courtData.sedeNombre.ifBlank { courtData.municipioNombre } }
    result = result.replace("[[datosjuzgado]]", datosJuzgado)

    // Juicio rápido
    result = result.replace("[[horajuicio]]", courtData.horaJuicioRapido)
    result = result.replace("[[fechajuicio]]", courtData.fechaJuicioRapido)

    return result
}
