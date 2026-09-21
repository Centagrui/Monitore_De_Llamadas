package com.asf.callmonitoring.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.asf.callmonitoring.data.local.AppDatabase
import com.asf.callmonitoring.data.remote.RetrofitClient
import com.asf.callmonitoring.data.repository.LlamadaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DEBUG_NETWORK"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (tieneInternet(context)) {
            Log.d(TAG, "¡Conexión a internet recuperada! Sincronizando llamadas pendientes de Room...")

            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(appContext)
                    val repository = LlamadaRepository(db.llamadaDao(), RetrofitClient.apiService)
                    val totalSincronizadas = repository.sincronizarPendientes()
                    Log.d(TAG, "Sincronización masiva completada. Total enviadas al servidor: $totalSincronizadas")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al sincronizar pendientes por cambio de red: ${e.message}")
                }
            }
        }
    }

    private fun tieneInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}