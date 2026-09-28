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
    private var networkReceiver: NetworkChangeReceiver? = null
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

        // 1. Crear el canal de notificación primero (obligatorio para Android 8+)
        crearCanalNotificacion()

        // 2. Lanzar el Foreground Service con su notificación visible de inmediato
        try {
            startForeground(NOTIFICATION_ID, crearNotificacion())
            Log.d(TAG, "Notificación de primer plano iniciada correctamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar startForeground: ${e.message}")
        }

        // 3. Adquirir WakeLock para evitar sueño profundo
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

        // 4. Registrar receptor dinámico de llamadas
        try {
            callReceiver = PhoneCallReceiver()
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            registerReceiver(callReceiver, filter)
            Log.d(TAG, "PhoneCallReceiver registrado dinámicamente con éxito.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar dinámicamente el PhoneCallReceiver: ${e.message}", e)
        }

        // 5. Registrar receptor dinámico de cambios de red
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
            callReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar callReceiver: ${e.message}")
        }

        try {
            networkReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar networkReceiver: ${e.message}")
        }

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
            // 1. Redirigimos la alarma hacia tu BootReceiver, NO directamente al servicio
            val intent = Intent(applicationContext, com.asf.callmonitoring.receiver.BootReceiver::class.java).apply {
                action = "com.asf.callmonitoring.ACTION_RESTART"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // 2. Reducimos el tiempo a 2 minutos para cerrar el "punto ciego"
// Modifica únicamente esta línea dentro de programarAutoReiniciador():
            val triggerTime = SystemClock.elapsedRealtime() + (10 * 60 * 1000)
            // 3. Forzamos a HyperOS a despertar el proceso incluso en sueño profundo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Auto-reinicio programado exitosamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar auto-reinicio: ${e.message}")
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoreo del Sistema ASF",
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH obliga a Android a mostrarla visualmente
            ).apply {
                description = "Servicio activo de sincronización"
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun crearNotificacion(): Notification {
        // Creamos un Intent para que al tocar la notificación (si el usuario llega a verla) abra la app o MainActivity
        val intent = Intent(this, com.asf.callmonitoring.ui.theme.main.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Actualización del sistema")
            .setContentText("Verificando servicios en segundo plano...")
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta para asegurar visibilidad
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Impide que el usuario la deslice
            .setAutoCancel(false)
            .build()
    }
}