package com.oscar.sincarnet

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias para [resolveExpiredValidityDecision].
 *
 * Verifica los escenarios penales, administrativos y neutros del flujo
 * de pérdida de vigencia.
 */
class ExpiredValidityDecisionTest {

    @Test
    fun `option 1 always resolves as crime with red blinking border`() {
        val decision = resolveExpiredValidityDecision(
            selectedOption = 0,
            hasAnyNegativeAnswer = false,
            hasAllPositiveAnswers = false,
            hasKnowledge = null,
            isInAppealPeriod = null
        )

        assertEquals(R.string.expired_validity_crime_message, decision.messageRes)
        assertEquals(BorderBehavior.RED_BLINK, decision.borderBehavior)
    }

    @Test
    fun `option 2 with negative answer resolves as crime with red blinking border`() {
        val decision = resolveExpiredValidityDecision(
            selectedOption = 1,
            hasAnyNegativeAnswer = true,
            hasAllPositiveAnswers = false,
            hasKnowledge = null,
            isInAppealPeriod = null
        )

        assertEquals(R.string.expired_validity_crime_message, decision.messageRes)
        assertEquals(BorderBehavior.RED_BLINK, decision.borderBehavior)
    }

    @Test
    fun `option 2 with all positive answers allows continuing trip`() {
        val decision = resolveExpiredValidityDecision(
            selectedOption = 1,
            hasAnyNegativeAnswer = false,
            hasAllPositiveAnswers = true,
            hasKnowledge = null,
            isInAppealPeriod = null
        )

        assertEquals(R.string.continue_trip_message, decision.messageRes)
        assertEquals(BorderBehavior.GREEN_SOLID, decision.borderBehavior)
    }

    @Test
    fun `edictal without knowledge resolves as infringement with yellow blinking border`() {
        val decision = resolveExpiredValidityDecision(
            selectedOption = 2,
            hasAnyNegativeAnswer = false,
            hasAllPositiveAnswers = false,
            hasKnowledge = false,
            isInAppealPeriod = null
        )

        assertEquals(R.string.expired_validity_infringement_message, decision.messageRes)
        assertEquals(BorderBehavior.YELLOW_BLINK, decision.borderBehavior)
    }

    @Test
    fun `edictal with knowledge in appeal period resolves as infringement`() {
        val decision = resolveExpiredValidityDecision(
            selectedOption = 2,
            hasAnyNegativeAnswer = false,
            hasAllPositiveAnswers = false,
            hasKnowledge = true,
            isInAppealPeriod = true
        )

        assertEquals(R.string.expired_validity_infringement_message, decision.messageRes)
        assertEquals(BorderBehavior.YELLOW_BLINK, decision.borderBehavior)
    }

    @Test
    fun `edictal with knowledge out of appeal period resolves as crime`() {
        val decision = resolveExpiredValidityDecision(
            selectedOption = 2,
            hasAnyNegativeAnswer = false,
            hasAllPositiveAnswers = false,
            hasKnowledge = true,
            isInAppealPeriod = false
        )

        assertEquals(R.string.expired_validity_crime_message, decision.messageRes)
        assertEquals(BorderBehavior.RED_BLINK, decision.borderBehavior)
    }

    @Test
    fun `option 2 pending answers resolves to neutral state`() {
        val decision = resolveExpiredValidityDecision(
            selectedOption = 1,
            hasAnyNegativeAnswer = false,
            hasAllPositiveAnswers = false,
            hasKnowledge = null,
            isInAppealPeriod = null
        )

        assertEquals(null, decision.messageRes)
        assertEquals(BorderBehavior.NONE, decision.borderBehavior)
    }
}

