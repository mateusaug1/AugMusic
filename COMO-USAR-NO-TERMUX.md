# WMusic 6.0.0 — aplicar no Termux e enviar ao GitHub

## Antes de começar
- Faça backup dos arquivos que você editou manualmente.
- O projeto precisa estar em `~/AugMusic` e o remoto GitHub já configurado.
- Este pacote traz o código-fonte completo que foi preparado nesta atualização. Algumas funções do briefing V6 ainda estão listadas em `WMUSIC-V6-NOTES.md` como pendentes; não considere tudo implementado até serem desenvolvidas e testadas.

## Aplicar o código
Copie `WMusic-v6-source.zip` para a pasta Download do celular e execute:

```bash
cd ~/AugMusic
unzip -o /storage/emulated/0/Download/WMusic-v6-source.zip -d ~/AugMusic
```

## Revisar e enviar ao GitHub
```bash
cd ~/AugMusic
git status --short
git add -A
git commit -m "feat: update WMusic to v6.0.0"
git push origin main
```

Se o Git disser `nothing to commit`, as mudanças já estão aplicadas/commitadas. Se pedir login, conclua a autenticação do GitHub no Termux.

## Compilar
O workflow do GitHub Actions pode compilar o APK após o push. Abra o repositório no GitHub → **Actions** → workflow Android → confira se terminou verde. Baixe o APK em **Artifacts**.

## Nota
Este arquivo é fonte Android/Gradle, não é um APK. A compilação precisa ser confirmada no GitHub Actions. Consulte `WMUSIC-V6-NOTES.md` para ver o que ainda falta do escopo completo V6.
