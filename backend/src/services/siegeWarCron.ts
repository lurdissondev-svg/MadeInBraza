import cron from 'node-cron';
import { prisma } from '../utils/prisma.js';
import { notifyAllMembers, notifyUsers } from './notification.js';

// Create a new Siege War for the upcoming week
// Called every Monday at 00:00h (right after Sunday SW ends)
async function createWeeklySiegeWar(): Promise<void> {
  try {
    console.log('[SiegeWarCron] Creating new weekly Siege War...');

    // Close any existing active siege wars
    await prisma.siegeWar.updateMany({
      where: { isActive: true },
      data: { isActive: false },
    });

    // Calculate Sunday date (SW event day)
    const now = new Date();
    const dayOfWeek = now.getDay();

    // Find next Sunday (0 = Sunday)
    // If today is Monday (1), Sunday is in 6 days
    // If today is Thursday (4), Sunday is in 3 days
    let daysUntilSunday = (7 - dayOfWeek) % 7;
    if (daysUntilSunday === 0) {
      // If it's Sunday, schedule for next Sunday
      daysUntilSunday = 7;
    }

    const eventDate = new Date(now);
    eventDate.setDate(now.getDate() + daysUntilSunday);
    eventDate.setHours(0, 0, 0, 0);

    // weekStart = when form opens (Monday)
    // weekEnd = event day (Sunday end of day)
    const weekStart = new Date(now);
    weekStart.setHours(0, 0, 0, 0);

    const weekEnd = new Date(eventDate);
    weekEnd.setHours(23, 59, 59, 999);

    const siegeWar = await prisma.siegeWar.create({
      data: {
        weekStart,
        weekEnd,
        isActive: true,
      },
    });

    console.log('[SiegeWarCron] Created Siege War:', {
      id: siegeWar.id,
      weekStart: weekStart.toISOString(),
      weekEnd: weekEnd.toISOString(),
    });

    // Don't send notification on Monday - wait until Thursday
    console.log('[SiegeWarCron] SW created, notification will be sent on Thursday');
  } catch (error) {
    console.error('[SiegeWarCron] Error creating weekly Siege War:', error);
  }
}

// Send notification on Thursday to remind members to fill the form
async function sendThursdayNotification(): Promise<void> {
  try {
    // Find active siege war
    const activeSW = await prisma.siegeWar.findFirst({
      where: { isActive: true },
      orderBy: { createdAt: 'desc' },
    });

    if (!activeSW) {
      console.log('[SiegeWarCron] No active Siege War, skipping Thursday notification');
      return;
    }

    // Format event date
    const formattedEventDate = activeSW.weekEnd.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
    });

    await notifyAllMembers(
      'Siege War - Domingo!',
      `SW domingo (${formattedEventDate})! Responda o formulário até sábado.`,
      { siegeWarId: activeSW.id }
    );

    console.log('[SiegeWarCron] Thursday notification sent');
  } catch (error) {
    console.error('[SiegeWarCron] Error sending Thursday notification:', error);
  }
}

// Send reminder to members who haven't responded (Saturday before Sunday SW)
async function sendSaturdayReminder(): Promise<void> {
  try {
    // Find active siege war
    const activeSW = await prisma.siegeWar.findFirst({
      where: { isActive: true },
      orderBy: { createdAt: 'desc' },
    });

    if (!activeSW) {
      console.log('[SiegeWarCron] No active Siege War, skipping reminder');
      return;
    }

    // Get all approved members
    const allMembers = await prisma.user.findMany({
      where: { status: 'APPROVED' },
      select: { id: true, fcmToken: true },
    });

    // Get members who have responded
    const responses = await prisma.sWResponse.findMany({
      where: { siegeWarId: activeSW.id },
      select: { userId: true },
    });

    const respondedIds = new Set(responses.map((r) => r.userId));

    // Filter to members who haven't responded and have FCM token
    const pendingMembers = allMembers.filter(
      (m) => !respondedIds.has(m.id) && m.fcmToken
    );

    if (pendingMembers.length === 0) {
      console.log('[SiegeWarCron] All members have responded, no reminder needed');
      return;
    }

    const tokens = pendingMembers.map((m) => m.fcmToken!);

    await notifyUsers(
      tokens,
      'Lembrete: SW Amanhã!',
      'Siege War é AMANHÃ! Responda o formulário e não esqueça de estar online para o evento!',
      { siegeWarId: activeSW.id }
    );

    console.log(`[SiegeWarCron] Sent Saturday reminder to ${pendingMembers.length} members`);
  } catch (error) {
    console.error('[SiegeWarCron] Error sending Saturday reminder:', error);
  }
}

