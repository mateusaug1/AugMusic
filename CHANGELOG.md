# WMusic — Changelog

## V13.0 (versionCode 13) — redesign completo

> Revisão estática (sem compilar no ambiente de desenvolvimento). Rode `./gradlew assembleDebug`
> e teste no aparelho. Dados da V12/V10 (favoritos, playlists, edições, ordenação, equalizador) são preservados.

### Nova identidade e navegação
- Tema **Claro / Escuro / Automático**, **cor de destaque** (paleta + matiz livre), **cores adaptativas**
  (Material You, Android 12+), **gradientes** e restaurar padrão. Tudo aplicado na hora e persistido.
- Navegação nova: **Início · Biblioteca · Pesquisa · Configurações**, com Mini Player sempre visível acima da barra.
- **Início:** continuar ouvindo, atalhos (aleatório, favoritas), recentes, favoritas, álbuns, artistas e estatísticas.
- **Biblioteca:** abas Músicas/Álbuns/Artistas/Playlists/Favoritos, detalhes com Tocar/Aleatório.
- **Pesquisa** própria (filtros Tudo/Músicas/Álbuns/Artistas, sem acentos). **Fila** com remover e "limpar a seguir".
- Menu contextual da música redesenhado (ações rápidas + itens; exclusão pede confirmação).
- Placeholder de capa com a identidade do WMusic.

### Player Full (sobreposição com transição suave)
- Cabeçalho (minimizar · TOCANDO AGORA · menu ⋮), capa 1:1, título (até 2 linhas) + artista + álbum + favorito,
  barra de progresso real, anterior/play/próxima, controles secundários segmentados (aleatório · repetir
  [off/playlist/música] · favorito), ferramentas (letras, timer, velocidade, fila, equalizador) e informações técnicas.
- **Fundo:** dinâmico (cores da capa, contraste ≥ 4,5:1 garantido, transição suave), sólido, gradiente ou desfocado.
  Cores calculadas uma vez por faixa e em cache (sem animação contínua).
- **Informações técnicas reais** lidas do decodificador (formato, bitrate, frequência). Dado indisponível não é exibido.
- Layout adaptativo (capa e espaçamentos proporcionais; rolagem em telas baixas ou com fonte grande).
- Menu ⋮: detalhes, editar, compartilhar, playlist, toque, abrir localização, gerenciar letras, configurações do player.

### Mini Player
- Estilos Flutuante/Barra/Compacto; capa, título, artista, próxima e progresso opcionais; ação ao tocar configurável.

### Configurações (8 categorias, cada uma com tela própria)
Personalização · Player · Biblioteca · Letras · Behavior · Backup · Localizador · Sobre.

### Biblioteca: Whitelist e Blacklist
- Seletor nativo de pastas; um modo ativo por vez; listas dos dois modos preservadas; filtro em memória
  (trocar de modo/pasta não refaz a varredura). Mensagens claras quando tudo está oculto.

### Letras
- Letras **salvas no app e associadas à faixa**: pesquisa online (LRCLIB, só quando você pede), colar/editar,
  arquivo .lrc/.txt. Exibição estática; versão sincronizada é guardada para o futuro (sem sincronização fictícia).
- Nova permissão: `INTERNET` (usada apenas na pesquisa de letras).

### Behavior (gestos na capa do Player Full)
- Deslizar ←/→/↑/↓, toque duplo e pressionar-e-segurar, cada um com liga/desliga e ação escolhida
  (anterior, próxima, volume ±, play/pause, favoritar, aleatório, repetição, fila, menu). Volume contínuo ao arrastar.

### Backup
- Exporta/importa JSON (configurações, equalizador, ordenação, playlists, favoritos). Valida formato/versão, pede
  confirmação antes de substituir, reencontra músicas por id+título ou título+artista+duração e confere as pastas.

### Localizador
- Busca em todas as músicas indexadas (inclusive as ocultas pelo filtro), mostra pasta/caminho/situação,
  abre a pasta no app Arquivos, mostra na biblioteca e verifica arquivos indisponíveis.

### Outros
- Versão 13.0.0 / código 13, data de compilação em Sobre (`BuildConfig.BUILD_DATE`), temas `values`/`values-night`.
- Workflow do GitHub Actions renomeado para V13.

---

## V12.0 (versionCode 12)

Revisão completa. Todos os recursos da V10 foram mantidos; favoritos, ordenação e edições
do usuário continuam valendo (mesmo arquivo de preferências). As playlists são migradas
automaticamente na primeira abertura (as chaves antigas não são apagadas).

> Esta versão foi revisada de forma estática (sem compilar). Faça `./gradlew assembleDebug`
> e teste no aparelho antes de publicar. Itens marcados com ⚠ merecem teste especial.

### Bugs corrigidos
- **Equalizador só funcionava com o diálogo aberto** (os efeitos eram liberados ao fechar) e as
  bandas voltavam a 0 ao reabrir. Agora os efeitos vivem no `MusicService`, são persistidos e
  reaplicados quando a sessão de áudio muda. Cada efeito é criado de forma independente.
