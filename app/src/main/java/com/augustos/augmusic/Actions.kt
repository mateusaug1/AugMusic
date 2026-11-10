package com.augustos.augmusic

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

internal fun audioPermissionName(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

internal fun hasAudioPermission(context: Context): Boolean =
    context.checkSelfPermission(audioPermissionName()) == PackageManager.PERMISSION_GRANTED

internal fun Context.toast(message: String, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

/** Todos os critérios do menu "Ordenar" são reais (colunas do MediaStore lidas uma única vez). */
internal fun sortTracks(list: List<AudioTrack>, sortBy: String, descending: Boolean): List<AudioTrack> {
    val byTitle = compareBy<AudioTrack, String>(AppCollator) { it.title }
    val comparator: Comparator<AudioTrack> = when (sortBy) {
        "Álbum" -> compareBy<AudioTrack, String>(AppCollator) { it.album }.thenBy { it.trackNumber }.then(byTitle)
        "Artista" -> compareBy<AudioTrack, String>(AppCollator) { it.artist }
            .then(compareBy<AudioTrack, String>(AppCollator) { it.album })
            .thenBy { it.trackNumber }.then(byTitle)
        "Artista do álbum" -> compareBy<AudioTrack, String>(AppCollator) { it.albumArtist.ifBlank { it.artist } }
            .then(compareBy<AudioTrack, String>(AppCollator) { it.album })
            .thenBy { it.trackNumber }.then(byTitle)
        "Gênero" -> compareBy<AudioTrack, String>(AppCollator) { it.genre }.then(byTitle)
        "Número da faixa" -> compareBy<AudioTrack> { it.trackNumber }.then(byTitle)
        "Duração" -> compareBy<AudioTrack> { it.duration }.then(byTitle)
        "Ano" -> compareBy<AudioTrack> { it.year }.then(byTitle)
        "Compositor" -> compareBy<AudioTrack, String>(AppCollator) { it.composer }.then(byTitle)
        "Data modificada" -> compareBy<AudioTrack> { it.dateModified }.then(byTitle)
        "Data adicionada" -> compareBy<AudioTrack> { it.dateAdded }.then(byTitle)
        else -> byTitle
    }
    return list.sortedWith(if (descending) comparator.reversed() else comparator)
}

internal enum class DeleteResult { DONE, PENDING, FAILED }

internal fun requestDelete(
    context: Context,
    track: AudioTrack,
    launcher: ActivityResultLauncher<IntentSenderRequest>
): DeleteResult = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(track.uri))
        launcher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
        DeleteResult.PENDING
    } else {
        try {
            if (context.contentResolver.delete(track.uri, null, null) > 0) DeleteResult.DONE else DeleteResult.FAILED
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                launcher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                DeleteResult.PENDING
            } else DeleteResult.FAILED
        }
    }
} catch (_: Exception) {
    DeleteResult.FAILED
}

internal fun setAsRingtone(context: Context, track: AudioTrack) {
    if (!Settings.System.canWrite(context)) {
        context.toast("Permita \"Modificar configurações do sistema\" para o WMusic e toque de novo na opção.", true)
        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
        return
    }
    try {
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, track.uri)
        context.toast("Definida como toque")
    } catch (_: Exception) {
        context.toast("O Android recusou definir este arquivo como toque.", true)
    }
}

internal fun shareTrack(context: Context, track: AudioTrack) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(track.uri) ?: "audio/*"
        putExtra(Intent.EXTRA_STREAM, track.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Compartilhar música"))
    } catch (_: ActivityNotFoundException) {
        context.toast("Nenhum app disponível para compartilhar.")
    }
}

/**
 * Abre a pasta da música no app Arquivos. Se nenhum app aceitar o pedido, copia o caminho
 * para a área de transferência (nunca falha em silêncio).
 */
internal fun openFileLocation(context: Context, track: AudioTrack) {
    val folder = LibraryFilter.folderOf(track.path)
    if (folder == null) {
        context.toast("O Android não informou o caminho deste arquivo.", true)
        return
    }
    val docId = LibraryFilter.toDocumentId(folder)
    if (docId != null) {
        try {
            val uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", docId)
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        } catch (_: Exception) {
        }
    }
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("Pasta da música", folder))
    context.toast("Caminho copiado: $folder", true)
}

internal fun adjustVolume(context: Context, raise: Boolean) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    am.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
        AudioManager.FLAG_SHOW_UI
    )
}

internal fun openAppSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}
