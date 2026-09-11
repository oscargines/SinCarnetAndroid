package com.oscar.sincarnet.domain.model

data class JuzgadoComunidadAutonoma(
    val id: Int,
    val nombre: String
)

data class JuzgadoProvincia(
    val id: Int,
    val nombre: String
)

data class JuzgadoMunicipio(
    val nombre: String
)

data class JuzgadoSede(
    val id: Int,
    val nombre: String,
    val direccion: String?,
    val telefono: String?,
    val codigoPostal: String?
)