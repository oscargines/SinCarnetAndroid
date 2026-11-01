package com.oscar.sincarnet

import com.oscar.sincarnet.domain.model.SavedBluetoothPrinter
import org.junit.Assert.assertEquals
import org.junit.Test

class BluetoothPrinterSelectionTest {

    private fun printers(vararg macs: String): List<SavedBluetoothPrinter> =
        macs.mapIndexed { index, mac -> SavedBluetoothPrinter(id = index + 1, nombre = "Impresora $index", mac = mac) }

    @Test
    fun prefersSelectedSavedMacWhenValid() {
        val saved = printers("AA:01", "AA:02")

        assertEquals("AA:02", resolveSelectedSavedMac(saved, preferredMac = "AA:02", currentMac = "AA:01", defaultMac = "AA:01"))
    }

    @Test
    fun ignoresPreferredMacNotInList() {
        val saved = printers("AA:01", "AA:02")

        assertEquals("AA:01", resolveSelectedSavedMac(saved, preferredMac = "ZZ:99", currentMac = "AA:01", defaultMac = "AA:02"))
    }

    @Test
    fun ignoresBlankPreferredMac() {
        val saved = printers("AA:01", "AA:02")

        assertEquals("AA:02", resolveSelectedSavedMac(saved, preferredMac = "", currentMac = "AA:02", defaultMac = "AA:01"))
    }

    @Test
    fun fallsBackToCurrentMacWhenPreferredInvalid() {
        val saved = printers("AA:01", "AA:02")

        assertEquals("AA:02", resolveSelectedSavedMac(saved, preferredMac = null, currentMac = "AA:02", defaultMac = "AA:01"))
    }

    @Test
    fun fallsBackToDefaultMacWhenNoCurrentSelection() {
        val saved = printers("AA:01", "AA:02")

        assertEquals("AA:02", resolveSelectedSavedMac(saved, preferredMac = null, currentMac = "", defaultMac = "AA:02"))
    }

    @Test
    fun fallsBackToFirstPrinterWhenNothingMatches() {
        val saved = printers("AA:01", "AA:02")

        assertEquals("AA:01", resolveSelectedSavedMac(saved, preferredMac = null, currentMac = "ZZ:99", defaultMac = "ZZ:98"))
    }

    @Test
    fun returnsEmptyWhenNoSavedPrinters() {
        assertEquals("", resolveSelectedSavedMac(emptyList(), preferredMac = "AA:01", currentMac = "AA:01", defaultMac = "AA:01"))
    }
}
