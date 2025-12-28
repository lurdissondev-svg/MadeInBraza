import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { AppError } from '../middleware/errorHandler.js';
import { encrypt, decrypt } from '../utils/crypto.js';
import { SWResponseType, SWTag } from '@prisma/client';

// Get current active Siege War
export async function getCurrentSiegeWar(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const siegeWar = await prisma.siegeWar.findFirst({
      where: { isActive: true },
      orderBy: { createdAt: 'desc' },
    });

    if (!siegeWar) {
      res.json({ siegeWar: null });
      return;
    }

    // Get user's response if exists
    const userId = req.user!.userId;
    const userResponse = await prisma.sWResponse.findUnique({
      where: { siegeWarId_userId: { siegeWarId: siegeWar.id, userId } },
      include: {
        pilotingFor: { select: { id: true, nick: true, playerClass: true } },
      },
    });

    res.json({
      siegeWar: {
        id: siegeWar.id,
        weekStart: siegeWar.weekStart,
        weekEnd: siegeWar.weekEnd,
        isActive: siegeWar.isActive,
      },
      userResponse: userResponse
        ? {
            id: userResponse.id,
            responseType: userResponse.responseType,
            tag: userResponse.tag,
            gameId: userResponse.gameId ? decrypt(userResponse.gameId) : null,
            sharedClass: userResponse.sharedClass,
            pilotingFor: userResponse.pilotingFor,
            preferredClass: userResponse.preferredClass,
            // Don't send password to client
          }
        : null,
    });
  } catch (err) {
    next(err);
  }
}

// Submit response to Siege War
export async function submitResponse(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { siegeWarId } = req.params;
    const { responseType, tag, gameId, password, sharedClass, pilotingForId, preferredClass } = req.body;
    const userId = req.user!.userId;

    // Validate siege war exists and is active
    const siegeWar = await prisma.siegeWar.findUnique({
      where: { id: siegeWarId },
    });

    if (!siegeWar) {
      throw new AppError(404, 'Siege War not found');
    }

    if (!siegeWar.isActive) {
      throw new AppError(400, 'Siege War is no longer active');
    }

    // Validate response type
    if (!Object.values(SWResponseType).includes(responseType)) {
      throw new AppError(400, 'Invalid response type');
    }

    // Validate tag if provided
    if (tag && !Object.values(SWTag).includes(tag)) {
      throw new AppError(400, 'Invalid tag');
    }

    // Validate SHARED response data
    let encryptedPassword: string | null = null;
    let encryptedGameId: string | null = null;
    if (responseType === 'SHARED') {
      if (!gameId || !password || !sharedClass) {
        throw new AppError(400, 'SHARED response requires gameId, password, and sharedClass');
      }
      encryptedPassword = encrypt(password);
      encryptedGameId = encrypt(gameId);
    }

    // Validate PILOT response data
    if (responseType === 'PILOT') {
      if (!pilotingForId) {
        throw new AppError(400, 'PILOT response requires pilotingForId');
      }
      // Verify the user being piloted has a SHARED response
      const sharedResponse = await prisma.sWResponse.findFirst({
        where: {
          siegeWarId,
          userId: pilotingForId,
          responseType: 'SHARED',
        },
      });
      if (!sharedResponse) {
        throw new AppError(400, 'The specified user has not shared their account');
      }
    }

    // Upsert response
    const response = await prisma.sWResponse.upsert({
      where: { siegeWarId_userId: { siegeWarId, userId } },
      update: {
        responseType,
        tag: tag || null,
        gameId: encryptedGameId,
        encryptedPassword: encryptedPassword,
        sharedClass: responseType === 'SHARED' ? sharedClass : null,
        pilotingForId: responseType === 'PILOT' ? pilotingForId : null,
        preferredClass: responseType === 'PILOT' ? preferredClass : null,
      },
      create: {
        siegeWarId,
        userId,
        responseType,
        tag: tag || null,
        gameId: encryptedGameId,
        encryptedPassword: encryptedPassword,
        sharedClass: responseType === 'SHARED' ? sharedClass : null,
        pilotingForId: responseType === 'PILOT' ? pilotingForId : null,
        preferredClass: responseType === 'PILOT' ? preferredClass : null,
      },
      include: {
        pilotingFor: { select: { id: true, nick: true, playerClass: true } },
      },
    });

    res.json({
      response: {
        id: response.id,
        responseType: response.responseType,
        tag: response.tag,
        gameId: response.gameId ? decrypt(response.gameId) : null,
        sharedClass: response.sharedClass,
        pilotingFor: response.pilotingFor,
        preferredClass: response.preferredClass,
      },
    });
  } catch (err) {
    next(err);
  }
}

