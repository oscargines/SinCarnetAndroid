package com.oscar.sincarnet.domain.model

data class ManifestacionData(
    val renunciaAsistenciaLetrada: Boolean? = null,
    val deseaDeclarar: Boolean? = null,
    val respuestasPreguntas: Map<Int, String> = emptyMap()
)