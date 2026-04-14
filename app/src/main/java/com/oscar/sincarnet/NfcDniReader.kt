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

/**
 * Datos de persona extraídos del DNI electrónico vía NFC.
 *
 * Los datos se extraen de dos grupos de datos (DG) del MRZ (Machine Readable Zone):
 * - **DG1**: Información básica (nombre, apellidos, fecha nacimiento, nacionalidad, sexo)
 * - **DG13**: Información ampliada (padres, domicilio, provincia, lugar nacimiento)
 *
 * Si DG13 no está disponible, algunos campos se rellenan con espacios vacíos.
 *
 * @property firstName Nombre de la persona
 * @property lastName1 Primer apellido
 * @property lastName2 Segundo apellido
 * @property documentNumber Número de soporte del DNI (campo docNumber de DG1, p.ej. "AAA000000"). Solo para log/debug.
 * @property optionalData Dato opcional de DG1 → NIF/NIE del titular (p.ej. "12345678A"). Puede estar vacío.
 * @property fatherName Nombre del padre (desde DG13)
 * @property motherName Nombre de la madre (desde DG13)
 * @property birthDateAammdd Fecha de nacimiento en formato AAMMDD (p.ej. "850615")
 * @property birthPlace Población de nacimiento (desde DG13)
 * @property birthProvince Provincia de nacimiento (desde DG13)
 * @property residenceAddress Dirección de residencia actual (desde DG13)
 * @property residencePopulation Población de residencia (desde DG13)
 * @property residenceProvince Provincia de residencia (desde DG13)
 * @property nationality Nacionalidad (código de 3 caracteres, p.ej. "ESP")
 * @property sex Sexo ("M" = Masculino, "F" = Femenino)
 */
