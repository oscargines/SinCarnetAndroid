package com.oscar.sincarnet

import android.nfc.Tag
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

/**
 * Información de depuración sobre la etiqueta NFC detectada más recientemente.
 *
 * @property hasTag Indica si hay una etiqueta NFC válida almacenada.
 * @property uid Identificador único de la etiqueta en formato hexadecimal (p. ej. "04:A1:B2:C3").
 * @property techList Tecnologías NFC soportadas por la etiqueta (p. ej. "android.nfc.tech.IsoDep, ...").
 * @property ageMs Edad de la etiqueta en milisegundos desde su captura (-1 si no hay etiqueta).
 * @property capturedAtMillis Timestamp de System.currentTimeMillis() cuando se capturó la etiqueta.
 */
internal data class NfcTagDebugInfo(
    val hasTag: Boolean,
    val uid: String,
    val techList: String,
    val ageMs: Long,
    val capturedAtMillis: Long
)

/**
 * Repositorio singleton que almacena la etiqueta NFC detectada más recientemente.
 *
 * Utiliza [AtomicReference] para thread-safety en operaciones de lectura/escritura.
 * Es principalmente utilizado por [MainActivity] para capturar etiquetas en ReaderMode
 * y por [NfcDniReader] para acceder a la etiqueta actual cuando se solicita lectura de DNI.
 *
 * El flujo típico es:
 * 1. MainActivity configura ReaderMode y detecta etiqueta → [update]
 * 2. Usuario toca "Leer DNI" → [getLatest] + [NfcDniReader.read]
 * 3. Después de procesar → [clear] para limpiar la referencia
 */
internal object NfcTagRepository {
    private const val TAG = "NfcTagRepo"
    private val latestTagRef = AtomicReference<Tag?>(null)
    private val latestTagMillisRef = AtomicReference<Long>(0L)

    /**
     * Actualiza el repositorio con una nueva etiqueta NFC detectada.
     *
     * Registra en log el UID y las tecnologías de la etiqueta. Si se recibe null,
     * emite una advertencia pero no actualiza el repositorio.
     *
     * @param tag Etiqueta NFC detectada, o null (será ignorado con advertencia).
     */
    fun update(tag: Tag?) {
        if (tag != null) {
            val now = System.currentTimeMillis()
            latestTagRef.set(tag)
            latestTagMillisRef.set(now)
            val uid = tag.id?.joinToString(":") { "%02X".format(it) }.orEmpty()
            Log.i(TAG, "update() uid=$uid techs=${tag.techList.joinToString()} timestamp=$now")
        } else {
            Log.w(TAG, "update() recibido tag=null (ignorado)")
        }
    }

    /**
     * Obtiene la etiqueta NFC más recientemente detectada.
     *
     * Registra en log información de depuración sobre la etiqueta
     * (si existe, UID, tecnologías, antigüedad).
     *
     * @return Etiqueta NFC almacenada, o null si no hay ninguna.
     */
    fun getLatest(): Tag? {
        val tag = latestTagRef.get()
        val info = debugInfo()
        Log.d(TAG, "getLatest() hasTag=${info.hasTag} uid=${info.uid} ageMs=${info.ageMs} capturedAt=${info.capturedAtMillis} techs=${info.techList}")
        return tag
    }

    /**
     * Limpia la referencia a la etiqueta NFC actual.
     *
     * Se invoca típicamente después de completar una operación NFC
     * (lectura de DNI, lectura de datos, etc.).
     */
    fun clear() {
        latestTagRef.set(null)
        latestTagMillisRef.set(0L)
        Log.i(TAG, "clear() repositorio NFC reiniciado")
    }

    /**
     * Obtiene información de depuración sobre la etiqueta NFC actual.
     *
     * Incluye UID, tecnologías, antigüedad y timestamp de captura.
     * Útil para logging y diagnóstico de problemas NFC.
     *
     * @return Estructura con información de depuración.
     */
    fun debugInfo(): NfcTagDebugInfo {
        val tag = latestTagRef.get()
        val ts = latestTagMillisRef.get()
        val now = System.currentTimeMillis()
        val uid = tag?.id?.joinToString(":") { "%02X".format(it) }.orEmpty()
        val techs = tag?.techList?.joinToString().orEmpty()
        val age = if (ts > 0L) now - ts else -1L
        return NfcTagDebugInfo(
            hasTag = tag != null,
            uid = uid,
            techList = techs,
            ageMs = age,
            capturedAtMillis = ts
        )
    }
}

