import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { AppError } from '../middleware/errorHandler.js';

export async function getAnnouncements(
  _req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const announcements = await prisma.announcement.findMany({
      select: {
        id: true,
        title: true,
        content: true,
        createdAt: true,
        updatedAt: true,
        createdBy: {
          select: {
            id: true,
            nick: true,
          },
        },
        whatsappMessageId: true,
        whatsappAuthor: true,
        whatsappTimestamp: true,
        mediaUrl: true,
        mediaType: true,
      },
    });

    // Ordenar por whatsappTimestamp (se existir) ou createdAt - mais recente primeiro
    announcements.sort((a, b) => {
      const dateA = a.whatsappTimestamp || a.createdAt;
      const dateB = b.whatsappTimestamp || b.createdAt;
      return new Date(dateB).getTime() - new Date(dateA).getTime();
    });

    res.json({ announcements });
  } catch (err) {
    next(err);
  }
}

export async function createAnnouncement(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { title, content } = req.body;

    if (!title || typeof title !== 'string' || title.trim().length === 0) {
      throw new AppError(400, 'Announcement title is required');
    }

    if (!content || typeof content !== 'string' || content.trim().length === 0) {
      throw new AppError(400, 'Announcement content is required');
    }

    const announcement = await prisma.announcement.create({
      data: {
        title: title.trim(),
        content: content.trim(),
        createdById: req.user!.userId,
      },
      select: {
        id: true,
        title: true,
        content: true,
        createdAt: true,
        updatedAt: true,
        createdBy: {
          select: {
            id: true,
            nick: true,
          },
        },
        whatsappMessageId: true,
        whatsappAuthor: true,
        whatsappTimestamp: true,
        mediaUrl: true,
        mediaType: true,
      },
    });

    res.status(201).json({ announcement });
  } catch (err) {
    next(err);
  }
}

export async function updateAnnouncement(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;
    const { title, content } = req.body;

    const existing = await prisma.announcement.findUnique({ where: { id } });
    if (!existing) {
      throw new AppError(404, 'Announcement not found');
    }

    const updateData: { title?: string; content?: string } = {};

    if (title !== undefined) {
      if (typeof title !== 'string' || title.trim().length === 0) {
        throw new AppError(400, 'Invalid title');
      }
      updateData.title = title.trim();
    }

    if (content !== undefined) {
      if (typeof content !== 'string' || content.trim().length === 0) {
        throw new AppError(400, 'Invalid content');
      }
      updateData.content = content.trim();
    }

    const announcement = await prisma.announcement.update({
      where: { id },
      data: updateData,
      select: {
        id: true,
        title: true,
        content: true,
        createdAt: true,
        updatedAt: true,
        createdBy: {
          select: {
            id: true,
            nick: true,
          },
        },
        whatsappMessageId: true,
        whatsappAuthor: true,
        whatsappTimestamp: true,
        mediaUrl: true,
        mediaType: true,
      },
    });

    res.json({ announcement });
  } catch (err) {
    next(err);
  }
}

export async function deleteAnnouncement(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;

    const announcement = await prisma.announcement.findUnique({ where: { id } });
    if (!announcement) {
      throw new AppError(404, 'Announcement not found');
    }

    await prisma.announcement.delete({ where: { id } });

    res.json({ success: true });
  } catch (err) {
    next(err);
  }
}
