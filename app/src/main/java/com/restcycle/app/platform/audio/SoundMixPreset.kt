package com.restcycle.app.platform.audio

import java.util.Locale

data class SoundMixPreset(
    val id: String,
    val name: String,
    val layers: Map<AmbientSoundMode, Float>
)

/**
 * Compact persistence codec for user-saved multi-track ambient sound mixes.
 * Format: `id~name~MODE:0.80|MODE2:0.45;...`
 */
object SoundMixCodec {
    const val MAX_SAVED_MIXES = 12
    const val MAX_LAYERS_PER_MIX = 3

    fun encode(presets: List<SoundMixPreset>): String {
        return presets
            .take(MAX_SAVED_MIXES)
            .mapNotNull { preset ->
                val cleanId = sanitize(preset.id).ifBlank { return@mapNotNull null }
                val cleanName = sanitize(preset.name).ifBlank { return@mapNotNull null }
                val encodedLayers = preset.layers.entries
                    .filter { it.key != AmbientSoundMode.OFF }
                    .take(MAX_LAYERS_PER_MIX)
                    .joinToString("|") { (mode, vol) ->
                        "${mode.name}:${String.format(Locale.US, "%.2f", vol.coerceIn(0.10f, 1.0f))}"
                    }
                if (encodedLayers.isBlank()) null else "$cleanId~$cleanName~$encodedLayers"
            }
            .joinToString(";")
    }

    fun decode(raw: String?): List<SoundMixPreset> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";")
            .mapNotNull { entry ->
                val parts = entry.split("~")
                if (parts.size != 3) return@mapNotNull null
                val id = parts[0].trim()
                val name = parts[1].trim()
                if (id.isBlank() || name.isBlank()) return@mapNotNull null
                val layers = LinkedHashMap<AmbientSoundMode, Float>()
                parts[2].split("|").forEach { token ->
                    val kv = token.split(":")
                    if (kv.size == 2) {
                        val mode = AmbientSoundMode.entries.firstOrNull {
                            it.name == kv[0] && it != AmbientSoundMode.OFF
                        }
                        val vol = kv[1].toFloatOrNull()?.coerceIn(0.10f, 1.0f)
                        if (mode != null && vol != null && layers.size < MAX_LAYERS_PER_MIX) {
                            layers[mode] = vol
                        }
                    }
                }
                if (layers.isEmpty()) null else SoundMixPreset(id = id, name = name, layers = layers)
            }
            .take(MAX_SAVED_MIXES)
    }

    private fun sanitize(input: String): String =
        input.replace("~", " ").replace(";", " ").replace("|", " ").trim().take(36)
}
