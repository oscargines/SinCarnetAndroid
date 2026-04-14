package com.oscar.sincarnet

import android.os.Looper
import android.util.Log
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.SGD

private const val ZEBRA_PROBE_TAG = "ZebraProbe"
private const val PRODUCT_NAME_SGD_KEY = "device.product_name"

/**
 * Detecta el modelo de una impresora Zebra mediante conexión Bluetooth usando el SDK de Zebra.
 *
 * Utiliza el protocolo SGD (Set-Get-Do) para consultar la propiedad `device.product_name`
 * de la impresora conectada. Maneja automáticamente la preparación del [Looper] si es necesario.
 *
 * El proceso:
 * 1. Crea una conexión Bluetooth a la MAC especificada (timeout: 2.5s, max retries: 500ms)
 * 2. Ejecuta comando SGD.GET para obtener el nombre del producto
 * 3. Limpia caracteres especiales (comillas, espacios)
 * 4. Cierra la conexión
 * 5. Limpia el Looper si fue creado
 *
 * @param mac Dirección MAC de la impresora Zebra (p. ej. "AA:BB:CC:DD:EE:FF").
 * @return Nombre del modelo si la consulta tuvo éxito (p. ej. "RW420", "ZQ521"), o null en caso de error.
 *
 * @throws Exception Si ocurre un error inesperado que no sea de conexión o seguridad.
 *
 * @see normalizeZebraModel
 * @see isAllowedZebraPrinterModel
 */
internal fun probeZebraPrinterModelByMacSdk(mac: String): String? {
    if (mac.isBlank()) return null

    var preparedLooper = false
    if (Looper.myLooper() == null) {
        Looper.prepare()
        preparedLooper = true
    }

    val connection = BluetoothConnection(mac, 2_500, 500)
    return runCatching {
        connection.open()
        val value = SGD.GET(PRODUCT_NAME_SGD_KEY, connection)
        value
            ?.trim()
            ?.trim('"')
            ?.takeIf { it.isNotBlank() }
    }.onFailure {
        when (it) {
            is ConnectionException,
            is SecurityException,
            is IllegalArgumentException -> Log.w(ZEBRA_PROBE_TAG, "probe failed for MAC=$mac", it)
            else -> throw it
        }
    }.getOrNull().also {
        runCatching { connection.close() }
        if (preparedLooper) {
            Looper.myLooper()?.quitSafely()
        }
    }
}

