package com.charly.soundboard

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private lateinit var repo: SlotRepository
    private val player = SoundPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SlotRepository(this)
        setContent {
            SoundboardTheme {
                SoundboardScreen(repo = repo, player = player)
            }
        }
    }

    override fun onDestroy() {
        player.stop()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundboardTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundboardScreen(repo: SlotRepository, player: SoundPlayer) {
    val context = LocalContext.current

    var slots by remember { mutableStateOf(repo.load()) }
    var pendingSlotId by remember { mutableStateOf<Int?>(null) }
    var editingId by remember { mutableStateOf<Int?>(null) }

    fun update(id: Int, transform: (Slot) -> Slot) {
        slots = slots.map { if (it.id == id) transform(it) else it }
        repo.save(slots)
    }

    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val id = pendingSlotId
        pendingSlotId = null
        if (id != null && uri != null) {
            // Permiso persistente para que el audio siga disponible al reiniciar la app.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            update(id) { it.copy(audioUri = uri.toString()) }
        }
    }

    fun play(slot: Slot) {
        val uri = slot.audioUri
        if (uri == null) {
            pendingSlotId = slot.id
            pickAudio.launch(arrayOf("audio/*"))
        } else if (!player.play(context, Uri.parse(uri))) {
            Toast.makeText(context, "No se pudo reproducir \"${slot.name}\"", Toast.LENGTH_SHORT).show()
        }
    }

    val editing = slots.firstOrNull { it.id == editingId }
    if (editing != null) {
        EditSlotDialog(
            slot = editing,
            onDismiss = { editingId = null },
            onSave = { newName ->
                update(editing.id) { it.copy(name = newName.trim().ifEmpty { it.name }) }
                editingId = null
            },
            onPickAudio = {
                pendingSlotId = editing.id
                editingId = null
                pickAudio.launch(arrayOf("audio/*"))
            },
            onClearAudio = {
                player.stop()
                update(editing.id) { it.copy(audioUri = null) }
                editingId = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Soundboard") },
                actions = {
                    IconButton(onClick = { player.stop() }) {
                        Icon(Icons.Default.Close, contentDescription = "Detener audio")
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(slots, key = { it.id }) { slot ->
                SoundButton(
                    slot = slot,
                    onClick = { play(slot) },
                    onLongClick = { editingId = slot.id },
                )
            }
        }
    }
}
