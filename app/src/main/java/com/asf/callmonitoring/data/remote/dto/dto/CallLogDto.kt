package com.asf.callmonitoring.data.remote.dto

data class CallLogDto(
    val usuario_id: Int? = null,
    val usuario_nombre: String? = null,
    val numero: String,
    val tipo: String,
    val duracion: Long,
    val fecha_hora: String
)