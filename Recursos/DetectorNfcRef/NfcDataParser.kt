package com.oscar.detectornfc

import android.util.Log
import de.tsenger.androsmex.mrtd.DG1_Dnie
import de.tsenger.androsmex.mrtd.DG11
import de.tsenger.androsmex.mrtd.DG13

/**
 * Parsea los DataGroups leídos del chip NFC usando las clases de la librería
 * dniedroid-release.aar (DG1_Dnie, DG11, DG13), que ya incorporan la lógica
 * de decodificación ASN.1 internamente.
 *
 * Soporta:
 *  - DG1  (MRZ): TD1 (DNIe) y TD3 (pasaporte)
 *  - DG11 (Datos adicionales ICAO — nombre completo, domicilio, fecha nacimiento)
 *  - DG13 (Datos extendidos exclusivos del DNIe español)
 *
 * Se prioriza DG13 > DG11 > DG1 para cada campo.
 */
class NfcDataParser {

    private val TAG = "NfcDataParser"

    fun parseRawData(rawData: RawNfcData): DniData {
        if (rawData.sessionStatus == NfcSessionStatus.FAILED) {
            val failureMessage = rawData.sessionError ?: "No se pudo completar la sesion NFC."
            Log.w(TAG, "Sesion NFC fallida: $failureMessage")
            return DniData(
                genero = null, nacionalidad = null, tipoDocumento = null,
                numeroDocumento = null, numeroSoporte = null, nombre = null,
                apellidos = null, nombrePadre = null, nombreMadre = null,
                fechaNacimiento = null, lugarNacimiento = null, domicilio = null,
                uid = rawData.uid, can = rawData.can,
                error = failureMessage
            )
        }

        val dg1Bytes  = rawData.dataGroups[1]
        val dg11Bytes = rawData.dataGroups[11]
        val dg13Bytes = rawData.dataGroups[13]

        Log.i(TAG, "parseRawData() - dg1=${dg1Bytes?.size ?: 0}B, " +
                "dg11=${dg11Bytes?.size ?: 0}B, dg13=${dg13Bytes?.size ?: 0}B")

        if (dg1Bytes == null && dg13Bytes == null) {
            Log.w(TAG, "Sin DG1 ni DG13 — datos insuficientes")
            val errorMessage = rawData.sessionError ?: "No se pudo leer DG1 ni DG13"
            return DniData(
                genero = null, nacionalidad = null, tipoDocumento = null,
                numeroDocumento = null, numeroSoporte = null, nombre = null,
                apellidos = null, nombrePadre = null, nombreMadre = null,
                fechaNacimiento = null, lugarNacimiento = null, domicilio = null,
                uid = rawData.uid, can = rawData.can,
                error = errorMessage
            )
        }

        // Construir objetos de datos. Los constructores toman los bytes crudos
        // y realizan el parseo ASN.1 internamente.
        val dg1  = dg1Bytes?.let  { safe { DG1_Dnie(it) } }
        val dg11 = dg11Bytes?.let { safe { DG11(it)     } }
        val dg13 = dg13Bytes?.let { safe { DG13(it)     } }

        val detected = detectDocumentProfile(dg1)

        // ── Nombre ──────────────────────────────────────────────────────
        val nombre = dg13?.getName()?.takeIf { it.isNotBlank() }
            ?: dg11?.getName()?.takeIf { it.isNotBlank() }
            ?: dg1?.getName()?.takeIf  { it.isNotBlank() }

        // ── Apellidos (DG13 tiene primer y segundo separados) ───────────
        val apellidos = if (dg13 != null) {
            val s1 = dg13.getSurName1()?.takeIf { it.isNotBlank() }
            val s2 = dg13.getSurName2()?.takeIf { it.isNotBlank() }
            when {
                s1 != null && s2 != null -> "$s1 $s2"
                s1 != null -> s1
                else -> dg1?.getSurname()?.takeIf { it.isNotBlank() }
            }
        } else {
            dg1?.getSurname()?.takeIf { it.isNotBlank() }
        }

        // ── Género ──────────────────────────────────────────────────────
        val genero = (dg13?.getSex() ?: dg1?.getSex())?.uppercase()?.let {
            when (it) { "F" -> "Femenino"; "M" -> "Masculino"; else -> null }
        }

        // ── Nacionalidad (solo disponible en DG1) ───────────────────────
        val nacionalidad = dg1?.getNationality()?.takeIf { it.isNotBlank() } ?: "ESP"

        // ── Tipo de documento ────────────────────────────────────────────
        val tipoDocumento = dg1?.getDocType()?.takeIf { it.isNotBlank() } ?: "DNI"

        // ── Número de documento (NIF/NIE): DG13.personalNumber o DG1.docNumber ──
        val numeroDocumento = dg13?.getPersonalNumber()?.takeIf { it.isNotBlank() }
            ?: dg1?.getDocNumber()?.takeIf { it.isNotBlank() }

        // ── Número de soporte (impreso en la tarjeta física) ────────────
        val numeroSoporte = dg1?.getDocNumber()?.takeIf { it.isNotBlank() }

        // ── Fecha de nacimiento ──────────────────────────────────────────
        val fechaNacimiento = (dg13?.getBirthDate()
            ?: dg11?.getBirthDate()
            ?: dg1?.getDateOfBirth())
            ?.let { formatDate(it) }

        // ── Lugar de nacimiento ──────────────────────────────────────────
        val lugarNacimiento = if (dg13 != null) {
            listOfNotNull(dg13.getBirthPopulation(), dg13.getBirthProvince())
                .filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }
                ?: dg11?.getBirthPlace()?.takeIf { it.isNotBlank() }
        } else {
            dg11?.getBirthPlace()?.takeIf { it.isNotBlank() }
        }

