# WMusic V12 — regras extras para o build de release (R8).
# Media3, Compose, Coil e AndroidX já trazem suas próprias regras de consumidor.

# ViewModels são criados por reflexão (viewModel()).
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Componentes declarados no AndroidManifest (mantidos automaticamente pelo AAPT,
# listados aqui apenas por clareza).
-keep class com.augustos.augmusic.MusicService
-keep class com.augustos.augmusic.ArtworkProvider
