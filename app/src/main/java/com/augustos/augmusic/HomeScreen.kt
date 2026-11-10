package com.augustos.augmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 12 -> "Bom dia"
        h < 18 -> "Boa tarde"
        else -> "Boa noite"
    }
}

@Composable
private fun QuickCard(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = WC
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(c.surface).clickable(onClick = onClick).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(c.accentSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.accentFg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun CoverCard(track: AudioTrack, title: String, subtitle: String, circle: Boolean, onClick: () -> Unit) {
    val c = WC
    Column(Modifier.width(132.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(bottom = 4.dp)) {
        if (circle) {
            Box(Modifier.width(132.dp), contentAlignment = Alignment.Center) { ArtistAvatar(track, 112.dp) }
        } else {
            ArtworkImage(track, Modifier.size(132.dp), RoundedCornerShape(20.dp), ArtSize.MEDIUM)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = if (circle) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
        Text(
            subtitle, color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = if (circle) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
    }
}

@Composable
internal fun HomeScreen(
    data: LibraryData,
    vm: MainViewModel,
    nav: AppNav,
    ps: PlayerState,
    current: AudioTrack?,
    actions: AppActions,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val c = WC
    val favorites = remember(data, vm.favorites) { data.sorted.filter { it.id in vm.favorites } }
    val hello = remember { greeting() }

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(hello, color = c.muted, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("W", color = c.accentFg, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
                        Text("music", color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
                    }
                }
            }
        }

        if (data.tracks.isEmpty()) {
            item {
                val (title, subtitle) = emptyLibraryHint(vm, hasPermission)
                EmptyState(
                    icon = Icons.Default.MusicOff, title = title, subtitle = subtitle,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    if (!hasPermission) {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)
                        ) { Text("Permitir acesso") }
                    } else if (!vm.isLoading) {
                        Button(
                            onClick = { vm.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)
                        ) { Text("Atualizar biblioteca") }
                    }
                }
            }
            return@LazyColumn
        }

        if (current != null) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(24.dp))
                        .background(if (c.useGradients) c.accentSoft else c.surface)
                        .clickable { actions.openFull() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtworkImage(current, Modifier.size(76.dp), RoundedCornerShape(18.dp), ArtSize.MEDIUM)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(if (ps.isPlaying) "Tocando agora" else "Continuar ouvindo", color = c.accentFg, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Text(current.title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(current.artist, color = c.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.size(46.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, "Abrir player", tint = c.onAccent)
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(Icons.Default.Shuffle, "Aleatório", Modifier.weight(1f)) { actions.playList(data.tracks, true) }
                QuickCard(Icons.Default.Favorite, "Favoritas", Modifier.weight(1f)) {
                    if (favorites.isNotEmpty()) actions.playList(favorites, false) else nav.openLibrary("Favoritos")
                }
                QuickCard(Icons.Default.LibraryMusic, "Biblioteca", Modifier.weight(1f)) { nav.openLibrary("Músicas") }
            }
        }

        if (data.recent.isNotEmpty()) {
            item { SectionTitle("Adicionadas recentemente", "Ver tudo") { nav.openLibrary("Músicas") } }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(data.recent, key = { it.id }) { t ->
                        CoverCard(t, t.title, t.artist, false) { actions.play(t, data.recent) }
                    }
                }
            }
        }
        if (favorites.isNotEmpty()) {
            item { SectionTitle("Suas favoritas", "Ver tudo") { nav.openLibrary("Favoritos") } }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favorites.take(20), key = { it.id }) { t ->
                        CoverCard(t, t.title, t.artist, false) { actions.play(t, favorites) }
                    }
                }
            }
        }
        if (data.albums.isNotEmpty()) {
            item { SectionTitle("Álbuns", "Ver tudo") { nav.openLibrary("Álbuns") } }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(data.albums.take(20), key = { it.key }) { g ->
                        CoverCard(g.rep, g.title, g.subtitle, false) { nav.openAlbum(g.key, g.title) }
                    }
                }
            }
        }
        if (data.artists.isNotEmpty()) {
            item { SectionTitle("Artistas", "Ver tudo") { nav.openLibrary("Artistas") } }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(data.artists.take(20), key = { it.key }) { g ->
                        CoverCard(g.rep, g.title, g.subtitle, true) { nav.openArtist(g.key, g.title) }
                    }
                }
            }
        }
        item {
            val hours = data.totalDurationMs / 3_600_000L
            val minutes = (data.totalDurationMs / 60_000L) % 60L
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp).clip(RoundedCornerShape(20.dp))
                    .background(c.surface).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    data.tracks.size.toString() to "músicas",
                    data.albums.size.toString() to "álbuns",
                    data.artists.size.toString() to "artistas",
                    "${hours}h${String.format(java.util.Locale.ROOT, "%02d", minutes)}" to "de música"
                ).forEach { (value, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, color = c.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(label, color = c.muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
