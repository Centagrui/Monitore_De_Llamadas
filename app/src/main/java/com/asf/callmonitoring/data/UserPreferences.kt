package com.asf.callmonitoring.data


import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveUser(id: Int, nombre: String) {
        prefs.edit()
            .putInt("USER_ID", id)
            .putString("USER_NAME", nombre)
            .apply()
    }

    fun getUserId(): Int = prefs.getInt("USER_ID", -1)
    fun getUserName(): String? = prefs.getString("USER_NAME", null)
    fun isUserConfigured(): Boolean = getUserId() != -1
}