package com.asf.callmonitoring.data.repository

import android.os.Build
import android.util.Log
import com.asf.callmonitoring.data.local.dao.LlamadaDao
import com.asf.callmonitoring.data.local.entity.LlamadaEntity
import com.asf.callmonitoring.data.remote.LlamadasApiService
import com.asf.callmonitoring.data.remote.dto.LlamadaRequestDto
import kotlinx.coroutines.flow.Flow
import com.asf.callmonitoring.data.UserPreferences


class LlamadaRepository(
    private val llamadaDao: LlamadaDao,
    private val apiService: LlamadasApiService,
    private val userPreferences: UserPreferences? = null
) {

    companion object {
        private const val TAG = "DEBUG_REPO"
    }

    val todasLasLlamadas: Flow<List<LlamadaEntity>> = llamadaDao.obtenerTodasLasLlamadas()

    // --- FUNCIÓN AUXILIAR PARA OBTENER EL NOMBRE SIN ERRORES DE TIPO ---
    private fun obtenerNombreSeguro(idEntidad: String): String {
        val nombrePref = userPreferences?.obtenerNombreDispositivo() ?: ""
        val modeloTelefono = Build.MODEL ?: "Desconocido"

        return when {
            nombrePref.isNotEmpty() -> nombrePref
            idEntidad.isNotEmpty() -> idEntidad
            else -> modeloTelefono.toString()
        }
    }

    suspend fun procesarNuevaLlamada(llamada: LlamadaEntity): Boolean {
        // 1. Obtener el nombre seguro usando la función auxiliar
        val nombreDispositivo = obtenerNombreSeguro(llamada.idDispositivo)

        // 2. Guardar en Room con el nombre actualizado
        val entidadAGuardar = llamada.copy(idDispositivo = nombreDispositivo)
        val idGenerado = llamadaDao.insertarLlamada(entidadAGuardar)
        Log.d(TAG, "Llamada guardada en Room local con ID: $idGenerado")

        // 3. Mapear la entidad al DTO que espera el servidor
        val dto = LlamadaRequestDto(
            numeroTelefono = entidadAGuardar.numeroTelefono,
            nombreContacto = entidadAGuardar.nombreContacto,
            tipoLlamada = entidadAGuardar.tipoLlamada,
            fechaHora = entidadAGuardar.fechaHora,
            duracionSegundos = entidadAGuardar.duracionSegundos,
            estadoLlamada = entidadAGuardar.estadoLlamada,
            idDispositivo = nombreDispositivo
        )

        // 4. Intentar subir al servidor Node.js
        return try {
            Log.d(TAG, "Enviando DTO a Node.js: $dto")
            val respuesta = apiService.registrarLlamada(dto)

            if (respuesta.isSuccessful) {
                Log.d(TAG, "ÉXITO en el servidor: Código ${respuesta.code()}")
                llamadaDao.marcarComoSincronizada(idGenerado)
                true
            } else {
                Log.e(TAG, "ERROR HTTP del servidor: Código ${respuesta.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "FALLO DE RED/CONEXIÓN al enviar llamada: ${e.message}")
            false
        }
    }

    suspend fun sincronizarPendientes(): Int {
        val pendientes = llamadaDao.obtenerLlamadasPendientes()
        var sincronizadas = 0
        Log.d(TAG, "Iniciando sincronización de ${pendientes.size} llamadas pendientes...")

        for (llamada in pendientes) {
            try {
                // Obtener el nombre seguro para las pendientes también
                val nombreDispositivo = obtenerNombreSeguro(llamada.idDispositivo)

                val dto = LlamadaRequestDto(
                    numeroTelefono = llamada.numeroTelefono,
                    nombreContacto = llamada.nombreContacto,
                    tipoLlamada = llamada.tipoLlamada,
                    fechaHora = llamada.fechaHora,
                    duracionSegundos = llamada.duracionSegundos,
                    estadoLlamada = llamada.estadoLlamada,
                    idDispositivo = nombreDispositivo
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