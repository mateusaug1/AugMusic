package com.augustos.augmusic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

internal object ArtSize {
    const val SMALL = 192
    const val MEDIUM = 384
    const val LARGE = 640
}

/**
 * Carregador de capas com cache em memória (LRU por tamanho em KB).
 *
 * Correções em relação à V10:
 *  - o disco (File.isFile) era consultado DURANTE a composição, para cada item da lista e a cada
 *    recomposição; agora tudo acontece em Dispatchers.IO e o resultado fica em cache;
 *  - "content://media/external/audio/albumart" não funciona de forma confiável no Android 10+;
 *    a capa embutida agora vem de ContentResolver.loadThumbnail (API 29+);
 *  - imagens são decodificadas já reduzidas (inSampleSize), nunca no tamanho original.
 * Ordem: capa personalizada > cover.jpg etc. da pasta > capa embutida/miniatura do sistema.
 */
internal object ArtworkLoader {
    private val cache = object : LruCache<String, Bitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024L) / 8L).toInt().coerceIn(4 * 1024, 64 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }
    private val misses: MutableSet<String> = ConcurrentHashMap.newKeySet<String>()
    private val folderCovers = ConcurrentHashMap<String, String>()
    private val COVER_NAMES = listOf("cover.jpg", "cover.jpeg", "cover.png", "folder.jpg", "folder.png")

    private fun key(track: AudioTrack, sizePx: Int): String {
        val base = when {
            track.coverOverride != null -> "o:${track.id}:${track.coverOverride}"
            track.albumId > 0L -> "a:${track.albumId}"
            else -> "t:${track.id}"
        }
        return "$base@$sizePx"
    }

    fun peek(track: AudioTrack, sizePx: Int): Bitmap? = cache.get(key(track, sizePx))

    /** Chamado quando a biblioteca é recarregada, para tentar de novo capas que falharam. */
    fun invalidateNegativeCaches() {
        misses.clear()
        folderCovers.clear()
    }

    suspend fun load(context: Context, track: AudioTrack, sizePx: Int): Bitmap? {
        val k = key(track, sizePx)
        cache.get(k)?.let { return it }
        if (k in misses) return null
        return withContext(Dispatchers.IO) {
            cache.get(k) ?: run {
                val bmp = try {
                    decode(context, track, sizePx)
                } catch (_: Exception) {
                    null
                } catch (_: OutOfMemoryError) {
                    null
                }
                if (bmp != null) cache.put(k, bmp) else misses.add(k)
                bmp
            }
        }
    }

    private inline fun <T> attempt(block: () -> T?): T? = try {
        block()
    } catch (_: Exception) {
        null
    }

    private fun decode(context: Context, track: AudioTrack, sizePx: Int): Bitmap? {
        track.coverOverride?.let { uri ->
            attempt { decodeUri(context, uri, sizePx) }?.let { return it }
        }
        attempt { findFolderCover(track.path)?.let { decodeFile(it, sizePx) } }?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            attempt { context.contentResolver.loadThumbnail(track.uri, Size(sizePx, sizePx), null) }
                ?.let { return it }
        }
        if (track.albumId > 0L) {
            attempt {
                decodeUri(context, Uri.parse("content://media/external/audio/albumart/${track.albumId}"), sizePx)
            }?.let { return it }
        }
        return null
    }

    private fun findFolderCover(path: String?): File? {
        if (path.isNullOrEmpty()) return null
        val parent = File(path).parent ?: return null
        val found = folderCovers.getOrPut(parent) {
            COVER_NAMES.map { File(parent, it) }
                .firstOrNull { it.isFile && it.canRead() }
                ?.absolutePath ?: ""
        }
        return if (found.isEmpty()) null else File(found)
    }

    private fun sampleSize(w: Int, h: Int, target: Int): Int {
        var sample = 1
        while (w / (sample * 2) >= target && h / (sample * 2) >= target) sample *= 2
        return sample
    }

    private fun decodeFile(file: File, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, sizePx)
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun decodeUri(context: Context, uri: Uri, sizePx: Int): Bitmap? {
        fun open(): InputStream? = context.contentResolver.openInputStream(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, sizePx)
        }
        return open()?.use { BitmapFactory.decodeStream(it, null, opts) }
    }
}
