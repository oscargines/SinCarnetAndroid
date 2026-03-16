package com.oscar.sincarnet

import android.content.Context

internal data class VehiculoData(
    val brand: String = "",
    val model: String = "",
    val plate: String = "",
    val registrationDate: String = "",
    val nationality: String = "España",
    val itvDate: String = "",
    val insurer: String = "",
    val vehicleType: String = "",
    val ownerIsOther: Boolean = false,
    val ownerName: String = "",
    val ownerLastNames: String = "",
    val ownerDni: String = "",
    val ownerAddress: String = "",
    val ownerPhone: String = ""
)

internal class VehiculoStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCurrent(): VehiculoData = VehiculoData(
        brand = prefs.getString(KEY_BRAND, "").orEmpty(),
        model = prefs.getString(KEY_MODEL, "").orEmpty(),
        plate = prefs.getString(KEY_PLATE, "").orEmpty(),
        registrationDate = prefs.getString(KEY_REGISTRATION_DATE, "").orEmpty(),
        nationality = prefs.getString(KEY_NATIONALITY, "España").orEmpty(),
        itvDate = prefs.getString(KEY_ITV_DATE, "").orEmpty(),
        insurer = prefs.getString(KEY_INSURER, "").orEmpty(),
        vehicleType = prefs.getString(KEY_VEHICLE_TYPE, "").orEmpty(),
        ownerIsOther = prefs.getBoolean(KEY_OWNER_IS_OTHER, false),
        ownerName = prefs.getString(KEY_OWNER_NAME, "").orEmpty(),
        ownerLastNames = prefs.getString(KEY_OWNER_LAST_NAMES, "").orEmpty(),
        ownerDni = prefs.getString(KEY_OWNER_DNI, "").orEmpty(),
        ownerAddress = prefs.getString(KEY_OWNER_ADDRESS, "").orEmpty(),
        ownerPhone = prefs.getString(KEY_OWNER_PHONE, "").orEmpty()
    )

    fun saveCurrent(data: VehiculoData) {
        prefs.edit()
            .putString(KEY_BRAND, data.brand)
            .putString(KEY_MODEL, data.model)
            .putString(KEY_PLATE, data.plate)
            .putString(KEY_REGISTRATION_DATE, data.registrationDate)
            .putString(KEY_NATIONALITY, data.nationality)
            .putString(KEY_ITV_DATE, data.itvDate)
            .putString(KEY_INSURER, data.insurer)
            .putString(KEY_VEHICLE_TYPE, data.vehicleType)
            .putBoolean(KEY_OWNER_IS_OTHER, data.ownerIsOther)
            .putString(KEY_OWNER_NAME, data.ownerName)
            .putString(KEY_OWNER_LAST_NAMES, data.ownerLastNames)
            .putString(KEY_OWNER_DNI, data.ownerDni)
            .putString(KEY_OWNER_ADDRESS, data.ownerAddress)
            .putString(KEY_OWNER_PHONE, data.ownerPhone)
            .apply()
    }

    fun clearCurrent() {
        prefs.edit()
            .remove(KEY_BRAND)
            .remove(KEY_MODEL)
            .remove(KEY_PLATE)
            .remove(KEY_REGISTRATION_DATE)
            .remove(KEY_NATIONALITY)
            .remove(KEY_ITV_DATE)
            .remove(KEY_INSURER)
            .remove(KEY_VEHICLE_TYPE)
            .remove(KEY_OWNER_IS_OTHER)
            .remove(KEY_OWNER_NAME)
            .remove(KEY_OWNER_LAST_NAMES)
            .remove(KEY_OWNER_DNI)
            .remove(KEY_OWNER_ADDRESS)
            .remove(KEY_OWNER_PHONE)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "vehiculo_storage"
        const val KEY_BRAND = "brand"
        const val KEY_MODEL = "model"
        const val KEY_PLATE = "plate"
        const val KEY_REGISTRATION_DATE = "registration_date"
        const val KEY_NATIONALITY = "nationality"
        const val KEY_ITV_DATE = "itv_date"
        const val KEY_INSURER = "insurer"
        const val KEY_VEHICLE_TYPE = "vehicle_type"
        const val KEY_OWNER_IS_OTHER = "owner_is_other"
        const val KEY_OWNER_NAME = "owner_name"
        const val KEY_OWNER_LAST_NAMES = "owner_last_names"
        const val KEY_OWNER_DNI = "owner_dni"
        const val KEY_OWNER_ADDRESS = "owner_address"
        const val KEY_OWNER_PHONE = "owner_phone"
    }
}