// Auto-close Siege War when it ends
async function closeExpiredSiegeWars(): Promise<void> {
  try {
    const now = new Date();

    const closed = await prisma.siegeWar.updateMany({
      where: {
        isActive: true,
        weekEnd: { lt: now },
      },
      data: { isActive: false },
    });

    if (closed.count > 0) {
      console.log(`[SiegeWarCron] Closed ${closed.count} expired Siege Wars`);
    }
  } catch (error) {
    console.error('[SiegeWarCron] Error closing expired Siege Wars:', error);
  }
}

// Delete rejected users after 7 days
async function cleanupRejectedUsers(): Promise<void> {
  try {
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

    const deleted = await prisma.user.deleteMany({
      where: {
        status: 'REJECTED',
        rejectedAt: { lt: sevenDaysAgo },
      },
    });

    if (deleted.count > 0) {
      console.log(`[UserCron] Deleted ${deleted.count} rejected users (after 7 days)`);
    }
  } catch (error) {
    console.error('[UserCron] Error cleaning up rejected users:', error);
  }
}

export function startSiegeWarCron(): void {
  // Run every Monday at 00:00 (midnight, Brazil time) - Create SW for next Sunday
  // Cron: minute hour day-of-month month day-of-week
  // Monday = 1
  cron.schedule('0 0 * * 1', () => {
    console.log('[SiegeWarCron] Running weekly Siege War creation job...');
    createWeeklySiegeWar();
  }, {
    timezone: 'America/Sao_Paulo',
  });

  // Thursday at 10:00 AM - Send notification to fill the form
  // Thursday = 4
  cron.schedule('0 10 * * 4', () => {
    console.log('[SiegeWarCron] Running Thursday notification job...');
    sendThursdayNotification();
  }, {
    timezone: 'America/Sao_Paulo',
  });

  // Saturday at 20:00 (8 PM) - Reminder for those who haven't responded
  // SW is on Sunday, so this is the day before
  // Saturday = 6
  cron.schedule('0 20 * * 6', () => {
    console.log('[SiegeWarCron] Running Saturday reminder job...');
    sendSaturdayReminder();
  }, {
    timezone: 'America/Sao_Paulo',
  });

  // Check for expired Siege Wars every hour
  cron.schedule('0 * * * *', () => {
    closeExpiredSiegeWars();
  }, {
    timezone: 'America/Sao_Paulo',
  });

  // Clean up rejected users daily at 3:00 AM
  cron.schedule('0 3 * * *', () => {
    console.log('[UserCron] Running rejected users cleanup...');
    cleanupRejectedUsers();
  }, {
    timezone: 'America/Sao_Paulo',
  });

  console.log('[SiegeWarCron] Cron jobs scheduled:');
  console.log('  - Weekly SW creation: Every Monday at 00:00 (America/Sao_Paulo)');
  console.log('  - Thursday notification: Every Thursday at 10:00 AM (America/Sao_Paulo)');
  console.log('  - Saturday reminder: Every Saturday at 8:00 PM (for Sunday SW)');
  console.log('  - Expired SW cleanup: Every hour');
  console.log('  - Rejected users cleanup: Every day at 3:00 AM');
}

// Export for manual creation (by leaders)
export { createWeeklySiegeWar };
