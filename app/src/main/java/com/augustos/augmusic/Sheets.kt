package com.augustos.augmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ menu contextual da música

internal class TrackMenuActions(
    val onPlay: () -> Unit,
    val onQueue: () -> Unit,
    val onFavorite: () -> Unit,
    val onDetails: () -> Unit,
    val onShare: () -> Unit,
    val onEdit: () -> Unit,
    val onRingtone: () -> Unit,
    val onPlaylist: () -> Unit,
    val onLocate: () -> Unit,
    val onRemoveFromPlaylist: (() -> Unit)?,
    val onDelete: () -> Unit
)

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = WC
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(if (active) c.accentSoft else c.surfaceHigh)
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = c.accentFg, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackMenuSheet(
    track: AudioTrack,
    isFavorite: Boolean,
    actions: TrackMenuActions,
    onDismiss: () -> Unit
) {
    val c = WC
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        contentColor = c.text
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtworkImage(track, Modifier.size(60.dp), RoundedCornerShape(16.dp), ArtSize.SMALL)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(track.title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = c.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Default.PlayArrow, "Tocar agora", false, Modifier.weight(1f), actions.onPlay)
                QuickAction(Icons.Default.QueueMusic, "Adicionar à fila", false, Modifier.weight(1f), actions.onQueue)
                QuickAction(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (isFavorite) "Favoritada" else "Favoritar", isFavorite, Modifier.weight(1f), actions.onFavorite
                )
            }
            MenuRow(Icons.Default.PlaylistAdd, "Adicionar à playlist", onClick = actions.onPlaylist)
            actions.onRemoveFromPlaylist?.let { MenuRow(Icons.Default.Close, "Remover desta playlist", onClick = it) }
            MenuRow(Icons.Default.Info, "Detalhes do arquivo", onClick = actions.onDetails)
            MenuRow(Icons.Default.FolderOpen, "Abrir localização do arquivo", onClick = actions.onLocate)
            MenuRow(Icons.Default.Edit, "Editar informações", onClick = actions.onEdit)
            MenuRow(Icons.Default.Share, "Compartilhar", onClick = actions.onShare)
            MenuRow(Icons.Default.PlayCircle, "Definir como toque", onClick = actions.onRingtone)
            MenuRow(Icons.Default.Delete, "Excluir música", "Remove o arquivo do aparelho", destructive = true, onClick = actions.onDelete)
            Spacer(Modifier.height(28.dp))
        }
    }
}

