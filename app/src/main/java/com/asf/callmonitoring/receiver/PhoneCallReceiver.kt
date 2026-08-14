package com.asf.callmonitoring.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.asf.callmonitoring.data.local.AppDatabase
import com.asf.callmonitoring.data.remote.RetrofitClient
import com.asf.callmonitoring.data.repository.LlamadaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhoneCallReceiver : BroadcastReceiver() {

    companion object {
        private var ultimoEstado: String = TelephonyManager.EXTRA_STATE_IDLE
        private var huboLlamadaPrevia: Boolean = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val estadoActual = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

            if (estadoActual == ultimoEstado) {
                return
            }

            when (estadoActual) {
                TelephonyManager.EXTRA_STATE_RINGING,
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    huboLlamadaPrevia = true
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (huboLlamadaPrevia) {
                        huboLlamadaPrevia = false
                        procesarLlamadaTerminada(context.applicationContext)
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
                // Pequeña espera para que el sistema operativo Android termine de escribir en el CallLog
                delay(1500)

                val llamada = CallLogHelper.obtenerUltimaLlamada(appContext)
                if (llamada != null) {
                    val db = AppDatabase.getDatabase(appContext)
                    val repository = LlamadaRepository(db.llamadaDao(), RetrofitClient.apiService)
                    repository.procesarNuevaLlamada(llamada)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}