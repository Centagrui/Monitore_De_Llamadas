package com.asf.callmonitoring.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asf.callmonitoring.data.local.AppDatabase
import com.asf.callmonitoring.data.remote.RetrofitClient
import com.asf.callmonitoring.data.repository.LlamadaRepository
import com.asf.callmonitoring.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LlamadaRepository

    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LlamadaRepository(database.llamadaDao(), RetrofitClient.apiService)

        verificarPermisos()
        observarLlamadas()
    }

    fun verificarPermisos() {
        val tienePermisos = PermissionUtils.hasRequiredPermissions(getApplication())
        _uiState.update { it.copy(hasPermissions = tienePermisos) }
    }

    private fun observarLlamadas() {
        viewModelScope.launch {
            repository.todasLasLlamadas.collect { listaLlamadas ->
                _uiState.update { it.copy(llamadas = listaLlamadas) }
            }
        }
    }

    fun forzarSincronizacion() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, mensaje = null) }
            try {
                val cantidad = repository.sincronizarPendientes()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        mensaje = if (cantidad > 0) "Se sincronizaron $cantidad llamadas" else "No hay llamadas pendientes"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        mensaje = "Error al sincronizar: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}