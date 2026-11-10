<div align="center">

  <img src="assets/icon_music_app.png" alt="WMusic" width="112" />

  <h1>WMusic</h1>

  <p><strong>Sua música. Seu estilo. Sua experiência.</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Android-App-10120F?style=flat-square&logo=android&logoColor=B7F36B" alt="Android app" />
    <img src="https://img.shields.io/badge/Kotlin-Development-10120F?style=flat-square&logo=kotlin&logoColor=B7F36B" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Status-Em%20desenvolvimento-B7F36B?style=flat-square&labelColor=10120F" alt="Em desenvolvimento" />
  </p>

  <p>
    <a href="https://github.com/mateusaug1/AugMusic">Repositório</a>
    ·
    <a href="https://github.com/mateusaug1/AugMusic/issues">Reportar problema</a>
  </p>

</div>

---

## Sobre

O **WMusic** é um projeto de aplicativo Android desenvolvido em Kotlin, com uma identidade visual escura, minimalista e marcada por detalhes em verde-lima.

O repositório também inclui uma landing page para apresentar o projeto e sua identidade.

## Identidade visual

| Elemento | Cor |
|:--|:--|
| Fundo | `#10120F` |
| Destaque | `#B7F36B` |

A proposta visual prioriza contraste, clareza e uma interface sem excesso de elementos.

## Tecnologias

- **Kotlin** — desenvolvimento Android
- **Gradle** — configuração e compilação
- **Android SDK** — ferramentas para Android
- **HTML e CSS** — landing page
- **Git e GitHub** — versionamento do projeto

## Estrutura

```text
AugMusic/
├── .github/workflows/
├── app/
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── java/com/augustos/augmusic/
│       │   ├── MainActivity.kt        # Activity, tema e conexão com o serviço
│       │   ├── AppRoot.kt             # navegação, permissões, painéis e Player Full (overlay)
│       │   ├── AppNav.kt              # estado de navegação e ações compartilhadas
│       │   ├── HomeScreen.kt          # tela inicial
│       │   ├── LibraryScreen.kt       # músicas, álbuns, artistas, playlists, favoritos
│       │   ├── SearchScreen.kt        # pesquisa
│       │   ├── SettingsScreens.kt     # 8 categorias de configurações
│       │   ├── FullPlayer.kt          # Player Full (fundo dinâmico, gestos)
│       │   ├── MiniPlayer.kt          # Mini Player personalizável
│       │   ├── Sheets.kt              # menu da música, fila, letras
│       │   ├── Dialogs.kt             # ordenar, editar, playlists, timer, velocidade
│       │   ├── EqualizerDialog.kt     # equalizador de 10 bandas
│       │   ├── Components.kt          # capas, chips, estados vazios
│       │   ├── Theme.kt               # tema claro/escuro/automático + cor de destaque
│       │   ├── AppSettings.kt         # todas as preferências (JSON)
│       │   ├── MainViewModel.kt       # estado: biblioteca, filtros, playlists, backup
│       │   ├── LibraryFilter.kt       # Whitelist/Blacklist por pasta
│       │   ├── BackupManager.kt       # exportar/validar/restaurar JSON
│       │   ├── LyricsLoader.kt        # letras salvas, arquivos e pesquisa (LRCLIB)
│       │   ├── MusicService.kt        # ExoPlayer + MediaSession (segundo plano)
│       │   ├── AudioEffects.kt, SleepTimer.kt, PlayerState.kt
│       │   ├── LibraryRepository.kt, LibraryData.kt, UserData.kt
│       │   ├── ArtworkLoader.kt, ArtworkProvider.kt
│       │   └── Actions.kt, Models.kt, Formatters.kt
│       ├── res/
│       └── AndroidManifest.xml
├── assets/
├── gradle/wrapper/
├── index.html
├── CHANGELOG.md
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

## Começar

### Requisitos

- Git
- JDK e Android SDK compatíveis com a configuração do projeto

### Clonar o projeto

```bash
git clone https://github.com/mateusaug1/AugMusic.git
cd AugMusic
```

### Compilar uma versão de depuração

```bash
chmod +x gradlew
./gradlew assembleDebug
```

Se a compilação for concluída, o APK normalmente será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

A compilação depende das versões e ferramentas exigidas pelo ambiente Android configurado.

## Landing page

A página de apresentação está no arquivo [`index.html`](index.html). Para visualizar a página, abra esse arquivo em um navegador ou publique-o em uma hospedagem de sites estáticos.

## Novidades da V12.0

Veja o [`CHANGELOG.md`](CHANGELOG.md) para a lista completa de correções, desempenho e estabilidade.

## Status

**Em desenvolvimento.** Recursos, compatibilidade e instruções podem mudar conforme o projeto evolui.

## Contribuir

1. Faça um fork do repositório.
2. Crie uma branch para sua alteração.
3. Faça as modificações e teste.
4. Abra um pull request explicando o que mudou.

## Desenvolvedor

**Mateus Augusto** · [@mateusaug1](https://github.com/mateusaug1)

---

<div align="center">
  <sub>WMusic — Sua música. Seu estilo.</sub>
</div>
