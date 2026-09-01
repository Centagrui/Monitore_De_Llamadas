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
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "==> Evento recibido en BroadcastReceiver: ${intent.action}")

        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val estadoActual = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            Log.d(TAG, "Estado detectado: $estadoActual (Último estado: $ultimoEstado, Previa: $huboLlamadaPrevia)")

            if (estadoActual == ultimoEstado) {
                return
            }

            when (estadoActual) {
                TelephonyManager.EXTRA_STATE_RINGING,
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    Log.d(TAG, "Llamada iniciada/timbrando. Marcando huboLlamadaPrevia = true")
                    huboLlamadaPrevia = true
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (huboLlamadaPrevia) {
                        Log.d(TAG, "Llamada finalizada (IDLE). Procesando envío...")
                        huboLlamadaPrevia = false
                        procesarLlamadaTerminada(context.applicationContext)
                    } else {
                        Log.d(TAG, "Estado IDLE ignorado (no hubo llamada previa)")
                    }
                }
            }

            ultimoEstado = estadoActual
        }
    }

    private fun procesarLlamadaTerminada(appContext: Context) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Esperando 2 segundos para sincronización con CallLog...")
                delay(2000)

                val llamada = CallLogHelper.obtenerUltimaLlamada(appContext)
                Log.d(TAG, "Resultado de CallLogHelper: $llamada")

                if (llamada != null) {
                    val db = AppDatabase.getDatabase(appContext)
                    val repository = LlamadaRepository(db.llamadaDao(), RetrofitClient.apiService)
                    Log.d(TAG, "Llamando a repository.procesarNuevaLlamada...")
                    repository.procesarNuevaLlamada(llamada)
                    Log.d(TAG, "Proceso completado exitosamente.")
                } else {
                    Log.e(TAG, "ERROR: CallLogHelper devolvió null. Revisa permisos de READ_CALL_LOG.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "EXCEPCIÓN al procesar llamada: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}