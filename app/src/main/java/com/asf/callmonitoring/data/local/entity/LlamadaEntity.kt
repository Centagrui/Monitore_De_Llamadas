package com.asf.callmonitoring.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llamadas_locales")
data class LlamadaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numeroTelefono: String,
    val nombreContacto: String?,
    val tipoLlamada: String,
    val fechaHora: Long,
    val duracionSegundos: Long,
    val estadoLlamada: String,
    val idDispositivo: String,
    val sincronizado: Boolean = false
)