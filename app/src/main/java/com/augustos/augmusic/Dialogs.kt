package com.augustos.augmusic

import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ ordenação

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortSheet(
    options: List<String>,
    sortBy: String,
    descending: Boolean,
    onChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = AugLime, selectedLabelColor = AugInk)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AugPanel, contentColor = Color.White) {
        Text("ORDENAR MÚSICAS", Modifier.padding(horizontal = 22.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = !descending,
                onClick = { onChange(sortBy, false) },
                label = { Text("↑ CRESC") },
                colors = chipColors
            )
            FilterChip(
                selected = descending,
                onClick = { onChange(sortBy, true) },
                label = { Text("↓ DECRESC") },
                colors = chipColors
            )
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
            items(options) { option ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onChange(option, descending); onDismiss() }
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option, Modifier.weight(1f), color = if (sortBy == option) AugLime else Color.White)
                    if (sortBy == option) Icon(Icons.Default.Check, null, tint = AugLime)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ fila

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QueueSheet(ps: PlayerState, controller: MediaController?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AugPanel, contentColor = Color.White) {
        Text("Fila de reprodução", Modifier.padding(horizontal = 22.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        val queue = ps.queue
        if (controller == null || queue.isEmpty()) {
            Text("A fila está vazia.", Modifier.padding(22.dp), color = AugMuted)
        } else {
            // Abre já rolado até a faixa atual (a fila pode ter milhares de itens).
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = (ps.currentIndex - 2).coerceIn(0, queue.lastIndex)
            )
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp), state = listState) {
                itemsIndexed(queue) { index, item ->
                    val current = index == ps.currentIndex
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                controller.seekToDefaultPosition(index)
                                controller.play()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = if (current) AugLime else AugMuted)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                item.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (current) AugLime else Color.White
                            )
                            Text(item.artist, color = AugMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (!current) {
                            IconButton(onClick = {
                                controller.seekToDefaultPosition(index)
                                controller.play()
                            }) { Icon(Icons.Default.PlayArrow, "Tocar esta faixa") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ detalhes

@Composable
internal fun DetailsDialog(track: AudioTrack, onDismiss: () -> Unit) {
    val format = track.path?.substringAfterLast('.', "")?.uppercase()?.takeIf { it.isNotBlank() }
        ?: track.mimeType?.substringAfterLast('/')?.uppercase()
        ?: "desconhecido"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes do arquivo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Título: ${track.title}")
                Text("Artista: ${track.artist}")
                Text("Álbum: ${track.album}")
                if (track.albumArtist.isNotBlank()) Text("Artista do álbum: ${track.albumArtist}")
                if (track.genre.isNotBlank()) Text("Gênero: ${track.genre}")
                if (track.year > 0) Text("Ano: ${track.year}")
                if (track.composer.isNotBlank()) Text("Compositor: ${track.composer}")
                if (track.trackNumber > 0) Text("Faixa: ${track.trackNumber % 1000}")
                Text("Formato: $format")
                Text("Tamanho: ${formatBytes(track.size)}")
                Text("Duração: ${formatDuration(track.duration)}")
                Text("Adicionada em: ${formatDateSeconds(track.dateAdded)}")
                Text("Modificada em: ${formatDateSeconds(track.dateModified)}")
                track.path?.let { Text("Caminho: $it", fontSize = 10.sp, color = AugMuted) }
                Text("URI: ${track.uri}", fontSize = 10.sp, color = AugMuted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

// ------------------------------------------------------------------ edição

@Composable
internal fun EditTrackDialog(
    track: AudioTrack,
    onSave: (title: String, artist: String, album: String, cover: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember(track.id) { mutableStateOf(track.title) }
    var artist by remember(track.id) { mutableStateOf(track.artist) }
    var album by remember(track.id) { mutableStateOf(track.album) }
    var cover by remember(track.id) { mutableStateOf(track.coverOverride?.toString()) }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            cover = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar informações") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Título da música") }, singleLine = true)
                OutlinedTextField(artist, { artist = it }, label = { Text("Artista") }, singleLine = true)
                OutlinedTextField(album, { album = it }, label = { Text("Álbum") }, singleLine = true)
                if (cover != null) {
                    AsyncImage(
                        model = cover, contentDescription = "Capa escolhida",
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                OutlinedButton(onClick = { coverPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Escolher capa da galeria")
                }
                if (cover != null) {
                    TextButton(onClick = { cover = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Usar a capa original", color = AugMuted)
                    }
                }
                Text(
                    "As alterações ficam salvas no WMusic e não reescrevem as tags internas do arquivo de áudio. " +
                        "Deixar um campo em branco volta a usar a informação do arquivo.",
                    color = AugMuted, fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, artist, album, cover) }) { Text("Salvar", color = AugLime) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ------------------------------------------------------------------ playlists

@Composable
internal fun CreatePlaylistDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova playlist") },
        text = {
            OutlinedTextField(name, { name = it }, label = { Text("Nome da playlist") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Criar", color = AugLime) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
internal fun AddToPlaylistDialog(
    names: List<String>,
    onPick: (String) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar à playlist") },
        text = {
            Column(
                Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (names.isEmpty()) Text("Você ainda não criou playlists.", color = AugMuted)
                names.forEach { name ->
                    TextButton(onClick = { onPick(name) }, modifier = Modifier.fillMaxWidth()) {
                        Text(name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                TextButton(onClick = onCreateNew) { Text("+ Criar playlist", color = AugLime) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = AugLime) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ------------------------------------------------------------------ letra

@Composable
internal fun LyricsDialog(
    track: AudioTrack?,
    lyrics: String?,
    loading: Boolean,
    hasCustomFile: Boolean,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Letra da música") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(track?.title ?: "Música", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                when {
                    loading -> Text("Carregando…", color = AugMuted)
                    lyrics != null -> Text(lyrics, color = Color.White)
                    else -> Text(
                        "Nenhuma letra encontrada. Coloque um arquivo .lrc ou .txt com o mesmo nome da música " +
                            "na mesma pasta, ou escolha o arquivo manualmente (necessário no Android 11 ou superior).",
                        color = AugMuted
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onPickFile) { Text("Escolher arquivo de letra…", color = AugLime) }
                if (hasCustomFile) TextButton(onClick = onClearFile) { Text("Remover arquivo escolhido", color = AugMuted) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

// ------------------------------------------------------------------ velocidade

@Composable
internal fun SpeedDialog(current: Float, onPick: (Float) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Velocidade de reprodução") },
        text = {
            Column {
                listOf(0.5f, 0.8f, 1f, 1.2f, 1.5f, 2f).forEach { speed ->
                    TextButton(onClick = { onPick(speed) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${speed}x", color = if (current == speed) AugLime else Color.White)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

// ------------------------------------------------------------------ timer

@Composable
internal fun TimerDialog(
    state: SleepState,
    onTimed: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state) {
        while (state.mode == SleepMode.TIMED) {
            now = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    // Os cinco botões ficavam numa única Row e estouravam a largura do diálogo; agora é uma coluna.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timer de desligamento") },
        text = {
            Column {
                when (state.mode) {
                    SleepMode.TIMED -> Text(
                        "Timer ativo — restam ${formatDuration((state.endsAtElapsedMs - now).coerceAtLeast(0L))}",
                        color = AugLime
                    )
                    SleepMode.END_OF_TRACK -> Text("Timer ativo — vai parar no fim da música", color = AugLime)
                    SleepMode.OFF -> Text("Pause a reprodução automaticamente após:")
                }
                Spacer(Modifier.height(8.dp))
                listOf(15, 30, 45, 60).forEach { minutes ->
                    TextButton(onClick = { onTimed(minutes) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (minutes == 60) "1 hora" else "$minutes minutos", color = Color.White)
                    }
                }
                TextButton(onClick = onEndOfTrack, modifier = Modifier.fillMaxWidth()) {
                    Text("Fim da música", color = Color.White)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        dismissButton = {
            if (state.mode != SleepMode.OFF) TextButton(onClick = onCancelTimer) { Text("Cancelar timer") }
        }
    )
}
