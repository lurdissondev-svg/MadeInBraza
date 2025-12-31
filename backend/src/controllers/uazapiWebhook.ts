import type { Request, Response, NextFunction } from 'express';
import { prisma } from '../utils/prisma.js';
import { downloadAndSaveMedia } from '../services/uazapiMedia.js';

// Configuração do grupo AVISOS (será preenchido após listar grupos)
const AVISOS_GROUP_ID = process.env.UAZAPI_AVISOS_GROUP_ID || '';

interface UazapiMessage {
  key: {
    remoteJid: string;
    fromMe: boolean;
    id: string;
    participant?: string;
  };
  pushName?: string;
  message?: {
    conversation?: string;
    extendedTextMessage?: {
      text: string;
    };
    imageMessage?: {
      caption?: string;
      mimetype?: string;
      url?: string;
    };
    videoMessage?: {
      caption?: string;
      mimetype?: string;
      url?: string;
    };
    documentMessage?: {
      caption?: string;
      mimetype?: string;
      fileName?: string;
      url?: string;
    };
    audioMessage?: {
      mimetype?: string;
      url?: string;
    };
  };
  messageTimestamp?: number | string;
}

interface UazapiWebhookPayload {
  event: string;
  instance: string;
  data?: {
    messages?: UazapiMessage[];
    message?: UazapiMessage;
  };
}

function extractMessageContent(message: UazapiMessage['message']): { text: string; mediaType?: string; mediaUrl?: string } {
  if (!message) return { text: '' };

  // Mensagem de texto simples
  if (message.conversation) {
    return { text: message.conversation };
  }

  // Mensagem de texto estendida (com links, etc)
  if (message.extendedTextMessage?.text) {
    return { text: message.extendedTextMessage.text };
  }

  // Imagem
  if (message.imageMessage) {
    return {
      text: message.imageMessage.caption || '',
      mediaType: 'image',
      mediaUrl: message.imageMessage.url,
    };
  }

  // Vídeo
  if (message.videoMessage) {
    return {
      text: message.videoMessage.caption || '',
      mediaType: 'video',
      mediaUrl: message.videoMessage.url,
    };
  }

  // Documento
  if (message.documentMessage) {
    return {
      text: message.documentMessage.caption || message.documentMessage.fileName || '',
      mediaType: 'document',
      mediaUrl: message.documentMessage.url,
    };
  }

  // Áudio
  if (message.audioMessage) {
    return {
      text: '[Áudio]',
      mediaType: 'audio',
      mediaUrl: message.audioMessage.url,
    };
  }

  return { text: '' };
}

function generateTitle(content: string, authorName: string): string {
  // Limita o título a 100 caracteres
  const maxLength = 100;

  if (content.length <= maxLength) {
    return content;
  }

  // Tenta cortar em uma palavra completa
  const truncated = content.substring(0, maxLength);
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

    // Log detalhado para debug
    console.log('[UAZAPI Webhook] ========== INCOMING WEBHOOK ==========');
    console.log('[UAZAPI Webhook] Event:', payload.event);
    console.log('[UAZAPI Webhook] Full payload:', JSON.stringify(payload, null, 2));

    // Só processa eventos de mensagens
    if (payload.event !== 'messages' && payload.event !== 'messages.upsert') {
      console.log('[UAZAPI Webhook] Ignoring non-message event:', payload.event);
      res.status(200).json({ received: true, processed: false });
      return;
    }

    // Extrai a mensagem do payload
    const messages = payload.data?.messages || (payload.data?.message ? [payload.data.message] : []);

    console.log('[UAZAPI Webhook] Messages found:', messages.length);

    if (messages.length === 0) {
      console.log('[UAZAPI Webhook] No messages in payload');
      res.status(200).json({ received: true, processed: false });
      return;
    }

    let processedCount = 0;

    for (const msg of messages) {
      // Verifica se é do grupo AVISOS
      const chatId = msg.key?.remoteJid;

      console.log('[UAZAPI Webhook] Processing message from chat:', chatId);
      console.log('[UAZAPI Webhook] Expected group ID:', AVISOS_GROUP_ID);

      if (!AVISOS_GROUP_ID) {
        console.warn('[UAZAPI Webhook] UAZAPI_AVISOS_GROUP_ID not configured');
        continue;
      }

      if (chatId !== AVISOS_GROUP_ID) {
        console.log('[UAZAPI Webhook] Ignoring message - chat ID mismatch');
        continue;
      }

      // Ignora mensagens enviadas pelo próprio bot
      if (msg.key.fromMe) {
        continue;
      }

      const messageId = msg.key.id;
      const authorName = msg.pushName || 'WhatsApp';
      const { text, mediaType, mediaUrl } = extractMessageContent(msg.message);

      // Ignora mensagens sem conteúdo
      if (!text && !mediaUrl) {
        continue;
      }

      // Verifica se a mensagem já existe
      const existing = await prisma.announcement.findUnique({
        where: { whatsappMessageId: messageId },
      });

      if (existing) {
        console.log('[UAZAPI Webhook] Message already exists:', messageId);
        continue;
      }

      // Converte timestamp
      let whatsappTimestamp: Date | undefined;
      if (msg.messageTimestamp) {
        const ts = typeof msg.messageTimestamp === 'string'
          ? parseInt(msg.messageTimestamp)
          : msg.messageTimestamp;
        whatsappTimestamp = new Date(ts * 1000);
      }

      // Se tem mídia, baixa e salva localmente
      let finalMediaUrl = mediaUrl;
      if (mediaUrl && mediaType) {
        console.log('[UAZAPI Webhook] Downloading media for message:', messageId);
        const downloaded = await downloadAndSaveMedia(messageId, chatId);
        if (downloaded) {
          finalMediaUrl = downloaded.url;
          console.log('[UAZAPI Webhook] Media saved to:', finalMediaUrl);
        } else {
          console.warn('[UAZAPI Webhook] Failed to download media, keeping original URL');
        }
      }

      // Cria o anúncio
      const title = generateTitle(text || `Mídia de ${authorName}`, authorName);

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

      console.log('[UAZAPI Webhook] Created announcement from:', authorName, '- ID:', messageId);
      processedCount++;
    }

    res.status(200).json({
      received: true,
      processed: true,
      count: processedCount,
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
    const { messages } = req.body as { messages: UazapiMessage[] };

    if (!Array.isArray(messages)) {
      res.status(400).json({ error: 'messages must be an array' });
      return;
    }

    let imported = 0;
    let skipped = 0;

    for (const msg of messages) {
      const messageId = msg.key.id;
      const authorName = msg.pushName || 'WhatsApp';
      const { text, mediaType, mediaUrl } = extractMessageContent(msg.message);

      // Ignora mensagens sem conteúdo
      if (!text && !mediaUrl) {
        skipped++;
        continue;
      }

      // Verifica se já existe
      const existing = await prisma.announcement.findUnique({
        where: { whatsappMessageId: messageId },
      });

      if (existing) {
        skipped++;
        continue;
      }

      // Converte timestamp
      let whatsappTimestamp: Date | undefined;
      if (msg.messageTimestamp) {
        const ts = typeof msg.messageTimestamp === 'string'
          ? parseInt(msg.messageTimestamp)
          : msg.messageTimestamp;
        whatsappTimestamp = new Date(ts * 1000);
      }

      const title = generateTitle(text || `Mídia de ${authorName}`, authorName);

      await prisma.announcement.create({
        data: {
          title,
          content: text || `[${mediaType}]`,
          whatsappMessageId: messageId,
          whatsappAuthor: authorName,
          whatsappTimestamp,
          mediaUrl,
          mediaType,
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
