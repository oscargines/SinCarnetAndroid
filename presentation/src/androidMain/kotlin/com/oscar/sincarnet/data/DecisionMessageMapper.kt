package com.oscar.sincarnet.data

import com.oscar.sincarnet.presentation.R

fun String?.toMessageRes(): Int? = when (this) {
    "expired_validity_crime_message" -> R.string.expired_validity_crime_message
    "expired_validity_infringement_message" -> R.string.expired_validity_infringement_message
    "continue_trip_message" -> R.string.continue_trip_message
    "without_permit_admin_report_message" -> R.string.without_permit_admin_report_message
    "judicial_infringement_lsv_message" -> R.string.judicial_infringement_lsv_message
    "special_case_psychophysical_loss_message" -> R.string.special_case_psychophysical_loss_message
    "special_case_missing_requirements_message" -> R.string.special_case_missing_requirements_message
    else -> null
}
