import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { downloadAndSaveMedia } from '../services/uazapiMedia.js';
import { sendAnnouncementNotification } from './announcements.js';

const AVISOS_GROUP_ID = process.env.UAZAPI_AVISOS_GROUP_ID || '';

interface UazapiWebhookPayload {
  EventType?: string;
  event?: string;
  chat?: {
    wa_chatid?: string;
    name?: string;
  };
  message?: {
    messageid?: string;
    id?: string;
    text?: string;
    senderName?: string;
    pushName?: string;
    name?: string;
    sender?: string;
    fromMe?: boolean;
    messageTimestamp?: number;
    messageType?: string;
    content?: {
      caption?: string;
      URL?: string;
    };
  };
  owner?: string;
}

function generateTitle(content: string): string {
  const maxLength = 100;
  const cleanContent = content.replace(/\n/g, ' ').trim();

  if (cleanContent.length <= maxLength) {
    return cleanContent;
  }

  const truncated = cleanContent.substring(0, maxLength);
  const lastSpace = truncated.lastIndexOf(' ');

  if (lastSpace > 50) {
    return truncated.substring(0, lastSpace) + '...';
  }

  return truncated + '...';
}

export async function handleUazapiWebhook(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const payload = req.body as UazapiWebhookPayload;

    console.log('[UAZAPI Webhook] ========== INCOMING WEBHOOK ==========');
    console.log('[UAZAPI Webhook] EventType:', payload.EventType);
    console.log('[UAZAPI Webhook] Chat ID:', payload.chat?.wa_chatid);

    // Aceita tanto 'EventType' quanto 'event'
    const eventType = payload.EventType || payload.event;

    if (eventType !== 'messages') {
      console.log('[UAZAPI Webhook] Ignoring non-message event:', eventType);
      res.status(200).json({ received: true, processed: false });
      return;
    }

    // Verifica se é do grupo AVISOS
    const chatId = payload.chat?.wa_chatid;

    if (!AVISOS_GROUP_ID) {
      console.warn('[UAZAPI Webhook] UAZAPI_AVISOS_GROUP_ID not configured');
      res.status(200).json({ received: true, processed: false, error: 'Group ID not configured' });
      return;
    }

    if (chatId !== AVISOS_GROUP_ID) {
      console.log('[UAZAPI Webhook] Ignoring message from different chat:', chatId);
      console.log('[UAZAPI Webhook] Expected:', AVISOS_GROUP_ID);
      res.status(200).json({ received: true, processed: false });
      return;
    }

    const msg = payload.message;
    if (!msg) {
      console.log('[UAZAPI Webhook] No message in payload');
      res.status(200).json({ received: true, processed: false });
      return;
    }

    // Nota: Removido filtro fromMe pois o dono da instância também envia avisos

    const messageId = msg.messageid || msg.id || '';

    // Prioriza nome visível (pushName/senderName/name) ao invés do número
    // Filtra valores que são apenas números (telefone) - só aceita se tiver letras
    const isValidName = (name?: string) => name && /[a-zA-ZÀ-ÿ]/.test(name);

    // Verifica se é o dono da instância (Vanderson)
    const INSTANCE_OWNER = '556581062401';
    const isInstanceOwner = msg.sender?.includes(INSTANCE_OWNER) || payload.owner === INSTANCE_OWNER;

    const authorName = (isValidName(msg.pushName) ? msg.pushName : null)
      || (isValidName(msg.senderName) ? msg.senderName : null)
      || (isValidName(msg.name) ? msg.name : null)
      || (isInstanceOwner ? 'Vanderson' : 'WhatsApp');
    console.log('[UAZAPI Webhook] Author fields - pushName:', msg.pushName, '| senderName:', msg.senderName, '| name:', msg.name, '| sender:', msg.sender);
    console.log('[UAZAPI Webhook] Using author name:', authorName);

    const text = msg.text || msg.content?.caption || '';
    const mediaUrl = msg.content?.URL;

    // Determina o tipo de mídia
    let mediaType: string | undefined;
    if (msg.messageType) {
      const type = msg.messageType.toLowerCase();
      if (type.includes('image')) mediaType = 'image';
      else if (type.includes('video')) mediaType = 'video';
      else if (type.includes('audio')) mediaType = 'audio';
      else if (type.includes('document')) mediaType = 'document';
    }

    // Ignora mensagens sem conteúdo
    if (!text && !mediaUrl) {
      console.log('[UAZAPI Webhook] Ignoring empty message');
      res.status(200).json({ received: true, processed: false });
      return;
    }

    // Verifica se a mensagem já existe
    const existing = await prisma.announcement.findUnique({
      where: { whatsappMessageId: messageId },
    });

    if (existing) {
      console.log('[UAZAPI Webhook] Message already exists:', messageId);
      res.status(200).json({ received: true, processed: false, reason: 'duplicate' });
      return;
    }

    // Converte timestamp (já vem em milliseconds)
    let whatsappTimestamp: Date | undefined;
    if (msg.messageTimestamp) {
      whatsappTimestamp = new Date(msg.messageTimestamp);
    }

    // Se tem mídia, baixa e salva localmente
    let finalMediaUrl = mediaUrl;
    if (mediaUrl && mediaType) {
      console.log('[UAZAPI Webhook] Downloading media for message:', messageId);
      const fullMessageId = payload.owner ? `${payload.owner}:${messageId}` : messageId;
      const downloaded = await downloadAndSaveMedia(fullMessageId);
      if (downloaded) {
        finalMediaUrl = downloaded.url;
        console.log('[UAZAPI Webhook] Media saved to:', finalMediaUrl);
      } else {
        console.warn('[UAZAPI Webhook] Failed to download media, keeping original URL');
      }
    }

    // Cria o anúncio
    const title = generateTitle(text || `Mídia de ${authorName}`);

    await prisma.announcement.create({
      data: {
        title,
        content: text || `[${mediaType}]`,
        whatsappMessageId: messageId,
        whatsappAuthor: authorName,
        whatsappTimestamp,
        mediaUrl: finalMediaUrl,
        mediaType,
      },
    });

    console.log('[UAZAPI Webhook] ✅ Created announcement from:', authorName, '- ID:', messageId);

    // Send notification to all users
    await sendAnnouncementNotification(
      title,
      text || `[${mediaType}]`,
      authorName
    );

    res.status(200).json({
      received: true,
      processed: true,
      messageId,
    });
  } catch (err) {
    console.error('[UAZAPI Webhook] Error:', err);
    next(err);
  }
}

