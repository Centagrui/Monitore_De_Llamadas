package com.asf.callmonitoring.receiver

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import com.asf.callmonitoring.data.local.entity.LlamadaEntity
import com.asf.callmonitoring.util.CallClassifier
import com.asf.callmonitoring.util.DeviceIdProvider

object CallLogHelper {

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
                val numero = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER)) ?: "Desconocido"
                val tipo = it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))
                val fecha = it.getLong(it.getColumnIndex(CallLog.Calls.DATE))
                val duracion = it.getLong(it.getColumnIndex(CallLog.Calls.DURATION))

                // Buscar el nombre del contacto en la agenda telefónica
                val nombreContacto = obtenerNombreContacto(context, numero)

                // Clasificar la llamada usando nuestro utilitario
                val (tipoLlamadaStr, estadoLlamadaStr) = CallClassifier.clasificarLlamada(tipo, duracion)
                val deviceId = DeviceIdProvider.getDeviceId(context)

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

    /**
     * Busca en la agenda de contactos de Android si el número tiene un nombre guardado.
     */
    @SuppressLint("Range")
    private fun obtenerNombreContacto(context: Context, numero: String): String? {
        if (numero.isBlank()) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(numero)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
                }
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }
}