#!/bin/bash

# Made in Braza - Release Script
# Uso: ./release.sh [patch|minor|major] "mensagem do commit"
# Exemplo: ./release.sh patch "fix: corrigido bug no mural"

set -e

cd "$(dirname "$0")"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Argumentos
BUMP_TYPE=${1:-patch}
COMMIT_MSG=${2:-"chore: release update"}

# Arquivo de versão
BUILD_GRADLE="android/app/build.gradle.kts"

# Extrai versão atual
CURRENT_VERSION=$(grep 'versionName = ' "$BUILD_GRADLE" | sed 's/.*versionName = "\(.*\)"/\1/')
CURRENT_CODE=$(grep 'versionCode = ' "$BUILD_GRADLE" | sed 's/.*versionCode = \([0-9]*\)/\1/')

echo -e "${YELLOW}Versão atual: v$CURRENT_VERSION (code: $CURRENT_CODE)${NC}"

# Divide a versão em partes
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

# Incrementa versão
case $BUMP_TYPE in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
    *)
        echo -e "${RED}Tipo inválido: $BUMP_TYPE (use: patch, minor, major)${NC}"
        exit 1
        ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
NEW_CODE=$((CURRENT_CODE + 1))

echo -e "${GREEN}Nova versão: v$NEW_VERSION (code: $NEW_CODE)${NC}"

# Verifica se há mudanças
if [[ -z $(git status --porcelain) ]]; then
    echo -e "${YELLOW}Nenhuma mudança detectada. Criando apenas a tag...${NC}"
else
    # Atualiza versão no build.gradle.kts
    sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" "$BUILD_GRADLE"
    sed -i "s/versionName = \"$CURRENT_VERSION\"/versionName = \"$NEW_VERSION\"/" "$BUILD_GRADLE"

    echo -e "${GREEN}Versão atualizada no build.gradle.kts${NC}"

    # Commit das mudanças
    git add -A
    git commit -m "$COMMIT_MSG

🤖 Generated with [Claude Code](https://claude.com/claude-code)"

    echo -e "${GREEN}Commit criado${NC}"
fi

# Cria e push da tag
git tag "v$NEW_VERSION"
git push origin master
git push origin "v$NEW_VERSION"

echo ""
echo -e "${GREEN}✅ Release v$NEW_VERSION publicada com sucesso!${NC}"
echo -e "${YELLOW}GitHub Actions está gerando o APK...${NC}"
echo -e "Acompanhe: https://github.com/lurdissondev-svg/MadeInBraza/actions"
