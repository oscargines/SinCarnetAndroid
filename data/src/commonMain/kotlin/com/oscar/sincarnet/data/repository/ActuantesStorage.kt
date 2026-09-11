package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.ActuantesData

class ActuantesStorage(private val storage: PlatformStorage) {
    private val delimiter = ","

    fun getTipHistory(): List<String> {
        val instructorTips = storage.getString(KEY_TIP_HISTORY_INSTRUCTOR, "").split(delimiter).filter { it.isNotBlank() }
        val secretaryTips = storage.getString(KEY_TIP_HISTORY_SECRETARY, "").split(delimiter).filter { it.isNotBlank() }
        return (instructorTips + secretaryTips).distinct()
    }

    fun getUnitHistory(): List<String> {
        val instructorUnits = storage.getString(KEY_UNIT_HISTORY_INSTRUCTOR, "")?.split(delimiter)?.filter { it.isNotBlank() } ?: emptyList()
        val secretaryUnits = storage.getString(KEY_UNIT_HISTORY_SECRETARY, "")?.split(delimiter)?.filter { it.isNotBlank() } ?: emptyList()
        return (instructorUnits + secretaryUnits).distinct()
    }

    fun addTipToHistory(tip: String, isInstructor: Boolean) {
        if (tip.isBlank()) return
        val key = if (isInstructor) KEY_TIP_HISTORY_INSTRUCTOR else KEY_TIP_HISTORY_SECRETARY
        val current = storage.getString(key, "").split(delimiter).filter { it.isNotBlank() }
        if (!current.contains(tip)) {
            val updated = (current + tip).joinToString(delimiter)
            storage.putString(key, updated)
        }
    }

    fun addUnitToHistory(unit: String, isInstructor: Boolean) {
        if (unit.isBlank()) return
        val key = if (isInstructor) KEY_UNIT_HISTORY_INSTRUCTOR else KEY_UNIT_HISTORY_SECRETARY
        val current = storage.getString(key, "").split(delimiter).filter { it.isNotBlank() }
        if (!current.contains(unit)) {
            val updated = (current + unit).joinToString(delimiter)
            storage.putString(key, updated)
        }
    }

    fun loadCurrent(): ActuantesData = ActuantesData(
        instructorEmployment = storage.getString(KEY_INSTRUCTOR_EMPLOYMENT, ""),
        instructorTip = storage.getString(KEY_INSTRUCTOR_TIP, ""),
        instructorUnit = storage.getString(KEY_INSTRUCTOR_UNIT, ""),
        secretaryEmployment = storage.getString(KEY_SECRETARY_EMPLOYMENT, ""),
        secretaryTip = storage.getString(KEY_SECRETARY_TIP, ""),
        secretaryUnit = storage.getString(KEY_SECRETARY_UNIT, ""),
        sameUnit = storage.getBoolean(KEY_SAME_UNIT, false)
    )

    fun saveCurrent(data: ActuantesData) {
        storage.putString(KEY_INSTRUCTOR_EMPLOYMENT, data.instructorEmployment)
        storage.putString(KEY_INSTRUCTOR_TIP, data.instructorTip)
        storage.putString(KEY_INSTRUCTOR_UNIT, data.instructorUnit)
        storage.putString(KEY_SECRETARY_EMPLOYMENT, data.secretaryEmployment)
        storage.putString(KEY_SECRETARY_TIP, data.secretaryTip)
        storage.putString(KEY_SECRETARY_UNIT, data.secretaryUnit)
        storage.putBoolean(KEY_SAME_UNIT, data.sameUnit)
    }

    fun deleteCurrentWithBackup() {
        val current = loadCurrent()
        storage.putString(KEY_BACKUP_INSTRUCTOR_EMPLOYMENT, current.instructorEmployment)
        storage.putString(KEY_BACKUP_INSTRUCTOR_TIP, current.instructorTip)
        storage.putString(KEY_BACKUP_INSTRUCTOR_UNIT, current.instructorUnit)
        storage.putString(KEY_BACKUP_SECRETARY_EMPLOYMENT, current.secretaryEmployment)
        storage.putString(KEY_BACKUP_SECRETARY_TIP, current.secretaryTip)
        storage.putString(KEY_BACKUP_SECRETARY_UNIT, current.secretaryUnit)
        storage.putBoolean(KEY_BACKUP_SAME_UNIT, current.sameUnit)
        storage.putBoolean(KEY_HAS_BACKUP, true)
        storage.remove(KEY_INSTRUCTOR_EMPLOYMENT)
        storage.remove(KEY_INSTRUCTOR_TIP)
        storage.remove(KEY_INSTRUCTOR_UNIT)
        storage.remove(KEY_SECRETARY_EMPLOYMENT)
        storage.remove(KEY_SECRETARY_TIP)
        storage.remove(KEY_SECRETARY_UNIT)
        storage.remove(KEY_SAME_UNIT)
    }

    fun recoverDeleted(): ActuantesData? {
        if (!storage.contains(KEY_HAS_BACKUP)) return null

        val recovered = ActuantesData(
            instructorEmployment = storage.getString(KEY_BACKUP_INSTRUCTOR_EMPLOYMENT, ""),
            instructorTip = storage.getString(KEY_BACKUP_INSTRUCTOR_TIP, ""),
            instructorUnit = storage.getString(KEY_BACKUP_INSTRUCTOR_UNIT, ""),
            secretaryEmployment = storage.getString(KEY_BACKUP_SECRETARY_EMPLOYMENT, ""),
            secretaryTip = storage.getString(KEY_BACKUP_SECRETARY_TIP, ""),
            secretaryUnit = storage.getString(KEY_BACKUP_SECRETARY_UNIT, ""),
            sameUnit = storage.getBoolean(KEY_BACKUP_SAME_UNIT, false)
        )

        saveCurrent(recovered)
        return recovered
    }

    fun hasRecoverableBackup(): Boolean = storage.contains(KEY_HAS_BACKUP)

    private companion object {
        const val KEY_TIP_HISTORY_INSTRUCTOR = "tip_history_instructor"
        const val KEY_TIP_HISTORY_SECRETARY = "tip_history_secretary"
        const val KEY_UNIT_HISTORY_INSTRUCTOR = "unit_history_instructor"
        const val KEY_UNIT_HISTORY_SECRETARY = "unit_history_secretary"

        const val KEY_INSTRUCTOR_EMPLOYMENT = "instructor_employment"
        const val KEY_INSTRUCTOR_TIP = "instructor_tip"
        const val KEY_INSTRUCTOR_UNIT = "instructor_unit"

        const val KEY_SECRETARY_EMPLOYMENT = "secretary_employment"
        const val KEY_SECRETARY_TIP = "secretary_tip"
        const val KEY_SECRETARY_UNIT = "secretary_unit"

        const val KEY_SAME_UNIT = "same_unit"

        const val KEY_BACKUP_INSTRUCTOR_EMPLOYMENT = "backup_instructor_employment"
        const val KEY_BACKUP_INSTRUCTOR_TIP = "backup_instructor_tip"
        const val KEY_BACKUP_INSTRUCTOR_UNIT = "backup_instructor_unit"

        const val KEY_BACKUP_SECRETARY_EMPLOYMENT = "backup_secretary_employment"
        const val KEY_BACKUP_SECRETARY_TIP = "backup_secretary_tip"
        const val KEY_BACKUP_SECRETARY_UNIT = "backup_secretary_unit"

        const val KEY_BACKUP_SAME_UNIT = "backup_same_unit"
        const val KEY_HAS_BACKUP = "has_backup"
    }
}
