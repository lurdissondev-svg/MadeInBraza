# Initial Concept

**Nome do App:** Braza
**Plataforma:** Android (APK)
**Infraestrutura:** VPS com Docker

**Funcionalidades Principais:**
- **Autenticação e Autorização:**
    - Cadastro com Nick e Classe.
    - Acesso restrito: Novos usuários precisam da aprovação do 'Lider de clã' (atribuição do cargo 'Membro Braza') para acessar o app.
    - Remoção de acesso se o membro sair do clã.
    - Cargos: 'Lider de clã', 'Membro Braza'.
- **Gestão de Usuários:**
    - Classes disponíveis: Assassin, Brawler, Atalanta, Pikeman, Fighter, Mechanic, Knight, Priestess, Shaman, Mage, Archer.
- **Eventos:**
    - Criação de eventos (Apenas Líderes): Nome, Quantidade de pessoas, Classes necessárias.
    - Notificações push para todos os membros ao criar evento.
    - Inscrição em eventos ('Ingressar').
- **Parties (Grupos):**
    - Membros podem criar grupos abertos vinculados a um evento.
- **Evento Recorrente (SW):**
    - Criação automática toda quinta-feira para o evento de domingo.
    - Enquete: 'Confirmado' ou 'Shared/Piloto'.
    - Lógica condicional: Se 'Shared', solicitar ID, Senha Compartilhada e Classe (se piloto).
- **Comunicação:**
    - Chat em tempo real com todos os membros.
- **Design:**
    - Preto e branco, simples e intuitivo.

**Observações:**
- O usuário fornecerá o ícone em PNG.
- Armazenamento de dados sensíveis (senhas compartilhadas) mencionado nos requisitos.


# Product Guide - Braza Clan App

## 1. Visão Geral
O Braza é um aplicativo Android (APK) exclusivo para a coordenação e gestão de um clã em um jogo MMORPG. O foco principal é a organização de eventos, gestão de membros e comunicação em tempo real, garantindo segurança e exclusividade através de um sistema de aprovação por líderes.

## 2. Personas (Cargos)
*   **Líder de Clã:** Possui controle total. Aprova novos membros, cria eventos oficiais, visualiza dados sensíveis (logins compartilhados) e gerencia todos os canais de chat.
*   **Membro Braza:** Membro aprovado. Pode visualizar eventos, ingressar em vagas, criar "parties" e participar dos chats (Geral e de Eventos).
*   **Visitante/Novo Usuário:** Pode se registrar com Nick e Classe, mas permanece em uma tela de espera até que um Líder atribua o cargo de "Membro Braza".

## 3. Funcionalidades Principais

### A. Autenticação e Gestão de Acesso
*   **Registro Inicial:** O usuário informa Nick e seleciona uma das 11 classes (Assassin, Brawler, Atalanta, Pikeman, Fighter, Mechanic, Knight, Priestess, Shaman, Mage, Archer).
*   **Filtro de Segurança:** Acesso às funcionalidades do app é bloqueado até a aprovação manual de um Líder.
*   **Revogação:** Líderes podem excluir usuários, removendo instantaneamente seu acesso ao app.

### B. Gestão de Eventos e Parties
*   **Eventos Oficiais:** Criados apenas por líderes. Definem nome, quantidade de vagas e classes necessárias.
*   **Inscrição:** Membros clicam em "Ingressar" para ocupar uma vaga compatível com sua classe.
*   **Parties (Grupos de Membros):** Membros podem criar grupos vinculados a um evento. Quando o grupo atinge o limite de vagas, ele é encerrado automaticamente e todos os participantes são notificados.

### C. Evento Recorrente: Siege War (SW)
*   **Automação:** Toda quinta-feira, o sistema gera um evento automático para o domingo seguinte.
*   **Formulário de Confirmação:** Pergunta "Confirmado ou Shared?".
*   **Lógica de Shared/Piloto:** Se o usuário escolher "Shared", deve obrigatoriamente informar ID, Senha e Classe desejada (se for piloto).
*   **Painel Administrativo:** Um resumo das respostas é gerado para os líderes, com acesso seguro e criptografado aos dados de login compartilhados.

### D. Comunicação e Notificações
*   **Canais de Chat:**
    *   **Geral:** Todos os membros aprovados.
    *   **Líderes:** Canal privado apenas para a liderança.
    *   **Temporários:** Canais criados automaticamente para cada Evento ou Party.
*   **Push Notifications:** Via Firebase Cloud Messaging (FCM). Alerta membros sobre novos eventos e fechamento de grupos.

## 4. Segurança e Infraestrutura
*   **Dados Sensíveis:** IDs e Senhas compartilhadas serão criptografados no banco de dados e visíveis exclusivamente para Líderes através de um painel seguro.
*   **Deploy:** O backend será hospedado em uma VPS utilizando Docker para garantir portabilidade e facilidade de manutenção.

## 5. Design e Experiência do Usuário (UX)
*   **Estética:** Minimalista, em Preto e Branco.
*   **Navegação:** Intuitiva, focada em ações rápidas (ingressar em eventos/ler chat).
*   **Ícone:** Fornecido pelo usuário em formato PNG.

