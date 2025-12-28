import { Request, Response } from 'express';
import { prisma } from '../utils/prisma.js';
import { ChannelType } from '@prisma/client';
import { getMediaType, getMediaUrl } from '../middleware/upload.js';
import fs from 'fs';

// Get channels accessible by current user
export async function getChannels(req: Request, res: Response): Promise<void> {
  try {
    const userId = req.user!.userId;
    const userRole = req.user!.role;

    // Get user's parties and events
    const user = await prisma.user.findUnique({
      where: { id: userId },
      include: {
        participations: { select: { eventId: true } },
        partyMemberships: { select: { partyId: true } },
      },
    });

    if (!user) {
      res.status(404).json({ error: 'Usuário não encontrado' });
      return;
    }

    const eventIds = user.participations.map((p) => p.eventId);
    const partyIds = user.partyMemberships.map((p) => p.partyId);

    // Build channel filter based on user role and memberships
    const channels = await prisma.channel.findMany({
      where: {
        OR: [
          { type: ChannelType.GENERAL },
          ...(userRole === 'LEADER' ? [{ type: ChannelType.LEADERS }] : []),
          { type: ChannelType.EVENT, eventId: { in: eventIds } },
          { type: ChannelType.PARTY, partyId: { in: partyIds } },
        ],
      },
      include: {
        event: { select: { id: true, title: true } },
        party: { select: { id: true, name: true } },
        _count: { select: { messages: true } },
      },
      orderBy: { createdAt: 'desc' },
    });

    res.json(channels);
  } catch (error) {
    console.error('Error fetching channels:', error);
    res.status(500).json({ error: 'Erro ao buscar canais' });
  }
}

// Get messages from a channel
export async function getChannelMessages(req: Request, res: Response): Promise<void> {
  try {
    const userId = req.user!.userId;
    const userRole = req.user!.role;
    const { channelId } = req.params;
    const { limit = '50', before } = req.query;

    // Check channel exists and user has access
    const channel = await prisma.channel.findUnique({
      where: { id: channelId },
      include: {
        event: {
          include: {
            participants: { where: { userId }, select: { id: true } },
          },
        },
        party: {
          include: {
            members: { where: { userId }, select: { id: true } },
          },
        },
      },
    });

    if (!channel) {
      res.status(404).json({ error: 'Canal não encontrado' });
      return;
    }

    // Check access
    const hasAccess = checkChannelAccess(channel, userId, userRole);
    if (!hasAccess) {
      res.status(403).json({ error: 'Sem acesso a este canal' });
      return;
    }

    // Fetch messages
    const messages = await prisma.message.findMany({
      where: {
        channelId,
        ...(before ? { createdAt: { lt: new Date(before as string) } } : {}),
      },
      include: {
        user: {
          select: { id: true, nick: true, playerClass: true, role: true },
        },
      },
      orderBy: { createdAt: 'desc' },
      take: parseInt(limit as string),
    });

    // Return in chronological order
    res.json(messages.reverse());
  } catch (error) {
    console.error('Error fetching messages:', error);
    res.status(500).json({ error: 'Erro ao buscar mensagens' });
  }
}

// Send message to channel
export async function sendMessage(req: Request, res: Response): Promise<void> {
  try {
    const userId = req.user!.userId;
    const userRole = req.user!.role;
    const { channelId } = req.params;
    const { content } = req.body;

    if (!content || content.trim().length === 0) {
      res.status(400).json({ error: 'Conteúdo da mensagem é obrigatório' });
      return;
    }

    // Check channel exists and user has access
    const channel = await prisma.channel.findUnique({
      where: { id: channelId },
      include: {
        event: {
          include: {
            participants: { where: { userId }, select: { id: true } },
          },
        },
        party: {
          include: {
            members: { where: { userId }, select: { id: true } },
          },
        },
      },
    });

    if (!channel) {
      res.status(404).json({ error: 'Canal não encontrado' });
      return;
    }

    // Check access
    const hasAccess = checkChannelAccess(channel, userId, userRole);
    if (!hasAccess) {
      res.status(403).json({ error: 'Sem acesso a este canal' });
      return;
    }

    // Create message
    const message = await prisma.message.create({
      data: {
        content: content.trim(),
        userId,
        channelId,
      },
      include: {
        user: {
          select: { id: true, nick: true, playerClass: true, role: true },
        },
      },
    });

    res.status(201).json(message);
  } catch (error) {
    console.error('Error sending message:', error);
    res.status(500).json({ error: 'Erro ao enviar mensagem' });
  }
}

