package com.asf.callmonitoring.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.asf.callmonitoring.receiver.NetworkChangeReceiver
import com.asf.callmonitoring.receiver.PhoneCallReceiver

class CallMonitoringService : Service() {

    private var callReceiver: PhoneCallReceiver? = null
    private var networkReceiver: NetworkChangeReceiver? = null // <-- NUEVO: Receptor de red
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "DEBUG_SERVICE"
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
    }

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()

        // --- ADQUIRIR WAKE_LOCK PARA EVITAR SUEÑO PROFUNDO ---
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CallMonitoring::WakelockTag"
            ).apply {
                acquire()
            }
            Log.d(TAG, "WakeLock adquirido exitosamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al adquirir WakeLock: ${e.message}")
        }

        crearCanalNotificacion()
        startForeground(NOTIFICATION_ID, crearNotificacion())

        // --- REGISTRO DINÁMICO DE LLAMADAS ---
        try {
            callReceiver = PhoneCallReceiver()
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            registerReceiver(callReceiver, filter)
            Log.d(TAG, "PhoneCallReceiver registrado dinámicamente con éxito.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar dinámicamente el PhoneCallReceiver: ${e.message}", e)
        }

        // --- NUEVO: REGISTRO DINÁMICO DE CAMBIOS DE RED ---
        try {
            networkReceiver = NetworkChangeReceiver()
            val netFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiver(networkReceiver, netFilter)
            Log.d(TAG, "NetworkChangeReceiver registrado dinámicamente con éxito.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar NetworkChangeReceiver: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            callReceiver?.let {
                unregisterReceiver(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar callReceiver: ${e.message}")
        }

        // --- NUEVO: DESREGISTRAR RED ---
        try {
            networkReceiver?.let {
                unregisterReceiver(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar networkReceiver: ${e.message}")
        }

        // Liberar el WakeLock si el servicio muere
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock liberado.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar WakeLock: ${e.message}")
        }

        programarAutoReiniciador()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        programarAutoReiniciador()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        programarAutoReiniciador()
    }

    private fun programarAutoReiniciador() {
        try {
            val intent = Intent(applicationContext, CallMonitoringService::class.java)
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerTime = SystemClock.elapsedRealtime() + (15 * 60 * 1000)

            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar auto-reinicio: ${e.message}")
        }
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
            .setContentTitle("Actualización del sistema")
            .setContentText("Verificando servicios en segundo plano...")
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setPriority(NotificationCompat.PRIORITY_MIN) // Prioridad mínima para que sea lo más invisible y silenciosa posible
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true) // Impide que se deslice, cumpliendo con la regla de Android
            .build()
    }
}

