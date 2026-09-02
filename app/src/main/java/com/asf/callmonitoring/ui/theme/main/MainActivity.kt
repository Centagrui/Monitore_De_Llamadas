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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    // Estado reactivo del usuario configurado
    var usuarioConfigurado by remember { mutableStateOf(prefs.obtenerNombreDispositivo()) }
    var listaUsuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var cargandoUsuarios by remember { mutableStateOf(false) }
    var menuAbierto by remember { mutableStateOf(false) }

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
            if (usuarioConfigurado.isNotBlank()) {
                CallMonitoringService.startService(context)
            }
        } else {
            Toast.makeText(context, "Se requieren permisos para monitorear", Toast.LENGTH_LONG).show()
        }
    }

    // Al entrar: verificar permisos y asegurar que el servicio esté corriendo si ya hay usuario
    LaunchedEffect(Unit) {
        val usuarioGuardado = prefs.obtenerNombreDispositivo()
        Log.d("DEBUG_SESION", "Usuario recuperado de disco al abrir: '$usuarioGuardado'")
        usuarioConfigurado = usuarioGuardado

        if (!permisosConcedidos) {
            launcherPermisos.launch(permisosRequeridos)
        } else {
            pedirIgnorarBateria()
            if (usuarioGuardado.isNotBlank()) {
                CallMonitoringService.startService(context)
            }
        }
    }

    // ÚNICAMENTE cargar usuarios de la API si NO hay usuario configurado
    LaunchedEffect(usuarioConfigurado) {
        if (usuarioConfigurado.isBlank()) {
            cargandoUsuarios = true
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
                        withContext(Dispatchers.Main) { cargandoUsuarios = false }
                    }
                } catch (e: Exception) {
                    Log.e("DEBUG_SESION", "Error al conectar API: ${e.message}")
                    withContext(Dispatchers.Main) { cargandoUsuarios = false }
                }
            }
        }
    }

    // =========================================================================
    // VISTA 1: SI YA HAY USUARIO -> SEGUNDA PANTALLA (BIENVENIDO + MENÚ SALIR)
    // =========================================================================
    if (usuarioConfigurado.isNotBlank()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Agro Santa Fé") },
                    actions = {
                        IconButton(onClick = { menuAbierto = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                        }
                        DropdownMenu(
                            expanded = menuAbierto,
                            onDismissRequest = { menuAbierto = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cambiar Vendedor / Salir") },
                                onClick = {
                                    menuAbierto = false
                                    CallMonitoringService.stopService(context)
                                    prefs.clearUser()
                                    usuarioConfigurado = "" // Forzar a volver a la pantalla de selección
                                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Bienvenido/a",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = usuarioConfigurado,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "✓ Monitoreo en segundo plano activo",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Puedes salir de la app o cerrarla. El servicio seguirá monitoreando y registrando todas las llamadas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    // =========================================================================
    // VISTA 2: NO HAY USUARIO -> PANTALLA DE SELECCIÓN DE VENDEDOR
    // =========================================================================
    else {
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
                    text = "Se necesitan permisos de teléfono y contactos para asociar las llamadas al vendedor.",
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
            UserSelectionScreen(
                usuarios = listaUsuarios,
                onUserConfigured = {
                    // Actualizamos el estado con lo que se acaba de guardar en disco
                    val nuevoUsuario = prefs.obtenerNombreDispositivo()
                    Log.d("DEBUG_SESION", "Vendedor seleccionado con éxito: '$nuevoUsuario'")
                    usuarioConfigurado = nuevoUsuario
                    CallMonitoringService.startService(context)
                }
            )
        }
    }
}