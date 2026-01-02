# Stage 1: Build web frontend
FROM node:22-alpine AS web-builder

WORKDIR /web

# Copy web source and build
COPY web/package*.json ./
RUN npm ci

COPY web/ ./
ENV BUILD_OUTPUT=dist
RUN npm run build

# Stage 2: Backend
FROM node:22-alpine

WORKDIR /app

# Install gifsicle for GIF compression (like Discord)
RUN apk add --no-cache gifsicle

# Install dependencies
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
RUN mkdir -p /app/uploads/channels /app/public

EXPOSE 3000

# Run migrations and start server
CMD ["sh", "-c", "npx prisma migrate deploy && npx tsx src/index.ts"]
