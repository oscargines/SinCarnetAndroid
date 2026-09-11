package com.oscar.sincarnet.domain.decision

enum class WithoutPermitBorderBehavior {
    NONE,
    RED_BLINK,
    GREEN_SOLID,
    ORANGE_SOLID
}

data class WithoutPermitDecision(
    val messageKey: String?,
    val borderBehavior: WithoutPermitBorderBehavior,
    val isCrimeCase: Boolean = false
)

fun resolveWithoutPermitDecision(
    hasEverObtainedPermit: Boolean?,
    isValidForDrivingInSpain: Boolean?
): WithoutPermitDecision {
    return when {
        hasEverObtainedPermit == false -> {
            WithoutPermitDecision(
                messageKey = "expired_validity_crime_message",
                borderBehavior = WithoutPermitBorderBehavior.RED_BLINK,
                isCrimeCase = true
            )
        }

        hasEverObtainedPermit == true && isValidForDrivingInSpain == true -> {
            WithoutPermitDecision(
                messageKey = "continue_trip_message",
                borderBehavior = WithoutPermitBorderBehavior.GREEN_SOLID
            )
        }

        hasEverObtainedPermit == true && isValidForDrivingInSpain == false -> {
            WithoutPermitDecision(
                messageKey = "without_permit_admin_report_message",
                borderBehavior = WithoutPermitBorderBehavior.ORANGE_SOLID
            )
        }

        else -> {
            WithoutPermitDecision(
                messageKey = null,
                borderBehavior = WithoutPermitBorderBehavior.NONE
            )
        }
    }
}
