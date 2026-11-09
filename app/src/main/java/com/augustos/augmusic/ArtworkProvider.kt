package com.augustos.augmusic

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 * Entrega a capa de cada faixa para a notificação de mídia e a tela de bloqueio.
 * O endereço antigo (content://media/external/audio/albumart/...) falha em muitos aparelhos
 * com Android 10+, deixando a notificação sem capa. Aqui a imagem é gerada pelo mesmo
 * ArtworkLoader do app e guardada em cache (cacheDir/notif_art). Uso estritamente interno.
 */
internal class ArtworkProvider : ContentProvider() {

    companion object {
        fun uriFor(context: Context, track: AudioTrack): Uri =
            Uri.Builder()
                .scheme("content")
                .authority("${context.packageName}.artwork")
                .appendPath("track")
                .appendPath(track.id.toString())
                .appendQueryParameter("v", (track.coverOverride?.hashCode() ?: 0).toString())
                .build()
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val ctx = context ?: throw FileNotFoundException("sem contexto")
        val id = uri.lastPathSegment?.toLongOrNull() ?: throw FileNotFoundException("id inválido")
        val version = uri.getQueryParameter("v") ?: "0"
        val dir = File(ctx.cacheDir, "notif_art").apply { mkdirs() }
        val file = File(dir, "$id-$version.jpg")

        if (!file.isFile || file.length() == 0L) {
            val track = LibraryRepository.loadTrack(ctx, UserData(ctx), id)
                ?: throw FileNotFoundException("faixa não encontrada")
            val bitmap = runBlocking { ArtworkLoader.load(ctx, track, 512) }
                ?: throw FileNotFoundException("sem capa")
            val tmp = File(dir, "$id-$version.tmp")
            FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            if (!tmp.renameTo(file)) throw FileNotFoundException("falha ao gravar capa")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String = "image/jpeg"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ): Int = 0
}
