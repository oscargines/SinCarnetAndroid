package com.oscar.sincarnet.domain.model

data class ActuantesData(
    val instructorEmployment: String = "",
    val instructorTip: String = "",
    val instructorUnit: String = "",
    val secretaryEmployment: String = "",
    val secretaryTip: String = "",
    val secretaryUnit: String = "",
    val sameUnit: Boolean = false
)