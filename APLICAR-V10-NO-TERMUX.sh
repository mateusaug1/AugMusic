#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ZIP="/storage/emulated/0/Download/WMusic-V10-GitHub.zip"
REPO="$HOME/AugMusic"

if [ ! -f "$ZIP" ]; then
  echo "ERRO: coloque WMusic-V10-GitHub.zip em Downloads."
  exit 1
fi
if [ ! -d "$REPO/.git" ]; then
  echo "ERRO: repositório Git não encontrado em $REPO."
  echo "Clone seu repositório primeiro: git clone URL_DO_SEU_REPOSITORIO ~/AugMusic"
  exit 1
fi
command -v unzip >/dev/null 2>&1 || pkg install unzip -y
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP="$HOME/AugMusic-backup-$STAMP"
mkdir -p "$BACKUP"
cp -a "$REPO/." "$BACKUP/"
echo "Backup criado em: $BACKUP"

cd "$REPO"
unzip -o "$ZIP" -x '.git/*'
chmod +x gradlew
git status --short

echo
echo "Arquivos atualizados. Revise antes de enviar:"
echo "  git diff --stat"
echo "  git diff"
echo
echo "Quando estiver satisfeito, envie:"
echo "  git add -A"
echo '  git commit -m "feat: upgrade WMusic to V10 sorting and navigation"'
echo "  git push origin HEAD"
