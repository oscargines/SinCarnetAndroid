package com.oscar.detectornfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.File

/**
 * Activity principal de escaneo NFC.
 *
 * Flujo:
 *  1. Se recibe el CAN desde la Activity anterior (6 dígitos del DNIe/TIE).
 *  2. Al detectar un tag NFC en foreground dispatch, se lee el documento en un hilo.
 *  3. Los datos crudos se parsean con NfcDataParser y se envían a ResultActivity como JSON.
 *
 * Para documentos ICAO sin CAN visible (pasaportes), llamar a DniReader.readWithMrz(mrz).
 */
class NFCScanActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        private const val TAG = "NFCScanActivity"
        const val EXTRA_CAN = "CAN"
        const val EXTRA_JSON_PATH = "JSON_PATH"
    }

    private var can: String = ""
    private var nfcAdapter: NfcAdapter? = null
    private var isReading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_scan)

        can = intent.getStringExtra(EXTRA_CAN) ?: ""
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        Log.i(TAG, "onCreate() - canLength=${can.length}, canMasked=${maskSecret(can)}, nfcSupported=${nfcAdapter != null}, nfcEnabled=${nfcAdapter?.isEnabled == true}")

        if (nfcAdapter == null) {
            Log.e(TAG, "Dispositivo sin NFC; cerrando pantalla de escaneo")
            Toast.makeText(this, "Este dispositivo no tiene NFC", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        updateStatus("Acerca el documento al teléfono…")
    }

    override fun onResume() {
        super.onResume()
        // Reader Mode: en API 36 ya no existe FLAG_READER_ISO_DEP.
        // Para documentos de identidad NFC basta con escuchar NFC-A / NFC-B
        // y omitir la comprobación NDEF.
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK // No necesitamos NDEF
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 300)
        }
        Log.d(TAG, "onResume() - enableReaderMode flags=$flags, isReading=$isReading")
        nfcAdapter?.enableReaderMode(this, this, flags, options)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() - disableReaderMode")
        nfcAdapter?.disableReaderMode(this)
    }

    // ------------------------------------------------------------------ //
    //  NfcAdapter.ReaderCallback
    // ------------------------------------------------------------------ //

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) {
            Log.w(TAG, "onTagDiscovered() recibido con tag nulo")
            return
        }
        if (isReading) {
            Log.w(TAG, "Tag ignorado porque ya hay una lectura en curso")
            return
        }

        Log.i(
            TAG,
            "Tag descubierto - id=${formatUid(tag.id)}, techs=${tag.techList.joinToString()}, isReading=$isReading"
        )
        isReading = true
        updateStatus("Leyendo documento…")

        Thread {
            try {
                Log.d(TAG, "Iniciando hilo de lectura NFC")
                val reader = DniReader(tag)
                val rawData: RawNfcData

                // Intentamos primero con CAN (DNIe 3/4 y TIE).
                // Si el CAN está vacío, caemos al modo MRZ (no implementado en esta pantalla).
                rawData = if (can.isNotBlank()) {
                    Log.d(TAG, "Leyendo documento con CAN enmascarado=${maskSecret(can)}")
                    reader.readDniSync(can)
                } else {
                    Log.w(TAG, "CAN vacío: no se puede leer sin código de acceso")
                    RawNfcData(
                        uid = null,
                        can = null,
                        dataGroups = emptyMap(),
                        sod = null,
                        sessionStatus = NfcSessionStatus.FAILED,
                        sessionError = "CAN vacio. No se puede iniciar la lectura NFC."
                    )
                }

                if (rawData.sessionStatus == NfcSessionStatus.FAILED) {
                    val failureMessage = rawData.sessionError
                        ?: "No se pudo completar la lectura NFC. Intentalo de nuevo."
                    Log.w(TAG, "Lectura fallida - uid=${rawData.uid ?: "<sin uid>"}, error=$failureMessage")
                    runOnUiThread {
                        updateStatus(failureMessage)
                        Toast.makeText(this, failureMessage, Toast.LENGTH_LONG).show()
                        isReading = false
                    }
                    return@Thread
                }

                if (rawData.uid == null) {
                    Log.w(TAG, "La lectura NFC no devolvio UID; se informa error al usuario")
                    runOnUiThread {
                        updateStatus("Error al conectar con el chip. Intentalo de nuevo.")
                        isReading = false
                    }
                    return@Thread
                }

                val availableDgs = rawData.dataGroups.filterValues { it != null }.keys.sorted()
                Log.i(TAG, "Lectura completada - uid=${rawData.uid}, dgCount=${availableDgs.size}, dgs=$availableDgs, sod=${rawData.sod != null}")

                if (rawData.sessionStatus == NfcSessionStatus.PARTIAL) {
                    Log.w(TAG, "Lectura parcial: ${rawData.sessionError ?: "sin detalle"}")
                }

                val parser = NfcDataParser()
                val dniData = parser.parseRawData(rawData)
                Log.i(
                    TAG,
                    "Parse completado - nombre=${dniData.nombre != null}, apellidos=${dniData.apellidos != null}, numeroDocumento=${dniData.numeroDocumento != null}, error=${dniData.error ?: "<ninguno>"}"
                )

                if (dniData.documentType == DocumentType.GERMAN_EID.name) {
                    runOnUiThread {
                        updateStatus("Documento aleman eID detectado: soporte parcial con flujo ICAO")
                        Toast.makeText(
                            this,
                            "Alemania eID detectado. Este documento usa un estandar especial y puede requerir integracion dedicada.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                val gson = Gson()
                val json = gson.toJson(
                    mapOf(
                        "raw" to mapOf(
                            "uid" to rawData.uid,
                            "can" to rawData.can,
                            "dgMap" to rawData.dataGroups.mapValues { (_, v) -> v },
                            "dgAnalysis" to rawData.dgAnalysis
                        ),
                        "dni" to dniData
                    )
                )
                Log.d(TAG, "JSON generado para resultados - length=${json.length}")

                // Evitamos TransactionTooLargeException pasando ruta de fichero en vez de un JSON gigante por Intent.
                val jsonFile = File(cacheDir, "scan_result_${System.currentTimeMillis()}.json")
                jsonFile.writeText(json, Charsets.UTF_8)
                Log.d(TAG, "JSON persistido en cache - path=${jsonFile.absolutePath}, bytes=${jsonFile.length()}")

                runOnUiThread {
                    Log.i(TAG, "Abriendo ResultActivity")
                    val intent = Intent(this, ResultActivity::class.java)
                    intent.putExtra(EXTRA_JSON_PATH, jsonFile.absolutePath)
                    startActivity(intent)
                    finish()
                }

            } catch (e: Throwable) {
                Log.e(TAG, "Error en lectura NFC: ${e.message}", e)
                runOnUiThread {
                    val userMessage = when (e) {
                        is NoClassDefFoundError, is LinkageError ->
                            "Falta una dependencia necesaria de la librería NFC. Revisa la instalación de la app."
                        is java.io.IOException ->
                            "Se perdio la conexion NFC durante la lectura. Manten el documento inmovil y reintenta."
                        else -> "Error inesperado: ${e.message}. Inténtalo de nuevo."
                    }
                    updateStatus(userMessage)
                    Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show()
                    isReading = false
                }
            }
        }.start()
    }

    // ------------------------------------------------------------------ //
    //  UI callbacks
    // ------------------------------------------------------------------ //

    /** Botón "Cancelar" del topbar — referenciado por android:onClick en el layout. */
    @Suppress("UNUSED_PARAMETER")
    fun onCancelClick(v: View) {
        Log.d(TAG, "Escaneo cancelado por el usuario desde el topbar")
        finish()
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private fun updateStatus(msg: String) {
        Log.d(TAG, "updateStatus() - $msg")
        runOnUiThread {
            try {
                findViewById<TextView>(R.id.tv_status)?.text = msg
            } catch (_: Exception) {}
        }
    }

    private fun maskSecret(value: String): String {
        if (value.isBlank()) return "<vacío>"
        if (value.length <= 2) return "*".repeat(value.length)
        return "${value.take(1)}${"*".repeat(value.length - 2)}${value.takeLast(1)}"
    }

    private fun formatUid(id: ByteArray?): String {
        if (id == null || id.isEmpty()) return "<sin uid>"
        return id.joinToString(":") { "%02X".format(it) }
    }
}
