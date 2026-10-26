# Aplicar WMusic V7 pelo Termux

1. Baixe `WMusic-V7-completo.zip` para a pasta Download do Android.
2. No Termux, rode:

```bash
termux-setup-storage
cd ~/AugMusic
cp -a . ../AugMusic-backup-v7-antes
cp /storage/emulated/0/Download/WMusic-V7-completo.zip .
unzip -o WMusic-V7-completo.zip -x '.git/*'
chmod +x gradlew
git status --short
```

3. Confira as mudanças antes de enviar ao GitHub:

```bash
git add -A
git commit -m "feat: WMusic V7 grid editor playlists equalizer"
git push origin main
```

4. Abra `https://github.com/mateusaug1/AugMusic/actions` para conferir a compilação.

O backup é criado antes de copiar/extrair. O ZIP contém o projeto Android, não um APK já compilado.
