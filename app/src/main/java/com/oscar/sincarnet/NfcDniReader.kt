package com.oscar.sincarnet

import android.nfc.Tag
import android.nfc.TagLostException
import android.util.Log
import es.gob.jmulticard.card.CryptoCardException
import es.gob.jmulticard.jse.provider.DnieProvider
import es.gob.jmulticard.jse.provider.MrtdKeyStoreImpl
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.Security
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal data class NfcDniPersonData(
    val firstName: String,
    val lastName1: String,
    val lastName2: String,
    val documentNumber: String,
    val fatherName: String,
    val motherName: String,
    val birthDateAammdd: String,
    val birthPlace: String,
    val birthProvince: String,
    val residenceAddress: String,
    val residencePopulation: String,
    val residenceProvince: String,
    val nationality: String,
    val sex: String
)

internal object NfcDniReader {
    private const val TAG = "NfcDniReader"

    fun read(can: String, tag: Tag): Result<NfcDniPersonData> = runCatching {
        val uid = tag.id?.joinToString(":") { "%02X".format(it) }.orEmpty()
        Log.i(TAG, "Inicio lectura NFC uid=$uid canLength=${can.length} techs=${tag.techList.joinToString()}")

        ensureRequiredRuntimeDependencies()
        registerDnieProviderIfAvailable()

        Log.d(TAG, "Clase MrtdKeyStoreImpl cargada")
        val ks = MrtdKeyStoreImpl(can, tag)
        Log.d(TAG, "Instancia MrtdKeyStoreImpl creada")

        try {
            ks.engineLoad(null as InputStream?, null)
            Log.d(TAG, "engineLoad ejecutado correctamente")
        }
        catch (e: Exception) {
            val cause = e.cause ?: e
            Log.e(TAG, "engineLoad fallido: ${cause.javaClass.name}: ${cause.message}", cause)
            throw cause
        }

        val dg1 = ks.getDataGroup1()
        val dg13 = ks.getDataGroup13()
        Log.d(TAG, "DG1 y DG13 obtenidos (objetos)")

        if (dg1 == null) {
            throw IllegalStateException("No se pudo obtener DG1 tras engineLoad")
        }
        if (dg13 == null) {
            Log.w(TAG, "DG13 no disponible; se continuará con campos opcionales vacíos")
        }

        // Llamadas directas a la API (sin reflexión)
        val firstName = dg1.name?.trim().orEmpty()
        val documentNumber = dg1.docNumber?.trim().orEmpty()
        val birthDateAammdd = dg1.dateOfBirth?.trim().orEmpty()
        val nationality = dg1.nationality?.trim().orEmpty()
        val sex = dg1.sex?.trim().orEmpty()

        // Apellidos: DG13 los provee ya separados (getSurName1/getSurName2);
        // si DG13 no está disponible se hace fallback al campo combinado de DG1.
        val lastName1: String
        val lastName2: String
        if (dg13 != null) {
            lastName1 = dg13.surName1?.trim().orEmpty()
            lastName2 = dg13.surName2?.trim().orEmpty()
        } else {
            val surnameRaw = dg1.surname?.trim().orEmpty()
            val split = splitSurnames(surnameRaw)
            lastName1 = split.first
            lastName2 = split.second
        }

        val fatherName      = dg13?.fatherName?.trim().orEmpty()
        val motherName      = dg13?.motherName?.trim().orEmpty()
        val birthPlace      = dg13?.birthPopulation?.trim().orEmpty()
        val birthProvince   = dg13?.birthProvince?.trim().orEmpty()
        val residenceAddress    = dg13?.actualAddress?.trim().orEmpty()
        val residencePopulation = dg13?.actualPopulation?.trim().orEmpty()
        val residenceProvince   = dg13?.actualProvince?.trim().orEmpty()

        Log.i(
            TAG,
            "Lectura OK uid=$uid nombre='${firstName.take(24)}' apellidos='${(lastName1 + " " + lastName2).trim().take(30)}' doc='${documentNumber.take(12)}'"
        )

        NfcDniPersonData(
            firstName = firstName,
            lastName1 = lastName1,
            lastName2 = lastName2,
            documentNumber = documentNumber,
            fatherName = fatherName,
            motherName = motherName,
            birthDateAammdd = birthDateAammdd,
            birthPlace = birthPlace,
            birthProvince = birthProvince,
            residenceAddress = residenceAddress,
            residencePopulation = residencePopulation,
            residenceProvince = residenceProvince,
            nationality = nationality,
            sex = sex
        )
    }.recoverCatching { error ->
        when (error) {
            is CryptoCardException -> {
                Log.e(TAG, "Error de tarjeta (CAN incorrecto o tarjeta bloqueada): ${error.message}", error)
                throw IllegalStateException("Error de tarjeta. Verifica CAN o estado del documento.", error)
            }
            is GeneralSecurityException -> {
                Log.e(TAG, "Error de seguridad durante PACE: ${error.message}", error)
                throw IllegalStateException("Fallo de seguridad durante PACE.", error)
            }
            is TagLostException, is IOException -> {
                Log.e(TAG, "Se ha perdido la conexión con el DNIe: ${error.message}", error)
                throw IllegalStateException("Se ha perdido la conexión con el DNIe. Mantén el documento inmóvil y reintenta.", error)
            }
            is LinkageError -> {
                Log.e(TAG, "Error de dependencias/librería durante la lectura NFC: ${error.message}", error)
                throw IllegalStateException("Error de dependencias de la librería NFC.", error)
            }
            else -> throw error
        }
    }

