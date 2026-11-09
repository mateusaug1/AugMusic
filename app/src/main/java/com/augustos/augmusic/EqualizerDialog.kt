package com.augustos.augmusic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Só edita AudioEffects (que vive no MusicService); fechar o diálogo NÃO desliga mais o efeito. */
@Composable
internal fun EqualizerDialog(onDismiss: () -> Unit) {
    val settings by AudioEffects.settings.collectAsStateWithLifecycle()
    val status by AudioEffects.status.collectAsStateWithLifecycle()
    val sliderColors = SliderDefaults.colors(thumbColor = AugLime, activeTrackColor = AugLime)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Equalizador", fontWeight = FontWeight.Bold)
                    Text("10 bandas · áudio do dispositivo", color = AugMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { on -> AudioEffects.update { it.copy(enabled = on) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = AugInk, checkedTrackColor = AugLime)
                )
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EQ_PRESETS.forEach { (name, values) ->
                        TextButton(
                            onClick = { AudioEffects.update { it.copy(preset = name, bands = values) } },
                            contentPadding = PaddingValues(horizontal = 3.dp)
                        ) {
                            Text(
                                name,
                                color = if (settings.preset == name) AugLime else AugMuted,
                                fontSize = 9.sp, maxLines = 1
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(235.dp).padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    settings.bands.forEachIndexed { index, value ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("+12", fontSize = 8.sp, color = AugMuted)
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Slider(
                                    value = value,
                                    onValueChange = { v ->
                                        AudioEffects.update { cur ->
                                            val list = cur.bands.toMutableList()
                                            list[index] = v
                                            cur.copy(bands = list, preset = "Personalizado")
                                        }
                                    },
                                    valueRange = -12f..12f,
                                    modifier = Modifier.fillMaxSize().verticalSlider(),
                                    colors = sliderColors
                                )
                            }
                            Text("-12", fontSize = 8.sp, color = AugMuted)
                            Text(EQ_LABELS[index], fontSize = 8.sp, color = Color.White, maxLines = 1)
                        }
                    }
                }
                Text(
                    "Bass Boost · ${settings.bassBoost.toInt()}%",
                    color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp)
                )
                Slider(
                    value = settings.bassBoost,
                    onValueChange = { v -> AudioEffects.update { it.copy(bassBoost = v) } },
                    valueRange = 0f..100f, colors = sliderColors
                )
                Text("Virtualizer · ${settings.virtualizer.toInt()}%", color = Color.White, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = settings.virtualizer,
                    onValueChange = { v -> AudioEffects.update { it.copy(virtualizer = v) } },
                    valueRange = 0f..100f, colors = sliderColors
                )
                Text("Reverb · ${settings.reverb.toInt()}%", color = Color.White, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = settings.reverb,
                    onValueChange = { v -> AudioEffects.update { it.copy(reverb = v) } },
                    valueRange = 0f..100f, colors = sliderColors
                )
                val note = when {
                    !status.attached ->
                        "Os efeitos serão aplicados assim que a reprodução iniciar. Suas configurações ficam salvas."
                    !status.equalizer && !status.bassBoost && !status.virtualizer && !status.reverb ->
                        "Este aparelho não disponibiliza efeitos de áudio para esta reprodução."
                    else -> {
                        val missing = buildList {
                            if (!status.equalizer) add("equalizador")
                            if (!status.bassBoost) add("bass boost")
                            if (!status.virtualizer) add("virtualizer")
                            if (!status.reverb) add("reverb")
                        }
                        if (missing.isEmpty()) null
                        else "Não suportado neste aparelho: ${missing.joinToString(", ")}."
                    }
                }
                if (note != null) {
                    Text(note, color = AugMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Concluir", color = AugLime) } }
    )
}
