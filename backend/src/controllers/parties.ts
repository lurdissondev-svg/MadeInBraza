import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { AppError } from '../middleware/errorHandler.js';
import { notifyPartyMembers } from '../services/notification.js';
import { createPartyChannel } from './channel.js';
import { PlayerClass } from '@prisma/client';

// Helper to transform party with slots for response
function transformPartyResponse(party: any) {
  return {
    id: party.id,
    name: party.name,
    description: party.description,
    isClosed: party.isClosed,
    createdAt: party.createdAt,
    eventId: party.eventId,
    createdBy: party.createdBy,
    slots: party.slots.map((slot: any) => ({
      id: slot.id,
      playerClass: slot.playerClass,
      filledAsClass: slot.filledAsClass, // Class chosen when filling a FREE slot
      filledBy: slot.filledBy ? {
        id: slot.filledBy.id,
        nick: slot.filledBy.nick,
        playerClass: slot.filledBy.playerClass,
      } : null,
    })),
    // Keep members for backwards compatibility
    members: party.slots
      .filter((slot: any) => slot.filledBy)
      .map((slot: any) => ({
        id: slot.filledBy.id,
        nick: slot.filledBy.nick,
        playerClass: slot.filledBy.playerClass,
        joinedAt: slot.filledBy.joinedAt || new Date().toISOString(),
      })),
  };
}

// Party select fields for queries
const partySelectFields = {
  id: true,
  name: true,
  description: true,
  isClosed: true,
  createdAt: true,
  eventId: true,
  createdBy: {
    select: {
      id: true,
      nick: true,
    },
  },
  slots: {
    select: {
      id: true,
      playerClass: true,
      filledAsClass: true,
      filledBy: {
        select: {
          id: true,
          nick: true,
          playerClass: true,
        },
      },
    },
  },
};

// Get all global parties (without event)
export async function getGlobalParties(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const parties = await prisma.party.findMany({
      where: { eventId: null },
      select: partySelectFields,
      orderBy: { createdAt: 'desc' },
    });

    const transformedParties = parties.map(transformPartyResponse);
    res.json({ parties: transformedParties });
  } catch (err) {
    next(err);
  }
}

