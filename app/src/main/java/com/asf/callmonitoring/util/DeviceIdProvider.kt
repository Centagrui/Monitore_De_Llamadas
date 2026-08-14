package com.asf.callmonitoring.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

object DeviceIdProvider {

    /**
     * Obtiene el identificador único de hardware/software del dispositivo (Android ID).
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "DESCONOCIDO"
    }
}