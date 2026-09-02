package com.asf.callmonitoring.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("asf_monitoring_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "USER_ID"
        private const val KEY_USER_NAME = "USER_NAME"
        private const val KEY_NOMBRE_DISPOSITIVO = "key_nombre_dispositivo"
    }

    // Guarda el usuario seleccionado en la app
    fun saveUser(id: Int, nombre: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, nombre.trim())
            .putString(KEY_NOMBRE_DISPOSITIVO, nombre.trim())
            .apply()
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun isUserConfigured(): Boolean = getUserId() != -1

    // Guarda el nombre del dispositivo manualmente
    fun guardarNombreDispositivo(nombre: String) {
        prefs.edit().putString(KEY_NOMBRE_DISPOSITIVO, nombre.trim()).apply()
    }

    // Método que llama LlamadaRepository
    fun obtenerNombreDispositivo(): String {
        return prefs.getString(KEY_NOMBRE_DISPOSITIVO, null)
            ?: prefs.getString(KEY_USER_NAME, "")
            ?: ""
    }
}