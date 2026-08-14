package com.asf.callmonitoring.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.asf.callmonitoring.data.local.AppDatabase
import com.asf.callmonitoring.data.remote.RetrofitClient
import com.asf.callmonitoring.data.repository.LlamadaRepository

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = LlamadaRepository(db.llamadaDao(), RetrofitClient.apiService)

            // Intenta subir todas las llamadas locales con sincronizado = false
            repository.sincronizarPendientes()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}