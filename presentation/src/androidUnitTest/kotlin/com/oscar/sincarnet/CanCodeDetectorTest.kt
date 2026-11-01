package com.oscar.sincarnet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanCodeDetectorTest {

    @Test
    fun detectsIsolatedSixDigitCode() {
        val lines = listOf(line("CAN 123456", left = 10, top = 10, right = 110, bottom = 30))

        assertEquals("123456", CanCodeDetector.detect(lines, imageWidth = 1000, imageHeight = 1000))
    }

    @Test
    fun rejectsDniNumberWithEightDigitsAndLetter() {
        val lines = listOf(line("12345678Z", left = 10, top = 10, right = 110, bottom = 30))

        assertNull(CanCodeDetector.detect(lines, imageWidth = 1000, imageHeight = 1000))
    }

    @Test
    fun rejectsDateWithEightDigits() {
        val lines = listOf(line("01/01/1990", left = 10, top = 10, right = 110, bottom = 30))

        assertNull(CanCodeDetector.detect(lines, imageWidth = 1000, imageHeight = 1000))
    }

    @Test
    fun rejectsSupportNumberAttachedToLetters() {
        val lines = listOf(line("ABC123456", left = 10, top = 10, right = 110, bottom = 30))

        assertNull(CanCodeDetector.detect(lines, imageWidth = 1000, imageHeight = 1000))
    }

    @Test
    fun prefersCandidateInBottomRightCorner() {
        val lines = listOf(
            line("111111", left = 10, top = 10, right = 110, bottom = 30),
            line("222222", left = 700, top = 700, right = 900, bottom = 750)
        )

        assertEquals(
            "222222",
            CanCodeDetector.detect(lines, imageWidth = 1000, imageHeight = 1000)
        )
    }

    @Test
    fun prefersCandidateInsideGuideArea() {
        val guide = CanGuideArea(0.2f, 0.2f, 0.8f, 0.8f)
        val lines = listOf(
            line("111111", left = 750, top = 900, right = 950, bottom = 980),
            line("222222", left = 400, top = 400, right = 500, bottom = 450)
        )

        assertEquals(
            "222222",
            CanCodeDetector.detect(
                lines = lines,
                imageWidth = 1000,
                imageHeight = 1000,
                guideArea = guide
            )
        )
    }

    @Test
    fun normalizesCommonOcrConfusions() {
        val lines = listOf(line("1O2S3G", left = 10, top = 10, right = 110, bottom = 30))

        assertEquals("102536", CanCodeDetector.detect(lines, imageWidth = 1000, imageHeight = 1000))
    }

    @Test
    fun rejectsLetterOnlyTokenWithFewDigits() {
        val lines = listOf(line("BOLLOS", left = 10, top = 10, right = 110, bottom = 30))

        assertNull(CanCodeDetector.detect(lines, imageWidth = 1000, imageHeight = 1000))
    }

    @Test
    fun returnsNullForInvalidImageSize() {
        val lines = listOf(line("123456", left = 10, top = 10, right = 110, bottom = 30))

        assertNull(CanCodeDetector.detect(lines, imageWidth = 0, imageHeight = 1000))
    }

    private fun line(
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) = CanTextLine(text, left, top, right, bottom)
}
