package com.augustos.augmusic

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.text.Normalizer

private val DIACRITICS = Regex("\\p{Mn}+")

/** Minúsculas e sem acentos: "Música" -> "musica". Usado na busca. */
internal fun normalizeForSearch(text: String): String =
    DIACRITICS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase()

private fun cleanTag(value: String?): String? = value?.trim()?.takeIf {
    it.isNotEmpty() && !it.equals("<unknown>", true) && !it.equals("unknown", true)
}

private fun Cursor.str(col: Int): String? = if (col >= 0 && !isNull(col)) getString(col) else null
private fun Cursor.lng(col: Int): Long = if (col >= 0 && !isNull(col)) getLong(col) else 0L
private fun Cursor.int(col: Int): Int = if (col >= 0 && !isNull(col)) getInt(col) else 0

/**
 * Leitura do MediaStore. Deve SEMPRE ser chamada fora da thread principal
 * (o original consultava o MediaStore direto na UI thread).
 */
internal object LibraryRepository {
    private val COLLECTION: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    fun loadTracks(context: Context, store: UserData): List<AudioTrack> =
        query(context, store, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null)

    fun loadTrack(context: Context, store: UserData, id: Long): AudioTrack? =
        query(context, store, "${MediaStore.Audio.Media._ID} = ?", arrayOf(id.toString())).firstOrNull()

    private fun query(context: Context, store: UserData, selection: String, args: Array<String>?): List<AudioTrack> {
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DATA)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.COMPOSER)
            add(MediaStore.Audio.Media.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
                add(MediaStore.Audio.Media.ALBUM_ARTIST)
            }
        }.toTypedArray()

        val out = ArrayList<AudioTrack>()
        context.contentResolver.query(COLLECTION, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            @Suppress("DEPRECATION")
            val pathCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
            val durationCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val sizeCol = c.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val addedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val modifiedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
            val trackCol = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val yearCol = c.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val composerCol = c.getColumnIndex(MediaStore.Audio.Media.COMPOSER)
            val mimeCol = c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                c.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
            val albumArtistCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST) else -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val albumId = c.lng(albumIdCol)

                val title = store.getOverride(id, "title") ?: cleanTag(c.str(titleCol)) ?: "Música sem título"
                val artist = store.getOverride(id, "artist")?.trim()
                    ?: cleanTag(c.str(artistCol)) ?: "Artista Desconhecido"
                val albumOverride = store.getOverride(id, "album")?.trim()
                val album = albumOverride ?: cleanTag(c.str(albumCol)) ?: "Álbum desconhecido"
                val albumArtist = cleanTag(c.str(albumArtistCol)).orEmpty()

                // Álbum editado manualmente passa a agrupar pelo nome; os demais pelo albumId do sistema.
                val albumKey = if (albumOverride != null || albumId <= 0L)
                    "n:${normalizeForSearch(album)}" else "i:$albumId"

                out += AudioTrack(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    albumId = albumId,
                    path = c.str(pathCol),
                    uri = ContentUris.withAppendedId(COLLECTION, id),
                    duration = c.lng(durationCol),
                    size = c.lng(sizeCol),
                    dateAdded = c.lng(addedCol),
                    dateModified = c.lng(modifiedCol),
                    trackNumber = c.int(trackCol),
                    year = c.int(yearCol),
                    genre = cleanTag(c.str(genreCol)).orEmpty(),
                    albumArtist = albumArtist,
                    composer = cleanTag(c.str(composerCol)).orEmpty(),
                    mimeType = c.str(mimeCol),
                    coverOverride = store.getOverride(id, "cover")?.let { runCatching { Uri.parse(it) }.getOrNull() },
                    albumKey = albumKey,
                    artistKey = artist.lowercase(),
                    searchKey = normalizeForSearch("$title $artist $album")
                )
            }
        }
        return out
    }
}
