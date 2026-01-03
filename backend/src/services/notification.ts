import { PrismaClient, UserStatus, Role } from '@prisma/client';
import { sendNotificationToMultiple } from './firebase';

const prisma = new PrismaClient();

export async function notifyAllMembers(
  title: string,
  body: string,
  data?: Record<string, string>,
  excludeUserId?: string
): Promise<{ success: number; failure: number }> {
  const users = await prisma.user.findMany({
    where: {
      status: UserStatus.APPROVED,
      fcmToken: { not: null },
      ...(excludeUserId && { id: { not: excludeUserId } }),
    },
    select: { fcmToken: true },
  });

  const tokens = users
    .map((u) => u.fcmToken)
    .filter((t): t is string => t !== null);

  console.log(`[notifyAllMembers] Found ${tokens.length} tokens`);

  if (tokens.length === 0) {
    return { success: 0, failure: 0 };
  }

  console.log(`[notifyAllMembers] Sending:`, { title, body });
  const result = await sendNotificationToMultiple(tokens, { title, body, data });
  console.log(`[notifyAllMembers] Result:`, result);
  return result;
}

export async function notifyUsersByClass(
  classes: string[],
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<{ success: number; failure: number }> {
  if (classes.length === 0) {
    return notifyAllMembers(title, body, data);
  }

  const users = await prisma.user.findMany({
    where: {
      status: UserStatus.APPROVED,
      fcmToken: { not: null },
      playerClass: { in: classes as any },
    },
    select: { fcmToken: true },
  });

  const tokens = users
    .map((u) => u.fcmToken)
    .filter((t): t is string => t !== null);

  if (tokens.length === 0) {
    return { success: 0, failure: 0 };
  }

  return sendNotificationToMultiple(tokens, { title, body, data });
}

export async function notifyEventParticipants(
  eventId: string,
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<{ success: number; failure: number }> {
  const participants = await prisma.eventParticipant.findMany({
    where: { eventId },
    include: {
      user: {
        select: { fcmToken: true },
      },
    },
  });

  const tokens = participants
    .map((p) => p.user.fcmToken)
    .filter((t): t is string => t !== null);

  console.log(`[notifyEventParticipants] Found ${tokens.length} tokens for event ${eventId}`);

  if (tokens.length === 0) {
    return { success: 0, failure: 0 };
  }

  console.log(`[notifyEventParticipants] Sending:`, { title, body });
  const result = await sendNotificationToMultiple(tokens, { title, body, data });
  console.log(`[notifyEventParticipants] Result:`, result);
  return result;
}

export async function notifyPartyMembers(
  partyId: string,
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<{ success: number; failure: number }> {
  const members = await prisma.partyMember.findMany({
    where: { partyId },
    include: {
      user: {
        select: { fcmToken: true },
      },
    },
  });

  const tokens = members
    .map((m) => m.user.fcmToken)
    .filter((t): t is string => t !== null);

  console.log(`[notifyPartyMembers] Found ${tokens.length} tokens for party ${partyId}`);

  if (tokens.length === 0) {
    return { success: 0, failure: 0 };
  }

  console.log(`[notifyPartyMembers] Sending:`, { title, body });
  const result = await sendNotificationToMultiple(tokens, { title, body, data });
  console.log(`[notifyPartyMembers] Result:`, result);
  return result;
}

export async function notifyUsers(
  tokens: string[],
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<{ success: number; failure: number }> {
  if (tokens.length === 0) {
    return { success: 0, failure: 0 };
  }

  console.log(`[notifyUsers] Sending to ${tokens.length} tokens:`, { title, body });
  const result = await sendNotificationToMultiple(tokens, { title, body, data });
  console.log(`[notifyUsers] Result:`, result);
  return result;
}

export async function notifyLeaders(
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<{ success: number; failure: number }> {
  const leaders = await prisma.user.findMany({
    where: {
      role: Role.LEADER,
      status: UserStatus.APPROVED,
      fcmToken: { not: null },
    },
    select: { fcmToken: true },
  });

  const tokens = leaders
    .map((l) => l.fcmToken)
    .filter((t): t is string => t !== null);

  console.log(`[notifyLeaders] Found ${tokens.length} leader tokens`);

  if (tokens.length === 0) {
    return { success: 0, failure: 0 };
  }

  console.log(`[notifyLeaders] Sending:`, { title, body });
  const result = await sendNotificationToMultiple(tokens, { title, body, data });
  console.log(`[notifyLeaders] Result:`, result);
  return result;
}
