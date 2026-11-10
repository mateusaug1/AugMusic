package com.augustos.augmusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SEARCH_FILTERS = listOf("Tudo", "Músicas", "Álbuns", "Artistas")

@Composable
internal fun SearchScreen(
    data: LibraryData,
    vm: MainViewModel,
    nav: AppNav,
    ps: PlayerState,
    actions: AppActions
) {
    val c = WC
    val focus = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Tudo") }
    LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }

    val q = remember(query) { normalizeForSearch(query.trim()) }
    val tracks = remember(data, q) { if (q.isEmpty()) emptyList() else data.sorted.filter { it.searchKey.contains(q) } }
    val albums = remember(data, q) { if (q.isEmpty()) emptyList() else data.albums.filter { normalizeForSearch(it.title).contains(q) } }
    val artists = remember(data, q) { if (q.isEmpty()) emptyList() else data.artists.filter { normalizeForSearch(it.title).contains(q) } }
    val showTracks = filter == "Tudo" || filter == "Músicas"
    val showAlbums = filter == "Tudo" || filter == "Álbuns"
    val showArtists = filter == "Tudo" || filter == "Artistas"

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("Pesquisa", color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().focusRequester(requester),
            placeholder = { Text("Música, artista ou álbum", color = c.muted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = c.muted) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Limpar busca") }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent, unfocusedBorderColor = c.outline,
                focusedContainerColor = c.surface, unfocusedContainerColor = c.surface
            )
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SEARCH_FILTERS.forEach { name -> WChip(name, filter == name, onClick = { filter = name }) }
        }

        if (q.isEmpty()) {
            EmptyState(Icons.Default.Search, "Encontre qualquer música", "A busca ignora acentos e maiúsculas.", Modifier.weight(1f))
        } else if (tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
            EmptyState(Icons.Default.Search, "Nada encontrado", "Nenhum resultado para \"${query.trim()}\".", Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                if (showArtists && artists.isNotEmpty()) {
                    item { SectionTitle("Artistas") }
                    items(artists.take(if (filter == "Tudo") 4 else 100), key = { "ar-${it.key}" }) { g ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { nav.openArtist(g.key, g.title) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtistAvatar(g.rep, 48.dp)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(g.title, color = c.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(g.subtitle, color = c.muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (showAlbums && albums.isNotEmpty()) {
                    item { SectionTitle("Álbuns") }
                    items(albums.take(if (filter == "Tudo") 4 else 100), key = { "al-${it.key}" }) { g ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { nav.openAlbum(g.key, g.title) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(g.rep, Modifier.size(48.dp), RoundedCornerShape(12.dp))
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(g.title, color = c.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(g.subtitle, color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (showTracks && tracks.isNotEmpty()) {
                    item { SectionTitle("Músicas") }
                    items(tracks.take(150), key = { "tr-${it.id}" }) { track ->
                        TrackRow(
                            track, ps.currentMediaId == track.id, ps.isPlaying, track.id in vm.favorites, vm.settings,
                            onClick = { actions.play(track, tracks) },
                            onLongClick = { actions.menu(track) },
                            onFavorite = { vm.toggleFavorite(track.id) }
                        )
                    }
                }
            }
        }
    }
}
