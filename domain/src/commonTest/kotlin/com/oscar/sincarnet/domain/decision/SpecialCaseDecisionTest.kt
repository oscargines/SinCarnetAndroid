package com.oscar.sincarnet.domain.decision

import kotlin.test.Test
import kotlin.test.assertEquals

class SpecialCaseDecisionTest {

    @Test
    fun `psychophysical loss resolves with orange border and expected message`() {
        val decision = resolveSpecialCaseDecision(SpecialCaseType.PSYCHOPHYSICAL_LOSS)

        assertEquals("special_case_psychophysical_loss_message", decision.messageKey)
        assertEquals(true, decision.hasOrangeBorder)
    }

    @Test
    fun `missing requirements resolves with orange border and expected message`() {
        val decision = resolveSpecialCaseDecision(SpecialCaseType.MISSING_REQUIREMENTS)

        assertEquals("special_case_missing_requirements_message", decision.messageKey)
        assertEquals(true, decision.hasOrangeBorder)
    }

    @Test
    fun `no selection resolves to neutral state`() {
        val decision = resolveSpecialCaseDecision(null)

        assertEquals(null, decision.messageKey)
        assertEquals(false, decision.hasOrangeBorder)
    }
}
