# WMusic 6.0.0

Player de música local para Android, com identidade visual verde-lima sobre fundo `#10120F`.

## Incluído neste pacote
- Projeto Android completo (Kotlin + Jetpack Compose + AndroidX Media3).
- Reprodução local e serviço de mídia em segundo plano.
- Biblioteca pesquisável, capa/arte do álbum, mini player e player em tela cheia.
- Abas de Músicas, Álbuns, Artistas, Favoritos e Fila.
- Favoritos persistidos no aparelho.
- Aleatório, repetição, barra de progresso, fila, compartilhamento e detalhes do arquivo.
- Velocidade 0.5x, 0.8x, 1x, 1.2x, 1.5x e 2x.
- Timer de 15, 30, 45 e 60 minutos e opção de parar no fim da faixa.
- Leitura de letras locais `.lrc`/`.txt` com o mesmo nome da música ou `lyrics.lrc`/`lyrics.txt`.
- Workflow do GitHub Actions para compilar e publicar o APK como artefato.

## Gerar APK
### Pelo GitHub
1. Extraia o ZIP e envie os arquivos para o seu repositório GitHub.
2. Na aba **Actions**, execute **Build AugMusic APK** (ou faça push para `main`).
3. Abra a execução concluída e baixe o artefato `AugMusic-debug`.

### Pelo Android Studio
1. Abra a pasta do projeto.
2. Use JDK 17 e Android SDK Platform 35.
3. Aguarde o Gradle sincronizar e execute `assembleDebug`.

## Requisitos
- Android 8.0 (API 26) ou superior.
- A primeira abertura pede permissão para ler os arquivos de áudio.
- A reprodução dos formatos depende dos codecs disponíveis no Android.

## Limitações conhecidas
Este é um projeto Android completo e editável, mas não afirmamos que todas as funções avançadas de um player comercial estejam concluídas. Equalizador DSP com Bass Boost/Virtualizer/Reverb/Preamp, edição de tags, exclusão segura com confirmação, definir toque e playlists persistentes precisam de implementação e teste próprios. A reordenação por arrastar da fila também não está implementada. Não foi possível gerar/testar o APK neste ambiente porque não há Android SDK instalado aqui.
