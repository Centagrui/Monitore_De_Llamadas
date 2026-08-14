package com.asf.callmonitoring.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import com.asf.callmonitoring.service.SyncWorker
import com.asf.callmonitoring.ui.components.LlamadaItem
import com.asf.callmonitoring.util.Constants
import com.asf.callmonitoring.util.PermissionUtils
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        programarSincronizacionSegundoPlano()

        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.verificarPermisos()
                }

                LaunchedEffect(Unit) {
                    if (!PermissionUtils.hasRequiredPermissions(this@MainActivity)) {
                        permissionLauncher.launch(PermissionUtils.REQUIRED_PERMISSIONS)
                    }
                }

                MainScreen(
                    state = state,
                    onSyncClick = { viewModel.forzarSincronizacion() },
                    onRequestPermissions = {
                        permissionLauncher.launch(PermissionUtils.REQUIRED_PERMISSIONS)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.verificarPermisos()
    }

    private fun programarSincronizacionSegundoPlano() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.WORK_SYNC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainState,
    onSyncClick: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitoreo de Llamadas ASF") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSyncClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sincronizar ahora",
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!state.hasPermissions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Se requieren permisos para registrar las llamadas.",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRequestPermissions) {
                            Text("Otorgar permisos")
                        }
                    }
                }
            }

            state.mensaje?.let { mensaje ->
                Text(
                    text = mensaje,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (state.llamadas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay llamadas registradas aún",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.llamadas, key = { it.id }) { llamada ->
                        LlamadaItem(llamada = llamada)
                    }
                }
            }
        }
    }
}