package com.asf.callmonitoring.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CallLogDto(

    @SerializedName("numeroTelefono") val numeroTelefono: String,
    @SerializedName("nombreContacto") val nombreContacto: String?,
    @SerializedName("tipoLlamada") val tipoLlamada: String,
    @SerializedName("fechaHora") val fechaHora: String,
    @SerializedName("duracionSegundos") val duracionSegundos: Long,
    @SerializedName("estadoLlamada") val estadoLlamada: String,
    @SerializedName("idDispositivo") val idDispositivo: String
)
