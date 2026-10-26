# WMusic V7.0 — notas

## Implementado nesta atualização
- Navegação e identidade Wmusic.
- Grade 2 colunas para álbuns e artistas e navegação para suas faixas.
- Normalização e agrupamento de artistas desconhecidos.
- Edição de título/artista/álbum e seleção de capa da galeria.
- Playlists salvas localmente e opção de adicionar música no menu de contexto.
- Painel de equalizador com dez bandas, presets e controles de Bass Boost, Virtualizer e Reverb.
- `versionCode = 7`, `versionName = 7.0.0`.

## Limitações transparentes
- A edição de metadados é um override local dentro do WMusic; não altera as tags embutidas do arquivo de áudio original.
- A capa da playlist é representada pela capa da primeira faixa disponível, não por uma montagem de várias capas.
- Efeitos de equalização dependem da implementação de AudioEffect do aparelho e da sessão de áudio ativa.
- O projeto não foi compilado neste ambiente, porque o wrapper Gradle não conseguiu baixar a distribuição por falta de acesso à rede. Compile via GitHub Actions e corrija eventuais erros adicionais antes de instalar.
