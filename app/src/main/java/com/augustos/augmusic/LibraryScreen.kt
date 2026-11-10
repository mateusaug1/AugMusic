package com.augustos.augmusic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackRow(
    track: AudioTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    settings: AppSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit
) {
    val c = WC
    val subtitle = listOfNotNull(
        track.artist.takeIf { settings.showArtist },
        track.album.takeIf { settings.showAlbum }
    ).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (isCurrent) c.accentSoft else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            ArtworkImage(track, Modifier.size(52.dp), RoundedCornerShape(14.dp))
            if (isCurrent && isPlaying) {
                Icon(
                    Icons.Default.GraphicEq, "Tocando", tint = c.onAccent,
                    modifier = Modifier.align(Alignment.BottomEnd).size(20.dp)
                        .background(c.accent, RoundedCornerShape(7.dp)).padding(2.dp)
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                track.title, color = if (isCurrent) c.accentFg else c.text,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (settings.showDuration) {
            Text(formatDuration(track.duration), color = c.muted, fontSize = 12.sp)
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                tint = if (isFavorite) c.accentFg else c.muted
            )
        }
    }
}

private val LIBRARY_TABS = listOf(
    "Músicas" to Icons.Default.LibraryMusic,
    "Álbuns" to Icons.Default.Album,
    "Artistas" to Icons.Default.Person,
    "Playlists" to Icons.Default.PlaylistPlay,
    "Favoritos" to Icons.Default.Favorite
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryScreen(
    data: LibraryData,
    vm: MainViewModel,
    nav: AppNav,
    ps: PlayerState,
    actions: AppActions,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onShowSort: () -> Unit,
    onNewPlaylist: () -> Unit,
    onDeletePlaylist: (String) -> Unit
) {
    val c = WC
    val settings = vm.settings
    val tab = nav.libraryTab
    val curKey = nav.collectionKey
    val curPlaylist = nav.playlist
    val inDetail = nav.inLibraryDetail

    val visible: List<AudioTrack> = remember(data, tab, curKey, curPlaylist, vm.favorites, vm.playlists) {
        when {
            tab == "Favoritos" -> data.sorted.filter { it.id in vm.favorites }
            tab == "Playlists" && curPlaylist != null -> vm.playlists[curPlaylist].orEmpty().mapNotNull { data.byId[it] }
            tab == "Álbuns" && curKey != null -> data.sorted.filter { it.albumKey == curKey }
                .sortedWith(compareBy<AudioTrack> { it.trackNumber }.then(compareBy<AudioTrack, String>(AppCollator) { it.title }))
            tab == "Artistas" && curKey != null -> data.sorted.filter { it.artistKey == curKey }
            else -> data.sorted
        }
    }
    val listState = rememberLazyListState()
    // "Mostrar na biblioteca" (Localizador): rola até a faixa pedida.
    LaunchedEffect(nav.focusTrackId, visible) {
        val id = nav.focusTrackId
        if (id != null && tab == "Músicas" && !inDetail) {
            val idx = visible.indexOfFirst { it.id == id }
            if (idx >= 0) listState.scrollToItem(idx)
            nav.focusTrackId = null
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        // ---- cabeçalho
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (inDetail) {
                IconButton(onClick = { nav.back() }) { Icon(Icons.Default.ArrowBack, "Voltar", tint = c.accentFg) }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    nav.collectionLabel ?: curPlaylist ?: "Biblioteca",
                    color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (!inDetail) Text("${data.tracks.size} músicas", color = c.muted, fontSize = 13.sp)
            }
            if (vm.isLoading) {
                CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            if (tab == "Músicas" && !inDetail) {
                IconButton(onClick = onShowSort) { Icon(Icons.Default.Sort, "Ordenar por ${vm.sortBy}", tint = c.accentFg) }
            }
            IconButton(onClick = { if (hasPermission) vm.refresh() else onRequestPermission() }) {
                Icon(Icons.Default.Refresh, "Atualizar biblioteca", tint = c.muted)
            }
        }

        // ---- abas
        if (!inDetail) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LIBRARY_TABS.forEach { (name, icon) ->
                    WChip(name, tab == name, onClick = { nav.openLibrary(name) }, icon = icon)
                }
            }
        }

        // ---- conteúdo
        when {
            !hasPermission -> {
                val (t, s) = emptyLibraryHint(vm, false)
                EmptyState(Icons.Default.MusicOff, t, s, Modifier.weight(1f)) {
                    Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)) {
                        Text("Permitir acesso")
                    }
                }
            }

            tab == "Playlists" && curPlaylist == null -> {
                val names = remember(vm.playlists) { vm.playlists.keys.sortedWith(compareBy<String, String>(AppCollator) { it }) }
                if (names.isEmpty()) {
                    EmptyState(Icons.Default.PlaylistAdd, "Crie sua primeira playlist", "Organize suas músicas em listas personalizadas.", Modifier.weight(1f)) {
                        Button(onClick = onNewPlaylist, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Nova playlist")
                        }
                    }
                } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                    item {
                        Row(Modifier.padding(vertical = 6.dp)) {
                            WChip("Nova playlist", false, onClick = onNewPlaylist, icon = Icons.Default.Add)
                        }
                    }
                    items(names, key = { it }) { name ->
                        val ids = vm.playlists[name].orEmpty()
                        val cover = ids.firstNotNullOfOrNull { data.byId[it] }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                .combinedClickable(onClick = { nav.openPlaylist(name) }, onLongClick = { onDeletePlaylist(name) })
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (cover != null) ArtworkImage(cover, Modifier.size(58.dp), RoundedCornerShape(16.dp))
                            else Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QueueMusic, null, tint = c.accentFg)
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(name, color = c.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (ids.size == 1) "1 música" else "${ids.size} músicas", color = c.muted, fontSize = 12.sp)
                            }
                            IconButton(onClick = { onDeletePlaylist(name) }) { Icon(Icons.Default.Delete, "Excluir playlist", tint = c.muted) }
                            Icon(Icons.Default.ChevronRight, null, tint = c.muted)
                        }
                    }
                }
            }

            (tab == "Álbuns" || tab == "Artistas") && curKey == null -> {
                val groups = if (tab == "Álbuns") data.albums else data.artists
                if (groups.isEmpty()) {
                    val (t, s) = emptyLibraryHint(vm, true)
                    EmptyState(Icons.Default.MusicOff, t, s, Modifier.weight(1f))
                } else LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp, top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    gridItems(groups, key = { it.key }) { g ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable {
                                if (tab == "Álbuns") nav.openAlbum(g.key, g.title) else nav.openArtist(g.key, g.title)
                            }.padding(bottom = 4.dp)
                        ) {
                            if (tab == "Artistas") {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ArtistAvatar(g.rep, 120.dp) }
                            } else {
                                ArtworkImage(g.rep, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(22.dp), ArtSize.MEDIUM)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                g.title, color = c.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                textAlign = if (tab == "Artistas") androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            )
                            Text(
                                g.subtitle, color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                textAlign = if (tab == "Artistas") androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            !vm.hasLoadedOnce || (vm.isLoading && data.tracks.isEmpty()) ->
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accent)
                }

            visible.isEmpty() -> {
                val (t, s) = when {
                    tab == "Favoritos" -> "Nenhuma favorita ainda" to "Toque no coração de uma música para guardá-la aqui."
                    tab == "Playlists" -> "Playlist vazia" to "Adicione músicas pelo menu de cada faixa."
                    else -> emptyLibraryHint(vm, true)
                }
                EmptyState(Icons.Default.MusicOff, t, s, Modifier.weight(1f))
            }

            else -> LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(bottom = 10.dp)) {
                if (inDetail) {
                    item {
                        val rep = visible.first()
                        val total = visible.sumOf { it.duration }
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (tab == "Artistas") ArtistAvatar(rep, 104.dp)
                            else ArtworkImage(rep, Modifier.size(104.dp), RoundedCornerShape(24.dp), ArtSize.MEDIUM)
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text("${visible.size} faixas · ${formatDuration(total)}", color = c.muted, fontSize = 13.sp)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { actions.playList(visible, false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)
                                    ) { Icon(Icons.Default.PlayArrow, null); Text("Tocar") }
                                    OutlinedButton(onClick = { actions.playList(visible, true) }) {
                                        Icon(Icons.Default.Shuffle, null, tint = c.accentFg)
                                    }
                                }
                            }
                        }
                    }
                }
                items(visible, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = ps.currentMediaId == track.id,
                        isPlaying = ps.isPlaying,
                        isFavorite = track.id in vm.favorites,
                        settings = settings,
                        onClick = { actions.play(track, visible) },
                        onLongClick = { actions.menu(track) },
                        onFavorite = { vm.toggleFavorite(track.id) }
                    )
                }
            }
        }
    }
}
