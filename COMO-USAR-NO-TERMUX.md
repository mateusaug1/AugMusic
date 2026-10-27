# Aplicar pelo Termux e enviar ao GitHub

1. Baixe `WMusic-V10-GitHub.zip` para a pasta Downloads do celular.
2. No Termux, execute:

```bash
termux-setup-storage
pkg install unzip git -y
cd ~
# Se ainda não tiver o repositório clonado:
git clone URL_DO_SEU_REPOSITORIO AugMusic
```

Troque `URL_DO_SEU_REPOSITORIO` pela URL real do seu repositório. Se `~/AugMusic` já existe e tem a pasta `.git`, não clone de novo.

3. Extraia e prepare a atualização:

```bash
cp /storage/emulated/0/Download/WMusic-V10-GitHub.zip ~/
cd ~/AugMusic
bash APLICAR-V10-NO-TERMUX.sh
```

Observação: o script está dentro do ZIP; caso não esteja no repositório antes de executar, extraia o ZIP numa pasta temporária e rode o script dali, ou use os comandos manuais abaixo.

Comandos manuais (mais simples):

```bash
cd ~/AugMusic
mkdir -p ~/AugMusic-backup-manual
cp -a . ~/AugMusic-backup-manual/
unzip -o /storage/emulated/0/Download/WMusic-V10-GitHub.zip -x '.git/*'
chmod +x gradlew
git diff --stat
git status --short
git add -A
git commit -m "feat: upgrade WMusic to V10 sorting and navigation"
git push origin HEAD
```

4. No GitHub, abra **Actions** e confira o workflow. Baixe `WMusic-V10-debug` nos artefatos se a compilação terminar com sucesso.

O push envia código-fonte, não instala o APK no celular. A compilação precisa terminar com sucesso para haver APK.
