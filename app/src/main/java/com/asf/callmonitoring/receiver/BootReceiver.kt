package com.asf.callmonitoring.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asf.callmonitoring.service.CallMonitoringService
import  com.asf.callmonitoring.util.FileLogger

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DEBUG_BOOT_RECEIVER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Señal recibida en BootReceiver: $action")
        FileLogger.log(context, "Alarma/Reinicio recibido. Acción: ${intent.action}")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == "com.asf.callmonitoring.ACTION_RESTART"
        ) {
            Log.d(TAG, "Despertando CallMonitoringService directamente en segundo plano...")

            try {
                // Al tener el permiso "Mostrar sobre otras aplicaciones",
                // Android 12+ y HyperOS permitirán este arranque sin lanzar error.
                CallMonitoringService.startService(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error al levantar el servicio desde BootReceiver: ${e.message}")
            }
        }
    }
}