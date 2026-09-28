package com.asf.callmonitoring.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager // <-- Importación movida correctamente a la cabecera
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
        private var ultimoTimestampProcesado: Long = 0L
        private var ultimaFechaLlamadaRegistrada: Long = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DEBUG_BROADCAST", "¡BroadcastReceiver disparado! Acción: ${intent.action}")
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val estadoActual = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        Log.d(TAG, "Estado detectado: $estadoActual (Previo: $ultimoEstado, HuboPrevia: $huboLlamadaPrevia)")

        when (estadoActual) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Se detecta que está sonando (entrante).
                // Marcamos como llamada previa para capturar si se convierte en perdida o si se contesta.
                huboLlamadaPrevia = true
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Llamada en curso (contestada o saliente)
                huboLlamadaPrevia = true
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val ahora = System.currentTimeMillis()

                // Si hubo un timbre o llamada activa previa y pasaron los 3.5 segundos de seguridad
                if (huboLlamadaPrevia && (ahora - ultimoTimestampProcesado > 3500)) {
                    Log.d(TAG, "Llamada finalizada o perdida válida (IDLE). Procesando envío...")
                    huboLlamadaPrevia = false
                    ultimoTimestampProcesado = ahora
                    procesarLlamadaTerminada(context.applicationContext)
                } else {
                    Log.d(TAG, "IDLE descartado (Duplicado o sin evento previo)")
                }
            }
        }
        ultimoEstado = estadoActual
    }

    private fun procesarLlamadaTerminada(appContext: Context) {
        // 1. Escudo para evitar que Android mate el Receiver
        val pendingResult = goAsync()

        // 2. WakeLock para mantener el procesador despierto
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CallReceiver::ProcesandoLlamada")
        wakeLock.acquire(10 * 1000L) // Inmunidad por 10 segundos máximo

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 3. Margen de seguridad para que Android guarde en la BD oficial
                delay(3500)

                // 4. Pedimos todas las llamadas nuevas a CallLogHelper
                val llamadasNuevas = CallLogHelper.obtenerLlamadasNuevas(appContext)
                Log.d(TAG, "Llamadas encontradas tras el delay: ${llamadasNuevas.size}")

                if (llamadasNuevas.isNotEmpty()) {
                    val db = AppDatabase.getDatabase(appContext)
                    val repository = LlamadaRepository(db.llamadaDao(), RetrofitClient.apiService)

                    // Procesamos y subimos una por una
                    for (llamada in llamadasNuevas) {
                        repository.procesarNuevaLlamada(llamada)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "EXCEPCIÓN al procesar llamada: ${e.message}", e)
            } finally {
                // 5. Liberamos siempre los recursos
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }
}