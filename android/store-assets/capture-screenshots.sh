#!/bin/bash

# Script para capturar screenshots do app para a Play Store
# Uso: ./capture-screenshots.sh [nome]

SCREENSHOTS_DIR="$(dirname "$0")/screenshots"
mkdir -p "$SCREENSHOTS_DIR"

if [ -z "$1" ]; then
    FILENAME="screenshot-$(date +%Y%m%d-%H%M%S).png"
else
    FILENAME="$1.png"
fi

OUTPUT="$SCREENSHOTS_DIR/$FILENAME"

echo "Capturando screenshot..."
adb exec-out screencap -p > "$OUTPUT"

if [ $? -eq 0 ]; then
    echo "Screenshot salvo em: $OUTPUT"

    # Mostrar dimensões
    if command -v file &> /dev/null; then
        file "$OUTPUT"
    fi
else
    echo "Erro ao capturar screenshot. Verifique se o emulador está rodando."
    exit 1
fi
