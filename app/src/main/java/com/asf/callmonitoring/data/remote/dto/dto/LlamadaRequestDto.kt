package com.asf.callmonitoring.data.remote.dto

data class LlamadaRequestDto(
    val numeroTelefono: String,
    val nombreContacto: String?,
    val tipoLlamada: String,
    val fechaHora: Long,
    val duracionSegundos: Long,
    val estadoLlamada: String,
    val idDispositivo: String
)