    private fun registerDnieProviderIfAvailable() {
        val providerName = "DNIeJCAProvider"
        if (Security.getProvider(providerName) != null) {
            Log.d(TAG, "Proveedor $providerName ya registrado")
            return
        }

        Security.addProvider(DnieProvider())
        Log.d(TAG, "Proveedor DNIe registrado dinámicamente")
    }

    private fun ensureRequiredRuntimeDependencies() {
        val missingClasses = missingRequiredRuntimeClasses()
        if (missingClasses.isNotEmpty()) {
            val msg = "Faltan clases NFC runtime: ${missingClasses.joinToString()}"
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
    }

    private fun missingRequiredRuntimeClasses(): List<String> {
        val required = listOf(
            "es.gob.jmulticard.jse.provider.MrtdKeyStoreImpl",
            "es.gob.jmulticard.jse.provider.DnieProvider",
            "de.tsenger.androsmex.mrtd.DG1_Dnie",
            "de.tsenger.androsmex.mrtd.DG13",
            // Solo clases raíz del proveedor BC; las internas (DERObjectIdentifier, etc.)
            // se verifican implícitamente al ejecutar engineLoad().
            "org.bouncycastle.jce.provider.BouncyCastleProvider"
        )
        val missing = mutableListOf<String>()
        for (className in required) {
            runCatching { Class.forName(className) }.onFailure { missing += className }
        }
        if (missing.isEmpty()) {
            Log.d(TAG, "Dependencias NFC runtime OK")
        }
        return missing
    }

}

internal fun String.toDisplayBirthDateFromAammddOrEmpty(): String {
    val value = trim()
    if (!Regex("^\\d{6}$").matches(value)) return ""

    val yy = value.substring(0, 2).toIntOrNull() ?: return ""
    val mm = value.substring(2, 4).toIntOrNull() ?: return ""
    val dd = value.substring(4, 6).toIntOrNull() ?: return ""

    val currentTwoDigits = LocalDate.now().year % 100
    val year = if (yy > currentTwoDigits) 1900 + yy else 2000 + yy

    return runCatching {
        LocalDate.of(year, mm, dd).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
    }.onFailure {
        Log.w("NfcDniReader", "Fecha DG1 inválida '$value': ${it.message}")
    }.getOrDefault("")
}

internal fun normalizeSexFromDg1(raw: String, maleLabel: String, femaleLabel: String, unknownLabel: String): String {
    return when (raw.trim().uppercase()) {
        "M" -> maleLabel
        "F" -> femaleLabel
        else -> unknownLabel
    }
}

private fun splitSurnames(raw: String): Pair<String, String> {
    val clean = raw.replace('<', ' ').replace("  ", " ").trim()
    if (clean.isBlank()) return "" to ""

    val parts = clean.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> parts.first() to parts.drop(1).joinToString(" ")
        else -> clean to ""
    }
}

