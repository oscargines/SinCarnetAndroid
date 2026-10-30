package com.oscar.sincarnet

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizeTipInputTest {

    @Test
    fun keepsAlreadyValidTip() {
        assertEquals("A12345B", sanitizeTipInput("A12345B"))
    }

    @Test
    fun uppercasesInput() {
        assertEquals("A12345B", sanitizeTipInput("a12345b"))
    }

    @Test
    fun removesInvalidCharacters() {
        assertEquals("A12345B", sanitizeTipInput(" a-12345b "))
    }

    @Test
    fun allowsDeletingFirstCharacter() {
        assertEquals("12345B", sanitizeTipInput("12345B"))
    }

    @Test
    fun allowsDeletingMiddleCharacter() {
        assertEquals("A1345B", sanitizeTipInput("A1345B"))
    }

    @Test
    fun truncatesToMaxLength() {
        assertEquals("A123456", sanitizeTipInput("A123456789"))
    }

    @Test
    fun clearsToEmptyString() {
        assertEquals("", sanitizeTipInput(""))
    }
}
