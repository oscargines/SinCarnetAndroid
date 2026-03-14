package com.oscar.sincarnet

import org.junit.Assert.assertEquals
import org.junit.Test

class SpecialCaseDecisionTest {

    @Test
    fun `psychophysical loss resolves with orange border and expected message`() {
        val decision = resolveSpecialCaseDecision(SpecialCaseType.PSYCHOPHYSICAL_LOSS)

        assertEquals(R.string.special_case_psychophysical_loss_message, decision.messageRes)
        assertEquals(true, decision.hasOrangeBorder)
    }

    @Test
    fun `missing requirements resolves with orange border and expected message`() {
        val decision = resolveSpecialCaseDecision(SpecialCaseType.MISSING_REQUIREMENTS)

        assertEquals(R.string.special_case_missing_requirements_message, decision.messageRes)
        assertEquals(true, decision.hasOrangeBorder)
    }

    @Test
    fun `no selection resolves to neutral state`() {
        val decision = resolveSpecialCaseDecision(null)

        assertEquals(null, decision.messageRes)
        assertEquals(false, decision.hasOrangeBorder)
    }
}

