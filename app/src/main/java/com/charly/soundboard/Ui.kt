package com.charly.soundboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Botón cuadrado tipo "pill grande": toque corto reproduce, mantener presionado edita. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundButton(
    slot: Slot,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val hasAudio = slot.audioUri != null

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (hasAudio) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (hasAudio) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (hasAudio) Icons.Default.PlayArrow else Icons.Default.Add,
                contentDescription = null,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = slot.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (hasAudio) "Toca para reproducir" else "Toca para cargar audio",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditSlotDialog(
    slot: Slot,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onPickAudio: () -> Unit,
    onClearAudio: () -> Unit,
) {
    var name by remember(slot.id) { mutableStateOf(slot.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar botón") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FilledTonalButton(
                    onClick = onPickAudio,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (slot.audioUri == null) "Cargar audio" else "Cambiar audio")
                }
                if (slot.audioUri != null) {
                    TextButton(onClick = onClearAudio) {
                        Text("Quitar audio")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
