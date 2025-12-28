import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { AppError } from '../middleware/errorHandler.js';

export async function getMembers(
  _req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const members = await prisma.user.findMany({
      where: { status: 'APPROVED' },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        role: true,
        createdAt: true,
      },
      orderBy: [
        { role: 'asc' }, // LEADER first
        { nick: 'asc' },
      ],
    });

    res.json({ members });
  } catch (err) {
    next(err);
  }
}

export async function getPendingUsers(
  _req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const users = await prisma.user.findMany({
      where: { status: 'PENDING' },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        createdAt: true,
      },
      orderBy: { createdAt: 'asc' },
    });

    res.json({ users });
  } catch (err) {
    next(err);
  }
}

export async function approveUser(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;

    const user = await prisma.user.findUnique({ where: { id } });
    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    if (user.status !== 'PENDING') {
      throw new AppError(400, 'Usuário não está pendente');
    }

    const updated = await prisma.user.update({
      where: { id },
      data: {
        status: 'APPROVED',
        approvedBy: req.user!.userId,
        approvedAt: new Date(),
      },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        status: true,
        approvedAt: true,
      },
    });

    res.json({ user: updated });
  } catch (err) {
    next(err);
  }
}

export async function rejectUser(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;

    const user = await prisma.user.findUnique({ where: { id } });
    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    if (user.status !== 'PENDING') {
      throw new AppError(400, 'Usuário não está pendente');
    }

    await prisma.user.delete({ where: { id } });

    res.json({ success: true, message: 'Usuário rejeitado' });
  } catch (err) {
    next(err);
  }
}

export async function banUser(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;

    const user = await prisma.user.findUnique({ where: { id } });
    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    if (user.role === 'LEADER') {
      throw new AppError(400, 'Não é possível banir um líder');
    }

    const updated = await prisma.user.update({
      where: { id },
      data: { status: 'BANNED' },
      select: {
        id: true,
        nick: true,
        status: true,
      },
    });

    res.json({ user: updated });
  } catch (err) {
    next(err);
  }
}

export async function promoteUser(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;

    const user = await prisma.user.findUnique({ where: { id } });
    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    if (user.status !== 'APPROVED') {
      throw new AppError(400, 'Usuário precisa estar aprovado primeiro');
    }

    if (user.role === 'LEADER') {
      throw new AppError(400, 'Usuário já é líder');
    }

    const updated = await prisma.user.update({
      where: { id },
      data: { role: 'LEADER' },
      select: {
        id: true,
        nick: true,
        role: true,
      },
    });

    res.json({ user: updated });
  } catch (err) {
    next(err);
  }
}

export async function demoteUser(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;

    // Cannot demote yourself
    if (id === req.user!.userId) {
      throw new AppError(400, 'Você não pode se rebaixar');
    }

    const user = await prisma.user.findUnique({ where: { id } });
    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    if (user.role !== 'LEADER') {
      throw new AppError(400, 'Usuário não é líder');
    }

    const updated = await prisma.user.update({
      where: { id },
      data: { role: 'MEMBER' },
      select: {
        id: true,
        nick: true,
        role: true,
      },
    });

    res.json({ user: updated });
  } catch (err) {
    next(err);
  }
}

export async function getBannedUsers(
  _req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const users = await prisma.user.findMany({
      where: { status: 'BANNED' },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        createdAt: true,
      },
      orderBy: { nick: 'asc' },
    });

    res.json({ users });
  } catch (err) {
    next(err);
  }
}

export async function unbanUser(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;

    const user = await prisma.user.findUnique({ where: { id } });
    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    if (user.status !== 'BANNED') {
      throw new AppError(400, 'Usuário não está banido');
    }

    const updated = await prisma.user.update({
      where: { id },
      data: { status: 'APPROVED' },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        status: true,
      },
    });

    res.json({ user: updated });
  } catch (err) {
    next(err);
  }
}

export async function getUserProfile(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;

    const user = await prisma.user.findUnique({
      where: { id },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        role: true,
        status: true,
        createdAt: true,
        approvedAt: true,
      },
    });

    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    // Only show approved users' profiles (or if requester is leader)
    if (user.status !== 'APPROVED' && req.user!.role !== 'LEADER') {
      throw new AppError(403, 'Perfil não disponível');
    }

    res.json({ user });
  } catch (err) {
    next(err);
  }
}
