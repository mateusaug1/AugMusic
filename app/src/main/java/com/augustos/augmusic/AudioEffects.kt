package com.augustos.augmusic

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

internal const val EQ_BAND_COUNT = 10
internal val EQ_FREQS_HZ = floatArrayOf(32f, 64f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
internal val EQ_LABELS = listOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

internal val EQ_PRESETS: List<Pair<String, List<Float>>> = listOf(
    "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Acoustic" to listOf(4f, 3f, 2f, 0f, 1f, 2f, 3f, 2f, 1f, 0f),
    "Bass Booster" to listOf(9f, 8f, 6f, 4f, 1f, 0f, 0f, 0f, 0f, 0f),
    "Bass Reducer" to listOf(-8f, -7f, -5f, -3f, 0f, 0f, 0f, 0f, 0f, 0f)
)

internal data class EqSettings(
    // Desligado por padrão: na V10 os efeitos só existiam enquanto o diálogo estava aberto,
    // ou seja, o som real do app era sempre "sem efeitos".
    val enabled: Boolean = false,
    val preset: String = "Flat",
    val bands: List<Float> = List(EQ_BAND_COUNT) { 0f },
    val bassBoost: Float = 35f,
    val virtualizer: Float = 20f,
    val reverb: Float = 0f
)

internal data class EffectsStatus(
    val attached: Boolean = false,
    val equalizer: Boolean = false,
    val bassBoost: Boolean = false,
    val virtualizer: Boolean = false,
    val reverb: Boolean = false
)

/**
 * Dono dos efeitos de áudio. Os efeitos vivem junto com o player (no MusicService), e não mais
 * dentro do diálogo: antes, fechar o equalizador liberava os efeitos e o som voltava ao normal;
 * as bandas também voltavam a 0 toda vez que o diálogo era reaberto.
 * As configurações são persistidas e reaplicadas sempre que a sessão de áudio muda.
 */
internal object AudioEffects {
    private var store: UserData? = null
    private val _settings = MutableStateFlow(EqSettings())
    val settings: StateFlow<EqSettings> = _settings.asStateFlow()
    private val _status = MutableStateFlow(EffectsStatus())
    val status: StateFlow<EffectsStatus> = _status.asStateFlow()

    private var equalizer: Equalizer? = null
    private var bass: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null

    @Synchronized
    fun init(context: Context) {
        if (store == null) {
            val s = UserData(context)
            store = s
            _settings.value = s.loadEq()
        }
    }

    /** Cada efeito é criado de forma independente: se um não existir no aparelho, os outros continuam. */
    @Synchronized
    fun attach(audioSessionId: Int) {
        releaseEffects()
        if (audioSessionId <= 0) {
            _status.value = EffectsStatus()
            return
        }
        equalizer = create { Equalizer(0, audioSessionId) }
        bass = create { BassBoost(0, audioSessionId) }
        virtualizer = create { Virtualizer(0, audioSessionId) }
        reverb = create { PresetReverb(0, audioSessionId) }
        _status.value = EffectsStatus(
            attached = true,
            equalizer = equalizer != null,
            bassBoost = bass != null,
            virtualizer = virtualizer != null,
            reverb = reverb != null
        )
        applySettings()
    }

    @Synchronized
    fun detach() {
        releaseEffects()
        _status.value = EffectsStatus()
    }

    @Synchronized
    fun update(transform: (EqSettings) -> EqSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        store?.saveEq(next)
        applySettings()
    }

    private fun <T> create(block: () -> T): T? = try {
        block()
    } catch (_: Exception) {
        null
    }

    private fun applySettings() {
        val s = _settings.value

        equalizer?.let { e ->
            try {
                val range = e.bandLevelRange
                val minLevel = range[0].toInt()
                val maxLevel = range[1].toInt()
                for (band in 0 until e.numberOfBands.toInt()) {
                    val level = levelForHardwareBand(e, band, s.bands).coerceIn(minLevel, maxLevel)
                    e.setBandLevel(band.toShort(), level.toShort())
                }
                e.setEnabled(s.enabled)
            } catch (_: Exception) {
            }
        }
        bass?.let {
            try {
                it.setStrength((s.bassBoost * 10f).toInt().coerceIn(0, 1000).toShort())
                it.setEnabled(s.enabled && s.bassBoost > 0f)
            } catch (_: Exception) {
            }
        }
        virtualizer?.let {
            try {
                it.setStrength((s.virtualizer * 10f).toInt().coerceIn(0, 1000).toShort())
                it.setEnabled(s.enabled && s.virtualizer > 0f)
            } catch (_: Exception) {
            }
        }
        reverb?.let {
            try {
                it.setPreset(
                    when {
                        s.reverb < 1f -> PresetReverb.PRESET_NONE
                        s.reverb < 35f -> PresetReverb.PRESET_SMALLROOM
                        s.reverb < 65f -> PresetReverb.PRESET_MEDIUMROOM
                        else -> PresetReverb.PRESET_LARGEHALL
                    }
                )
                it.setEnabled(s.enabled && s.reverb > 0f)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * O hardware costuma ter só 5 bandas, a UI tem 10. Cada banda do hardware recebe a média dos
     * sliders cuja frequência cai dentro da sua faixa (antes, vários sliders sobrescreviam a mesma
     * banda e o último vencia). O nível também é limitado à faixa suportada pelo aparelho.
     */
    private fun levelForHardwareBand(e: Equalizer, band: Int, ui: List<Float>): Int {
        val range = e.getBandFreqRange(band.toShort())
        val lo = range[0] / 1000f
        val hi = if (range[1] <= range[0]) Float.MAX_VALUE else range[1] / 1000f
        var sum = 0f
        var count = 0
        for (i in EQ_FREQS_HZ.indices) {
            val f = EQ_FREQS_HZ[i]
            if (f >= lo && f < hi) {
                sum += ui.getOrElse(i) { 0f }
                count++
            }
        }
        val db = if (count > 0) {
            sum / count
        } else {
            val center = e.getCenterFreq(band.toShort()) / 1000f
            val nearest = EQ_FREQS_HZ.indices.minByOrNull { abs(EQ_FREQS_HZ[it] - center) } ?: 0
            ui.getOrElse(nearest) { 0f }
        }
        return (db * 100f).toInt()
    }

    private fun releaseEffects() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bass?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { reverb?.release() } catch (_: Exception) {}
        equalizer = null
        bass = null
        virtualizer = null
        reverb = null
    }
}
