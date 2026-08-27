package com.oscar.sincarnet.nfc

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal para acceder al controlador NFC desde cualquier Composable.
 * Debe ser provisto por [MainActivity] en la raíz del árbol de composición.
 */
val LocalNfcReaderController = staticCompositionLocalOf<NfcReaderController> {
    object : NfcReaderController {
        override fun enableNfcReaderModeForDniRead() {
            // No-op por defecto; MainActivity debe proveer la implementación real
        }
        override fun disableNfcReaderModeForDniRead() {
            // No-op por defecto
        }
    }
}