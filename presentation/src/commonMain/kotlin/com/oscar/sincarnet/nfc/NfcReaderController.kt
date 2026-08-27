package com.oscar.sincarnet.nfc

/**
 * Interfaz para controlar el modo lector NFC desde la capa de presentación.
 * Implementada por [MainActivity] y proporcionada via CompositionLocal.
 */
interface NfcReaderController {
    fun enableNfcReaderModeForDniRead()
    fun disableNfcReaderModeForDniRead()
}