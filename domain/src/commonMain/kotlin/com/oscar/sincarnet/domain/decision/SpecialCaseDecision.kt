package com.oscar.sincarnet.domain.decision

enum class SpecialCaseType {
    PSYCHOPHYSICAL_LOSS,
    MISSING_REQUIREMENTS
}

data class SpecialCaseDecision(
    val messageKey: String?,
    val hasOrangeBorder: Boolean
)

fun resolveSpecialCaseDecision(selectedSpecialCase: SpecialCaseType?): SpecialCaseDecision {
    return when (selectedSpecialCase) {
        SpecialCaseType.PSYCHOPHYSICAL_LOSS -> SpecialCaseDecision(
            messageKey = "special_case_psychophysical_loss_message",
            hasOrangeBorder = true
        )

        SpecialCaseType.MISSING_REQUIREMENTS -> SpecialCaseDecision(
            messageKey = "special_case_missing_requirements_message",
            hasOrangeBorder = true
        )

        null -> SpecialCaseDecision(
            messageKey = null,
            hasOrangeBorder = false
        )
    }
}