- **Sliders do equalizador eram horizontais de 30 dp**; agora são verticais de verdade.
- 10 sliders → bandas do hardware (geralmente 5): média por faixa de frequência, com limite do nível
  suportado (antes vários sliders sobrescreviam a mesma banda).
- **Aba "Favoritos" inalcançável** — não existia no menu inferior. Adicionada.
- **Abrir álbum/artista mostrava o nome do álbum/artista em todas as faixas** em vez do título.
- Álbum/artista agrupavam por critérios diferentes do filtro (álbuns homônimos se misturavam).
- **Ordenação**: Gênero, Nº da faixa, Ano e Compositor não faziam nada; "Artista do álbum" era igual a
  "Artista". Agora usam as colunas reais do MediaStore. "Data modificada" lia o disco dentro do comparador.
- **Timer de soneca** morria com a Activity; agora vive no serviço. "Fim da música" usa eventos do player
  (correto mesmo se pausar/pular/arrastar). Botões do diálogo estouravam a largura; viraram lista.
- Detalhes do arquivo: "Formato" sempre dava "DESCONHECIDO" e a data saía em segundos brutos.
- Shuffle, repetição e velocidade voltavam ao padrão visual quando a tela era recriada.
- "Anterior" agora reinicia a faixa após 3 s (e não fica morto na primeira faixa).
- Editar com campo em branco gravava texto fixo ("Música sem título") por cima da tag real;
  agora volta a usar a tag do arquivo. Há botão para voltar à capa original.
- Playlists: chave por `hashCode()` do nome podia colidir ("Aa" × "BB"); agora JSON próprio.
  Playlist agora respeita a própria ordem, não permite nomes duplicados (sem distinguir maiúsculas)
  e ganhou **excluir playlist** (toque longo / ícone) e **remover faixa da playlist**.
- Voltar (gesto/botão) dentro de álbum/playlist saía do app; agora volta à lista.
- Tocar na aba "Playlists" dentro de uma playlist não voltava à raiz.
- `Context.mainExecutor` (API 28) quebrava no Android 8.0/8.1 (minSdk 26) → `ContextCompat.getMainExecutor`.
- Tema `Material.Light` deixava a barra de status ilegível e piscava branco ao abrir → tema escuro + edge-to-edge.
- Rotação/mudança de fonte reiniciava tudo e recarregava a biblioteca → `ViewModel` + `configChanges`.
- Permissão negada em definitivo deixava um botão que não fazia nada → atalho para as configurações do app;
  a permissão concedida em Configurações é detectada ao voltar.
- Sem `POST_NOTIFICATIONS` (Android 13+) a notificação de mídia não aparecia → solicitada (uma vez).
- Sem `WAKE_LOCK`/`setWakeMode` a música podia engasgar com a tela apagada.
- Arquivo ilegível/apagado travava o player em silêncio → pula para a próxima faixa.
- Remover o app dos recentes sem nada tocando deixava o serviço vivo → encerra.
- Tocar na notificação agora abre o app.
- `icon_music.png` era na verdade um JPEG 1254×1254 → PNG real 512×512.
- Letras: marcações `[00:12.34]` do .lrc eram exibidas; agora são removidas. No Android 11+ o acesso direto
  a `.lrc/.txt` é bloqueado, então há seletor de arquivo de letra por faixa. ⚠

### Desempenho e estabilidade
- Consulta ao MediaStore agora em `Dispatchers.IO` (antes, na thread principal) e atualização automática
  quando músicas são adicionadas/removidas (`ContentObserver` com debounce). Em erro, mantém a lista atual.
- O loop de 400 ms que recompunha a tela inteira (mesmo pausado) foi substituído por estado granular:
  só mini-player/player completo recompõem, e só enquanto tocando e com o app visível.
- Busca, ordenação e agrupamentos são memorizados (antes refeitos a cada frame); contagens de álbum/artista
  eram O(n²) por item. Busca ignora acentos ("musica" encontra "Música").
- Capas: sem I/O de disco durante a composição; cache LRU; decodificação reduzida; capa embutida via
  `loadThumbnail` (API 29+). Cor do player vem da capa já carregada (sem decodificar tudo de novo).
- Notificação/tela de bloqueio ganham capa de forma confiável via `ArtworkProvider`. ⚠
- Release com R8 (`minify` + `shrinkResources`) e `proguard-rules.pro`.

### Novos (antes eram avisos "será implementado")
- **Excluir música** com confirmação + diálogo do sistema (`createDeleteRequest` no Android 11+). ⚠
- **Definir como toque** (pede "Modificar configurações do sistema"; alguns aparelhos recusam). ⚠

### Mudança de comportamento
- Equalizador começa **desligado** (na V10 o interruptor aparecia ligado, mas nenhum efeito persistia).
  Os valores padrão de Bass Boost 35% / Virtualizer 20% continuam, ativados ao ligar o interruptor.
- Sheet de opções da faixa agora rola (cortava em telas baixas).

### Não alterados
`gradlew`, `gradle/wrapper`, `.github/workflows`, `assets/`, `index.html` e os scripts de Termux
foram mantidos exatamente como estavam.
