package com.oscar.sincarnet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothPrinterPolicyTest {

    @Test
    fun allows_rw520_variants() {
        assertTrue(isAllowedZebraPrinterModel("RW520"))
        assertTrue(isAllowedZebraPrinterModel("RW-520"))
        assertTrue(isAllowedZebraPrinterModel("RW 520"))
    }

    @Test
    fun allows_zq521_variants() {
        assertTrue(isAllowedZebraPrinterModel("ZQ521"))
        assertTrue(isAllowedZebraPrinterModel("ZQ-521"))
        assertTrue(isAllowedZebraPrinterModel("zq 521"))
    }

    @Test
    fun rejects_other_or_empty_models() {
        assertFalse(isAllowedZebraPrinterModel("ZQ520"))
        assertFalse(isAllowedZebraPrinterModel("RW420"))
        assertFalse(isAllowedZebraPrinterModel(""))
        assertFalse(isAllowedZebraPrinterModel(null))
    }
}

