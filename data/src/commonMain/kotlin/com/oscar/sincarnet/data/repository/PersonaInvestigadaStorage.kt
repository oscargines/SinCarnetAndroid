package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.PersonaInvestigadaData

class PersonaInvestigadaStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): PersonaInvestigadaData = PersonaInvestigadaData(
        nationality = storage.getString(KEY_NATIONALITY, "España"),
        sex = storage.getString(KEY_SEX, "Desconocido"),
        firstName = storage.getString(KEY_FIRST_NAME, ""),
        lastName1 = storage.getString(KEY_LAST_NAME_1, ""),
        lastName2 = storage.getString(KEY_LAST_NAME_2, ""),
        documentIdentification = storage.getString(KEY_DOCUMENT_IDENTIFICATION, ""),
        address = storage.getString(KEY_ADDRESS, ""),
        birthDate = storage.getString(KEY_BIRTH_DATE, ""),
        birthPlace = storage.getString(KEY_BIRTH_PLACE, ""),
        birthProvince = storage.getString(KEY_BIRTH_PROVINCE, ""),
        fatherName = storage.getString(KEY_FATHER_NAME, ""),
        motherName = storage.getString(KEY_MOTHER_NAME, ""),
        residencePopulation = storage.getString(KEY_RESIDENCE_POPULATION, ""),
        residenceProvince = storage.getString(KEY_RESIDENCE_PROVINCE, ""),
        phone = storage.getString(KEY_PHONE, ""),
        email = storage.getString(KEY_EMAIL, ""),
        rightToRemainSilentInformed = getNullableBoolean(KEY_RIGHT_TO_REMAIN_SILENT_INFORMED),
        waivesLegalAssistance = getNullableBoolean(KEY_WAIVES_LEGAL_ASSISTANCE),
        requestsPrivateLawyer = getNullableBoolean(KEY_REQUESTS_PRIVATE_LAWYER),
        requestsDutyLawyer = getNullableBoolean(KEY_REQUESTS_DUTY_LAWYER),
        accessesEssentialProceedings = getNullableBoolean(KEY_ACCESSES_ESSENTIAL_PROCEEDINGS),
        needsInterpreter = getNullableBoolean(KEY_NEEDS_INTERPRETER),
        otrosDocumentos = if (storage.contains(KEY_OTROS_DOCUMENTOS)) storage.getString(KEY_OTROS_DOCUMENTOS, "") else null
    )

    fun saveCurrent(data: PersonaInvestigadaData) {
        storage.putString(KEY_NATIONALITY, data.nationality)
        storage.putString(KEY_SEX, data.sex)
        storage.putString(KEY_FIRST_NAME, data.firstName)
        storage.putString(KEY_LAST_NAME_1, data.lastName1)
        storage.putString(KEY_LAST_NAME_2, data.lastName2)
        storage.putString(KEY_DOCUMENT_IDENTIFICATION, data.documentIdentification)
        storage.putString(KEY_ADDRESS, data.address)
        storage.putString(KEY_BIRTH_DATE, data.birthDate)
        storage.putString(KEY_BIRTH_PLACE, data.birthPlace)
        storage.putString(KEY_BIRTH_PROVINCE, data.birthProvince)
        storage.putString(KEY_FATHER_NAME, data.fatherName)
        storage.putString(KEY_MOTHER_NAME, data.motherName)
        storage.putString(KEY_RESIDENCE_POPULATION, data.residencePopulation)
        storage.putString(KEY_RESIDENCE_PROVINCE, data.residenceProvince)
        storage.putString(KEY_PHONE, data.phone)
        storage.putString(KEY_EMAIL, data.email)
        putBooleanOrRemove(KEY_RIGHT_TO_REMAIN_SILENT_INFORMED, data.rightToRemainSilentInformed)
        putBooleanOrRemove(KEY_WAIVES_LEGAL_ASSISTANCE, data.waivesLegalAssistance)
        putBooleanOrRemove(KEY_REQUESTS_PRIVATE_LAWYER, data.requestsPrivateLawyer)
        putBooleanOrRemove(KEY_REQUESTS_DUTY_LAWYER, data.requestsDutyLawyer)
        putBooleanOrRemove(KEY_ACCESSES_ESSENTIAL_PROCEEDINGS, data.accessesEssentialProceedings)
        putBooleanOrRemove(KEY_NEEDS_INTERPRETER, data.needsInterpreter)
        val otros = data.otrosDocumentos
        if (otros != null) storage.putString(KEY_OTROS_DOCUMENTOS, otros)
        else storage.remove(KEY_OTROS_DOCUMENTOS)
    }

    fun saveRightsSelections(
        rightToRemainSilentInformed: Boolean?,
        waivesLegalAssistance: Boolean?,
        requestsPrivateLawyer: Boolean?,
        requestsDutyLawyer: Boolean?,
        accessesEssentialProceedings: Boolean?,
        needsInterpreter: Boolean?
    ) {
        putBooleanOrRemove(KEY_RIGHT_TO_REMAIN_SILENT_INFORMED, rightToRemainSilentInformed)
        putBooleanOrRemove(KEY_WAIVES_LEGAL_ASSISTANCE, waivesLegalAssistance)
        putBooleanOrRemove(KEY_REQUESTS_PRIVATE_LAWYER, requestsPrivateLawyer)
        putBooleanOrRemove(KEY_REQUESTS_DUTY_LAWYER, requestsDutyLawyer)
        putBooleanOrRemove(KEY_ACCESSES_ESSENTIAL_PROCEEDINGS, accessesEssentialProceedings)
        putBooleanOrRemove(KEY_NEEDS_INTERPRETER, needsInterpreter)
    }

    fun clearCurrent() {
        storage.remove(KEY_NATIONALITY)
        storage.remove(KEY_SEX)
        storage.remove(KEY_FIRST_NAME)
        storage.remove(KEY_LAST_NAME_1)
        storage.remove(KEY_LAST_NAME_2)
        storage.remove(KEY_DOCUMENT_IDENTIFICATION)
        storage.remove(KEY_ADDRESS)
        storage.remove(KEY_BIRTH_DATE)
        storage.remove(KEY_BIRTH_PLACE)
        storage.remove(KEY_BIRTH_PROVINCE)
        storage.remove(KEY_FATHER_NAME)
        storage.remove(KEY_MOTHER_NAME)
        storage.remove(KEY_RESIDENCE_POPULATION)
        storage.remove(KEY_RESIDENCE_PROVINCE)
        storage.remove(KEY_PHONE)
        storage.remove(KEY_EMAIL)
        storage.remove(KEY_RIGHT_TO_REMAIN_SILENT_INFORMED)
        storage.remove(KEY_WAIVES_LEGAL_ASSISTANCE)
        storage.remove(KEY_REQUESTS_PRIVATE_LAWYER)
        storage.remove(KEY_REQUESTS_DUTY_LAWYER)
        storage.remove(KEY_ACCESSES_ESSENTIAL_PROCEEDINGS)
        storage.remove(KEY_NEEDS_INTERPRETER)
        storage.remove(KEY_OTROS_DOCUMENTOS)
    }

    private fun getNullableBoolean(key: String): Boolean? {
        return if (storage.contains(key)) storage.getBoolean(key, false) else null
    }

    private fun putBooleanOrRemove(key: String, value: Boolean?) {
        if (value == null) storage.remove(key) else storage.putBoolean(key, value)
    }

    private companion object {
        const val KEY_NATIONALITY = "nationality"
        const val KEY_SEX = "sex"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME_1 = "last_name_1"
        const val KEY_LAST_NAME_2 = "last_name_2"
        const val KEY_DOCUMENT_IDENTIFICATION = "document_identification"
        const val KEY_ADDRESS = "address"
        const val KEY_BIRTH_DATE = "birth_date"
        const val KEY_BIRTH_PLACE = "birth_place"
        const val KEY_BIRTH_PROVINCE = "birth_province"
        const val KEY_FATHER_NAME = "father_name"
        const val KEY_MOTHER_NAME = "mother_name"
        const val KEY_RESIDENCE_POPULATION = "residence_population"
        const val KEY_RESIDENCE_PROVINCE = "residence_province"
        const val KEY_PHONE = "phone"
        const val KEY_EMAIL = "email"
        const val KEY_RIGHT_TO_REMAIN_SILENT_INFORMED = "right_to_remain_silent_informed"
        const val KEY_WAIVES_LEGAL_ASSISTANCE = "waives_legal_assistance"
        const val KEY_REQUESTS_PRIVATE_LAWYER = "requests_private_lawyer"
        const val KEY_REQUESTS_DUTY_LAWYER = "requests_duty_lawyer"
        const val KEY_ACCESSES_ESSENTIAL_PROCEEDINGS = "accesses_essential_proceedings"
        const val KEY_NEEDS_INTERPRETER = "needs_interpreter"
        const val KEY_OTROS_DOCUMENTOS = "otros_documentos"
    }
}
