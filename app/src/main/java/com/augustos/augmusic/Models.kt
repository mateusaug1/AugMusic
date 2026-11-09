package com.augustos.augmusic

import android.net.Uri

/**
 * Faixa da biblioteca. Os campos *Key são calculados uma única vez no carregamento
 * (agrupamento e busca) para que a UI nunca precise recalculá-los a cada frame.
 */
internal data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val path: String?,
    val uri: Uri,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long = 0L,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    val albumArtist: String = "",
    val composer: String = "",
    val mimeType: String? = null,
    val coverOverride: Uri? = null,
    val albumKey: String = "",
    val artistKey: String = "",
    val searchKey: String = ""
)

internal data class QueueEntry(val mediaId: Long?, val title: String, val artist: String)

internal val APP_TABS = listOf("Músicas", "Álbuns", "Artistas", "Playlists", "Favoritos")
