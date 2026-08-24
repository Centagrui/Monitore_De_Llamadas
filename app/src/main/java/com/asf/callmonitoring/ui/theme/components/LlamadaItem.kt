package com.asf.callmonitoring.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asf.callmonitoring.data.local.entity.LlamadaEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LlamadaItem(
    llamada: LlamadaEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = llamada.nombreContacto ?: llamada.numeroTelefono,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (llamada.sincronizado) "✓ Sincronizada" else "Pendiente",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (llamada.sincronizado) Color(0xFF2E7D32) else Color(0xFFE65100)
                )
            }

            if (llamada.nombreContacto != null) {
                Text(
                    text = llamada.numeroTelefono,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${llamada.tipoLlamada} • ${llamada.duracionSegundos}s (${llamada.estadoLlamada})",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                Text(
                    text = formatoFecha.format(Date(llamada.fechaHora)),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}