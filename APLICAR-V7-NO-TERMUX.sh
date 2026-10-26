#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ZIP="/storage/emulated/0/Download/WMusic-V7-completo.zip"
REPO="$HOME/AugMusic"
if [ ! -f "$ZIP" ]; then echo "Não encontrei $ZIP"; exit 1; fi
if [ ! -d "$REPO/.git" ]; then echo "Não encontrei o repositório Git em $REPO"; exit 1; fi
command -v unzip >/dev/null 2>&1 || { echo "Instale unzip: pkg install unzip"; exit 1; }
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP="$HOME/AugMusic-backup-v7-$STAMP"
mkdir -p "$BACKUP"
cp -a "$REPO/app" "$REPO/gradle" "$REPO/.github" "$REPO/README.md" "$REPO/build.gradle.kts" "$REPO/settings.gradle.kts" "$REPO/gradle.properties" "$REPO/gradlew" "$BACKUP/" 2>/dev/null || true
cd "$REPO"
unzip -o "$ZIP" -x '.git/*'
chmod +x gradlew
rm -f app/src/main/java/com/augustos/augmusic/MainActivity.kt.bak
echo "V7 extraído. Backup: $BACKUP"
echo "Confira as alterações antes do commit:"
git status --short
