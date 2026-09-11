package com.oscar.sincarnet.domain.decision

enum class SentenceType {
    UP_TO_TWO_YEARS,
    OVER_TWO_YEARS
}

enum class DrivingMoment {
    WITHIN_PERIOD,
    AFTER_PERIOD
}

enum class JudicialBorderBehavior {
    NONE,
    RED_BLINK,
    YELLOW_BLINK,
    GREEN_SOLID
}

data class JudicialSuspensionDecision(
    val messageKey: String?,
    val borderBehavior: JudicialBorderBehavior,
    val isCrimeCase: Boolean = false
)

fun resolveJudicialSuspensionDecision(
    sentenceType: SentenceType?,
    drivingMoment: DrivingMoment?,
    hasCompletedCourseForLowerSentence: Boolean?,
    hasCompletedCourseForHigherSentence: Boolean?,
    hasPassedExamsForHigherSentence: Boolean?
): JudicialSuspensionDecision {
    return when {
        drivingMoment == DrivingMoment.WITHIN_PERIOD -> {
            JudicialSuspensionDecision(
                messageKey = "expired_validity_crime_message",
                borderBehavior = JudicialBorderBehavior.RED_BLINK,
                isCrimeCase = true
            )
        }

        sentenceType == SentenceType.UP_TO_TWO_YEARS && drivingMoment == DrivingMoment.AFTER_PERIOD -> {
            when (hasCompletedCourseForLowerSentence) {
                true -> JudicialSuspensionDecision(
                    messageKey = "continue_trip_message",
                    borderBehavior = JudicialBorderBehavior.GREEN_SOLID
                )

                false -> JudicialSuspensionDecision(
                    messageKey = "judicial_infringement_lsv_message",
                    borderBehavior = JudicialBorderBehavior.YELLOW_BLINK
                )

                null -> JudicialSuspensionDecision(
                    messageKey = null,
                    borderBehavior = JudicialBorderBehavior.NONE
                )
            }
        }

        sentenceType == SentenceType.OVER_TWO_YEARS && drivingMoment == DrivingMoment.AFTER_PERIOD -> {
            if (hasCompletedCourseForHigherSentence == true && hasPassedExamsForHigherSentence == true) {
                JudicialSuspensionDecision(
                    messageKey = "continue_trip_message",
                    borderBehavior = JudicialBorderBehavior.GREEN_SOLID
                )
            } else if (
                hasCompletedCourseForHigherSentence != null &&
                hasPassedExamsForHigherSentence != null
            ) {
                JudicialSuspensionDecision(
                    messageKey = "expired_validity_infringement_message",
                    borderBehavior = JudicialBorderBehavior.YELLOW_BLINK
                )
            } else {
                JudicialSuspensionDecision(
                    messageKey = null,
                    borderBehavior = JudicialBorderBehavior.NONE
                )
            }
        }

        else -> JudicialSuspensionDecision(
            messageKey = null,
            borderBehavior = JudicialBorderBehavior.NONE
        )
    }
}
