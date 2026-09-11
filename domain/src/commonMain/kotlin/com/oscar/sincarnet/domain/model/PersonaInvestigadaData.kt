package com.oscar.sincarnet.domain.model

data class PersonaInvestigadaData(
    val nationality: String = "España",
    val sex: String = "Desconocido",
    val firstName: String = "",
    val lastName1: String = "",
    val lastName2: String = "",
    val documentIdentification: String = "",
    val address: String = "",
    val birthDate: String = "",
    val birthPlace: String = "",
    val birthProvince: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val residencePopulation: String = "",
    val residenceProvince: String = "",
    val phone: String = "",
    val email: String = "",
    val rightToRemainSilentInformed: Boolean? = null,
    val waivesLegalAssistance: Boolean? = null,
    val requestsPrivateLawyer: Boolean? = null,
    val requestsDutyLawyer: Boolean? = null,
    val accessesEssentialProceedings: Boolean? = null,
    val needsInterpreter: Boolean? = null,
    val otrosDocumentos: String? = null
)