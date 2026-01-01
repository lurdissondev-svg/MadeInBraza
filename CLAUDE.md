# Claude Code Instructions for MadeInBraza

## Critical Rule: Platform Parity

**IMPORTANT: This project has TWO platforms that MUST always be synchronized:**

1. **Android App** (`/android`) - Kotlin + Jetpack Compose
2. **Web App** (`/web`) - Vue.js 3 + TypeScript

### Synchronization Rules

When making ANY change to one platform, you MUST also apply the equivalent change to the other platform:

| If you change... | You MUST also update... |
|------------------|------------------------|
| Android feature | Web equivalent |
| Web feature | Android equivalent |
| API endpoint usage | Both platforms |
| Data models | Both `Models.kt` and `/web/src/types/index.ts` |
| UI/UX behavior | Both platforms (same user experience) |
| New screens/views | Both platforms |
| Bug fixes | Check if same bug exists on other platform |

### Key Corresponding Files

| Android | Web |
|---------|-----|
| `BrazaApi.kt` | `/web/src/services/api/*.ts` |
| `Models.kt` | `/web/src/types/index.ts` |
| `*Screen.kt` | `/web/src/views/*.vue` |
| `*ViewModel.kt` | `/web/src/stores/*.ts` |
| `*Repository.kt` | `/web/src/services/api/*.ts` |

### Naming Conventions

- **Player Classes**: Use original English names (Assassin, Brawler, Atalanta, etc.) - NO translations
- **Abbreviations**: ASS, BS, ATA, PIKE, FIGHT, MECH, KNT, PRS, SHA, MAGE, ARC
- **UI Labels**: Portuguese (pt-BR) for user-facing text
- **Code/Variables**: English

### Shared Backend

Both platforms use the SAME backend API:
- Production: `https://braza.lurdisson.com.br/api`
- Same database, same endpoints, same data

### Before Completing Any Task

Always ask yourself:
1. Does this change affect the Android app?
2. Does this change affect the Web app?
3. Did I update BOTH platforms?

### Web App Routing

The web app uses Vue Router with base path `/web/`. When defining routes:
- Use relative paths: `/channels`, `/members`, `/login`
- DO NOT include `/web/` prefix in route definitions or `router.push()` calls
- The base path is already configured in the router

### Deployment

- Web app builds to `/backend/web/dist/`
- Backend serves web files at `/web/*`
- After building web app, commit the dist folder
- Production server needs restart/pull to get new changes

## Project Structure

```
MadeInBraza/
├── android/          # Android app (Kotlin)
├── web/              # Web app (Vue.js)
├── backend/          # Node.js + Express + Prisma
│   └── web/dist/     # Built web app files
└── CLAUDE.md         # This file
```

## Common Tasks Checklist

### Adding a new feature:
- [ ] Implement in Android
- [ ] Implement in Web
- [ ] Update types/models if needed
- [ ] Test on both platforms
- [ ] Build web app
- [ ] Commit and push

### Fixing a bug:
- [ ] Fix in the reported platform
- [ ] Check if same bug exists on other platform
- [ ] Fix on other platform if needed
- [ ] Build web app
- [ ] Commit and push

### Changing API usage:
- [ ] Update Android API calls
- [ ] Update Web API calls
- [ ] Update types/models on both sides
- [ ] Test both platforms
