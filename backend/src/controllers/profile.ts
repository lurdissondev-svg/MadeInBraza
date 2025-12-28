import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { AppError } from '../middleware/errorHandler.js';

export async function getProfile(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.user!.userId },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        status: true,
        role: true,
        createdAt: true,
        _count: {
          select: {
            messages: true,
            participations: true,
          },
        },
      },
    });

    if (!user) {
      throw new AppError(404, 'User not found');
    }

    res.json({
      profile: {
        id: user.id,
        nick: user.nick,
        playerClass: user.playerClass,
        status: user.status,
        role: user.role,
        createdAt: user.createdAt,
        stats: {
          messagesCount: user._count.messages,
          eventsParticipated: user._count.participations,
        },
      },
    });
  } catch (err) {
    next(err);
  }
}

export async function updateProfile(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { nick, playerClass } = req.body;

    // Validate nick if provided
    if (nick !== undefined) {
      if (typeof nick !== 'string' || nick.trim().length < 3) {
        throw new AppError(400, 'Nick must be at least 3 characters');
      }

      // Check if nick is already taken by another user
      const existingUser = await prisma.user.findUnique({
        where: { nick: nick.trim() },
      });

      if (existingUser && existingUser.id !== req.user!.userId) {
        throw new AppError(400, 'Nick already taken');
      }
    }

    // Validate playerClass if provided
    const validClasses = [
      'ASSASSIN', 'BRAWLER', 'ATALANTA', 'PIKEMAN', 'FIGHTER',
      'MECHANIC', 'KNIGHT', 'PRIESTESS', 'SHAMAN', 'MAGE', 'ARCHER'
    ];

    if (playerClass !== undefined && !validClasses.includes(playerClass)) {
      throw new AppError(400, 'Invalid player class');
    }

    const updateData: any = {};
    if (nick !== undefined) updateData.nick = nick.trim();
    if (playerClass !== undefined) updateData.playerClass = playerClass;

    const updated = await prisma.user.update({
      where: { id: req.user!.userId },
      data: updateData,
      select: {
        id: true,
        nick: true,
        playerClass: true,
        status: true,
        role: true,
      },
    });

    res.json({ user: updated });
  } catch (err) {
    next(err);
  }
}
