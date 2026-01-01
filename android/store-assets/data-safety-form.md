# Formulário de Segurança de Dados - Google Play

Este documento contém as respostas para o formulário de segurança de dados do Google Play Console.

---

## Seção 1: Visão Geral da Coleta de Dados

### O app coleta ou compartilha algum dos tipos de dados de usuário necessários?
**Resposta:** Sim

### Todos os dados de usuário coletados pelo app são criptografados em trânsito?
**Resposta:** Sim (HTTPS)

### Você fornece uma maneira para os usuários solicitarem a exclusão dos dados?
**Resposta:** Sim (através de contato por email)

---

## Seção 2: Tipos de Dados

### Localização
- [ ] Localização aproximada
- [ ] Localização precisa

**Não coletamos dados de localização.**

---

### Informações pessoais
- [x] **Nome** - Nickname escolhido pelo usuário
  - Coletado: Sim
  - Compartilhado: Sim (visível para outros membros)
  - Obrigatório: Sim
  - Finalidade: Funcionalidade do app, Gerenciamento de conta

- [ ] Endereço de e-mail
- [ ] IDs de usuário (além do nickname interno)
- [ ] Endereço
- [ ] Número de telefone
- [ ] Raça e etnia
- [ ] Crenças políticas ou religiosas
- [ ] Orientação sexual
- [ ] Outras informações

---

### Informações financeiras
- [ ] Informações de pagamento do usuário
- [ ] Histórico de compras
- [ ] Pontuação de crédito
- [ ] Outras informações financeiras

**Não coletamos informações financeiras.**

---

### Saúde e fitness
- [ ] Informações de saúde
- [ ] Informações de fitness

**Não coletamos dados de saúde.**

---

### Mensagens
- [x] **E-mails** - Não
- [x] **SMS ou MMS** - Não
- [x] **Outras mensagens no app**
  - Coletado: Sim
  - Compartilhado: Sim (visível para membros do canal)
  - Obrigatório: Não
  - Finalidade: Funcionalidade do app

---

### Fotos e vídeos
- [x] **Fotos**
  - Coletado: Sim (quando usuário envia no chat)
  - Compartilhado: Sim (visível para membros)
  - Obrigatório: Não
  - Finalidade: Funcionalidade do app

- [x] **Vídeos**
  - Coletado: Sim (quando usuário envia no chat)
  - Compartilhado: Sim (visível para membros)
  - Obrigatório: Não
  - Finalidade: Funcionalidade do app

---

### Arquivos de áudio
- [ ] Gravações de voz ou som
- [ ] Arquivos de música
- [ ] Outros arquivos de áudio

**Não coletamos arquivos de áudio.**

---

### Arquivos e documentos
- [ ] Arquivos e documentos

**Não coletamos arquivos/documentos.**

---

### Calendário
- [ ] Eventos do calendário

**Não acessamos o calendário.**

---

### Contatos
- [ ] Contatos

**Não acessamos contatos.**

---

### Atividade do app
- [x] **Interações no app**
  - Coletado: Sim (participação em eventos, respostas de Siege War)
  - Compartilhado: Sim (visível para membros)
  - Obrigatório: Não
  - Finalidade: Funcionalidade do app

- [ ] Histórico de pesquisa no app
- [x] **Apps instalados** - Não
- [x] **Outro conteúdo gerado pelo usuário**
  - Coletado: Sim (mensagens, respostas)
  - Compartilhado: Sim
  - Finalidade: Funcionalidade do app

- [ ] Outras ações

---

### Navegação na Web
- [ ] Histórico de navegação na Web

**Não coletamos histórico de navegação.**

---

### Informações e desempenho do app
- [ ] Registros de erros
- [ ] Diagnósticos

**Não coletamos logs de erro automaticamente.**

---

### Identificadores do dispositivo ou outros
- [x] **Identificadores do dispositivo**
  - Tipo: Token FCM (Firebase Cloud Messaging)
  - Coletado: Sim
  - Compartilhado: Sim (com Google/Firebase para notificações)
  - Obrigatório: Não (notificações são opcionais)
  - Finalidade: Funcionalidade do app (notificações push)

---

## Seção 3: Práticas de Segurança

### Os dados são criptografados em trânsito?
**Resposta:** Sim
- Toda comunicação usa HTTPS/TLS

### Os dados podem ser excluídos?
**Resposta:** Sim
- Usuários podem solicitar exclusão via email: contato@madeinbraza.com.br
- Prazo de exclusão: até 30 dias

### O app está comprometido com a Política de Famílias do Google Play?
**Resposta:** Não (app não é direcionado a crianças)

---

## Seção 4: Finalidades da Coleta de Dados

Para cada tipo de dado coletado, marque as finalidades:

| Dado | Funcionalidade | Gerenciamento de Conta | Análise | Comunicação | Personalização | Publicidade | Segurança |
|------|----------------|------------------------|---------|-------------|----------------|-------------|-----------|
| Nome/Nickname | ✅ | ✅ | | | | | |
| Mensagens | ✅ | | | ✅ | | | |
| Fotos/Vídeos | ✅ | | | ✅ | | | |
| Token FCM | ✅ | | | ✅ | | | |
| Atividade no app | ✅ | | | | | | |

---

## Resumo para Preenchimento

Ao preencher o formulário no Google Play Console:

1. **Coleta de dados:** Sim
2. **Criptografia em trânsito:** Sim
3. **Exclusão de dados:** Sim (por solicitação)
4. **Tipos de dados:**
   - Informações pessoais (nome)
   - Mensagens
   - Fotos e vídeos
   - Identificadores do dispositivo
   - Atividade do app
5. **Compartilhamento:** Com outros usuários do app e Firebase (Google)
6. **Finalidade principal:** Funcionalidade do app
