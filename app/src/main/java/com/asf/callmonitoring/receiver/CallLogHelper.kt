package com.asf.callmonitoring.receiver

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import com.asf.callmonitoring.data.local.entity.LlamadaEntity
import com.asf.callmonitoring.util.CallClassifier
import com.asf.callmonitoring.util.DeviceIdProvider

object CallLogHelper {

    private const val TAG = "DEBUG_CALL_LOG_HELPER"

    /**
     * Consulta el registro de llamadas más reciente del sistema Android.
     */
    @SuppressLint("Range")
    fun obtenerUltimaLlamada(context: Context): LlamadaEntity? {
        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val fecha = it.getLong(it.getColumnIndex(CallLog.Calls.DATE))
                val ahora = System.currentTimeMillis()

                if (ahora - fecha > 25000) {
                    Log.w(TAG, "La última llamada en CallLog es antigua (${(ahora - fecha) / 1000}s atrás). Se ignora.")
                    return null
                }

                val numero = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER)) ?: "Desconocido"
                val tipo = it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))
                val duracion = it.getLong(it.getColumnIndex(CallLog.Calls.DURATION))

                // Nombre del contacto
                val nombreContacto = obtenerNombreContacto(context, numero)

                // Clasificación corregida
                val (tipoLlamadaStr, estadoLlamadaStr) = CallClassifier.clasificarLlamada(tipo, duracion)
                val deviceId = DeviceIdProvider.getDeviceId(context)

                Log.d(TAG, "Llamada detectada -> Tipo: $tipoLlamadaStr, Estado: $estadoLlamadaStr, Duración: ${duracion}s")

                return LlamadaEntity(
                    numeroTelefono = numero,
                    nombreContacto = nombreContacto,
                    tipoLlamada = tipoLlamadaStr,
                    fechaHora = fecha,
                    duracionSegundos = duracion,
                    estadoLlamada = estadoLlamadaStr,
                    idDispositivo = deviceId,
                    sincronizado = false
                )
            }
        }
        return null
    }

    @SuppressLint("Range")
    private fun obtenerNombreContacto(context: Context, numero: String): String? {
        if (numero.isBlank() || numero == "Desconocido") return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(numero)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}