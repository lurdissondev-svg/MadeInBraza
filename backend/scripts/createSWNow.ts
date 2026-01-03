// Script para criar um Siege War imediatamente
// Execute com: npx tsx scripts/createSWNow.ts

import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function createSWNow() {
  try {
    console.log('Fechando SWs ativos existentes...');

    // Close any existing active siege wars
    await prisma.siegeWar.updateMany({
      where: { isActive: true },
      data: { isActive: false },
    });

    // Calculate Sunday date (SW event day)
    const now = new Date();
    const dayOfWeek = now.getDay();

    // Find next Sunday (0 = Sunday)
    let daysUntilSunday = (7 - dayOfWeek) % 7;
    if (daysUntilSunday === 0) {
      // If it's Sunday, use today
      daysUntilSunday = 0;
    }

    const eventDate = new Date(now);
    eventDate.setDate(now.getDate() + daysUntilSunday);
    eventDate.setHours(0, 0, 0, 0);

    // weekStart = when form opens (now)
    const weekStart = new Date(now);
    weekStart.setHours(0, 0, 0, 0);

    // weekEnd = event day (Sunday end of day)
    const weekEnd = new Date(eventDate);
    weekEnd.setHours(23, 59, 59, 999);

    console.log('Criando novo SW...');
    console.log('  weekStart:', weekStart.toISOString());
    console.log('  weekEnd:', weekEnd.toISOString());

    const siegeWar = await prisma.siegeWar.create({
      data: {
        weekStart,
        weekEnd,
        isActive: true,
      },
    });

    console.log('SW criado com sucesso!');
    console.log('  ID:', siegeWar.id);
    console.log('  Data do evento:', weekEnd.toLocaleDateString('pt-BR'));
  } catch (error) {
    console.error('Erro ao criar SW:', error);
  } finally {
    await prisma.$disconnect();
  }
}

createSWNow();