// Send media message to channel
export async function sendMediaMessage(req: Request, res: Response): Promise<void> {
  try {
    const userId = req.user!.userId;
    const userRole = req.user!.role;
    const { channelId } = req.params;
    const content = req.body.content || null;
    const file = req.file;

    if (!file) {
      res.status(400).json({ error: 'Arquivo é obrigatório' });
      return;
    }

    // Check channel exists and user has access
    const channel = await prisma.channel.findUnique({
      where: { id: channelId },
      include: {
        event: {
          include: {
            participants: { where: { userId }, select: { id: true } },
          },
        },
        party: {
          include: {
            members: { where: { userId }, select: { id: true } },
          },
        },
      },
    });

    if (!channel) {
      // Remove uploaded file if channel not found
      fs.unlinkSync(file.path);
      res.status(404).json({ error: 'Canal não encontrado' });
      return;
    }

    // Check access
    const hasAccess = checkChannelAccess(channel, userId, userRole);
    if (!hasAccess) {
      // Remove uploaded file if no access
      fs.unlinkSync(file.path);
      res.status(403).json({ error: 'Sem acesso a este canal' });
      return;
    }

    // Create message with media
    const mediaType = getMediaType(file.mimetype);
    const mediaUrl = getMediaUrl(channelId, file.filename);

    const message = await prisma.message.create({
      data: {
        content: content?.trim() || null,
        mediaUrl,
        mediaType,
        fileName: file.originalname,
        fileSize: file.size,
        userId,
        channelId,
      },
      include: {
        user: {
          select: { id: true, nick: true, playerClass: true, role: true },
        },
      },
    });

    res.status(201).json(message);
  } catch (error) {
    console.error('Error sending media message:', error);
    // Try to clean up file on error
    if (req.file) {
      try {
        fs.unlinkSync(req.file.path);
      } catch {}
    }
    res.status(500).json({ error: 'Erro ao enviar mídia' });
  }
}

// Create default channels (GENERAL and LEADERS) - Admin only
export async function createDefaultChannels(req: Request, res: Response): Promise<void> {
  try {
    const userRole = req.user!.role;

    if (userRole !== 'LEADER') {
      res.status(403).json({ error: 'Apenas líderes podem criar canais padrão' });
      return;
    }

    // Check if default channels already exist
    const existingGeneral = await prisma.channel.findFirst({
      where: { type: ChannelType.GENERAL },
    });

    const existingLeaders = await prisma.channel.findFirst({
      where: { type: ChannelType.LEADERS },
    });

    const created = [];

    if (!existingGeneral) {
      const general = await prisma.channel.create({
        data: {
          type: ChannelType.GENERAL,
          name: 'Geral',
        },
      });
      created.push(general);
    }

    if (!existingLeaders) {
      const leaders = await prisma.channel.create({
        data: {
          type: ChannelType.LEADERS,
          name: 'Líderes',
        },
      });
      created.push(leaders);
    }

    if (created.length === 0) {
      res.json({ message: 'Canais padrão já existem' });
      return;
    }

    res.status(201).json({ message: 'Canais criados', channels: created });
  } catch (error) {
    console.error('Error creating default channels:', error);
    res.status(500).json({ error: 'Erro ao criar canais padrão' });
  }
}

