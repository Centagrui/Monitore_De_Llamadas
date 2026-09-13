package com.asf.callmonitoring.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.asf.callmonitoring.data.local.AppDatabase
import com.asf.callmonitoring.data.remote.RetrofitClient
import com.asf.callmonitoring.data.repository.LlamadaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhoneCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DEBUG_CALL_RECEIVER"
        private var ultimoEstado: String = TelephonyManager.EXTRA_STATE_IDLE
        private var huboLlamadaPrevia: Boolean = false

        // CANDADO 1: Evita que dos eventos IDLE en menos de 3 segundos se procesen doble
        private var ultimoTimestampProcesado: Long = 0L

        // CANDADO 2: Guarda el identificador único o fecha de la última llamada subida
        private var ultimaFechaLlamadaRegistrada: Long = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val estadoActual = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        Log.d(TAG, "Estado detectado: $estadoActual (Previo: $ultimoEstado, HuboPrevia: $huboLlamadaPrevia)")

        when (estadoActual) {
            TelephonyManager.EXTRA_STATE_RINGING,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Se inició la llamada (entrante o saliente)
                huboLlamadaPrevia = true
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val ahora = System.currentTimeMillis()

                // Si hubo llamada y no se ha disparado este bloque en los últimos 3.5 segundos
                if (huboLlamadaPrevia && (ahora - ultimoTimestampProcesado > 3500)) {
                    Log.d(TAG, "Llamada finalizada válida (IDLE). Procesando envío...")
                    huboLlamadaPrevia = false
                    ultimoTimestampProcesado = ahora
                    procesarLlamadaTerminada(context.applicationContext)
                } else {
                    Log.d(TAG, "IDLE descartado (Duplicado o sin llamada previa)")
                }
            }
        }

        ultimoEstado = estadoActual
    }

    private fun procesarLlamadaTerminada(appContext: Context) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Tiempo para que Android termine de escribir el registro final en CallLog
                delay(2500)

                val llamada = CallLogHelper.obtenerUltimaLlamada(appContext)
                Log.d(TAG, "Resultado de CallLogHelper: $llamada")

                if (llamada != null) {
                    // Si tu entidad o modelo llamada tiene fecha_hora como timestamp (o campo único)
                    // Validamos que no sea la misma que acabamos de enviar hace unos segundos
                    // Por esto:
                    val fechaLlamadaMillis = llamada.fechaHora
                    if (fechaLlamadaMillis == ultimaFechaLlamadaRegistrada) {
                        Log.w(TAG, "Llamada duplicada detectada en CallLog. Omitiendo envío.")
                        return@launch
                    }
                    ultimaFechaLlamadaRegistrada = fechaLlamadaMillis

                    val db = AppDatabase.getDatabase(appContext)
                    val repository = LlamadaRepository(db.llamadaDao(), RetrofitClient.apiService)
                    Log.d(TAG, "Enviando llamada única a repository...")
                    repository.procesarNuevaLlamada(llamada)
                    Log.d(TAG, "Llamada procesada exitosamente.")
                } else {
                    Log.e(TAG, "ERROR: CallLogHelper devolvió null.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "EXCEPCIÓN al procesar llamada: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}