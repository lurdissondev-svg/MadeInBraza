# Assets para Google Play Store - Braza

Este diretório contém todos os materiais necessários para publicar o app na Google Play Store.

## Arquivos Incluídos

| Arquivo | Descrição |
|---------|-----------|
| `privacy-policy.html` | Política de Privacidade completa (hospedar online) |
| `store-listing.md` | Textos da listagem (título, descrição, etc.) |
| `data-safety-form.md` | Respostas para o formulário de segurança de dados |

---

## Checklist de Publicação

### 1. Política de Privacidade
- [ ] Hospedar `privacy-policy.html` em um servidor web
- [ ] URL sugerida: `https://madeinbraza.com.br/privacy-policy`
- [ ] Testar se a página está acessível publicamente

### 2. Ícone do App (512x512)
- [ ] Criar ícone em alta resolução
- [ ] Formato: PNG 32-bit com alpha
- [ ] Tamanho exato: 512 x 512 pixels
- [ ] Salvar como: `icon-512.png`

**Como criar:**
```bash
# Opção 1: Se tiver o arquivo fonte vetorial (SVG/AI)
# Exporte em 512x512

# Opção 2: Use ferramentas online como:
# - Canva (canva.com)
# - Figma (figma.com)
# - Android Asset Studio (romannurik.github.io/AndroidAssetStudio/)
```

### 3. Feature Graphic (1024x500)
- [ ] Criar banner promocional
- [ ] Formato: PNG ou JPEG
- [ ] Tamanho exato: 1024 x 500 pixels
- [ ] Salvar como: `feature-graphic.png`

**Sugestão de design:**
- Logo centralizado
- Cores da guilda como fundo
- Texto opcional: "Guilda Made in Braza"

### 4. Screenshots (mínimo 2)
- [ ] Capturar telas do app no emulador ou dispositivo
- [ ] Resolução: 1080 x 1920 (ou maior)
- [ ] Mínimo 2, máximo 8 screenshots

**Telas sugeridas:**
1. Login
2. Anúncios (Home)
3. Siege War
4. Eventos
5. Chat
6. Membros
7. Perfil

**Como capturar no emulador:**
```bash
# No Android Studio: Botão de câmera na barra lateral do emulador
# Ou via ADB:
adb exec-out screencap -p > screenshot.png
```

### 5. Google Play Console

#### Criar Conta de Desenvolvedor
- Taxa única: US$ 25
- Acesse: https://play.google.com/console

#### Preencher Formulário de Segurança de Dados
Use as informações de `data-safety-form.md`

#### Fazer Upload do App
```bash
# Gerar APK/AAB de release
cd /home/lucas/Dev/MadeInBraza/android
./gradlew bundleRelease

# O arquivo estará em:
# app/build/outputs/bundle/release/app-release.aab
```

---

## Comandos Úteis

### Gerar APK de Debug (para testes)
```bash
./gradlew assembleDebug
# Saída: app/build/outputs/apk/debug/app-debug.apk
```

### Gerar Bundle de Release (para Play Store)
```bash
./gradlew bundleRelease
# Saída: app/build/outputs/bundle/release/app-release.aab
```

### Gerar APK de Release
```bash
./gradlew assembleRelease
# Saída: app/build/outputs/apk/release/app-release.apk
```

---

## Estrutura de Arquivos Esperada

```
store-assets/
├── README.md                 # Este arquivo
├── privacy-policy.html       # Política de Privacidade
├── store-listing.md          # Textos da loja
├── data-safety-form.md       # Formulário de segurança
├── icon-512.png             # [CRIAR] Ícone 512x512
├── feature-graphic.png      # [CRIAR] Banner 1024x500
└── screenshots/             # [CRIAR] Pasta com screenshots
    ├── 01-login.png
    ├── 02-home.png
    ├── 03-siege-war.png
    └── ...
```

---

## Contato

Para dúvidas sobre a publicação:
- Email: contato@madeinbraza.com.br
