import type { Request, Response, NextFunction } from 'express';
import { verifyToken, type TokenPayload } from '../utils/jwt.js';
import { AppError } from './errorHandler.js';
import { prisma } from '../utils/prisma.js';

declare global {
  namespace Express {
    interface Request {
      user?: TokenPayload;
    }
  }
}

export function authenticate(req: Request, _res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;

  if (!authHeader?.startsWith('Bearer ')) {
    throw new AppError(401, 'Token não fornecido');
  }

  const token = authHeader.slice(7);

  try {
    const payload = verifyToken(token);
    req.user = payload;
    next();
  } catch {
    throw new AppError(401, 'Token inválido');
  }
}

export async function requireApproved(req: Request, _res: Response, next: NextFunction): Promise<void> {
  if (!req.user?.userId) {
    throw new AppError(401, 'Usuário não autenticado');
  }

  // Always check current status from database, not from JWT token
  const user = await prisma.user.findUnique({
    where: { id: req.user.userId },
    select: { status: true }
  });

  if (!user) {
    throw new AppError(404, 'Usuário não encontrado');
  }

  if (user.status !== 'APPROVED') {
    throw new AppError(403, 'Conta não aprovada');
  }

  next();
}

export function requireLeader(req: Request, _res: Response, next: NextFunction): void {
  if (req.user?.role !== 'LEADER') {
    throw new AppError(403, 'Acesso restrito a líderes');
  }
  next();
}
