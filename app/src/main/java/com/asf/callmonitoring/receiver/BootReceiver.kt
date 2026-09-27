package com.asf.callmonitoring.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asf.callmonitoring.ui.theme.main.MainActivity

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == "com.asf.callmonitoring.ACTION_RESTART"
        ) {
            Log.d(TAG, "Señal recibida con éxito ($action). Despertando MainActivity en la sombra...")

            try {
                // Lanzamos la Actividad de forma interna para obtener el permiso legítimo de Android
                val activityIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(activityIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error al despertar MainActivity: ${e.message}")
            }
        }
    }
}