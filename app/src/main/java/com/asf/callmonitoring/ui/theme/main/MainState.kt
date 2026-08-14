package com.asf.callmonitoring.ui.main

import com.asf.callmonitoring.data.local.entity.LlamadaEntity

data class MainState(
    val llamadas: List<LlamadaEntity> = emptyList(),
    val isSyncing: Boolean = false,
    val hasPermissions: Boolean = false,
    val mensaje: String? = null
)