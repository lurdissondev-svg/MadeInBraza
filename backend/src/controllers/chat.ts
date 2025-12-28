import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { AppError } from '../middleware/errorHandler.js';
import { ChannelType } from '@prisma/client';

// Helper to get or create the general channel
async function getGeneralChannel(): Promise<{ id: string }> {
  let general = await prisma.channel.findFirst({
    where: { type: ChannelType.GENERAL },
    select: { id: true },
  });

  if (!general) {
    general = await prisma.channel.create({
      data: {
        type: ChannelType.GENERAL,
        name: 'Geral',
      },
      select: { id: true },
    });
  }

  return general;
}

export async function getMessages(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const limit = Math.min(parseInt(req.query.limit as string) || 50, 100);
    const before = req.query.before as string | undefined;

    // Get the general channel for backward compatibility
    const generalChannel = await getGeneralChannel();

    const where = {
      channelId: generalChannel.id,
      ...(before ? { createdAt: { lt: new Date(before) } } : {}),
    };

    const messages = await prisma.message.findMany({
      where,
      select: {
        id: true,
        content: true,
        createdAt: true,
        user: {
          select: {
            id: true,
            nick: true,
            playerClass: true,
            role: true,
          },
        },
      },
      orderBy: { createdAt: 'desc' },
      take: limit,
    });

    res.json({ messages: messages.reverse() });
  } catch (err) {
    next(err);
  }
}

export async function sendMessage(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { content } = req.body;

    if (!content || typeof content !== 'string' || content.trim().length === 0) {
      throw new AppError(400, 'Message content is required');
    }

    if (content.length > 500) {
      throw new AppError(400, 'Message too long (max 500 characters)');
    }

    // Get the general channel for backward compatibility
    const generalChannel = await getGeneralChannel();

    const message = await prisma.message.create({
      data: {
        content: content.trim(),
        userId: req.user!.userId,
        channelId: generalChannel.id,
      },
      select: {
        id: true,
        content: true,
        createdAt: true,
        user: {
          select: {
            id: true,
            nick: true,
            playerClass: true,
            role: true,
          },
        },
      },
    });

    res.status(201).json({ message });
  } catch (err) {
    next(err);
  }
}
