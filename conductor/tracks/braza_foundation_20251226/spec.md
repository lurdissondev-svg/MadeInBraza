# Spec: Fundação e Autenticação Segura

## Contexto
Esta trilha estabelece a fundação técnica do projeto Braza, incluindo o ambiente de servidor (Docker), o banco de dados e o fluxo crítico de segurança onde novos usuários se registram mas dependem da aprovação manual dos líderes do clã.

## Requisitos
- **Infraestrutura:** Docker Compose gerenciando Node.js (Backend) e PostgreSQL.
- **Backend:** 
    - API de Registro (Nick, Classe, Senha).
    - Sistema de status de usuário (PENDING, APPROVED, BANNED).
    - Autenticação via JWT.
- **Android:**
    - Tela de Registro com seleção de classe.
    - Tela de Espera ("Aguardando Aprovação").
    - Verificação de status ao abrir o app.
- **Segurança:** Apenas usuários com status 'APPROVED' podem gerar tokens válidos para as rotas protegidas.

## Tech Stack
- Android: Kotlin, Jetpack Compose, Retrofit.
- Backend: Node.js, TypeScript, Express, Prisma ORM.
- DB: PostgreSQL.
- Infra: Docker, Docker Compose.

