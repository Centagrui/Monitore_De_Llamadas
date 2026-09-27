package com.asf.callmonitoring.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class CallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.asf.callmonitoring.ACTION_RESTART") {
            Log.d("DEBUG_BROADCAST", "¡Se recibió la orden de MacroDroid! Despertando servicio...")
            try {
                val serviceIntent = Intent(context, CallMonitoringService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("DEBUG_BROADCAST", "Error al arrancar servicio desde Broadcast: ${e.message}")
            }
        }
    }
}