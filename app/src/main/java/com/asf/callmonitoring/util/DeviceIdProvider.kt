package com.asf.callmonitoring.util

import android.content.Context
import android.os.Build
import com.asf.callmonitoring.data.UserPreferences

object DeviceIdProvider {

    fun getDeviceId(context: Context): String {
        val userPrefs = UserPreferences(context)
        val nombreConfigurado = userPrefs.obtenerNombreDispositivo()

        // 1. Si el usuario guardó un nombre en la app, usar ese
        if (nombreConfigurado.isNotBlank()) {
            return nombreConfigurado
        }

        // 2. Si no ha guardado nada, usar el modelo del teléfono
        return Build.MODEL ?: "Dispositivo ASF"
    }
}