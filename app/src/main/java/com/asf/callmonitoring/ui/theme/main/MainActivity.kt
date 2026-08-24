package com.asf.callmonitoring.ui.theme.main

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.asf.callmonitoring.data.UserPreferences
import com.asf.callmonitoring.data.api.ApiService
import com.asf.callmonitoring.service.CallMonitorService
import com.asf.callmonitoring.ui.theme.UserSelectionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.asf.callmonitoring.data.model.Usuario

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = UserPreferences(this)

        if (prefs.isUserConfigured()) {
            CallMonitorService.startService(this)
            finish()
            return
        }

        setContent {
            var listaUsuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        val retrofit = Retrofit.Builder()
                            .baseUrl("http://192.168.1.50:3000/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()

                        val api = retrofit.create(ApiService::class.java)
                        val response = api.getUsuarios()

                        if (response.isSuccessful) {
                            val data = response.body() ?: emptyList()
                            Log.d("DEBUG_USUARIOS", "Usuarios obtenidos: ${data.size} -> $data")
                            withContext(Dispatchers.Main) {
                                listaUsuarios = data
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("DEBUG_USUARIOS", "Error en respuesta HTTP: ${response.code()} - $errorBody")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Error del servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DEBUG_USUARIOS", "Error al conectar con el servidor: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            UserSelectionScreen(
                usuarios = listaUsuarios,
                onUserConfigured = { finish() }
            )
        }
    }
}