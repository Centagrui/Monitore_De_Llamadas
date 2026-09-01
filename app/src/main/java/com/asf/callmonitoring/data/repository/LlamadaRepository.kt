package com.asf.callmonitoring.data.repository

import android.util.Log
import com.asf.callmonitoring.data.local.dao.LlamadaDao
import com.asf.callmonitoring.data.local.entity.LlamadaEntity
import com.asf.callmonitoring.data.remote.LlamadasApiService
import com.asf.callmonitoring.data.remote.dto.LlamadaRequestDto
import kotlinx.coroutines.flow.Flow

class LlamadaRepository(
    private val llamadaDao: LlamadaDao,
    private val apiService: LlamadasApiService
) {

    companion object {
        private const val TAG = "DEBUG_REPO"
    }

    val todasLasLlamadas: Flow<List<LlamadaEntity>> = llamadaDao.obtenerTodasLasLlamadas()

    suspend fun procesarNuevaLlamada(llamada: LlamadaEntity): Boolean {
        // 1. Guardar primero en la base de datos local (Room)
        val idGenerado = llamadaDao.insertarLlamada(llamada)
        Log.d(TAG, "Llamada guardada en Room local con ID: $idGenerado")

        // 2. Mapear la entidad al DTO que espera el servidor
        val dto = LlamadaRequestDto(
            numeroTelefono = llamada.numeroTelefono,
            nombreContacto = llamada.nombreContacto,
            tipoLlamada = llamada.tipoLlamada,
            fechaHora = llamada.fechaHora,
            duracionSegundos = llamada.duracionSegundos,
            estadoLlamada = llamada.estadoLlamada,
            idDispositivo = llamada.idDispositivo
        )

        // 3. Intentar subir al servidor Node.js
        return try {
            Log.d(TAG, "Enviando DTO a Node.js: $dto")
            val respuesta = apiService.registrarLlamada(dto)

            if (respuesta.isSuccessful) {
                Log.d(TAG, "ÉXITO en el servidor: Código ${respuesta.code()} Body: ${respuesta.body()}")
                llamadaDao.marcarComoSincronizada(idGenerado)
                true
            } else {
                val errorBody = respuesta.errorBody()?.string()
                Log.e(TAG, "ERROR HTTP del servidor: Código ${respuesta.code()} - Detalle: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "FALLO DE RED/CONEXIÓN al enviar llamada: ${e.message}", e)
            false
        }
    }

    suspend fun sincronizarPendientes(): Int {
        val pendientes = llamadaDao.obtenerLlamadasPendientes()
        var sincronizadas = 0
        Log.d(TAG, "Iniciando sincronización de ${pendientes.size} llamadas pendientes...")

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
                } else {
                    Log.w(TAG, "Fallo al sincronizar llamada ID ${llamada.id}: Código ${respuesta.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Interrupción en sincronización: ${e.message}")
                break
            }
        }
        Log.d(TAG, "Sincronización finalizada. Total sincronizadas: $sincronizadas")
        return sincronizadas
    }


}