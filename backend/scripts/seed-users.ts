import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcrypt';

const prisma = new PrismaClient();

async function main() {
  console.log('Creating default users...');
  
  const users = [
    {
      nick: 'Lurdisson',
      email: 'lurdissondev@gmail.com',
      password: 'hulkhulk123',
      playerClass: 'ASSASSIN' as const,
      role: 'LEADER' as const,
    },
    {
      nick: 'Senpai',
      email: 'camilamunizq@gmail.com',
      password: '2bewizyou',
      playerClass: 'PRIESTESS' as const,
      role: 'LEADER' as const,
    },
    {
      nick: 'Belion',
      email: 'rafael.vanderson@gmail.com',
      password: 'Filhodevander',
      playerClass: 'KNIGHT' as const,
      role: 'LEADER' as const,
    },
  ];

  for (const user of users) {
    const passwordHash = await bcrypt.hash(user.password, 10);
    
    await prisma.user.create({
      data: {
        nick: user.nick,
        email: user.email,
        passwordHash,
        playerClass: user.playerClass,
        role: user.role,
        status: 'APPROVED',
      },
    });
    
    console.log(`Created user: ${user.nick}`);
  }

  // Create default channels
  await prisma.channel.createMany({
    data: [
      { type: 'GENERAL', name: 'Geral' },
      { type: 'LEADERS', name: 'Líderes' },
    ],
  });
  
  console.log('Created default channels');
  console.log('Seed completed!');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