        // ── Domicilio actual ─────────────────────────────────────────────
        val domicilio = if (dg13 != null) {
            listOfNotNull(
                dg13.getActualAddress(),
                dg13.getActualPopulation(),
                dg13.getActualProvince()
            ).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }
        } else if (dg11 != null) {
            listOfNotNull(
                dg11.getAddress(DG11.ADDR_DIRECCION),
                dg11.getAddress(DG11.ADDR_LOCALIDAD),
                dg11.getAddress(DG11.ADDR_PROVINCIA)
            ).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }
        } else null

        val extractionError = if (nombre == null && apellidos == null && numeroDocumento == null)
            "No se pudieron extraer datos clave del DNI" else null
        val errorMessage = when {
            rawData.sessionStatus == NfcSessionStatus.PARTIAL && !rawData.sessionError.isNullOrBlank() ->
                rawData.sessionError
            extractionError != null -> extractionError
            else -> null
        }

        Log.i(TAG, "parseRawData() OK — nombre=${nombre != null}, " +
                "apellidos=${apellidos != null}, nif=${numeroDocumento != null}, error=${errorMessage ?: "<ninguno>"}")

        return DniData(
            genero          = genero,
            nacionalidad    = nacionalidad,
            tipoDocumento   = tipoDocumento,
            numeroDocumento = numeroDocumento,
            numeroSoporte   = numeroSoporte,
            nombre          = nombre,
            apellidos       = apellidos,
            nombrePadre     = dg13?.getFatherName()?.takeIf { it.isNotBlank() },
            nombreMadre     = dg13?.getMotherName()?.takeIf { it.isNotBlank() },
            fechaNacimiento = fechaNacimiento,
            lugarNacimiento = lugarNacimiento,
            domicilio       = domicilio,
            uid             = rawData.uid,
            can             = rawData.can,
            error           = errorMessage,
            documentType    = detected.documentType.name,
            countryCode     = detected.countryCode,
            countryName     = detected.countryName,
            architecture    = detected.architecture.name
        )
    }

    private fun detectDocumentProfile(dg1: DG1_Dnie?): ClassificationResult {
        if (dg1 == null) {
            return ClassificationResult(
                documentType = DocumentType.UNKNOWN,
                countryCode = "UNK",
                countryName = "Desconocido",
                architecture = DocumentArchitecture.UNKNOWN
            )
        }
        return DocumentClassifier.classify(dg1.getDocType(), dg1.getIssuer())
    }

    // ------------------------------------------------------------------ //
    //  Formateo de fechas
    // ------------------------------------------------------------------ //

    /**
     * Convierte varios formatos de fecha a "DD de mes de YYYY":
     *  - "YYMMDD"     (DG1 TD3 — pasaportes)
     *  - "YYYYMMDD"   (DG1 TD1 — DNIe)
     *  - "DD MM YYYY" (DG13 / DG11 español — con espacios)
     *  - "DD.MM.YYYY" (variante con puntos)
     */
    internal fun formatDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim()

        return when {
            // "DD MM YYYY" o "DD.MM.YYYY"
            s.contains(" ") || s.contains(".") -> {
                val parts = s.split(Regex("[\\s.]+"))
                if (parts.size != 3) return null
                buildDate(parts[0], parts[1].toIntOrNull() ?: return null, parts[2])
            }
            // "YYMMDD"
            s.length == 6 -> {
                val yy    = s.substring(0, 2).toIntOrNull() ?: return null
                val year  = if (yy > 30) "19$yy" else "20$yy"
                val month = s.substring(2, 4).toIntOrNull() ?: return null
                val day   = s.substring(4, 6)
                buildDate(day, month, year)
            }
            // "YYYYMMDD"
            s.length == 8 -> {
                val year  = s.substring(0, 4)
                val month = s.substring(4, 6).toIntOrNull() ?: return null
                val day   = s.substring(6, 8)
                buildDate(day, month, year)
            }
            else -> null
        }
    }

    private fun buildDate(day: String, month: Int, year: String): String? {
        val m = monthName(month) ?: return null
        return "$day de $m de $year"
    }

    private fun monthName(m: Int) = when (m) {
        1  -> "enero";    2  -> "febrero";  3  -> "marzo"
        4  -> "abril";    5  -> "mayo";     6  -> "junio"
        7  -> "julio";    8  -> "agosto";   9  -> "septiembre"
        10 -> "octubre"; 11 -> "noviembre"; 12 -> "diciembre"
        else -> null
    }

    /** Ejecuta el bloque y devuelve null si lanza cualquier excepción. */
    private fun <T> safe(block: () -> T): T? = runCatching(block).getOrNull()
}
