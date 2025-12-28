import jwt from 'jsonwebtoken';
import type { Role, UserStatus } from '@prisma/client';

const JWT_SECRET = process.env.JWT_SECRET || 'dev_secret';

export interface TokenPayload {
  userId: string;
  nick: string;
  role: Role;
  status: UserStatus;
}

export function generateToken(payload: TokenPayload): string {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: '7d' });
}

export function verifyToken(token: string): TokenPayload {
  return jwt.verify(token, JWT_SECRET) as TokenPayload;
}
