package com.oscar.sincarnet.domain.model

data class VehiculoData(
    val brand: String = "",
    val model: String = "",
    val plate: String = "",
    val registrationDate: String = "",
    val nationality: String = "España",
    val itvDate: String = "",
    val insurer: String = "",
    val vehicleType: String = "",
    val clasePermiso: String = "",
    val ownerIsOther: Boolean = false,
    val ownerName: String = "",
    val ownerLastNames: String = "",
    val ownerDni: String = "",
    val ownerAddress: String = "",
    val ownerPhone: String = ""
)