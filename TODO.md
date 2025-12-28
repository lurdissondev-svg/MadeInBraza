# Braza App - Status de Desenvolvimento

## Implementado

### Autenticação
- [x] Tela de Login
- [x] Tela de Registro (Nick + Classe)
- [x] Persistência de sessão (manter logado)
- [x] Splash screen com auto-login
- [x] Tela de espera para usuários pendentes
- [x] Logout

### Gestão de Usuários
- [x] Lista de membros aprovados
- [x] Lista de pendentes (Líder)
- [x] Aprovar usuário (Líder)
- [x] Rejeitar usuário (Líder)
- [x] Banir membro (Líder)
- [x] Promover a Líder (Líder)
- [x] Rebaixar a Membro (Líder)

### Perfil
- [x] Visualizar próprio perfil
- [x] Editar nick
- [x] Editar classe

### Eventos
- [x] Lista de eventos
- [x] Criar evento (Líder) - título, descrição, data/hora
- [x] Deletar evento (Líder)
- [x] Participar de evento
- [x] Sair de evento
- [x] Ver participantes do evento

### Chat
- [x] Chat geral da guilda
- [x] Enviar mensagens
- [x] Listar mensagens
- [x] Polling automático (3s)

### UX
- [x] Pull-to-refresh em todas as listas
- [x] Tema preto e branco
- [x] Indicadores de carregamento
- [x] Tratamento de erros

---

## A Implementar

### Prioridade Alta

#### Push Notifications (Firebase)
- [ ] Configurar Firebase Cloud Messaging
- [ ] Notificar membros quando novo evento é criado
- [ ] Notificar quando grupo/party fecha

#### Eventos - Melhorias
- [ ] Quantidade de vagas (limite de participantes)
- [ ] Classes necessárias para o evento
- [ ] Fechar inscrição quando atingir limite

#### Gestão de Usuários
- [ ] Desbanir usuário (Líder)
- [ ] Ver perfil de outros membros

### Prioridade Média

#### Parties (Grupos)
- [ ] Criar party vinculada a evento
- [ ] Limite de membros na party
- [ ] Fechamento automático ao atingir limite
- [ ] Notificar participantes quando party fecha

#### Evento Recorrente - Siege War (SW)
- [ ] Criação automática toda quinta-feira (para domingo)
- [ ] Formulário de confirmação: "Confirmado" ou "Shared/Piloto"
- [ ] Se Shared: campos ID, Senha, Classe (se piloto)
- [ ] Painel para Líderes verem respostas
- [ ] Criptografia dos dados sensíveis (senhas)

#### Chat - Canais Adicionais
- [ ] Chat privado de Líderes
- [ ] Chat temporário por Evento
- [ ] Chat temporário por Party

### Prioridade Baixa

#### UX/UI
- [ ] Bottom Navigation Bar
- [ ] Mural de Anúncios na Home (feed de avisos)
- [ ] Ícone customizado do app (PNG do usuário)

#### Segurança
- [ ] Alterar senha
- [ ] Criptografia de dados sensíveis no banco

---

## Stack Técnica

### Android
- Kotlin + Jetpack Compose
- Material3
- Hilt (DI)
- Retrofit + OkHttp
- DataStore (persistência local)
- Navigation Compose

### Backend
- Node.js + Express
- TypeScript
- Prisma ORM
- PostgreSQL
- JWT (autenticação)

### Infraestrutura
- Docker + Docker Compose
- VPS

---

## Notas

### Classes Disponíveis
1. Assassin (Assassino)
2. Brawler (Lutador)
3. Atalanta
4. Pikeman (Lanceiro)
5. Fighter (Guerreiro)
6. Mechanic (Mecânico)
7. Knight (Cavaleiro)
8. Priestess (Sacerdotisa)
9. Shaman (Xamã)
10. Mage (Mago)
11. Archer (Arqueiro)

### Cargos
- **LEADER**: Controle total (aprovar, banir, criar eventos, ver dados sensíveis)
- **MEMBER**: Membro aprovado (ver eventos, participar, chat)
- **PENDING**: Aguardando aprovação (apenas tela de espera)
- **BANNED**: Banido (sem acesso)
