import type { Request, Response, NextFunction } from 'express';
import bcrypt from 'bcrypt';
import { z } from 'zod';
import { prisma } from '../utils/prisma.js';
import { generateToken } from '../utils/jwt.js';
import { AppError } from '../middleware/errorHandler.js';
import { PlayerClass } from '@prisma/client';

const registerSchema = z.object({
  nick: z.string().min(3).max(20),
  password: z.string().min(6),
  playerClass: z.nativeEnum(PlayerClass),
});

const loginSchema = z.object({
  nick: z.string(),
  password: z.string(),
});

export async function register(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { nick, password, playerClass } = registerSchema.parse(req.body);

    const existing = await prisma.user.findUnique({ where: { nick } });
    if (existing) {
      throw new AppError(409, 'Este nick já está em uso');
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const user = await prisma.user.create({
      data: {
        nick,
        passwordHash,
        playerClass,
      },
      select: {
        id: true,
        nick: true,
        playerClass: true,
        status: true,
        role: true,
        createdAt: true,
      },
    });

    const token = generateToken({
      userId: user.id,
      nick: user.nick,
      role: user.role,
      status: user.status,
    });

    res.status(201).json({ user, token });
  } catch (err) {
    next(err);
  }
}

export async function login(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { nick, password } = loginSchema.parse(req.body);

    const user = await prisma.user.findUnique({ where: { nick } });
    if (!user) {
      throw new AppError(401, 'Nick ou senha incorretos');
    }

    if (user.status === 'BANNED') {
      throw new AppError(403, 'Sua conta foi banida');
    }

    const validPassword = await bcrypt.compare(password, user.passwordHash);
    if (!validPassword) {
      throw new AppError(401, 'Nick ou senha incorretos');
    }

    const token = generateToken({
      userId: user.id,
      nick: user.nick,
      role: user.role,
      status: user.status,
    });

    res.json({
      user: {
        id: user.id,
        nick: user.nick,
        playerClass: user.playerClass,
        status: user.status,
        role: user.role,
      },
      token,
    });
  } catch (err) {
    next(err);
  }
}

export async function checkStatus(
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
      },
    });

    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    res.json({ user });
  } catch (err) {
    next(err);
  }
}

const fcmTokenSchema = z.object({
  fcmToken: z.string().min(1),
});

const changePasswordSchema = z.object({
  currentPassword: z.string().min(1),
  newPassword: z.string().min(6),
});

const forgotPasswordSchema = z.object({
  nick: z.string().min(1),
});

export async function registerFcmToken(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { fcmToken } = fcmTokenSchema.parse(req.body);

    await prisma.user.update({
      where: { id: req.user!.userId },
      data: { fcmToken },
    });

    res.json({ success: true });
  } catch (err) {
    next(err);
  }
}

export async function changePassword(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { currentPassword, newPassword } = changePasswordSchema.parse(req.body);

    const user = await prisma.user.findUnique({
      where: { id: req.user!.userId },
    });

    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    const validPassword = await bcrypt.compare(currentPassword, user.passwordHash);
    if (!validPassword) {
      throw new AppError(401, 'Senha atual incorreta');
    }

    const newPasswordHash = await bcrypt.hash(newPassword, 10);

    await prisma.user.update({
      where: { id: user.id },
      data: { passwordHash: newPasswordHash },
    });

    res.json({ success: true });
  } catch (err) {
    next(err);
  }
}

function generateRandomPassword(length: number = 8): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789';
  let password = '';
  for (let i = 0; i < length; i++) {
    password += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return password;
}

export async function forgotPassword(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { nick } = forgotPasswordSchema.parse(req.body);

    const user = await prisma.user.findUnique({
      where: { nick },
    });

    if (!user) {
      throw new AppError(404, 'Usuário não encontrado');
    }

    if (user.status === 'BANNED') {
      throw new AppError(403, 'Esta conta foi banida');
    }

    // Generate a new random password
    const newPassword = generateRandomPassword(8);
    const newPasswordHash = await bcrypt.hash(newPassword, 10);

    // Update the password in the database
    await prisma.user.update({
      where: { id: user.id },
      data: { passwordHash: newPasswordHash },
    });

    res.json({
      message: 'Senha resetada com sucesso!',
      newPassword,
    });
  } catch (err) {
    next(err);
  }
}
