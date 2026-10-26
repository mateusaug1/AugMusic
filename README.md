# WMusic 7.0

Player de música local Android em Kotlin + Jetpack Compose, com identidade visual escura e verde-lima.

## Atualizações V7
- Marca Wmusic e navegação: Início, Músicas, Álbuns, Playlists e Artistas.
- Álbuns e artistas em grade de duas colunas; toque em um cartão para abrir as faixas daquele grupo.
- Artista vazio/`<unknown>` normalizado para **Artista Desconhecido**, com agrupamento dos desconhecidos.
- Editor de título, artista, álbum e capa escolhida da galeria. As alterações de metadados são salvas localmente nas preferências do WMusic; não reescrevem as tags embutidas no arquivo de áudio.
- Playlists locais persistentes, criação pelo botão + e inclusão de faixas pelo menu de pressionar e segurar. A capa da playlist usa a capa da primeira faixa disponível.
- Equalizador com 10 bandas visuais, presets, Bass Boost, Virtualizer e Reverb; a aplicação real dos efeitos depende do suporte do dispositivo/Android.
- Número da versão Android atualizado para 7.0.0.

## Compilar
Abra no Android Studio ou envie para GitHub Actions. O APK ainda precisa ser compilado e testado em um dispositivo; este ZIP é o projeto Android completo, não um APK pronto.

## Permissões
O app solicita permissão de leitura de áudio para listar as músicas locais. A capa escolhida é acessada pela URI autorizada pelo seletor de documentos do Android.
