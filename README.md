# AugMusic

Player de música local para Android, com identidade visual própria inspirada em players modernos.

## Requisitos
- Android Studio recente
- JDK 17
- Android SDK instalado
- Conexão com a internet na primeira compilação para baixar dependências Gradle

## Abrir e compilar
1. Extraia o ZIP.
2. Abra a pasta `AugMusic` no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Execute em um dispositivo Android ou emulador.
5. Para gerar APK: **Build > Build APK(s)**.

Também é possível, em um ambiente com Gradle e Android SDK configurados, executar `./gradlew assembleDebug`.

## O que está implementado
- Interface Jetpack Compose com tema escuro, navegação e mini player.
- Leitura de áudio local via MediaStore.
- Reprodução via AndroidX Media3 ExoPlayer.
- Serviço de reprodução com MediaSession para manter controles de mídia disponíveis.
- Busca local, favoritos e fila de reprodução em memória.
- Ações básicas de reprodução: play/pause, anterior/próxima e barra de progresso.

## Observações
- O projeto-fonte foi gerado, mas não foi compilado neste ambiente. A compilação final deve ser validada no Android Studio.
- Alguns recursos avançados solicitados (equalizador integrado, edição completa de metadados, letras LRC e estatísticas persistentes) ainda não estão implementados nesta versão inicial.
- A biblioteca exibe arquivos de áudio que o Android disponibiliza pelo MediaStore. Os formatos efetivamente reproduzidos dependem dos codecs do dispositivo.
- A primeira abertura pede a permissão de acesso a áudio necessária para a versão do Android.

## Identidade
Nome: **AugMusic**. O projeto não utiliza o logotipo, o código nem os elementos proprietários do Oto Music.
