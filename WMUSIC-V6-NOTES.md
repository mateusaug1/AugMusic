# WMusic 6.0.0 — pacote do projeto

Este pacote contém o projeto Android completo para abrir no Android Studio ou compilar no GitHub Actions.

## Recursos disponíveis
- Identidade WMusic e ícone do app.
- Biblioteca local com busca e abas de Músicas, Álbuns, Artistas, Favoritos e Fila.
- Favoritos salvos localmente.
- Reprodução Media3 com serviço de mídia, controles básicos, mini player e player em tela cheia.
- Velocidade variável, timer de desligamento e letras locais LRC/TXT.
- Compartilhamento e detalhes de arquivos.

## Ainda não implementado
Para evitar prometer funções que não existem no código, estes itens continuam pendentes: equalizador completo (EQ, Bass Boost, Virtualizer, Reverb e Preamp), reordenação da fila por arrastar, playlists persistentes, edição de metadados, exclusão de arquivo com confirmação/permissão e definir música como toque.

## Compilação
O APK não foi compilado neste ambiente. Use Android Studio com JDK 17 e SDK 35, ou o workflow `.github/workflows/android.yml` no GitHub Actions.
