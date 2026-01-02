# Stage 1: Build web frontend
FROM node:20-slim AS web-builder

WORKDIR /web

# Install build dependencies for native modules
RUN apt-get update && apt-get install -y python3 make g++ && rm -rf /var/lib/apt/lists/*

# Copy web source and build
COPY web/package*.json ./
RUN npm ci

COPY web/ ./
ENV BUILD_OUTPUT=dist
RUN npm run build

# Stage 2: Backend
FROM node:20-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gifsicle \
    python3 \
    make \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# Install dependencies (rebuild native modules for glibc)
COPY backend/package*.json ./
RUN npm ci --only=production

# Copy prisma and generate client
COPY backend/prisma ./prisma/
RUN npx prisma generate

# Copy backend source code
COPY backend/ ./

# Copy built web files from builder stage
COPY --from=web-builder /web/dist ./web/dist

# Create required directories
RUN mkdir -p /app/uploads/channels /app/uploads/avatars /app/uploads/media /app/public

EXPOSE 3000

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD node -e "require('http').get('http://localhost:3000/health', (r) => process.exit(r.statusCode === 200 ? 0 : 1)).on('error', () => process.exit(1))"

# Run migrations, sync schema, and start server
CMD ["sh", "-c", "npx prisma migrate deploy && npx prisma db push --accept-data-loss && npx tsx src/index.ts"]