// Create event channel
export async function createEventChannel(eventId: string, eventTitle: string): Promise<void> {
  try {
    await prisma.channel.create({
      data: {
        type: ChannelType.EVENT,
        name: `Evento: ${eventTitle}`,
        eventId,
      },
    });
    console.log(`[Channel] Created event channel for event ${eventId}`);
  } catch (error) {
    console.error('Error creating event channel:', error);
  }
}

// Create party channel
export async function createPartyChannel(partyId: string, partyName: string): Promise<void> {
  try {
    await prisma.channel.create({
      data: {
        type: ChannelType.PARTY,
        name: `Party: ${partyName}`,
        partyId,
      },
    });
    console.log(`[Channel] Created party channel for party ${partyId}`);
  } catch (error) {
    console.error('Error creating party channel:', error);
  }
}

// Get channel members
export async function getChannelMembers(req: Request, res: Response): Promise<void> {
  try {
    const userId = req.user!.userId;
    const userRole = req.user!.role;
    const { channelId } = req.params;

    const channel = await prisma.channel.findUnique({
      where: { id: channelId },
      include: {
        event: {
          include: {
            participants: {
              include: {
                user: {
                  select: { id: true, nick: true, playerClass: true, role: true },
                },
              },
            },
          },
        },
        party: {
          include: {
            members: {
              include: {
                user: {
                  select: { id: true, nick: true, playerClass: true, role: true },
                },
              },
            },
          },
        },
      },
    });

    if (!channel) {
      res.status(404).json({ error: 'Canal não encontrado' });
      return;
    }

    let members: { id: string; nick: string; playerClass: string; role: string }[] = [];

    switch (channel.type) {
      case ChannelType.GENERAL:
        // All approved members
        const allMembers = await prisma.user.findMany({
          where: { status: 'APPROVED' },
          select: { id: true, nick: true, playerClass: true, role: true },
          orderBy: { nick: 'asc' },
        });
        members = allMembers;
        break;

      case ChannelType.LEADERS:
        // Only leaders
        if (userRole !== 'LEADER') {
          res.status(403).json({ error: 'Sem acesso a este canal' });
          return;
        }
        const leaders = await prisma.user.findMany({
          where: { status: 'APPROVED', role: 'LEADER' },
          select: { id: true, nick: true, playerClass: true, role: true },
          orderBy: { nick: 'asc' },
        });
        members = leaders;
        break;

      case ChannelType.EVENT:
        // Event participants
        const isEventParticipant = channel.event?.participants.some(p => p.user.id === userId);
        if (!isEventParticipant && userRole !== 'LEADER') {
          res.status(403).json({ error: 'Sem acesso a este canal' });
          return;
        }
        members = channel.event?.participants.map(p => p.user) || [];
        break;

      case ChannelType.PARTY:
        // Party members
        const isPartyMember = channel.party?.members.some(m => m.user.id === userId);
        if (!isPartyMember && userRole !== 'LEADER') {
          res.status(403).json({ error: 'Sem acesso a este canal' });
          return;
        }
        members = channel.party?.members.map(m => m.user) || [];
        break;
    }

    res.json({ members, count: members.length });
  } catch (error) {
    console.error('Error fetching channel members:', error);
    res.status(500).json({ error: 'Erro ao buscar membros do canal' });
  }
}

// Helper function to check channel access
function checkChannelAccess(
  channel: {
    type: ChannelType;
    event?: { participants: { id: string }[] } | null;
    party?: { members: { id: string }[] } | null;
  },
  userId: string,
  userRole: string
): boolean {
  switch (channel.type) {
    case ChannelType.GENERAL:
      return true;
    case ChannelType.LEADERS:
      return userRole === 'LEADER';
    case ChannelType.EVENT:
      return (channel.event?.participants?.length ?? 0) > 0;
    case ChannelType.PARTY:
      return (channel.party?.members?.length ?? 0) > 0;
    default:
      return false;
  }
}
