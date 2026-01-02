import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  // Encontra todos os announcements onde whatsappAuthor parece ser um número de telefone
  const announcements = await prisma.announcement.findMany({
    where: {
      whatsappAuthor: { not: null }
    },
    select: { id: true, whatsappAuthor: true }
  });

  // Filtra os que são apenas números
  const phoneNumbers = announcements.filter(a =>
    a.whatsappAuthor && /^[0-9]+$/.test(a.whatsappAuthor)
  );

  console.log(`Found ${phoneNumbers.length} announcements with phone number authors`);

  if (phoneNumbers.length > 0) {
    const result = await prisma.announcement.updateMany({
      where: {
        id: { in: phoneNumbers.map(a => a.id) }
      },
      data: {
        whatsappAuthor: 'WhatsApp'
      }
    });

    console.log(`Updated ${result.count} records`);
  }
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect());
