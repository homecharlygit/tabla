package com.charly.soundboard

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Un botón de la tabla: nombre editable y URI del audio cargado (null si está vacío). */
data class Slot(val id: Int, val name: String, val audioUri: String? = null)

class SlotRepository(context: Context) {

    private val prefs = context.getSharedPreferences("soundboard", Context.MODE_PRIVATE)

    fun load(): List<Slot> {
        val raw = prefs.getString(KEY, null)
            ?: return List(GRID_SIZE) { Slot(it, "Sonido ${it + 1}") }

        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Slot(
                id = o.getInt("id"),
                name = o.getString("name"),
                audioUri = o.optString("uri").takeIf { it.isNotEmpty() },
            )
        }
    }

    fun save(slots: List<Slot>) {
        val arr = JSONArray()
        slots.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("uri", s.audioUri ?: "")
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "slots"
        const val GRID_SIZE = 12
    }
}

/** Reproduce un solo audio a la vez; al tocar otro botón, corta el anterior. */
class SoundPlayer {

    private var player: MediaPlayer? = null

    /** Devuelve false si el audio no se pudo abrir o reproducir. */
    fun play(context: Context, uri: Uri): Boolean {
        stop()
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(context, uri)
            mp.setOnCompletionListener { done ->
                done.release()
                if (player === done) player = null
            }
            mp.prepare()
            mp.start()
            player = mp
            true
        } catch (e: Exception) {
            mp.release()
            false
        }
    }

    fun stop() {
        player?.runCatching { release() }
        player = null
    }
}
