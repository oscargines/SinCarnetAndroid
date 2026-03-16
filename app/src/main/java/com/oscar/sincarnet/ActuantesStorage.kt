package com.oscar.sincarnet

import android.content.Context

internal data class ActuantesData(
    val instructorEmployment: String = "",
    val instructorTip: String = "",
    val instructorUnit: String = "",
    val secretaryEmployment: String = "",
    val secretaryTip: String = "",
    val secretaryUnit: String = "",
    val sameUnit: Boolean = false
)

internal class ActuantesStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val delimiter = ","

    fun getTipHistory(): List<String> {
        val instructorTips = prefs.getString(KEY_TIP_HISTORY_INSTRUCTOR, "")?.split(delimiter)?.filter { it.isNotBlank() } ?: emptyList()
        val secretaryTips = prefs.getString(KEY_TIP_HISTORY_SECRETARY, "")?.split(delimiter)?.filter { it.isNotBlank() } ?: emptyList()
        return (instructorTips + secretaryTips).distinct()
    }

    fun getUnitHistory(): List<String> {
        val instructorUnits = prefs.getString(KEY_UNIT_HISTORY_INSTRUCTOR, "")?.split(delimiter)?.filter { it.isNotBlank() } ?: emptyList()
        val secretaryUnits = prefs.getString(KEY_UNIT_HISTORY_SECRETARY, "")?.split(delimiter)?.filter { it.isNotBlank() } ?: emptyList()
        return (instructorUnits + secretaryUnits).distinct()
    }

    fun addTipToHistory(tip: String, isInstructor: Boolean) {
        if (tip.isBlank()) return
        val key = if (isInstructor) KEY_TIP_HISTORY_INSTRUCTOR else KEY_TIP_HISTORY_SECRETARY
        val current = prefs.getString(key, "")?.split(delimiter)?.filter { it.isNotBlank() } ?: emptyList()
        if (!current.contains(tip)) {
            val updated = (current + tip).joinToString(delimiter)
            prefs.edit().putString(key, updated).apply()
        }
    }

    fun addUnitToHistory(unit: String, isInstructor: Boolean) {
        if (unit.isBlank()) return
        val key = if (isInstructor) KEY_UNIT_HISTORY_INSTRUCTOR else KEY_UNIT_HISTORY_SECRETARY
        val current = prefs.getString(key, "")?.split(delimiter)?.filter { it.isNotBlank() } ?: emptyList()
        if (!current.contains(unit)) {
            val updated = (current + unit).joinToString(delimiter)
            prefs.edit().putString(key, updated).apply()
        }
    }

    fun loadCurrent(): ActuantesData = ActuantesData(
        instructorEmployment = prefs.getString(KEY_INSTRUCTOR_EMPLOYMENT, "").orEmpty(),
        instructorTip = prefs.getString(KEY_INSTRUCTOR_TIP, "").orEmpty(),
        instructorUnit = prefs.getString(KEY_INSTRUCTOR_UNIT, "").orEmpty(),
        secretaryEmployment = prefs.getString(KEY_SECRETARY_EMPLOYMENT, "").orEmpty(),
        secretaryTip = prefs.getString(KEY_SECRETARY_TIP, "").orEmpty(),
        secretaryUnit = prefs.getString(KEY_SECRETARY_UNIT, "").orEmpty(),
        sameUnit = prefs.getBoolean(KEY_SAME_UNIT, false)
    )

    fun saveCurrent(data: ActuantesData) {
        prefs.edit()
            .putString(KEY_INSTRUCTOR_EMPLOYMENT, data.instructorEmployment)
            .putString(KEY_INSTRUCTOR_TIP, data.instructorTip)
            .putString(KEY_INSTRUCTOR_UNIT, data.instructorUnit)
            .putString(KEY_SECRETARY_EMPLOYMENT, data.secretaryEmployment)
            .putString(KEY_SECRETARY_TIP, data.secretaryTip)
            .putString(KEY_SECRETARY_UNIT, data.secretaryUnit)
            .putBoolean(KEY_SAME_UNIT, data.sameUnit)
            .apply()
    }

    fun deleteCurrentWithBackup() {
        val current = loadCurrent()
        prefs.edit()
            .putString(KEY_BACKUP_INSTRUCTOR_EMPLOYMENT, current.instructorEmployment)
            .putString(KEY_BACKUP_INSTRUCTOR_TIP, current.instructorTip)
            .putString(KEY_BACKUP_INSTRUCTOR_UNIT, current.instructorUnit)
            .putString(KEY_BACKUP_SECRETARY_EMPLOYMENT, current.secretaryEmployment)
            .putString(KEY_BACKUP_SECRETARY_TIP, current.secretaryTip)
            .putString(KEY_BACKUP_SECRETARY_UNIT, current.secretaryUnit)
            .putBoolean(KEY_BACKUP_SAME_UNIT, current.sameUnit)
            .putBoolean(KEY_HAS_BACKUP, true)
            .remove(KEY_INSTRUCTOR_EMPLOYMENT)
            .remove(KEY_INSTRUCTOR_TIP)
            .remove(KEY_INSTRUCTOR_UNIT)
            .remove(KEY_SECRETARY_EMPLOYMENT)
            .remove(KEY_SECRETARY_TIP)
            .remove(KEY_SECRETARY_UNIT)
            .remove(KEY_SAME_UNIT)
            .apply()
    }

    fun recoverDeleted(): ActuantesData? {
        if (!prefs.getBoolean(KEY_HAS_BACKUP, false)) return null

        val recovered = ActuantesData(
            instructorEmployment = prefs.getString(KEY_BACKUP_INSTRUCTOR_EMPLOYMENT, "").orEmpty(),
            instructorTip = prefs.getString(KEY_BACKUP_INSTRUCTOR_TIP, "").orEmpty(),
            instructorUnit = prefs.getString(KEY_BACKUP_INSTRUCTOR_UNIT, "").orEmpty(),
            secretaryEmployment = prefs.getString(KEY_BACKUP_SECRETARY_EMPLOYMENT, "").orEmpty(),
            secretaryTip = prefs.getString(KEY_BACKUP_SECRETARY_TIP, "").orEmpty(),
            secretaryUnit = prefs.getString(KEY_BACKUP_SECRETARY_UNIT, "").orEmpty(),
            sameUnit = prefs.getBoolean(KEY_BACKUP_SAME_UNIT, false)
        )

        saveCurrent(recovered)
        return recovered
    }

    fun hasRecoverableBackup(): Boolean = prefs.getBoolean(KEY_HAS_BACKUP, false)

    private companion object {
        const val PREFS_NAME = "actuantes_storage"

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
