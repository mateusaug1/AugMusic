# WMusic V10 — atualização inicial e GitHub Actions

Este pacote parte do projeto WMusic V7 fornecido pelo usuário. Ele altera a versão Android para 10.0.0, remove a aba Início da navegação, define Músicas como aba inicial e adiciona menu de ordenação persistente (SharedPreferences) com crescente/decrescente.

## O que foi atualizado neste pacote
- Navegação com Músicas, Álbuns, Artistas e Playlists.
- Aba Músicas como tela inicial.
- Menu “ORDENAR POR” em Bottom Sheet.
- Opções: Nome, Álbum, Artista, Artista do álbum, Gênero, Número da faixa, Duração, Ano, Compositor, Data modificada e Data adicionada.
- Persistência da opção e direção de ordenação entre sessões.
- Versão Android 10.0.0.
- GitHub Actions para compilar APK debug ao fazer push em `main`/`master` ou manualmente.

## Limitações importantes
Esta é uma atualização parcial funcional do código V7, não uma implementação completa de todos os requisitos V10. A ordenação por Gênero, Número da faixa, Ano e Compositor aparece no menu, mas a estrutura atual do V7 não consulta esses metadados e ainda precisa ser ampliada para ordenar corretamente por esses campos. Ainda faltam implementação/testes específicos para: restauração persistente da fila e posição, crossfade, cortar áudio/toque, edição do arquivo original, busca de capas na web, navegação de pastas/SD, equalizador em tela cheia com presets persistentes, e garantir a abertura do Player Full ao tocar na notificação. Audio focus e comportamento em segundo plano dependem de testes no aparelho.

Não foi possível compilar neste ambiente porque não há acesso de rede ao servidor de distribuição do Gradle. O GitHub Actions poderá baixar as dependências e informar erros reais de compilação.

## Compilar pelo GitHub
1. Extraia o ZIP para o repositório ou use o script Termux incluído.
2. Revise `git diff`.
3. Faça commit e push.
4. Abra a aba **Actions** no GitHub e baixe o artefato `WMusic-V10-debug` quando a execução terminar.
