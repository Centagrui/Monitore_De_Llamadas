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
    private const val PREFS_NAME = "CallSyncPrefs"
    private const val LAST_SYNC_KEY = "last_sync_time"

    @SuppressLint("Range")
    fun obtenerLlamadasNuevas(context: Context): List<LlamadaEntity> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var lastSync = prefs.getLong(LAST_SYNC_KEY, 0L)
        val now = System.currentTimeMillis()
        val listaLlamadas = mutableListOf<LlamadaEntity>()

        // Si es la primera vez que corre (instalación), empieza desde AHORA
        // Si es la primera vez que corre (instalación)
        if (lastSync == 0L) {
            // Retrocedemos 5 minutos en el tiempo para asegurarnos de atrapar la llamada que acaba de ocurrir
            lastSync = now - (5 * 60 * 1000)
            prefs.edit().putLong(LAST_SYNC_KEY, lastSync).apply()
            Log.d(TAG, "Primera instalación: lastSync configurado hace 5 min. Atrapando la primera llamada.")

            // ¡ELIMINAMOS el "return listaLlamadas" que estaba aquí!
            // Ahora dejamos que el código siga hacia abajo para que procese esta primera llamada.
        }

        val selection = "${CallLog.Calls.DATE} > ?"
        val selectionArgs = arrayOf(lastSync.toString())

        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            selection,
            selectionArgs,
            "${CallLog.Calls.DATE} ASC" // ASC = De la más antigua a la más nueva
        )

        var nuevoLastSync = lastSync

        cursor?.use {
            while (it.moveToNext()) {
                val fecha = it.getLong(it.getColumnIndex(CallLog.Calls.DATE))

                // 1. Mantenemos la línea de tiempo avanzando, incluso si ignoramos la llamada
                if (fecha > nuevoLastSync) {
                    nuevoLastSync = fecha
                }

                val tipo = it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))

                // 2. EL FILTRO ESTRICTO: Si la llamada NO es saliente (OUTGOING_TYPE), la ignoramos por completo
                if (tipo != CallLog.Calls.OUTGOING_TYPE) {
                    Log.d(TAG, "Llamada ignorada: No es saliente (Tipo en Android: $tipo)")
                    continue // Salta al siguiente registro sin guardar nada
                }

                val numero = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER)) ?: "Desconocido"
                val duracion = it.getLong(it.getColumnIndex(CallLog.Calls.DURATION))

                val nombreContacto = obtenerNombreContacto(context, numero)
                val (tipoLlamadaStr, estadoLlamadaStr) = CallClassifier.clasificarLlamada(tipo, duracion)
                val deviceId = DeviceIdProvider.getDeviceId(context)

                listaLlamadas.add(
                    LlamadaEntity(
                        numeroTelefono = numero,
                        nombreContacto = nombreContacto,
                        tipoLlamada = tipoLlamadaStr,
                        fechaHora = fecha,
                        duracionSegundos = duracion,
                        estadoLlamada = estadoLlamadaStr,
                        idDispositivo = deviceId,
                        sincronizado = false
                    )
                )
            }
        }

        // Actualizamos la marca de tiempo global al final
        if (nuevoLastSync > lastSync) {
            prefs.edit().putLong(LAST_SYNC_KEY, nuevoLastSync).apply()
            Log.d(TAG, "Última sincronización actualizada a: $nuevoLastSync. Llamadas SALIENTES nuevas procesadas: ${listaLlamadas.size}")
        }

        return listaLlamadas
    }

    @SuppressLint("Range")
    private fun obtenerNombreContacto(context: Context, numero: String): String? {
        if (numero.isBlank() || numero == "Desconocido") return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numero))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)) else null
            }
        } catch (e: Exception) { null }
    }
}