// ------------------------------------------------------------------ fila de reprodução

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QueueSheet(ps: PlayerState, controller: MediaController?, onDismiss: () -> Unit) {
    val c = WC
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface, contentColor = c.text) {
        val queue = ps.queue
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Fila de reprodução", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.text)
                Text("${queue.size} faixas", color = c.muted, fontSize = 12.sp)
            }
            val hasNext = controller != null && ps.currentIndex in 0 until queue.size - 1
            if (hasNext) {
                TextButton(onClick = {
                    controller?.removeMediaItems(ps.currentIndex + 1, queue.size)
                }) { Text("Limpar a seguir", color = c.accentFg) }
            }
        }
        if (controller == null || queue.isEmpty()) {
            Text("A fila está vazia.", Modifier.padding(22.dp), color = c.muted)
        } else {
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = (ps.currentIndex - 2).coerceIn(0, queue.lastIndex)
            )
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp), state = listState) {
                itemsIndexed(queue) { index, item ->
                    val current = index == ps.currentIndex
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (current) c.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { controller.seekToDefaultPosition(index); controller.play() }
                            .padding(start = 22.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = if (current) c.accentFg else c.muted)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (current) c.accentFg else c.text, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
                            Text(item.artist, color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (!current) {
                            IconButton(onClick = { controller.removeMediaItem(index) }) {
                                Icon(Icons.Default.Close, "Remover da fila", tint = c.muted)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ letras

private enum class LyricsMode { VIEW, SEARCH, EDIT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricsSheet(
    track: AudioTrack,
    settings: AppSettings,
    lyrics: String?,
    loading: Boolean,
    hasSaved: Boolean,
    hasPickedFile: Boolean,
    onSave: suspend (plain: String, synced: String?) -> Unit,
    onDelete: suspend () -> Unit,
    onPickFile: () -> Unit,
    onClearPickedFile: () -> Unit,
    onEnable: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = WC
    val scope = rememberCoroutineScope()
    var mode by remember(track.id) { mutableStateOf(LyricsMode.VIEW) }
    var draft by remember(track.id) { mutableStateOf("") }
    var searching by remember(track.id) { mutableStateOf(false) }
    var results by remember(track.id) { mutableStateOf<List<LyricsCandidate>?>(null) }
    var error by remember(track.id) { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        contentColor = c.text
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
            Text("Letra", color = c.muted, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(track.title, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = c.muted, fontSize = 13.sp, maxLines = 1)
            Spacer(Modifier.height(12.dp))

            if (!settings.lyricsEnabled) {
                Text("A exibição de letras está desativada.", color = c.muted)
                TextButton(onClick = onEnable) { Text("Ativar letras", color = c.accentFg) }
            } else when (mode) {
                LyricsMode.VIEW -> {
                    Column(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 380.dp).verticalScroll(rememberScrollState())) {
                        when {
                            loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.accent)
                                Spacer(Modifier.width(10.dp))
                                Text("Carregando…", color = c.muted)
                            }
                            lyrics != null -> Text(
                                lyrics, color = c.text,
                                fontSize = settings.lyricsTextSize.sp, lineHeight = (settings.lyricsTextSize * 1.5f).sp
                            )
                            else -> Text(
                                "Nenhuma letra encontrada para esta música.\n\n" +
                                    "Você pode pesquisar online, colar a letra, ou escolher um arquivo .lrc/.txt. " +
                                    "A letra fica salva no app e associada a esta faixa.",
                                color = c.muted
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (settings.lyricsOnlineSearch) {
                        MenuRow(Icons.Default.Cloud, "Pesquisar online", "Busca no LRCLIB (usa a internet)") {
                            mode = LyricsMode.SEARCH
                            if (results == null && !searching) {
                                searching = true; error = null
                                scope.launch {
                                    val r = LrcLib.search(track)
                                    searching = false
                                    r.onSuccess { results = it }
                                    r.onFailure { error = "Não foi possível pesquisar: ${it.message ?: "sem conexão"}" }
                                }
                            }
                        }
                    }
                    MenuRow(Icons.Default.ContentPaste, "Colar ou editar letra") {
                        draft = lyrics.orEmpty(); mode = LyricsMode.EDIT
                    }
                    MenuRow(Icons.Default.Subtitles, "Escolher arquivo .lrc / .txt", onClick = onPickFile)
                    if (hasPickedFile) MenuRow(Icons.Default.Close, "Desvincular arquivo escolhido", onClick = onClearPickedFile)
                    if (hasSaved) MenuRow(Icons.Default.Delete, "Remover letra salva", destructive = true) {
                        scope.launch { onDelete() }
                    }
                }

                LyricsMode.SEARCH -> {
                    Text("Resultados", color = c.text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Column(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 400.dp).verticalScroll(rememberScrollState())) {
                        when {
                            searching -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.accent)
                                Spacer(Modifier.width(10.dp)); Text("Pesquisando…", color = c.muted)
                            }
                            error != null -> Text(error!!, color = c.danger)
                            results.isNullOrEmpty() -> Text("Nenhuma letra encontrada online para esta música.", color = c.muted)
                            else -> results!!.forEach { cand ->
                                Column(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            scope.launch {
                                                onSave(cand.plain, cand.synced)
                                                mode = LyricsMode.VIEW
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    Text("${cand.title} — ${cand.artist}", color = c.text, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val dur = if (cand.durationSec > 0) " · ${formatDuration(cand.durationSec * 1000L)}" else ""
                                    Text(
                                        cand.album + dur + (if (cand.synced != null) " · com versão sincronizada (exibida como texto)" else ""),
                                        color = c.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                                    )
                                    Text(cand.plain.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(), color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { mode = LyricsMode.VIEW }) { Text("Voltar", color = c.accentFg) }
                }

                LyricsMode.EDIT -> {
                    OutlinedTextField(
                        value = draft, onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp),
                        placeholder = { Text("Cole a letra aqui") }
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { mode = LyricsMode.VIEW }) { Text("Cancelar", color = c.muted) }
                        TextButton(
                            enabled = draft.isNotBlank(),
                            onClick = { scope.launch { onSave(draft, null); mode = LyricsMode.VIEW } }
                        ) { Text("Salvar", color = c.accentFg) }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
