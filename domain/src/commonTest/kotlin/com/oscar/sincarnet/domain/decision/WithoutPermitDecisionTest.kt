package com.oscar.sincarnet.domain.decision

import kotlin.test.Test
import kotlin.test.assertEquals

class WithoutPermitDecisionTest {

    @Test
    fun `never obtained permit resolves as crime`() {
        val decision = resolveWithoutPermitDecision(
            hasEverObtainedPermit = false,
            isValidForDrivingInSpain = null
        )

        assertEquals("expired_validity_crime_message", decision.messageKey)
        assertEquals(WithoutPermitBorderBehavior.RED_BLINK, decision.borderBehavior)
    }

    @Test
    fun `obtained permit and valid in Spain allows continuing trip`() {
        val decision = resolveWithoutPermitDecision(
            hasEverObtainedPermit = true,
            isValidForDrivingInSpain = true
        )

        assertEquals("continue_trip_message", decision.messageKey)
        assertEquals(WithoutPermitBorderBehavior.GREEN_SOLID, decision.borderBehavior)
    }

    @Test
    fun `obtained permit but not valid in Spain resolves as administrative report`() {
        val decision = resolveWithoutPermitDecision(
            hasEverObtainedPermit = true,
            isValidForDrivingInSpain = false
        )

        assertEquals("without_permit_admin_report_message", decision.messageKey)
        assertEquals(WithoutPermitBorderBehavior.ORANGE_SOLID, decision.borderBehavior)
    }

    @Test
    fun `pending answer resolves to neutral state`() {
        val decision = resolveWithoutPermitDecision(
            hasEverObtainedPermit = true,
            isValidForDrivingInSpain = null
        )

        assertEquals(null, decision.messageKey)
        assertEquals(WithoutPermitBorderBehavior.NONE, decision.borderBehavior)
    }
}