// Create a new global party (without event)
export async function createGlobalParty(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { name, description, slots, creatorSlotClass } = req.body;
    const userId = req.user!.userId;

    if (!name || typeof name !== 'string' || name.trim().length === 0) {
      throw new AppError(400, 'Party name is required');
    }

    // Validate slots
    if (!slots || !Array.isArray(slots) || slots.length === 0) {
      throw new AppError(400, 'At least one slot is required');
    }

    // Validate each slot entry: { playerClass: "MAGE" | "FREE", count: 2 }
    const validClasses = [
      'ASSASSIN', 'BRAWLER', 'ATALANTA', 'PIKEMAN', 'FIGHTER',
      'MECHANIC', 'KNIGHT', 'PRIESTESS', 'SHAMAN', 'MAGE', 'ARCHER',
      'FREE' // FREE = null in database, any class can join
    ];

    // Validate creator's chosen class (can be FREE too)
    if (!creatorSlotClass || !validClasses.includes(creatorSlotClass)) {
      throw new AppError(400, 'Creator must choose a valid class slot to occupy');
    }

    const slotEntries: { playerClass: PlayerClass | null; count: number }[] = [];
    let totalSlots = 0;
    let creatorClassExists = false;

    for (const slot of slots) {
      if (!slot.playerClass || !validClasses.includes(slot.playerClass)) {
        throw new AppError(400, `Invalid player class: ${slot.playerClass}`);
      }
      const count = parseInt(slot.count, 10);
      if (isNaN(count) || count < 1 || count > 10) {
        throw new AppError(400, 'Slot count must be between 1 and 10');
      }
      // Convert "FREE" to null for database storage
      const dbPlayerClass = slot.playerClass === 'FREE' ? null : slot.playerClass as PlayerClass;
      slotEntries.push({ playerClass: dbPlayerClass, count });
      totalSlots += count;
      if (slot.playerClass === creatorSlotClass) {
        creatorClassExists = true;
      }
    }

    if (!creatorClassExists) {
      throw new AppError(400, 'Creator must choose a class that exists in the party slots');
    }

    if (totalSlots < 2 || totalSlots > 6) {
      throw new AppError(400, 'Total slots must be between 2 and 6');
    }

    // Get creator's info
    const creator = await prisma.user.findUnique({
      where: { id: userId },
      select: { nick: true, playerClass: true },
    });

    if (!creator) {
      throw new AppError(404, 'User not found');
    }

    // Create party with slots
    const party = await prisma.party.create({
      data: {
        name: name.trim(),
        description: description?.trim() || null,
        eventId: null,
        createdById: userId,
        slots: {
          create: slotEntries.flatMap(entry =>
            Array.from({ length: entry.count }, () => ({
              playerClass: entry.playerClass,
            }))
          ),
        },
      },
      select: partySelectFields,
    });

    // Creator fills the first slot of their chosen class (FREE matches null)
    const creatorDbClass = creatorSlotClass === 'FREE' ? null : creatorSlotClass;
    const creatorSlot = party.slots.find(s => s.playerClass === creatorDbClass);
    if (creatorSlot) {
      // If FREE slot, creator must select a class - use their profile class as default
      const filledAsClass = creatorDbClass === null ? creator.playerClass : null;

      await prisma.partySlot.update({
        where: { id: creatorSlot.id },
        data: {
          filledById: userId,
          filledAsClass: filledAsClass,
        },
      });

      // Refetch to get updated data
      const updatedParty = await prisma.party.findUnique({
        where: { id: party.id },
        select: partySelectFields,
      });

      // Create party channel for chat
      createPartyChannel(party.id, party.name)
        .catch(err => console.error('Failed to create party channel:', err));

      res.status(201).json({ party: transformPartyResponse(updatedParty) });
    } else {
      res.status(201).json({ party: transformPartyResponse(party) });
    }
  } catch (err) {
    next(err);
  }
}

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
      select: partySelectFields,
      orderBy: { createdAt: 'asc' },
    });

    const transformedParties = parties.map(transformPartyResponse);
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
    const { name, description, slots, creatorSlotClass } = req.body;
    const userId = req.user!.userId;

    if (!name || typeof name !== 'string' || name.trim().length === 0) {
      throw new AppError(400, 'Party name is required');
    }

    const event = await prisma.event.findUnique({ where: { id: eventId } });
    if (!event) {
      throw new AppError(404, 'Event not found');
    }

    // Validate slots
    if (!slots || !Array.isArray(slots) || slots.length === 0) {
      throw new AppError(400, 'At least one slot is required');
    }

    // Validate each slot entry: { playerClass: "MAGE" | "FREE", count: 2 }
    const validClasses = [
      'ASSASSIN', 'BRAWLER', 'ATALANTA', 'PIKEMAN', 'FIGHTER',
      'MECHANIC', 'KNIGHT', 'PRIESTESS', 'SHAMAN', 'MAGE', 'ARCHER',
      'FREE' // FREE = null in database, any class can join
    ];

    // Validate creator's chosen class (can be FREE too)
    if (!creatorSlotClass || !validClasses.includes(creatorSlotClass)) {
      throw new AppError(400, 'Creator must choose a valid class slot to occupy');
    }

    const slotEntries: { playerClass: PlayerClass | null; count: number }[] = [];
    let totalSlots = 0;
    let creatorClassExists = false;

    for (const slot of slots) {
      if (!slot.playerClass || !validClasses.includes(slot.playerClass)) {
        throw new AppError(400, `Invalid player class: ${slot.playerClass}`);
      }
      const count = parseInt(slot.count, 10);
      if (isNaN(count) || count < 1 || count > 10) {
        throw new AppError(400, 'Slot count must be between 1 and 10');
      }
      // Convert "FREE" to null for database storage
      const dbPlayerClass = slot.playerClass === 'FREE' ? null : slot.playerClass as PlayerClass;
      slotEntries.push({ playerClass: dbPlayerClass, count });
      totalSlots += count;
      if (slot.playerClass === creatorSlotClass) {
        creatorClassExists = true;
      }
    }

    if (!creatorClassExists) {
      throw new AppError(400, 'Creator must choose a class that exists in the party slots');
    }

    if (totalSlots < 2 || totalSlots > 6) {
      throw new AppError(400, 'Total slots must be between 2 and 6');
    }

    // Get creator's info
    const creator = await prisma.user.findUnique({
      where: { id: userId },
      select: { nick: true, playerClass: true },
    });

    if (!creator) {
      throw new AppError(404, 'User not found');
    }

    // Create party with slots
    const party = await prisma.party.create({
      data: {
        name: name.trim(),
        description: description?.trim() || null,
        eventId,
        createdById: userId,
        slots: {
          create: slotEntries.flatMap(entry =>
            Array.from({ length: entry.count }, () => ({
              playerClass: entry.playerClass,
            }))
          ),
        },
      },
      select: partySelectFields,
    });

    // Creator fills the first slot of their chosen class (FREE matches null)
    const creatorDbClass = creatorSlotClass === 'FREE' ? null : creatorSlotClass;
    const creatorSlot = party.slots.find(s => s.playerClass === creatorDbClass);
    if (creatorSlot) {
      // If FREE slot, creator must select a class - use their profile class as default
      const filledAsClass = creatorDbClass === null ? creator.playerClass : null;

      await prisma.partySlot.update({
        where: { id: creatorSlot.id },
        data: {
          filledById: userId,
          filledAsClass: filledAsClass,
        },
      });

      // Refetch to get updated data
      const updatedParty = await prisma.party.findUnique({
        where: { id: party.id },
        select: partySelectFields,
      });

      // Create party channel for chat
      createPartyChannel(party.id, party.name)
        .catch(err => console.error('Failed to create party channel:', err));

      res.status(201).json({ party: transformPartyResponse(updatedParty) });
    } else {
      res.status(201).json({ party: transformPartyResponse(party) });
    }
  } catch (err) {
    next(err);
  }
}

