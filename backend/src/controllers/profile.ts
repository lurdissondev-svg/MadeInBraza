import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { AppError } from '../middleware/errorHandler.js';
import { getAvatarUrl, deleteAvatarFile, optimizeGif } from '../middleware/upload.js';
import fs from 'fs';

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
        avatarUrl: true,
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
        avatarUrl: user.avatarUrl,
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
        avatarUrl: true,
      },
    });

    res.json({ user: updated });
  } catch (err) {
    next(err);
  }
}

// Upload avatar
export async function uploadUserAvatar(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const file = req.file;

    if (!file) {
      throw new AppError(400, 'Arquivo de avatar é obrigatório');
    }

    // Optimize GIF if it's a GIF file (like Discord does)
    if (file.mimetype === 'image/gif') {
      await optimizeGif(file.path);
    }

    // Get current user to delete old avatar if exists
    const currentUser = await prisma.user.findUnique({
      where: { id: req.user!.userId },
      select: { avatarUrl: true },
    });

    // Delete old avatar file if exists
    if (currentUser?.avatarUrl) {
      deleteAvatarFile(currentUser.avatarUrl);
    }

    // Update user with new avatar URL
    const avatarUrl = getAvatarUrl(file.filename);

    const updated = await prisma.user.update({
      where: { id: req.user!.userId },
      data: { avatarUrl },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        status: true,
        role: true,
        avatarUrl: true,
      },
    });

    res.json({ avatarUrl, user: updated });
  } catch (err) {
    // Try to clean up file on error
    if (req.file) {
      try {
        fs.unlinkSync(req.file.path);
      } catch {}
    }
    next(err);
  }
}

// Delete avatar
export async function deleteUserAvatar(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    // Get current user to delete avatar file
    const currentUser = await prisma.user.findUnique({
      where: { id: req.user!.userId },
      select: { avatarUrl: true },
    });

    if (!currentUser?.avatarUrl) {
      throw new AppError(400, 'Usuário não possui avatar');
    }

    // Delete avatar file
    deleteAvatarFile(currentUser.avatarUrl);

    // Update user to remove avatar URL
    const updated = await prisma.user.update({
      where: { id: req.user!.userId },
      data: { avatarUrl: null },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        status: true,
        role: true,
        avatarUrl: true,
      },
    });

    res.json({ user: updated });
  } catch (err) {
    next(err);
  }
}
