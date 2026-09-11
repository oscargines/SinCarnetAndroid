package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.VehiculoData

class VehiculoStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): VehiculoData = VehiculoData(
        brand = storage.getString(KEY_BRAND, ""),
        model = storage.getString(KEY_MODEL, ""),
        plate = storage.getString(KEY_PLATE, ""),
        registrationDate = storage.getString(KEY_REGISTRATION_DATE, ""),
        nationality = storage.getString(KEY_NATIONALITY, "España"),
        itvDate = storage.getString(KEY_ITV_DATE, ""),
        insurer = storage.getString(KEY_INSURER, ""),
        vehicleType = storage.getString(KEY_VEHICLE_TYPE, ""),
        clasePermiso = storage.getString(KEY_CLASE_PERMISO, ""),
        ownerIsOther = storage.getBoolean(KEY_OWNER_IS_OTHER, false),
        ownerName = storage.getString(KEY_OWNER_NAME, ""),
        ownerLastNames = storage.getString(KEY_OWNER_LAST_NAMES, ""),
        ownerDni = storage.getString(KEY_OWNER_DNI, ""),
        ownerAddress = storage.getString(KEY_OWNER_ADDRESS, ""),
        ownerPhone = storage.getString(KEY_OWNER_PHONE, "")
    )

    fun saveCurrent(data: VehiculoData) {
        storage.putString(KEY_BRAND, data.brand)
        storage.putString(KEY_MODEL, data.model)
        storage.putString(KEY_PLATE, data.plate)
        storage.putString(KEY_REGISTRATION_DATE, data.registrationDate)
        storage.putString(KEY_NATIONALITY, data.nationality)
        storage.putString(KEY_ITV_DATE, data.itvDate)
        storage.putString(KEY_INSURER, data.insurer)
        storage.putString(KEY_VEHICLE_TYPE, data.vehicleType)
        storage.putString(KEY_CLASE_PERMISO, data.clasePermiso)
        storage.putBoolean(KEY_OWNER_IS_OTHER, data.ownerIsOther)
        storage.putString(KEY_OWNER_NAME, data.ownerName)
        storage.putString(KEY_OWNER_LAST_NAMES, data.ownerLastNames)
        storage.putString(KEY_OWNER_DNI, data.ownerDni)
        storage.putString(KEY_OWNER_ADDRESS, data.ownerAddress)
        storage.putString(KEY_OWNER_PHONE, data.ownerPhone)
    }

    fun clearCurrent() {
        storage.remove(KEY_BRAND)
        storage.remove(KEY_MODEL)
        storage.remove(KEY_PLATE)
        storage.remove(KEY_REGISTRATION_DATE)
        storage.remove(KEY_NATIONALITY)
        storage.remove(KEY_ITV_DATE)
        storage.remove(KEY_INSURER)
        storage.remove(KEY_VEHICLE_TYPE)
        storage.remove(KEY_CLASE_PERMISO)
        storage.remove(KEY_OWNER_IS_OTHER)
        storage.remove(KEY_OWNER_NAME)
        storage.remove(KEY_OWNER_LAST_NAMES)
        storage.remove(KEY_OWNER_DNI)
        storage.remove(KEY_OWNER_ADDRESS)
        storage.remove(KEY_OWNER_PHONE)
    }

    private companion object {
        const val KEY_BRAND = "brand"
        const val KEY_MODEL = "model"
        const val KEY_PLATE = "plate"
        const val KEY_REGISTRATION_DATE = "registration_date"
        const val KEY_NATIONALITY = "nationality"
        const val KEY_ITV_DATE = "itv_date"
        const val KEY_INSURER = "insurer"
        const val KEY_VEHICLE_TYPE = "vehicle_type"
        const val KEY_CLASE_PERMISO = "clase_permiso"
        const val KEY_OWNER_IS_OTHER = "owner_is_other"
        const val KEY_OWNER_NAME = "owner_name"
        const val KEY_OWNER_LAST_NAMES = "owner_last_names"
        const val KEY_OWNER_DNI = "owner_dni"
        const val KEY_OWNER_ADDRESS = "owner_address"
        const val KEY_OWNER_PHONE = "owner_phone"
    }
}
