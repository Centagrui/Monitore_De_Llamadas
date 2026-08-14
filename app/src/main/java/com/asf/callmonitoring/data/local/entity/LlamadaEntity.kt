package com.asf.callmonitoring.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llamadas_locales")
data class LlamadaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numeroTelefono: String,
    val nombreContacto: String?,
    val tipoLlamada: String,          // SALIENTE, ENTRANTE, PERDIDA
    val fechaHora: Long,              // Timestamp en milisegundos
    val duracionSegundos: Long,       // Duración en segundos
    val estadoLlamada: String,        // CONTESTADA, NO CONTESTADA, BUZON
    val idDispositivo: String,        // Android ID del celular
    val sincronizado: Boolean = false // false = pendiente de subir, true = subido al servidor
)