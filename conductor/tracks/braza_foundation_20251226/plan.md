# Plan: Fundação e Autenticação Segura

## Phase 1: Infrastructure & Project Scaffolding
- [ ] Task: Configurar Docker Compose com PostgreSQL e Backend base.
- [ ] Task: Inicializar projeto Backend (Node.js/TypeScript/Prisma).
- [ ] Task: Inicializar projeto Android (Kotlin/Jetpack Compose).
- [ ] Task: Conductor - User Manual Verification 'Infrastructure Setup' (Protocol in workflow.md)

## Phase 2: Backend Auth & Database
- [ ] Task: Definir Schema do Prisma (User, Role, Class).
- [ ] Task: Implementar Endpoint de Registro de Usuário.
- [ ] Task: Implementar Lógica de Login e geração de JWT (considerando status PENDING).
- [ ] Task: Conductor - User Manual Verification 'Backend Auth' (Protocol in workflow.md)

## Phase 3: Android Registration Flow
- [ ] Task: Desenvolver UI de Registro (Seleção de Nick e 11 Classes).
- [ ] Task: Integrar com API de Registro do Backend.
- [ ] Task: Desenvolver Tela de Espera (Banner de status via Webhooks/Polling).
- [ ] Task: Conductor - User Manual Verification 'Android Registration' (Protocol in workflow.md)

## Phase 4: Leader Approval Logic (MVP)
- [ ] Task: Implementar Endpoint para Líderes listarem usuários PENDING.
- [ ] Task: Implementar Endpoint para aprovação de usuários (mudar status para APPROVED).
- [ ] Task: Testar fluxo completo: Registro -> Pendente -> Aprovação -> Acesso Liberado.
- [ ] Task: Conductor - User Manual Verification 'Leader Approval' (Protocol in workflow.md)

