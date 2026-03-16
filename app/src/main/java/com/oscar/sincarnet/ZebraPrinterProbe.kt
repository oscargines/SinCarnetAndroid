package com.oscar.sincarnet

import android.os.Looper
import android.util.Log
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.SGD

private const val ZEBRA_PROBE_TAG = "ZebraProbe"
private const val PRODUCT_NAME_SGD_KEY = "device.product_name"

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

