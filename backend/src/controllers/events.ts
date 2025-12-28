import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { AppError } from '../middleware/errorHandler.js';
import type { PlayerClass } from '@prisma/client';
import { notifyAllMembers, notifyUsersByClass, notifyEventParticipants } from '../services/notification.js';
import { createEventChannel } from './channel.js';

const VALID_CLASSES: PlayerClass[] = [
  'ASSASSIN', 'BRAWLER', 'ATALANTA', 'PIKEMAN', 'FIGHTER',
  'MECHANIC', 'KNIGHT', 'PRIESTESS', 'SHAMAN', 'MAGE', 'ARCHER'
];

export async function getEvents(
  _req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const events = await prisma.event.findMany({
      select: {
        id: true,
        title: true,
        description: true,
        eventDate: true,
        maxParticipants: true,
        requiredClasses: true,
        createdAt: true,
        createdBy: {
          select: {
            id: true,
            nick: true,
          },
        },
        participants: {
          select: {
            user: {
              select: {
                id: true,
                nick: true,
                playerClass: true,
              },
            },
          },
        },
      },
      orderBy: { eventDate: 'asc' },
    });

    // Transform to flatten participants
    const transformedEvents = events.map(event => ({
      ...event,
      participants: event.participants.map(p => p.user),
    }));

    res.json({ events: transformedEvents });
  } catch (err) {
    next(err);
  }
}

export async function createEvent(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { title, description, eventDate, maxParticipants, requiredClasses } = req.body;

    if (!title || typeof title !== 'string' || title.trim().length === 0) {
      throw new AppError(400, 'Event title is required');
    }

    if (!eventDate) {
      throw new AppError(400, 'Event date is required');
    }

    const parsedDate = new Date(eventDate);
    if (isNaN(parsedDate.getTime())) {
      throw new AppError(400, 'Invalid event date');
    }

    // Validate maxParticipants
    let validatedMaxParticipants: number | null = null;
    if (maxParticipants !== undefined && maxParticipants !== null) {
      const parsed = parseInt(maxParticipants, 10);
      if (isNaN(parsed) || parsed < 1) {
        throw new AppError(400, 'maxParticipants must be a positive integer');
      }
      validatedMaxParticipants = parsed;
    }

    // Validate requiredClasses
    let validatedClasses: PlayerClass[] = [];
    if (requiredClasses && Array.isArray(requiredClasses)) {
      for (const cls of requiredClasses) {
        if (!VALID_CLASSES.includes(cls as PlayerClass)) {
          throw new AppError(400, `Invalid class: ${cls}`);
        }
      }
      validatedClasses = requiredClasses as PlayerClass[];
    }

    const event = await prisma.event.create({
      data: {
        title: title.trim(),
        description: description?.trim() || null,
        eventDate: parsedDate,
        maxParticipants: validatedMaxParticipants,
        requiredClasses: validatedClasses,
        createdById: req.user!.userId,
      },
      select: {
        id: true,
        title: true,
        description: true,
        eventDate: true,
        maxParticipants: true,
        requiredClasses: true,
        createdAt: true,
        createdBy: {
          select: {
            id: true,
            nick: true,
          },
        },
      },
    });

    // Send push notification to members
    const dateStr = parsedDate.toLocaleDateString('pt-BR');
    const notificationBody = validatedMaxParticipants
      ? `${event.title} - ${dateStr} (${validatedMaxParticipants} vagas)`
      : `${event.title} - ${dateStr}`;

    // Notify only specific classes if requiredClasses is set, otherwise all members
    if (validatedClasses.length > 0) {
      notifyUsersByClass(
        validatedClasses,
        'Novo Evento!',
        notificationBody,
        { eventId: event.id }
      ).catch(err => console.error('Failed to send notification:', err));
    } else {
      notifyAllMembers(
        'Novo Evento!',
        notificationBody,
        { eventId: event.id }
        // TODO: restore after testing: req.user!.userId
      ).catch(err => console.error('Failed to send notification:', err));
    }

    // Create event channel for chat
    createEventChannel(event.id, event.title)
      .catch(err => console.error('Failed to create event channel:', err));

    res.status(201).json({ event: { ...event, participants: [] } });
  } catch (err) {
    next(err);
  }
}

export async function deleteEvent(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;

    const event = await prisma.event.findUnique({ where: { id } });
    if (!event) {
      throw new AppError(404, 'Event not found');
    }

    await prisma.event.delete({ where: { id } });

    res.json({ success: true });
  } catch (err) {
    next(err);
  }
}

export async function joinEvent(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;
    const userId = req.user!.userId;

    // Get event with participants count
    const event = await prisma.event.findUnique({
      where: { id },
      include: {
        participants: true,
      },
    });

    if (!event) {
      throw new AppError(404, 'Event not found');
    }

    // Check if already participating
    const existing = await prisma.eventParticipant.findUnique({
      where: { eventId_userId: { eventId: id, userId } },
    });

    if (existing) {
      throw new AppError(400, 'Already participating in this event');
    }

    // Check if event is full
    if (event.maxParticipants !== null && event.participants.length >= event.maxParticipants) {
      throw new AppError(400, 'Event is full');
    }

    // Check if user's class is allowed
    if (event.requiredClasses.length > 0) {
      const user = await prisma.user.findUnique({
        where: { id: userId },
        select: { playerClass: true },
      });

      if (!user) {
        throw new AppError(404, 'User not found');
      }

      if (!event.requiredClasses.includes(user.playerClass)) {
        throw new AppError(400, `This event requires one of these classes: ${event.requiredClasses.join(', ')}`);
      }
    }

    await prisma.eventParticipant.create({
      data: { eventId: id, userId },
    });

    // Check if event is now full and notify participants
    const newParticipantCount = event.participants.length + 1;
    if (event.maxParticipants !== null && newParticipantCount >= event.maxParticipants) {
      notifyEventParticipants(
        id,
        'Party Completa!',
        `${event.title} - Todas as vagas preenchidas!`,
        { eventId: id }
      ).catch(err => console.error('Failed to send party full notification:', err));
    }

    res.json({ success: true });
  } catch (err) {
    next(err);
  }
}

export async function leaveEvent(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;
    const userId = req.user!.userId;

    const participation = await prisma.eventParticipant.findUnique({
      where: { eventId_userId: { eventId: id, userId } },
    });

    if (!participation) {
      throw new AppError(400, 'Not participating in this event');
    }

    await prisma.eventParticipant.delete({
      where: { eventId_userId: { eventId: id, userId } },
    });

    res.json({ success: true });
  } catch (err) {
    next(err);
  }
}
