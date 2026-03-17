package com.oscar.sincarnet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothPrinterPolicyTest {

    @Test
    fun allows_rw420_variants() {
        assertTrue(isAllowedZebraPrinterModel("RW420"))
        assertTrue(isAllowedZebraPrinterModel("RW-420"))
        assertTrue(isAllowedZebraPrinterModel("RW 420"))
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
        assertFalse(isAllowedZebraPrinterModel("RW520"))
        assertFalse(isAllowedZebraPrinterModel(""))
        assertFalse(isAllowedZebraPrinterModel(null))
    }
}

