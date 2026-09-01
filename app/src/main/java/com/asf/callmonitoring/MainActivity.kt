package com.asf.callmonitoring

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.asf.callmonitoring.ui.theme.MonitoreoLlamadasASFTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonitoreoLlamadasASFTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Permisos esenciales requeridos
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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val todosAceptados = resultados.values.all { it }
        permisosConcedidos = todosAceptados

        if (todosAceptados) {
            Toast.makeText(context, "Permisos concedidos correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                "Se requieren todos los permisos para monitorear llamadas",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Solicitar automáticamente al abrir la pantalla si faltan permisos
    LaunchedEffect(Unit) {
        if (!permisosConcedidos) {
            launcher.launch(permisosRequeridos)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (permisosConcedidos) {
            Text(
                text = "✓ Monitoreo Activo y Autorizado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "La aplicación está lista para detectar llamadas en segundo plano.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "Permisos pendientes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { launcher.launch(permisosRequeridos) }) {
                Text("Conceder Permisos de Telefonía")
            }
        }
    }
}