// Update a party (name and description only)
export async function updateParty(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { partyId } = req.params;
    const { name, description } = req.body;
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
      throw new AppError(403, 'Only the party creator or a leader can edit this party');
    }

    if (!name || typeof name !== 'string' || name.trim().length === 0) {
      throw new AppError(400, 'Party name is required');
    }

    const updatedParty = await prisma.party.update({
      where: { id: partyId },
      data: {
        name: name.trim(),
        description: description?.trim() || null,
      },
      select: partySelectFields,
    });

    res.json({ party: transformPartyResponse(updatedParty) });
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

// Join a party (fill a slot)
export async function joinParty(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { partyId } = req.params;
    const { slotId, selectedClass } = req.body;
    const userId = req.user!.userId;

    if (!slotId) {
      throw new AppError(400, 'Slot ID is required');
    }

    const party = await prisma.party.findUnique({
      where: { id: partyId },
      include: {
        slots: true,
        event: { select: { title: true } },
      },
    });

    if (!party) {
      throw new AppError(404, 'Party not found');
    }

    if (party.isClosed) {
      throw new AppError(400, 'Party is closed');
    }

    // Check if user already has a slot in this party
    const existingSlot = party.slots.find(s => s.filledById === userId);
    if (existingSlot) {
      throw new AppError(400, 'Already a member of this party');
    }

    // Find the requested slot
    const slot = party.slots.find(s => s.id === slotId);
    if (!slot) {
      throw new AppError(404, 'Slot not found');
    }

    if (slot.filledById) {
      throw new AppError(400, 'Slot is already filled');
    }

    // For FREE slots (playerClass is null), selectedClass is required
    const validClasses = [
      'ASSASSIN', 'BRAWLER', 'ATALANTA', 'PIKEMAN', 'FIGHTER',
      'MECHANIC', 'KNIGHT', 'PRIESTESS', 'SHAMAN', 'MAGE', 'ARCHER'
    ];

    let filledAsClass: PlayerClass | null = null;
    if (slot.playerClass === null) {
      // FREE slot - must select a class
      if (!selectedClass || !validClasses.includes(selectedClass)) {
        throw new AppError(400, 'Must select a valid class for FREE slots');
      }
      filledAsClass = selectedClass as PlayerClass;
    }

    // Fill the slot
    await prisma.partySlot.update({
      where: { id: slotId },
      data: {
        filledById: userId,
        filledAsClass: filledAsClass,
      },
    });

    // Check if party is now full and close it
    const filledSlots = party.slots.filter(s => s.filledById).length + 1;
    if (filledSlots >= party.slots.length) {
      await prisma.party.update({
        where: { id: partyId },
        data: { isClosed: true },
      });

      // Notify all party members
      const partyContext = party.event ? `${party.name} (${party.event.title})` : party.name;
      const memberIds = party.slots
        .filter(s => s.filledById)
        .map(s => s.filledById!)
        .concat(userId);

      notifyPartyMembers(
        partyId,
        'Party Completa!',
        `${partyContext} - Todas as vagas preenchidas!`,
        party.eventId ? { partyId, eventId: party.eventId } : { partyId }
      ).catch(err => console.error('Failed to send party full notification:', err));
    }

    // Fetch and return the updated party
    const updatedParty = await prisma.party.findUnique({
      where: { id: partyId },
      select: partySelectFields,
    });

    res.json({ party: transformPartyResponse(updatedParty) });
  } catch (err) {
    next(err);
  }
}

// Leave a party (free a slot)
export async function leaveParty(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { partyId } = req.params;
    const userId = req.user!.userId;

    const party = await prisma.party.findUnique({
      where: { id: partyId },
      include: { slots: true },
    });

    if (!party) {
      throw new AppError(404, 'Party not found');
    }

    // Find user's slot
    const userSlot = party.slots.find(s => s.filledById === userId);
    if (!userSlot) {
      throw new AppError(400, 'Not a member of this party');
    }

    const wasClosed = party.isClosed;

    // Free the slot (also clear filledAsClass)
    await prisma.partySlot.update({
      where: { id: userSlot.id },
      data: {
        filledById: null,
        filledAsClass: null,
      },
    });

    // Reopen party if it was closed
    if (wasClosed) {
      await prisma.party.update({
        where: { id: partyId },
        data: { isClosed: false },
      });
    }

    // Fetch and return the updated party
    const updatedParty = await prisma.party.findUnique({
      where: { id: partyId },
      select: partySelectFields,
    });

    res.json({ party: transformPartyResponse(updatedParty) });
  } catch (err) {
    next(err);
  }
}
