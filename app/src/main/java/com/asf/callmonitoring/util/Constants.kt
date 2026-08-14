package com.asf.callmonitoring.util

object Constants {

    // Tipos de llamada
    const val TIPO_SALIENTE = "SALIENTE"
    const val TIPO_ENTRANTE = "ENTRANTE"
    const val TIPO_PERDIDA = "PERDIDA"

    // Estados clasificados de la llamada
    const val ESTADO_CONTESTADA = "CONTESTADA"
    const val ESTADO_NO_CONTESTADA = "NO CONTESTADA"
    const val ESTADO_POSIBLE_BUZON = "POSIBLE BUZON"

    // Umbral en segundos para considerar posible buzón (por ejemplo, llamadas de 1 a 5 seg)
    const val UMBRAL_BUZON_SEGUNDOS = 5L

    // Nombre de la tarea periódica de WorkManager
    const val WORK_SYNC_NAME = "asf_sync_calls_worker"
}