package com.asf.callmonitoring.data.repository

import com.asf.callmonitoring.data.local.dao.LlamadaDao
import com.asf.callmonitoring.data.local.entity.LlamadaEntity
import com.asf.callmonitoring.data.remote.LlamadasApiService
import com.asf.callmonitoring.data.remote.dto.LlamadaRequestDto
import kotlinx.coroutines.flow.Flow

class LlamadaRepository(
    private val llamadaDao: LlamadaDao,
    private val apiService: LlamadasApiService
) {

    val todasLasLlamadas: Flow<List<LlamadaEntity>> = llamadaDao.obtenerTodasLasLlamadas()

    suspend fun procesarNuevaLlamada(llamada: LlamadaEntity): Boolean {
        // 1. Guardar primero en la base de datos local (SQLite / Room)
        val idGenerado = llamadaDao.insertarLlamada(llamada)

        // 2. Intentar subirla al servidor vía Retrofit
        return try {
            val dto = LlamadaRequestDto(
                numeroTelefono = llamada.numeroTelefono,
                nombreContacto = llamada.nombreContacto,
                tipoLlamada = llamada.tipoLlamada,
                fechaHora = llamada.fechaHora,
                duracionSegundos = llamada.duracionSegundos,
                estadoLlamada = llamada.estadoLlamada,
                idDispositivo = llamada.idDispositivo
            )

            val respuesta = apiService.registrarLlamada(dto)
            if (respuesta.isSuccessful) {
                // 3. Marcar como sincronizada si el servidor respondió OK (HTTP 200/201)
                llamadaDao.marcarComoSincronizada(idGenerado)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            // Sin conexión o servidor apagado: queda guardada localmente con sincronizado = false
            false
        }
    }

    suspend fun sincronizarPendientes(): Int {
        val pendientes = llamadaDao.obtenerLlamadasPendientes()
        var sincronizadas = 0

        for (llamada in pendientes) {
            try {
                val dto = LlamadaRequestDto(
                    numeroTelefono = llamada.numeroTelefono,
                    nombreContacto = llamada.nombreContacto,
                    tipoLlamada = llamada.tipoLlamada,
                    fechaHora = llamada.fechaHora,
                    duracionSegundos = llamada.duracionSegundos,
                    estadoLlamada = llamada.estadoLlamada,
                    idDispositivo = llamada.idDispositivo
                )
                val respuesta = apiService.registrarLlamada(dto)
                if (respuesta.isSuccessful) {
                    llamadaDao.marcarComoSincronizada(llamada.id)
                    sincronizadas++
                }
            } catch (e: Exception) {
                // Si falla una por falta de red, se detiene el ciclo para reintentar después
                break
            }
        }
        return sincronizadas
    }
}