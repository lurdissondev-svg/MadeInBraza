import type { Request, Response, NextFunction } from 'express';
import bcrypt from 'bcrypt';
import crypto from 'crypto';
import { z } from 'zod';
import { prisma } from '../utils/prisma.js';
import { generateToken } from '../utils/jwt.js';
import { AppError } from '../middleware/errorHandler.js';
import { PlayerClass } from '@prisma/client';
import { sendPasswordResetEmail, isEmailConfigured } from '../utils/email.js';

const registerSchema = z.object({
  nick: z.string().min(3).max(20),
  password: z.string().min(6),
  playerClass: z.nativeEnum(PlayerClass),
  email: z.string().email().optional(),
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
    const { nick, password, playerClass, email } = registerSchema.parse(req.body);

    const existing = await prisma.user.findUnique({ where: { nick } });
    if (existing) {
      throw new AppError(409, 'Este nick ja esta em uso');
    }

    // Check if email is already in use
    if (email) {
      const existingEmail = await prisma.user.findUnique({ where: { email } });
      if (existingEmail) {
        throw new AppError(409, 'Este email ja esta em uso');
      }
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const user = await prisma.user.create({
      data: {
        nick,
        passwordHash,
        playerClass,
        email: email || null,
      },
      select: {
        id: true,
        nick: true,
        email: true,
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
        email: user.email,
        playerClass: user.playerClass,
        status: user.status,
        role: user.role,
        avatarUrl: user.avatarUrl,
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
        email: true,
        playerClass: true,
        status: true,
        role: true,
        avatarUrl: true,
      },
    });

    if (!user) {
      throw new AppError(404, 'Usuario nao encontrado');
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

// Request password reset - sends email with reset link
export async function requestPasswordReset(
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
      // Don't reveal if user exists
      res.json({
        message: 'Se o usuario existir e tiver email cadastrado, um link sera enviado.',
      });
      return;
    }

    if (user.status === 'BANNED') {
      throw new AppError(403, 'Esta conta foi banida');
    }

    if (!user.email) {
      throw new AppError(400, 'Este usuario nao possui email cadastrado. Entre em contato com um lider.');
    }

    // Generate secure token
    const resetToken = crypto.randomBytes(32).toString('hex');
    const expiresAt = new Date(Date.now() + 60 * 60 * 1000); // 1 hour

    // Delete any existing tokens for this user
    await prisma.passwordResetToken.deleteMany({
      where: { userId: user.id },
    });

    // Create new reset token
    await prisma.passwordResetToken.create({
      data: {
        token: resetToken,
        userId: user.id,
        expiresAt,
      },
    });

    // Send email
    const emailSent = await sendPasswordResetEmail(user.email, user.nick, resetToken);

    if (!emailSent) {
      throw new AppError(500, 'Erro ao enviar email. Tente novamente mais tarde.');
    }

    res.json({
      message: 'Email de recuperacao enviado! Verifique sua caixa de entrada.',
    });
  } catch (err) {
    next(err);
  }
}

// Verify reset token
const verifyTokenSchema = z.object({
  token: z.string().min(1),
});

export async function verifyResetToken(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { token } = verifyTokenSchema.parse(req.body);

    const resetToken = await prisma.passwordResetToken.findUnique({
      where: { token },
      include: { user: { select: { nick: true } } },
    });

    if (!resetToken) {
      throw new AppError(400, 'Token invalido ou expirado');
    }

    if (resetToken.usedAt) {
      throw new AppError(400, 'Este token ja foi utilizado');
    }

    if (resetToken.expiresAt < new Date()) {
      throw new AppError(400, 'Token expirado. Solicite um novo link.');
    }

    res.json({
      valid: true,
      nick: resetToken.user.nick,
    });
  } catch (err) {
    next(err);
  }
}

// Reset password with token
const resetPasswordSchema = z.object({
  token: z.string().min(1),
  newPassword: z.string().min(6),
});

export async function resetPasswordWithToken(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { token, newPassword } = resetPasswordSchema.parse(req.body);

    const resetToken = await prisma.passwordResetToken.findUnique({
      where: { token },
      include: { user: true },
    });

    if (!resetToken) {
      throw new AppError(400, 'Token invalido ou expirado');
    }

    if (resetToken.usedAt) {
      throw new AppError(400, 'Este token ja foi utilizado');
    }

    if (resetToken.expiresAt < new Date()) {
      throw new AppError(400, 'Token expirado. Solicite um novo link.');
    }

    // Hash new password
    const passwordHash = await bcrypt.hash(newPassword, 10);

    // Update password and mark token as used
    await prisma.$transaction([
      prisma.user.update({
        where: { id: resetToken.userId },
        data: { passwordHash },
      }),
      prisma.passwordResetToken.update({
        where: { id: resetToken.id },
        data: { usedAt: new Date() },
      }),
    ]);

    res.json({
      message: 'Senha alterada com sucesso!',
    });
  } catch (err) {
    next(err);
  }
}

// Legacy forgotPassword - now redirects to secure email flow
// SECURITY FIX: Removed insecure password generation that returned password in response
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
      // Don't reveal if user exists
      res.json({
        message: 'Se o usuario existir e tiver email cadastrado, um link sera enviado.',
        requiresEmail: true,
      });
      return;
    }

    if (user.status === 'BANNED') {
      throw new AppError(403, 'Esta conta foi banida');
    }

    // SECURITY: Always require email for password recovery
    if (!user.email) {
      throw new AppError(400, 'Este usuario nao possui email cadastrado. Entre em contato com um lider para cadastrar seu email.');
    }

    // Generate secure token
    const resetToken = crypto.randomBytes(32).toString('hex');
    const expiresAt = new Date(Date.now() + 60 * 60 * 1000); // 1 hour

    // Delete any existing tokens for this user
    await prisma.passwordResetToken.deleteMany({
      where: { userId: user.id },
    });

    // Create new reset token
    await prisma.passwordResetToken.create({
      data: {
        token: resetToken,
        userId: user.id,
        expiresAt,
      },
    });

    // Send email
    const emailSent = await sendPasswordResetEmail(user.email, user.nick, resetToken);

    if (!emailSent) {
      throw new AppError(500, 'Erro ao enviar email. Tente novamente mais tarde.');
    }

    res.json({
      message: 'Email de recuperacao enviado! Verifique sua caixa de entrada.',
      requiresEmail: true,
    });
  } catch (err) {
    next(err);
  }
}

// Update email for current user
const updateEmailSchema = z.object({
  email: z.string().email(),
});

export async function updateEmail(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { email } = updateEmailSchema.parse(req.body);

    // Check if email is already in use
    const existingEmail = await prisma.user.findUnique({ where: { email } });
    if (existingEmail && existingEmail.id !== req.user!.userId) {
      throw new AppError(409, 'Este email ja esta em uso');
    }

    await prisma.user.update({
      where: { id: req.user!.userId },
      data: { email },
    });

    res.json({ success: true, email });
  } catch (err) {
    next(err);
  }
}
