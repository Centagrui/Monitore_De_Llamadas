package com.asf.callmonitoring.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter // <-- 1. IMPORTANTE: Añadir este import
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.telephony.TelephonyManager // <-- 2. IMPORTANTE: Añadir este import
import android.util.Log
import androidx.core.app.NotificationCompat
import com.asf.callmonitoring.receiver.PhoneCallReceiver // <-- 3. IMPORTANTE: Importar tu receptor

class CallMonitoringService : Service() {

    // Instancia del receptor para controlarlo dinámicamente
    private var callReceiver: PhoneCallReceiver? = null

    companion object {
        private const val TAG = "DEBUG_SERVICE" // Etiqueta de depuración
        private const val CHANNEL_ID = "asf_monitoring_channel"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, CallMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CallMonitoringService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        startForeground(NOTIFICATION_ID, crearNotificacion())

        // --- REGISTRO DINÁMICO CRUCIAL ---
        // Esto le avisa a Android/HyperOS que este servicio activo quiere escuchar llamadas
        try {
            callReceiver = PhoneCallReceiver()
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            registerReceiver(callReceiver, filter)
            Log.d(TAG, "PhoneCallReceiver registrado dinámicamente con éxito.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar dinámicamente el PhoneCallReceiver: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desregistramos el receiver para evitar fugas de memoria si se destruye el servicio
        try {
            callReceiver?.let {
                unregisterReceiver(it)
                Log.d(TAG, "PhoneCallReceiver desregistrado correctamente.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar receiver: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        val restartServiceIntent = Intent(applicationContext, CallMonitoringService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sincronización del Sistema",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitoreo continuo en segundo plano"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun crearNotificacion(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Servicio activo")
            .setContentText("Sincronizando información")
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }
}