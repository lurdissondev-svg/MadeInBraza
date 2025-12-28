import bcrypt from 'bcrypt';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function resetPassword(nick: string, newPassword: string) {
  const passwordHash = await bcrypt.hash(newPassword, 10);

  const user = await prisma.user.update({
    where: { nick },
    data: { passwordHash },
  });

  console.log(`Senha resetada para usuário: ${user.nick}`);
  console.log(`Nova senha: ${newPassword}`);
}

const nick = process.argv[2];
const newPassword = process.argv[3];

if (!nick || !newPassword) {
  console.log('Uso: npx tsx scripts/reset-password.ts <nick> <nova_senha>');
  process.exit(1);
}

resetPassword(nick, newPassword)
  .then(() => prisma.$disconnect())
  .catch((e) => {
    console.error(e);
    prisma.$disconnect();
    process.exit(1);
  });