// Get all responses for a Siege War (Leaders only)
export async function getResponses(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { siegeWarId } = req.params;
    const userId = req.user!.userId;

    // Check if user is a leader
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: { role: true },
    });

    if (user?.role !== 'LEADER') {
      throw new AppError(403, 'Only leaders can view all responses');
    }

    const siegeWar = await prisma.siegeWar.findUnique({
      where: { id: siegeWarId },
    });

    if (!siegeWar) {
      throw new AppError(404, 'Siege War not found');
    }

    const responses = await prisma.sWResponse.findMany({
      where: { siegeWarId },
      include: {
        user: { select: { id: true, nick: true, playerClass: true } },
        pilotingFor: { select: { id: true, nick: true, playerClass: true } },
      },
      orderBy: [{ responseType: 'asc' }, { createdAt: 'asc' }],
    });

    // Decrypt sensitive data for SHARED responses (for leaders)
    const transformedResponses = responses.map((r) => ({
      id: r.id,
      user: r.user,
      responseType: r.responseType,
      tag: r.tag,
      gameId: r.gameId ? decrypt(r.gameId) : null,
      password: r.encryptedPassword ? decrypt(r.encryptedPassword) : null,
      sharedClass: r.sharedClass,
      pilotingFor: r.pilotingFor,
      preferredClass: r.preferredClass,
      createdAt: r.createdAt,
    }));

    // Get all approved members to show who hasn't responded
    const allMembers = await prisma.user.findMany({
      where: { status: 'APPROVED' },
      select: { id: true, nick: true, playerClass: true },
    });

    const respondedUserIds = new Set(responses.map((r) => r.userId));
    const notResponded = allMembers.filter((m) => !respondedUserIds.has(m.id));

    // Get available shared accounts (SHARED without a pilot)
    const sharedResponses = responses.filter((r) => r.responseType === 'SHARED');
    const pilotedUserIds = new Set(
      responses.filter((r) => r.pilotingForId).map((r) => r.pilotingForId)
    );
    const availableShares = sharedResponses
      .filter((r) => !pilotedUserIds.has(r.userId))
      .map((r) => ({
        userId: r.userId,
        nick: r.user.nick,
        sharedClass: r.sharedClass,
      }));

    res.json({
      responses: transformedResponses,
      notResponded,
      availableShares,
      summary: {
        total: allMembers.length,
        responded: responses.length,
        confirmed: responses.filter((r) => r.responseType === 'CONFIRMED').length,
        shared: responses.filter((r) => r.responseType === 'SHARED').length,
        pilots: responses.filter((r) => r.responseType === 'PILOT').length,
        absent: responses.filter((r) => r.responseType === 'ABSENT').length,
      },
    });
  } catch (err) {
    next(err);
  }
}

// Get available shared accounts for piloting
export async function getAvailableShares(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { siegeWarId } = req.params;

    const siegeWar = await prisma.siegeWar.findUnique({
      where: { id: siegeWarId },
    });

    if (!siegeWar || !siegeWar.isActive) {
      throw new AppError(404, 'Active Siege War not found');
    }

    // Get all SHARED responses
    const sharedResponses = await prisma.sWResponse.findMany({
      where: { siegeWarId, responseType: 'SHARED' },
      include: {
        user: { select: { id: true, nick: true, playerClass: true } },
      },
    });

    // Get all PILOT responses to see who's already being piloted
    const pilotResponses = await prisma.sWResponse.findMany({
      where: { siegeWarId, responseType: 'PILOT' },
      select: { pilotingForId: true },
    });

    const pilotedUserIds = new Set(pilotResponses.map((r) => r.pilotingForId));

    // Filter to only available shares
    const availableShares = sharedResponses
      .filter((r) => !pilotedUserIds.has(r.userId))
      .map((r) => ({
        userId: r.userId,
        nick: r.user.nick,
        sharedClass: r.sharedClass,
      }));

    res.json({ availableShares });
  } catch (err) {
    next(err);
  }
}

// Create a new Siege War (Leaders only or cron job)
export async function createSiegeWar(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const userId = req.user!.userId;

    // Check if user is a leader
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: { role: true },
    });

    if (user?.role !== 'LEADER') {
      throw new AppError(403, 'Only leaders can create Siege Wars');
    }

    // Close any existing active siege wars
    await prisma.siegeWar.updateMany({
      where: { isActive: true },
      data: { isActive: false },
    });

    // Calculate week dates (Thursday to Sunday)
    const now = new Date();
    const dayOfWeek = now.getDay();
    const daysUntilThursday = (4 - dayOfWeek + 7) % 7;
    const weekStart = new Date(now);
    weekStart.setDate(now.getDate() + daysUntilThursday);
    weekStart.setHours(0, 0, 0, 0);

    const weekEnd = new Date(weekStart);
    weekEnd.setDate(weekStart.getDate() + 3); // Sunday
    weekEnd.setHours(23, 59, 59, 999);

    const siegeWar = await prisma.siegeWar.create({
      data: {
        weekStart,
        weekEnd,
        isActive: true,
      },
    });

    res.status(201).json({ siegeWar });
  } catch (err) {
    console.error('Error in createSiegeWar:', err);
    next(err);
  }
}

// Close Siege War (Leaders only)
export async function closeSiegeWar(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { siegeWarId } = req.params;
    const userId = req.user!.userId;

    // Check if user is a leader
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: { role: true },
    });

    if (user?.role !== 'LEADER') {
      throw new AppError(403, 'Only leaders can close Siege Wars');
    }

    const siegeWar = await prisma.siegeWar.update({
      where: { id: siegeWarId },
      data: { isActive: false },
    });

    res.json({ siegeWar });
  } catch (err) {
    next(err);
  }
}

// Get Siege War history
export async function getSiegeWarHistory(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const siegeWars = await prisma.siegeWar.findMany({
      orderBy: { createdAt: 'desc' },
      take: 10,
      include: {
        _count: {
          select: { responses: true },
        },
      },
    });

    res.json({
      siegeWars: siegeWars.map((sw) => ({
        id: sw.id,
        weekStart: sw.weekStart,
        weekEnd: sw.weekEnd,
        isActive: sw.isActive,
        responseCount: sw._count.responses,
      })),
    });
  } catch (err) {
    next(err);
  }
}