internal data class NfcDniPersonData(
    val firstName: String,
    val lastName1: String,
    val lastName2: String,
    /** Número de soporte (campo docNumber de DG1, p.ej. "AAA000000"). Solo para log/debug. */
    val documentNumber: String,
    /** Dato opcional de DG1 → NIF/NIE del titular (p.ej. "12345678A"). */
    val optionalData: String,
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

/**
 * Lector de DNI electrónico vía NFC.
 *
 * Encapsula la lectura de datos personales desde un documento de identidad español (DNI-e)
 * usando comunicación NFC. Utiliza la librería jMulticard (proveedor DNIe) que implementa
 * el protocolo PACE (Password Authenticated Connection Establishment) para autenticación segura.
 *
 * **Arquitectura**:
 * - Usa [MrtdKeyStoreImpl] para establecer la conexión PACE con el DNI
 * - Lee datos de grupos DG1 (básico) y DG13 (ampliado)
 * - Maneja fallbacks si DG13 no está disponible
 * - Traduce excepciones de bajo nivel a errores de usuario legibles
 *
 * **Flujo típico**:
 * 1. Usuario proporciona CAN (Card Access Number) de 6 dígitos
 * 2. Se acerca el DNI al lector NFC
 * 3. [read] autentica vía PACE y extrae DG1/DG13
 * 4. Retorna [NfcDniPersonData] o error
 *
 * **Errores manejados**:
 * - [CryptoCardException]: CAN incorrecto, tarjeta bloqueada
 * - [GeneralSecurityException]: Fallo en PACE
 * - [TagLostException]/[IOException]: Conexión perdida
 * - [LinkageError]: Dependencias NFC runtime faltantes
 *
 * @see NfcDniPersonData Estructura de datos retornada
 * @see registerDnieProviderIfAvailable Para setup de dependencias de seguridad
 */
internal object NfcDniReader {
    private const val TAG = "NfcDniReader"

    /**
     * Lee datos personales del DNI electrónico.
     *
     * Proceso completo:
     * 1. Valida que existan las dependencias de runtime requeridas
     * 2. Registra el proveedor DNIe (si no está registrado)
     * 3. Crea instancia [MrtdKeyStoreImpl] y autentifica vía PACE
     * 4. Carga DG1 y DG13 del documento
     * 5. Extrae campos de ambos grupos
     * 6. Maneja fallback de DG13 para algunos campos
     * 7. Retorna [NfcDniPersonData] con todos los datos
     *
     * **Errores**:
     * - [CryptoCardException]: Traduce a "Error de tarjeta. Verifica CAN o estado del documento."
     * - [GeneralSecurityException]: "Fallo de seguridad durante PACE."
     * - [TagLostException]/[IOException]: "Se ha perdido la conexión con el DNIe. Mantén el documento inmóvil y reintenta."
     * - [LinkageError]: "Error de dependencias de la librería NFC."
     *
     * @param can Card Access Number: 6 dígitos del DNI (necesarios para autenticación PACE)
     * @param tag Etiqueta NFC del dispositivo Android (obtenida del adaptador NFC)
     * @return [Result<NfcDniPersonData>] con los datos extraídos o excepción
     *
     * @throws IllegalStateException Si faltan dependencias, PACE falla, o DG1 no se puede obtener
     */
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
        // optData contiene el NIF/NIE en el DNIe español (dato opcional zona 1 del MRZ).
        // El MRZ rellena con '<'; se eliminan antes de devolver el valor.
        val optionalData = dg1.optData?.trim()?.trimEnd('<')?.trim().orEmpty()
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
            optionalData = optionalData,
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

    /**
     * Registra dinámicamente el proveedor DNIe en el contexto de seguridad de Java.
     *
     * El proveedor "DNIeJCAProvider" proporciona la implementación del protocolo PACE
     * y otras funcionalidades de criptografía requeridas para comunicarse con el DNI.
     *
     * Solo se registra si no está ya registrado (para evitar duplicados).
     *
     * @throws IllegalStateException Si el proveedor no se puede cargar
     */
     private fun registerDnieProviderIfAvailable() {
        val providerName = "DNIeJCAProvider"
        if (Security.getProvider(providerName) != null) {
            Log.d(TAG, "Proveedor $providerName ya registrado")
            return
        }

        Security.addProvider(DnieProvider())
        Log.d(TAG, "Proveedor DNIe registrado dinámicamente")
    }

     /**
      * Verifica que todas las dependencias de runtime requeridas estén disponibles.
      *
      * Necesario porque jMulticard requiere clases que solo están disponibles si:
      * 1. El AAR/JAR está incluido en el build
      * 2. Las transitividades están resueltas
      * 3. No hay conflictos de versiones
      *
      * Se llama antes de cualquier operación NFC.
      *
      * @throws IllegalStateException Si alguna clase requerida falta
      */
     private fun ensureRequiredRuntimeDependencies() {
         val missingClasses = missingRequiredRuntimeClasses()
         if (missingClasses.isNotEmpty()) {
             val msg = "Faltan clases NFC runtime: ${missingClasses.joinToString()}"
             Log.e(TAG, msg)
             throw IllegalStateException(msg)
         }
     }

     /**
      * Lista las clases de runtime que faltan.
      *
      * Valida la presencia de:
      * - `es.gob.jmulticard.jse.provider.MrtdKeyStoreImpl` (lectura del MRZ)
      * - `es.gob.jmulticard.jse.provider.DnieProvider` (proveedor de criptografía)
      * - `de.tsenger.androsmex.mrtd.DG1_Dnie` (extracción de datos básicos)
      * - `de.tsenger.androsmex.mrtd.DG13` (extracción de datos ampliados)
      * - `org.bouncycastle.jce.provider.BouncyCastleProvider` (soporte criptográfico)
      *
      * @return Lista vacía si todas están presentes, lista con nombres de clases faltantes si no
      */
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

/**
 * Convierte una fecha del MRZ en formato AAMMDD a una cadena legible "dd-MM-yyyy".
 *
 * Maneja el cambio de siglo (YY → AAAA):
 * - Si YY > año actual (últimos 2 dígitos), suma 1900
 * - Si YY ≤ año actual, suma 2000
 *
 * Ejemplo: "850615" → "15-06-1985"
 *
 * Retorna cadena vacía si:
 * - El formato no es "AAMMDD" (6 dígitos exactos)
 * - Los dígitos no se pueden parsear a Int
 * - La fecha es inválida (p.ej. 32 de enero)
 *
 * @receiver Cadena con fecha en formato AAMMDD del MRZ
 * @return Fecha formateada como "dd-MM-yyyy" o cadena vacía si es inválida
 */
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

/**
 * Normaliza el valor de sexo del DG1 a etiquetas localizadas.
 *
 * Convierte:
 * - "M" → [maleLabel]
 * - "F" → [femaleLabel]
 * - Cualquier otro valor → [unknownLabel]
 *
 * Se usa para mostrar el sexo en el idioma de la aplicación.
 *
 * @param raw Valor del campo de sexo desde DG1 (generalmente "M" o "F")
 * @param maleLabel Etiqueta a mostrar si es masculino (p.ej. "Hombre")
 * @param femaleLabel Etiqueta a mostrar si es femenino (p.ej. "Mujer")
 * @param unknownLabel Etiqueta a mostrar si es desconocido (p.ej. "Desconocido")
 * @return Etiqueta correspondiente localizada
 */
internal fun normalizeSexFromDg1(raw: String, maleLabel: String, femaleLabel: String, unknownLabel: String): String {
     return when (raw.trim().uppercase()) {
         "M" -> maleLabel
         "F" -> femaleLabel
         else -> unknownLabel
     }
 }

/**
 * Divide una cadena de apellidos del MRZ en primer y segundo apellido.
 *
 * El MRZ usa '<' como separador de campos. Esta función:
 * 1. Reemplaza '<' con espacios
 * 2. Limpia espacios múltiples
 * 3. Divide por espacios
 * 4. Si hay 2+ partes, primero es 1º apellido y resto es 2º
 * 5. Si hay 1 sola parte, todo va al 1º apellido
 * 6. Si está vacío, retorna ("", "")
 *
 * Ejemplos:
 * - "GARCIA<MARTINEZ" → ("GARCIA", "MARTINEZ")
 * - "GARCIA" → ("GARCIA", "")
 * - "GARCIA<RODRIGUEZ<LOPEZ" → ("GARCIA", "RODRIGUEZ LOPEZ")
 *
 * @param raw Cadena de apellidos del MRZ (puede contener '<' como separadores)
 * @return Par (1º apellido, 2º apellido)
 */
private fun splitSurnames(raw: String): Pair<String, String> {
    val clean = raw.replace('<', ' ').replace("  ", " ").trim()
    if (clean.isBlank()) return "" to ""

    val parts = clean.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> parts.first() to parts.drop(1).joinToString(" ")
        else -> clean to ""
    }
}