// Endpoint para importar histórico de mensagens
export async function importMessagesFromGroup(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { messages } = req.body;

    if (!Array.isArray(messages)) {
      res.status(400).json({ error: 'messages must be an array' });
      return;
    }

    let imported = 0;
    let skipped = 0;

    for (const msg of messages) {
      const messageId = msg.messageid || msg.key?.id;
      // Filtra valores que são apenas números (telefone) - só aceita se tiver letras
      const isValidName = (name?: string) => name && /[a-zA-ZÀ-ÿ]/.test(name);

      // Verifica se é o dono da instância (Vanderson)
      const INSTANCE_OWNER = '556581062401';
      const isInstanceOwner = msg.sender?.includes(INSTANCE_OWNER);

      const authorName = (isValidName(msg.pushName) ? msg.pushName : null)
        || (isValidName(msg.senderName) ? msg.senderName : null)
        || (isInstanceOwner ? 'Vanderson' : 'WhatsApp');
      const text = msg.text || msg.message?.conversation || '';

      if (!text) {
        skipped++;
        continue;
      }

      const existing = await prisma.announcement.findUnique({
        where: { whatsappMessageId: messageId },
      });

      if (existing) {
        skipped++;
        continue;
      }

      let whatsappTimestamp: Date | undefined;
      if (msg.messageTimestamp) {
        whatsappTimestamp = new Date(msg.messageTimestamp);
      }

      const title = generateTitle(text);

      await prisma.announcement.create({
        data: {
          title,
          content: text,
          whatsappMessageId: messageId,
          whatsappAuthor: authorName,
          whatsappTimestamp,
        },
      });

      imported++;
    }

    res.json({
      success: true,
      imported,
      skipped,
      total: messages.length,
    });
  } catch (err) {
    next(err);
  }
}
