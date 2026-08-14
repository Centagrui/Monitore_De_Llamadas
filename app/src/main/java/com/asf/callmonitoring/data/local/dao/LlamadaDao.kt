package com.asf.callmonitoring.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asf.callmonitoring.data.local.entity.LlamadaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LlamadaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLlamada(llamada: LlamadaEntity): Long

    @Query("SELECT * FROM llamadas_locales ORDER BY fechaHora DESC")
    fun obtenerTodasLasLlamadas(): Flow<List<LlamadaEntity>>

    @Query("SELECT * FROM llamadas_locales WHERE sincronizado = 0")
    suspend fun obtenerLlamadasPendientes(): List<LlamadaEntity>

    @Query("UPDATE llamadas_locales SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Long)
}