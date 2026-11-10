package com.augustos.augmusic

import androidx.compose.runtime.Immutable
import java.text.Collator

internal val AppCollator: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

internal data class Group(
    val key: String,
    val title: String,
    val subtitle: String,
    val rep: AudioTrack,
    val count: Int
)

/** Tudo que as telas derivam da lista de faixas, calculado UMA vez por mudança de dados/ordenação. */
@Immutable
internal class LibraryData(
    val tracks: List<AudioTrack>,
    val sorted: List<AudioTrack>,
    val byId: Map<Long, AudioTrack>,
    val albums: List<Group>,
    val artists: List<Group>,
    val recent: List<AudioTrack>,
    val totalDurationMs: Long
)

internal fun buildLibraryData(tracks: List<AudioTrack>, sortBy: String, descending: Boolean): LibraryData {
    val artistCounts = tracks.groupingBy { it.artistKey }.eachCount()
    val albumCounts = tracks.groupingBy { it.albumKey }.eachCount()
    val byTitle = compareBy<Group, String>(AppCollator) { it.title }
    return LibraryData(
        tracks = tracks,
        sorted = sortTracks(tracks, sortBy, descending),
        byId = tracks.associateBy { it.id },
        albums = tracks.distinctBy { it.albumKey }
            .map { Group(it.albumKey, it.album, it.albumArtist.ifBlank { it.artist }, it, albumCounts[it.albumKey] ?: 0) }
            .sortedWith(byTitle),
        artists = tracks.distinctBy { it.artistKey }
            .map {
                val n = artistCounts[it.artistKey] ?: 0
                Group(it.artistKey, it.artist, if (n == 1) "1 música" else "$n músicas", it, n)
            }
            .sortedWith(byTitle),
        recent = tracks.sortedByDescending { it.dateAdded }.take(20),
        totalDurationMs = tracks.sumOf { it.duration }
    )
}
