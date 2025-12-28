# Deploy Braza Backend - VPS com Docker

## Pré-requisitos

- VPS com Ubuntu 22.04+ ou Debian 12+
- Docker e Docker Compose instalados
- Domínio apontando para a VPS (opcional, para HTTPS)

## 1. Instalar Docker (se necessário)

```bash
# Instalar Docker
curl -fsSL https://get.docker.com | sh

# Adicionar usuário ao grupo docker
sudo usermod -aG docker $USER

# Reiniciar sessão ou executar
newgrp docker
```

## 2. Clonar o Repositório

```bash
cd /opt
git clone https://github.com/SEU_USUARIO/MadeInBraza.git
cd MadeInBraza
```

## 3. Configurar Variáveis de Ambiente

Criar arquivo `.env` na raiz do projeto:

```bash
nano .env
```

Conteúdo:

```env
# Database
POSTGRES_USER=braza
POSTGRES_PASSWORD=sua_senha_segura_aqui
POSTGRES_DB=braza

# Backend
NODE_ENV=production
JWT_SECRET=sua_chave_jwt_muito_segura_aqui

# Firebase (copiar do console Firebase)
FIREBASE_PROJECT_ID=seu-projeto
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxx@seu-projeto.iam.gserviceaccount.com
```

## 4. Criar Diretório de Uploads

```bash
mkdir -p backend/uploads/channels
chmod 755 backend/uploads
```

## 5. Build e Deploy

```bash
# Na raiz do projeto (onde está o docker-compose.yml)
cd /opt/MadeInBraza

# Build e iniciar
docker compose up -d --build

# Verificar se está rodando
docker compose ps

# Ver logs
docker compose logs -f backend
```

## 6. Verificar Deploy

```bash
# Testar health check
curl http://localhost:3000/health

# Deve retornar:
# {"status":"ok","timestamp":"..."}
```

## 7. Configurar Nginx (Proxy Reverso)

```bash
sudo apt install nginx -y

sudo nano /etc/nginx/sites-available/braza
```

Conteúdo:

```nginx
server {
    listen 80;
    server_name seu-dominio.com;

    client_max_body_size 30M;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

Ativar:

```bash
sudo ln -s /etc/nginx/sites-available/braza /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 8. HTTPS com Certbot (Opcional)

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d seu-dominio.com
```

## 9. Comandos Úteis

```bash
# Parar
docker compose down

# Reiniciar
docker compose restart

# Rebuild após mudanças
docker compose up -d --build

# Ver logs em tempo real
docker compose logs -f

# Ver logs só do backend
docker compose logs -f backend

# Entrar no container do backend
docker compose exec backend sh

# Acessar banco PostgreSQL
docker compose exec postgres psql -U braza -d braza

# Backup do banco
docker compose exec postgres pg_dump -U braza braza > backup.sql

# Restaurar backup
cat backup.sql | docker compose exec -T postgres psql -U braza -d braza
```

## 10. Atualizar Deploy

```bash
cd /opt/MadeInBraza
git pull origin master
docker compose up -d --build
```

## Estrutura de Arquivos no VPS

```
/opt/MadeInBraza/
├── .env                  # Configurações (não commitado)
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── prisma/
│   │   └── schema.prisma
│   └── uploads/          # Arquivos de mídia (volume persistente)
│       └── channels/
└── ...
```

## Volumes Persistentes

O `docker-compose.yml` configura volumes para:
- `postgres_data` - Banco de dados PostgreSQL
- `./backend/uploads:/app/uploads` - Arquivos de mídia enviados

## Troubleshooting

### Porta 3000 em uso
```bash
sudo lsof -i :3000
sudo kill -9 <PID>
```

### Permissão negada no Docker
```bash
sudo chmod 666 /var/run/docker.sock
```

### Container não inicia
```bash
docker compose logs backend
```

### Erro de migração do banco
```bash
# Resetar banco (CUIDADO: apaga todos os dados)
docker compose down -v
docker compose up -d --build
```

### Problemas com uploads
```bash
# Verificar permissões
ls -la backend/uploads/
chmod -R 755 backend/uploads/

# Verificar se o volume está montado
docker compose exec backend ls -la /app/uploads/
```

## Configuração do App Android

Após o deploy, atualize a URL da API no Android:

**Arquivo:** `android/app/build.gradle.kts`

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://seu-dominio.com/api/\"")
```

Depois rebuild o APK:
```bash
cd android
./gradlew assembleRelease
```
