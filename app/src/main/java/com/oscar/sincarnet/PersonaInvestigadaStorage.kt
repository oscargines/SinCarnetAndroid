package com.oscar.sincarnet

import android.content.Context

internal data class PersonaInvestigadaData(
    val nationality: String = "España",
    val sex: String = "Desconocido",
    val firstName: String = "",
    val lastName1: String = "",
    val lastName2: String = "",
    val documentIdentification: String = "",
    val address: String = "",
    val birthDate: String = "",
    val birthPlace: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val phone: String = "",
    val email: String = "",
    val rightToRemainSilentInformed: Boolean? = null,
    val waivesLegalAssistance: Boolean? = null,
    val requestsPrivateLawyer: Boolean? = null,
    val requestsDutyLawyer: Boolean? = null,
    val accessesEssentialProceedings: Boolean? = null,
    val needsInterpreter: Boolean? = null
)

internal class PersonaInvestigadaStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCurrent(): PersonaInvestigadaData = PersonaInvestigadaData(
        nationality = prefs.getString(KEY_NATIONALITY, "España").orEmpty(),
        sex = prefs.getString(KEY_SEX, "Desconocido").orEmpty(),
        firstName = prefs.getString(KEY_FIRST_NAME, "").orEmpty(),
        lastName1 = prefs.getString(KEY_LAST_NAME_1, "").orEmpty(),
        lastName2 = prefs.getString(KEY_LAST_NAME_2, "").orEmpty(),
        documentIdentification = prefs.getString(KEY_DOCUMENT_IDENTIFICATION, "").orEmpty(),
        address = prefs.getString(KEY_ADDRESS, "").orEmpty(),
        birthDate = prefs.getString(KEY_BIRTH_DATE, "").orEmpty(),
        birthPlace = prefs.getString(KEY_BIRTH_PLACE, "").orEmpty(),
        fatherName = prefs.getString(KEY_FATHER_NAME, "").orEmpty(),
        motherName = prefs.getString(KEY_MOTHER_NAME, "").orEmpty(),
        phone = prefs.getString(KEY_PHONE, "").orEmpty(),
        email = prefs.getString(KEY_EMAIL, "").orEmpty(),
        rightToRemainSilentInformed = prefs.getNullableBoolean(KEY_RIGHT_TO_REMAIN_SILENT_INFORMED),
        waivesLegalAssistance = prefs.getNullableBoolean(KEY_WAIVES_LEGAL_ASSISTANCE),
        requestsPrivateLawyer = prefs.getNullableBoolean(KEY_REQUESTS_PRIVATE_LAWYER),
        requestsDutyLawyer = prefs.getNullableBoolean(KEY_REQUESTS_DUTY_LAWYER),
        accessesEssentialProceedings = prefs.getNullableBoolean(KEY_ACCESSES_ESSENTIAL_PROCEEDINGS),
        needsInterpreter = prefs.getNullableBoolean(KEY_NEEDS_INTERPRETER)
    )

    fun saveCurrent(data: PersonaInvestigadaData) {
        prefs.edit()
            .putString(KEY_NATIONALITY, data.nationality)
            .putString(KEY_SEX, data.sex)
            .putString(KEY_FIRST_NAME, data.firstName)
            .putString(KEY_LAST_NAME_1, data.lastName1)
            .putString(KEY_LAST_NAME_2, data.lastName2)
            .putString(KEY_DOCUMENT_IDENTIFICATION, data.documentIdentification)
            .putString(KEY_ADDRESS, data.address)
            .putString(KEY_BIRTH_DATE, data.birthDate)
            .putString(KEY_BIRTH_PLACE, data.birthPlace)
            .putString(KEY_FATHER_NAME, data.fatherName)
            .putString(KEY_MOTHER_NAME, data.motherName)
            .putString(KEY_PHONE, data.phone)
            .putString(KEY_EMAIL, data.email)
            .putBooleanOrRemove(KEY_RIGHT_TO_REMAIN_SILENT_INFORMED, data.rightToRemainSilentInformed)
            .putBooleanOrRemove(KEY_WAIVES_LEGAL_ASSISTANCE, data.waivesLegalAssistance)
            .putBooleanOrRemove(KEY_REQUESTS_PRIVATE_LAWYER, data.requestsPrivateLawyer)
            .putBooleanOrRemove(KEY_REQUESTS_DUTY_LAWYER, data.requestsDutyLawyer)
            .putBooleanOrRemove(KEY_ACCESSES_ESSENTIAL_PROCEEDINGS, data.accessesEssentialProceedings)
            .putBooleanOrRemove(KEY_NEEDS_INTERPRETER, data.needsInterpreter)
            .apply()
    }

    fun saveRightsSelections(
        rightToRemainSilentInformed: Boolean?,
        waivesLegalAssistance: Boolean?,
        requestsPrivateLawyer: Boolean?,
        requestsDutyLawyer: Boolean?,
        accessesEssentialProceedings: Boolean?,
        needsInterpreter: Boolean?
    ) {
        prefs.edit()
            .putBooleanOrRemove(KEY_RIGHT_TO_REMAIN_SILENT_INFORMED, rightToRemainSilentInformed)
            .putBooleanOrRemove(KEY_WAIVES_LEGAL_ASSISTANCE, waivesLegalAssistance)
            .putBooleanOrRemove(KEY_REQUESTS_PRIVATE_LAWYER, requestsPrivateLawyer)
            .putBooleanOrRemove(KEY_REQUESTS_DUTY_LAWYER, requestsDutyLawyer)
            .putBooleanOrRemove(KEY_ACCESSES_ESSENTIAL_PROCEEDINGS, accessesEssentialProceedings)
            .putBooleanOrRemove(KEY_NEEDS_INTERPRETER, needsInterpreter)
            .apply()
    }

    fun clearCurrent() {
        prefs.edit()
            .remove(KEY_NATIONALITY)
            .remove(KEY_SEX)
            .remove(KEY_FIRST_NAME)
            .remove(KEY_LAST_NAME_1)
            .remove(KEY_LAST_NAME_2)
            .remove(KEY_DOCUMENT_IDENTIFICATION)
            .remove(KEY_ADDRESS)
            .remove(KEY_BIRTH_DATE)
            .remove(KEY_BIRTH_PLACE)
            .remove(KEY_FATHER_NAME)
            .remove(KEY_MOTHER_NAME)
            .remove(KEY_PHONE)
            .remove(KEY_EMAIL)
            .remove(KEY_RIGHT_TO_REMAIN_SILENT_INFORMED)
            .remove(KEY_WAIVES_LEGAL_ASSISTANCE)
            .remove(KEY_REQUESTS_PRIVATE_LAWYER)
            .remove(KEY_REQUESTS_DUTY_LAWYER)
            .remove(KEY_ACCESSES_ESSENTIAL_PROCEEDINGS)
            .remove(KEY_NEEDS_INTERPRETER)
            .apply()
    }

    private fun android.content.SharedPreferences.getNullableBoolean(key: String): Boolean? {
        return if (contains(key)) getBoolean(key, false) else null
    }

    private fun android.content.SharedPreferences.Editor.putBooleanOrRemove(
        key: String,
        value: Boolean?
    ): android.content.SharedPreferences.Editor {
        return if (value == null) remove(key) else putBoolean(key, value)
    }

    private companion object {
        const val PREFS_NAME = "persona_investigada_storage"
        const val KEY_NATIONALITY = "nationality"
        const val KEY_SEX = "sex"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME_1 = "last_name_1"
        const val KEY_LAST_NAME_2 = "last_name_2"
        const val KEY_DOCUMENT_IDENTIFICATION = "document_identification"
        const val KEY_ADDRESS = "address"
        const val KEY_BIRTH_DATE = "birth_date"
        const val KEY_BIRTH_PLACE = "birth_place"
        const val KEY_FATHER_NAME = "father_name"
        const val KEY_MOTHER_NAME = "mother_name"
        const val KEY_PHONE = "phone"
        const val KEY_EMAIL = "email"
        const val KEY_RIGHT_TO_REMAIN_SILENT_INFORMED = "right_to_remain_silent_informed"
        const val KEY_WAIVES_LEGAL_ASSISTANCE = "waives_legal_assistance"
        const val KEY_REQUESTS_PRIVATE_LAWYER = "requests_private_lawyer"
        const val KEY_REQUESTS_DUTY_LAWYER = "requests_duty_lawyer"
        const val KEY_ACCESSES_ESSENTIAL_PROCEEDINGS = "accesses_essential_proceedings"
        const val KEY_NEEDS_INTERPRETER = "needs_interpreter"
    }
}

