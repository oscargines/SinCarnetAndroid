package com.oscar.sincarnet.domain.decision

enum class BorderBehavior {
    NONE,
    RED_BLINK,
    YELLOW_BLINK,
    GREEN_SOLID
}

data class ExpiredValidityDecision(
    val messageKey: String?,
    val borderBehavior: BorderBehavior,
    val isCrimeCase: Boolean = false
)

fun resolveExpiredValidityDecision(
    selectedOption: Int,
    hasAnyNegativeAnswer: Boolean,
    hasAllPositiveAnswers: Boolean,
    hasKnowledge: Boolean?,
    isInAppealPeriod: Boolean?
): ExpiredValidityDecision {
    return when {
        selectedOption == 0 || (selectedOption == 1 && hasAnyNegativeAnswer) -> {
            ExpiredValidityDecision(
                messageKey = "expired_validity_crime_message",
                borderBehavior = BorderBehavior.RED_BLINK,
                isCrimeCase = true
            )
        }

        selectedOption == 2 && hasKnowledge == false -> {
            ExpiredValidityDecision(
                messageKey = "expired_validity_infringement_message",
                borderBehavior = BorderBehavior.YELLOW_BLINK
            )
        }

        selectedOption == 2 && hasKnowledge == true && isInAppealPeriod == true -> {
            ExpiredValidityDecision(
                messageKey = "expired_validity_infringement_message",
                borderBehavior = BorderBehavior.YELLOW_BLINK
            )
        }

        selectedOption == 2 && hasKnowledge == true && isInAppealPeriod == false -> {
            ExpiredValidityDecision(
                messageKey = "expired_validity_crime_message",
                borderBehavior = BorderBehavior.RED_BLINK,
                isCrimeCase = true
            )
        }

        selectedOption == 1 && hasAllPositiveAnswers -> {
            ExpiredValidityDecision(
                messageKey = "continue_trip_message",
                borderBehavior = BorderBehavior.GREEN_SOLID
            )
        }

        else -> {
            ExpiredValidityDecision(
                messageKey = null,
                borderBehavior = BorderBehavior.NONE
            )
        }
    }
}
