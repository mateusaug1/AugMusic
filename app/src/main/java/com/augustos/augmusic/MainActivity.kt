package com.augustos.augmusic

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controllerState = mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AudioEffects.init(applicationContext)
        connectController()

        setContent {
            val vm: MainViewModel = viewModel()
            val settings = vm.settings
            WMusicTheme(settings) {
                // Ícones das barras do sistema acompanham o tema escolhido (claro/escuro/automático).
                val dark = isDarkFor(settings)
                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                        navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark }
                    )
                    onDispose { }
                }
                AugMusicApp(controller = controllerState.value, vm = vm)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Se a conexão inicial com o serviço falhou, tenta de novo ao voltar para o app.
        if (controllerFuture == null) connectController()
    }

    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controllerState.value = future.get()
            } catch (_: Exception) {
                controllerState.value = null
                if (controllerFuture === future) controllerFuture = null
            }
            // ContextCompat.getMainExecutor: Context.getMainExecutor() só existe a partir da API 28 (minSdk é 26).
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controllerState.value = null
        super.onDestroy()
    }
}
