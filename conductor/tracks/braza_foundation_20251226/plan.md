# Plan: Fundação e Autenticação Segura

## Phase 1: Infrastructure & Project Scaffolding ✅
- [x] Task: Configurar Docker Compose com PostgreSQL e Backend base.
- [x] Task: Inicializar projeto Backend (Node.js/TypeScript/Prisma).
- [x] Task: Inicializar projeto Android (Kotlin/Jetpack Compose).

## Phase 2: Backend Auth & Database ✅
- [x] Task: Definir Schema do Prisma (User, Role, Class, Message, Event).
- [x] Task: Implementar Endpoint de Registro de Usuário.
- [x] Task: Implementar Lógica de Login e geração de JWT (considerando status PENDING).

## Phase 3: Android Registration Flow ✅
- [x] Task: Desenvolver UI de Registro (Seleção de Nick e 11 Classes).
- [x] Task: Integrar com API de Registro do Backend.
- [x] Task: Desenvolver Tela de Espera (Polling de status).
- [x] Task: Implementar Splash Screen com auto-login.

## Phase 4: Leader Approval Logic ✅
- [x] Task: Implementar Endpoint para Líderes listarem usuários PENDING.
- [x] Task: Implementar Endpoint para aprovação de usuários.
- [x] Task: Implementar Endpoint para rejeição de usuários.
- [x] Task: Implementar Endpoint para banir membros.
- [x] Task: Implementar Endpoint para promover/rebaixar membros.
- [x] Task: Testar fluxo completo: Registro -> Pendente -> Aprovação -> Acesso Liberado.

## Phase 5: Core Features (MVP) ✅
- [x] Task: Implementar CRUD de Eventos (Líder).
- [x] Task: Implementar participação em Eventos.
- [x] Task: Implementar visualização de participantes.
- [x] Task: Implementar Chat Geral da guilda.
- [x] Task: Implementar Perfil (visualizar e editar).
- [x] Task: Implementar Logout.

## Phase 6: UX Polish ✅
- [x] Task: Tema Preto e Branco (Material3).
- [x] Task: Pull-to-refresh em todas as listas.
- [x] Task: Indicadores de carregamento.
- [x] Task: Tratamento de erros.

---

# Próximas Fases (A Implementar)

## Phase 7: Eventos - Melhorias ✅
- [x] Task: Adicionar campo `maxParticipants` no schema de Event.
- [x] Task: Adicionar campo `requiredClasses` (array) no schema de Event.
- [x] Task: Atualizar CreateEventScreen para incluir vagas e classes.
- [x] Task: Implementar fechamento automático de inscrição.
- [x] Task: Atualizar UI de eventos para mostrar vagas disponíveis.
- [x] Task: Adicionar validação de classe do usuário ao participar.
- [x] Task: Mostrar chips com classes requeridas nos cards de eventos.

## Phase 8: Push Notifications (Firebase) ✅
- [x] Task: Configurar Firebase Cloud Messaging no projeto Android.
- [x] Task: Implementar registro de FCM token no backend.
- [x] Task: Notificar membros quando novo evento é criado.
- [x] Task: Notificar quando grupo/party fecha.

## Phase 9: Gestão de Usuários - Melhorias ✅
- [x] Task: Implementar Desbanir usuário (Líder).
- [x] Task: Implementar Ver perfil de outros membros.

## Phase 10: Parties (Grupos) ✅
- [x] Task: Criar modelo Party no Prisma (vinculada a Event).
- [x] Task: Implementar CRUD de Party.
- [x] Task: Implementar limite de membros na party.
- [x] Task: Implementar fechamento automático ao atingir limite.
- [x] Task: Notificar participantes quando party fecha.
- [x] Task: Implementar UI Android para Parties.

## Phase 11: Siege War (SW) - Evento Recorrente ✅
- [x] Task: Implementar Cron job para criação automática (quinta → domingo).
- [x] Task: Criar modelo SWResponse no Prisma.
- [x] Task: Implementar formulário: "Confirmado" ou "Shared/Piloto".
- [x] Task: Se Shared: campos ID, Senha, Classe (se piloto).
- [x] Task: Implementar criptografia dos dados sensíveis (senhas).
- [x] Task: Implementar Painel para Líderes verem respostas.
- [x] Task: Implementar UI Android para Siege War.

## Phase 12: Chat - Canais Adicionais ✅
- [x] Task: Implementar modelo Channel no Prisma.
- [x] Task: Implementar Chat privado de Líderes.
- [x] Task: Implementar Chat temporário por Evento.
- [x] Task: Implementar Chat temporário por Party.
- [x] Task: Implementar visualização de membros por canal.

## Phase 13: UX/UI - Melhorias ✅
- [x] Task: Implementar Bottom Navigation Bar.
- [x] Task: Implementar Mural de Anúncios na Home (feed de avisos).
- [x] Task: Adicionar ícone customizado do app (PNG do usuário).

## Phase 14: Segurança ✅
- [x] Task: Implementar alteração de senha.
- [x] Task: Implementar criptografia de dados sensíveis no banco.

---

# Notas

## Stack Técnica
- **Android**: Kotlin, Jetpack Compose, Material3, Hilt, Retrofit, DataStore
- **Backend**: Node.js, Express, TypeScript, Prisma, PostgreSQL, JWT
- **Infra**: Docker, Docker Compose, VPS

## Classes Disponíveis
Assassin, Brawler, Atalanta, Pikeman, Fighter, Mechanic, Knight, Priestess, Shaman, Mage, Archer

## Cargos
- **LEADER**: Controle total
- **MEMBER**: Membro aprovado
- **PENDING**: Aguardando aprovação
- **BANNED**: Banido
