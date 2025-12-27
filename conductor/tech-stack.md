# Tech Stack - Braza Clan App

## 1. Frontend (Mobile)
*   **Linguagem:** Kotlin
*   **Plataforma:** Android Nativo (Mínimo SDK: 24 - Android 7.0)
*   **UI Framework:** Jetpack Compose (Moderno, declarativo e eficiente)
*   **Notificações:** Firebase Cloud Messaging (FCM)
*   **Comunicação Real-time:** Socket.io Client para Java/Kotlin

## 2. Backend (API & Real-time)
*   **Linguagem:** TypeScript
*   **Ambiente de Execução:** Node.js
*   **Framework de API:** Express.js ou Fastify
*   **Servidor Real-time:** Socket.io (Gerenciamento de canais e mensagens)
*   **Segurança:** JSON Web Tokens (JWT) para autenticação e bcrypt para hashes de senha.

## 3. Armazenamento de Dados (Database)
*   **Banco Principal:** PostgreSQL (Relacional)
*   **ORM:** Prisma ou TypeORM (Para facilitar a integração segura com TypeScript)
*   **Criptografia:** AES-256 para dados sensíveis (IDs/Senhas compartilhadas) no banco de dados.

## 4. Infraestrutura e DevOps
*   **Hospedagem:** VPS (Virtual Private Server)
*   **Containerização:** Docker
*   **Orquestração:** Docker Compose (Gerenciando Backend, DB e Proxy)
*   **Proxy Reverso:** Nginx (Tratamento de SSL e roteamento de tráfego)

## 5. Integrações de Terceiros
*   **Firebase:** Utilizado exclusivamente para Push Notifications no Android.

