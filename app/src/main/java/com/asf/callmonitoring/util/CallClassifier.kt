package com.asf.callmonitoring.util

import android.provider.CallLog

object CallClassifier {

    /**
     * Clasifica el tipo de llamada (Saliente, Entrante, Perdida)
     * y su estado (Contestada, No contestada, Posible buzón)
     */
    fun clasificarLlamada(tipoCallLog: Int, duracionSegundos: Long): Pair<String, String> {
        val tipoLlamada = when (tipoCallLog) {
            CallLog.Calls.INCOMING_TYPE -> Constants.TIPO_ENTRANTE
            CallLog.Calls.OUTGOING_TYPE -> Constants.TIPO_SALIENTE
            CallLog.Calls.MISSED_TYPE -> Constants.TIPO_PERDIDA
            CallLog.Calls.REJECTED_TYPE -> Constants.TIPO_PERDIDA
            else -> "DESCONOCIDO" // Evita asumir saliente por error si el CallLog aún no resuelve el tipo
        }

        val estadoLlamada = when {
            tipoCallLog == CallLog.Calls.MISSED_TYPE -> Constants.ESTADO_NO_CONTESTADA
            tipoCallLog == CallLog.Calls.REJECTED_TYPE -> Constants.ESTADO_NO_CONTESTADA
            duracionSegundos == 0L -> Constants.ESTADO_NO_CONTESTADA
            duracionSegundos in 1..Constants.UMBRAL_BUZON_SEGUNDOS -> Constants.ESTADO_POSIBLE_BUZON
            else -> Constants.ESTADO_CONTESTADA
        }

        return Pair(tipoLlamada, estadoLlamada)
    }
}