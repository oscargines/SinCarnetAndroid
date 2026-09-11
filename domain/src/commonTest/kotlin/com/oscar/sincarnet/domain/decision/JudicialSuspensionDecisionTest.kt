package com.oscar.sincarnet.domain.decision

import kotlin.test.Test
import kotlin.test.assertEquals

class JudicialSuspensionDecisionTest {

    @Test
    fun `driving within sentence period resolves as crime`() {
        val decision = resolveJudicialSuspensionDecision(
            sentenceType = SentenceType.UP_TO_TWO_YEARS,
            drivingMoment = DrivingMoment.WITHIN_PERIOD,
            hasCompletedCourseForLowerSentence = null,
            hasCompletedCourseForHigherSentence = null,
            hasPassedExamsForHigherSentence = null
        )

        assertEquals("expired_validity_crime_message", decision.messageKey)
        assertEquals(JudicialBorderBehavior.RED_BLINK, decision.borderBehavior)
    }

    @Test
    fun `lower sentence after period with completed course allows continuing trip`() {
        val decision = resolveJudicialSuspensionDecision(
            sentenceType = SentenceType.UP_TO_TWO_YEARS,
            drivingMoment = DrivingMoment.AFTER_PERIOD,
            hasCompletedCourseForLowerSentence = true,
            hasCompletedCourseForHigherSentence = null,
            hasPassedExamsForHigherSentence = null
        )

        assertEquals("continue_trip_message", decision.messageKey)
        assertEquals(JudicialBorderBehavior.GREEN_SOLID, decision.borderBehavior)
    }

    @Test
    fun `lower sentence after period without course resolves as infringement`() {
        val decision = resolveJudicialSuspensionDecision(
            sentenceType = SentenceType.UP_TO_TWO_YEARS,
            drivingMoment = DrivingMoment.AFTER_PERIOD,
            hasCompletedCourseForLowerSentence = false,
            hasCompletedCourseForHigherSentence = null,
            hasPassedExamsForHigherSentence = null
        )

        assertEquals("judicial_infringement_lsv_message", decision.messageKey)
        assertEquals(JudicialBorderBehavior.YELLOW_BLINK, decision.borderBehavior)
    }

    @Test
    fun `higher sentence after period with course and exam allows continuing trip`() {
        val decision = resolveJudicialSuspensionDecision(
            sentenceType = SentenceType.OVER_TWO_YEARS,
            drivingMoment = DrivingMoment.AFTER_PERIOD,
            hasCompletedCourseForLowerSentence = null,
            hasCompletedCourseForHigherSentence = true,
            hasPassedExamsForHigherSentence = true
        )

        assertEquals("continue_trip_message", decision.messageKey)
        assertEquals(JudicialBorderBehavior.GREEN_SOLID, decision.borderBehavior)
    }

    @Test
    fun `higher sentence after period with any failed condition resolves as infringement`() {
        val decision = resolveJudicialSuspensionDecision(
            sentenceType = SentenceType.OVER_TWO_YEARS,
            drivingMoment = DrivingMoment.AFTER_PERIOD,
            hasCompletedCourseForLowerSentence = null,
            hasCompletedCourseForHigherSentence = false,
            hasPassedExamsForHigherSentence = false
        )

        assertEquals("expired_validity_infringement_message", decision.messageKey)
        assertEquals(JudicialBorderBehavior.YELLOW_BLINK, decision.borderBehavior)
    }

    @Test
    fun `higher sentence after period with pending answers resolves to neutral state`() {
        val decision = resolveJudicialSuspensionDecision(
            sentenceType = SentenceType.OVER_TWO_YEARS,
            drivingMoment = DrivingMoment.AFTER_PERIOD,
            hasCompletedCourseForLowerSentence = null,
            hasCompletedCourseForHigherSentence = true,
            hasPassedExamsForHigherSentence = null
        )

        assertEquals(null, decision.messageKey)
        assertEquals(JudicialBorderBehavior.NONE, decision.borderBehavior)
    }
}
