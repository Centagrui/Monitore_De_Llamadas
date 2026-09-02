package com.asf.callmonitoring.ui.theme.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.asf.callmonitoring.data.UserPreferences
import com.asf.callmonitoring.data.api.ApiService
import com.asf.callmonitoring.data.model.Usuario
import com.asf.callmonitoring.service.CallMonitoringService
import com.asf.callmonitoring.ui.theme.UserSelectionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                MainContainer(
                    modifier = Modifier.padding(innerPadding),
                    onConfigured = {
                        // Cuando configure el usuario, iniciamos el servicio
                        CallMonitoringService.startService(this)
                        Toast.makeText(this, "Configuración completada", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun MainContainer(
    modifier: Modifier = Modifier,
    onConfigured: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    var listaUsuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var cargandoUsuarios by remember { mutableStateOf(true) }

    // 1. Permisos esenciales
    val permisosRequeridos = remember {
        val lista = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lista.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        lista.toTypedArray()
    }

    var permisosConcedidos by remember {
        mutableStateOf(
            permisosRequeridos.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // 2. Auxiliar para ignorar optimizaciones de batería
    fun pedirIgnorarBateria() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("DEBUG_BATTERY", "Error al solicitar exclusión de batería: ${e.message}")
        }
    }

    // 3. Launcher de permisos en tiempo de ejecución
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val todosAceptados = resultados.values.all { it }
        permisosConcedidos = todosAceptados

        if (todosAceptados) {
            pedirIgnorarBateria()
            try {
                CallMonitoringService.startService(context)
            } catch (e: Exception) {
                Log.e("DEBUG_SERVICE", "Error al iniciar servicio: ${e.message}", e)
            }
        } else {
            Toast.makeText(context, "Se requieren todos los permisos para monitorear", Toast.LENGTH_LONG).show()
        }
    }

    // Solicitar permisos de inmediato al abrir la pantalla si aún no están concedidos
    LaunchedEffect(Unit) {
        if (!permisosConcedidos) {
            launcher.launch(permisosRequeridos)
        } else {
            pedirIgnorarBateria()
            try {
                CallMonitoringService.startService(context)
            } catch (e: Exception) {
                Log.e("DEBUG_SERVICE", "Error al iniciar servicio: ${e.message}", e)
            }
        }
    }

    // Cargar la lista de vendedores desde el servidor Node.js
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("http://192.168.1.65:3000/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ApiService::class.java)
                val response = api.getUsuarios()

                if (response.isSuccessful) {
                    val data = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        listaUsuarios = data
                        cargandoUsuarios = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        cargandoUsuarios = false
                        Toast.makeText(context, "Error al obtener usuarios: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DEBUG_USUARIOS", "Error al conectar con el servidor: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    cargandoUsuarios = false
                }
            }
        }
    }

    // UI: Si faltan permisos, mostramos botón para solicitarlos
    if (!permisosConcedidos) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Permisos requeridos pendientes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "La aplicación requiere acceso a llamadas y contactos para poder operar.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { launcher.launch(permisosRequeridos) }) {
                Text("Conceder Permisos")
            }
        }
    } else if (cargandoUsuarios && listaUsuarios.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        // Pantalla de selección del vendedor
        UserSelectionScreen(
            usuarios = listaUsuarios,
            onUserConfigured = {
                onConfigured()
            }
        )
    }
}