#!/bin/bash

echo "=== Reiniciando containers MadeInBraza ==="

echo "Reiniciando PostgreSQL..."
docker restart postgres
sleep 3

echo "Reiniciando Backend..."
docker restart braza-backend
sleep 2

echo "Verificando status..."
docker ps --filter "name=postgres" --filter "name=braza-backend" --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "Verificando conexões do banco..."
docker exec postgres psql -U lurdisson -d braza -c "SELECT count(*) as conexoes FROM pg_stat_activity WHERE datname = 'braza';" 2>/dev/null

echo ""
echo "=== Reinício concluído ==="
