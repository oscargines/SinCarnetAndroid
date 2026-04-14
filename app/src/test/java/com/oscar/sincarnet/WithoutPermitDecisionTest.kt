package com.oscar.sincarnet

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias para [resolveWithoutPermitDecision].
 *
 * Verifica clasificación en delito, vía administrativa y estado neutro.
 */
class WithoutPermitDecisionTest {

    @Test
    fun `never obtained permit resolves as crime`() {
        val decision = resolveWithoutPermitDecision(
            hasEverObtainedPermit = false,
            isValidForDrivingInSpain = null
        )

        assertEquals(R.string.expired_validity_crime_message, decision.messageRes)
        assertEquals(WithoutPermitBorderBehavior.RED_BLINK, decision.borderBehavior)
    }

    @Test
    fun `obtained permit and valid in Spain allows continuing trip`() {
        val decision = resolveWithoutPermitDecision(
            hasEverObtainedPermit = true,
            isValidForDrivingInSpain = true
        )

        assertEquals(R.string.continue_trip_message, decision.messageRes)
        assertEquals(WithoutPermitBorderBehavior.GREEN_SOLID, decision.borderBehavior)
    }

    @Test
    fun `obtained permit but not valid in Spain resolves as administrative report`() {
        val decision = resolveWithoutPermitDecision(
            hasEverObtainedPermit = true,
            isValidForDrivingInSpain = false
        )

        assertEquals(R.string.without_permit_admin_report_message, decision.messageRes)
        assertEquals(WithoutPermitBorderBehavior.ORANGE_SOLID, decision.borderBehavior)
    }

    @Test
    fun `pending answer resolves to neutral state`() {
        val decision = resolveWithoutPermitDecision(
            hasEverObtainedPermit = true,
            isValidForDrivingInSpain = null
        )

        assertEquals(null, decision.messageRes)
        assertEquals(WithoutPermitBorderBehavior.NONE, decision.borderBehavior)
    }
}

