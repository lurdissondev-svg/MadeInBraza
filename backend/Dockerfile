FROM node:22-alpine

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci --only=production

# Copy prisma and generate client
COPY prisma ./prisma/
RUN npx prisma generate

# Copy source code
COPY . .

# Create required directories
RUN mkdir -p /app/uploads/channels /app/public

EXPOSE 3000

# Run migrations and start server
CMD ["sh", "-c", "npx prisma migrate deploy && npx tsx src/index.ts"]
