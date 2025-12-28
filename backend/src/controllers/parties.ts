import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { AppError } from '../middleware/errorHandler.js';
import { notifyPartyMembers } from '../services/notification.js';
import { createPartyChannel } from './channel.js';

// Get all parties for an event
export async function getPartiesByEvent(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { eventId } = req.params;

    const event = await prisma.event.findUnique({ where: { id: eventId } });
    if (!event) {
      throw new AppError(404, 'Event not found');
    }

    const parties = await prisma.party.findMany({
      where: { eventId },
      select: {
        id: true,
        name: true,
        description: true,
        maxMembers: true,
        isClosed: true,
        createdAt: true,
        createdBy: {
          select: {
            id: true,
            nick: true,
          },
        },
        members: {
          select: {
            user: {
              select: {
                id: true,
                nick: true,
                playerClass: true,
              },
            },
            joinedAt: true,
          },
        },
      },
      orderBy: { createdAt: 'asc' },
    });

    // Transform to flatten members
    const transformedParties = parties.map(party => ({
      ...party,
      members: party.members.map(m => ({
        ...m.user,
        joinedAt: m.joinedAt,
      })),
    }));

    res.json({ parties: transformedParties });
  } catch (err) {
    next(err);
  }
}

// Create a new party for an event
export async function createParty(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { eventId } = req.params;
    const { name, description, maxMembers } = req.body;
    const userId = req.user!.userId;

    if (!name || typeof name !== 'string' || name.trim().length === 0) {
      throw new AppError(400, 'Party name is required');
    }

    const event = await prisma.event.findUnique({ where: { id: eventId } });
    if (!event) {
      throw new AppError(404, 'Event not found');
    }

    // Validate maxMembers
    let validatedMaxMembers = 5; // Default
    if (maxMembers !== undefined && maxMembers !== null) {
      const parsed = parseInt(maxMembers, 10);
      if (isNaN(parsed) || parsed < 2 || parsed > 50) {
        throw new AppError(400, 'maxMembers must be between 2 and 50');
      }
      validatedMaxMembers = parsed;
    }

    const party = await prisma.party.create({
      data: {
        name: name.trim(),
        description: description?.trim() || null,
        eventId,
        maxMembers: validatedMaxMembers,
        createdById: userId,
      },
      select: {
        id: true,
        name: true,
        description: true,
        maxMembers: true,
        isClosed: true,
        createdAt: true,
        createdBy: {
          select: {
            id: true,
            nick: true,
            playerClass: true,
          },
        },
      },
    });

    // Creator automatically joins the party
    await prisma.partyMember.create({
      data: {
        partyId: party.id,
        userId,
      },
    });

    // Create party channel for chat
    createPartyChannel(party.id, party.name)
      .catch(err => console.error('Failed to create party channel:', err));

    res.status(201).json({
      party: {
        ...party,
        members: [{
          id: userId,
          nick: party.createdBy.nick,
          playerClass: party.createdBy.playerClass,
          joinedAt: new Date().toISOString(),
        }],
      },
    });
  } catch (err) {
    next(err);
  }
}

// Delete a party
export async function deleteParty(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { partyId } = req.params;
    const userId = req.user!.userId;

    const party = await prisma.party.findUnique({
      where: { id: partyId },
      include: { createdBy: true },
    });

    if (!party) {
      throw new AppError(404, 'Party not found');
    }

    // Check if user is the creator or a leader
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: { role: true },
    });

    if (party.createdById !== userId && user?.role !== 'LEADER') {
      throw new AppError(403, 'Only the party creator or a leader can delete this party');
    }

    await prisma.party.delete({ where: { id: partyId } });

    res.json({ success: true });
  } catch (err) {
    next(err);
  }
}

// Join a party
export async function joinParty(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { partyId } = req.params;
    const userId = req.user!.userId;

    const party = await prisma.party.findUnique({
      where: { id: partyId },
      include: {
        members: true,
        event: { select: { title: true } },
      },
    });

    if (!party) {
      throw new AppError(404, 'Party not found');
    }

    if (party.isClosed) {
      throw new AppError(400, 'Party is closed');
    }

    // Check if already a member
    const existing = await prisma.partyMember.findUnique({
      where: { partyId_userId: { partyId, userId } },
    });

    if (existing) {
      throw new AppError(400, 'Already a member of this party');
    }

    // Check if party is full
    if (party.members.length >= party.maxMembers) {
      throw new AppError(400, 'Party is full');
    }

    await prisma.partyMember.create({
      data: { partyId, userId },
    });

    // Check if party is now full and close it
    const newMemberCount = party.members.length + 1;
    if (newMemberCount >= party.maxMembers) {
      await prisma.party.update({
        where: { id: partyId },
        data: { isClosed: true },
      });

      // Notify all party members
      notifyPartyMembers(
        partyId,
        'Party Completa!',
        `${party.name} (${party.event.title}) - Todas as vagas preenchidas!`,
        { partyId, eventId: party.eventId }
      ).catch(err => console.error('Failed to send party full notification:', err));
    }

    res.json({ success: true, isClosed: newMemberCount >= party.maxMembers });
  } catch (err) {
    next(err);
  }
}

// Leave a party
export async function leaveParty(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { partyId } = req.params;
    const userId = req.user!.userId;

    const membership = await prisma.partyMember.findUnique({
      where: { partyId_userId: { partyId, userId } },
    });

    if (!membership) {
      throw new AppError(400, 'Not a member of this party');
    }

    const party = await prisma.party.findUnique({
      where: { id: partyId },
    });

    if (!party) {
      throw new AppError(404, 'Party not found');
    }

    // If leaving and party was closed, reopen it
    const wasClosed = party.isClosed;

    await prisma.partyMember.delete({
      where: { partyId_userId: { partyId, userId } },
    });

    // Reopen party if it was closed
    if (wasClosed) {
      await prisma.party.update({
        where: { id: partyId },
        data: { isClosed: false },
      });
    }

    res.json({ success: true });
  } catch (err) {
    next(err);
  }
}
