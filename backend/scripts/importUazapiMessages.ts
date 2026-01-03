/**
 * Script para importar mensagens do grupo AVISOS da UAZAPI
 *
 * Uso:
 *   UAZAPI_URL=https://lucas.uazapi.com UAZAPI_TOKEN=seu_token npx ts-node scripts/importUazapiMessages.ts
 *
 * Ou configure as variáveis no .env e rode:
 *   npx ts-node scripts/importUazapiMessages.ts
 */

import fetch from 'node-fetch';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

const UAZAPI_URL = process.env.UAZAPI_URL || 'https://lucas.uazapi.com';
const UAZAPI_TOKEN = process.env.UAZAPI_TOKEN || '';
const AVISOS_GROUP_ID = process.env.UAZAPI_AVISOS_GROUP_ID || '';

interface UazapiGroup {
  JID: string;
  Name?: string;
  Topic?: string;
}

interface UazapiMessage {
  messageid: string;
  text?: string;
  senderName?: string;
  sender?: string;
  messageTimestamp?: number;
  messageType?: string;
  content?: {
    caption?: string;
    URL?: string;
    mimetype?: string;
  };
}

interface UazapiMessagesResponse {
  messages?: UazapiMessage[];
  hasMore?: boolean;
}

async function uazapiFetch(endpoint: string, options?: RequestInit) {
  const response = await fetch(`${UAZAPI_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'token': UAZAPI_TOKEN,
      ...options?.headers,
    },
  });

  if (!response.ok) {
    throw new Error(`UAZAPI error: ${response.status} ${response.statusText}`);
  }

  return response.json();
}

async function listGroups(): Promise<UazapiGroup[]> {
  console.log('📋 Listando grupos...');
  const data = await uazapiFetch('/group/list') as { groups?: UazapiGroup[] };
  return data.groups || [];
}

async function findAvisosGroup(): Promise<string | null> {
  const groups = await listGroups();

  console.log('\n📋 Grupos encontrados:');
  groups.forEach((g, i) => {
    const name = g.Name || g.Topic || 'Sem nome';
    console.log(`  ${i + 1}. ${name} (${g.JID})`);
  });

  const avisosGroup = groups.find((g) => {
    const name = (g.Name || g.Topic || '').toLowerCase();
    return name.includes('avisos') || name.includes('aviso');
  });

  if (avisosGroup) {
    console.log(`\n✅ Grupo AVISOS encontrado: ${avisosGroup.Name || avisosGroup.Topic} (${avisosGroup.JID})`);
    return avisosGroup.JID;
  }

  console.log('\n❌ Grupo AVISOS não encontrado automaticamente.');
  return null;
}

function extractMessageContent(msg: UazapiMessage): { text: string; mediaType?: string; mediaUrl?: string } {
  const text = msg.text || msg.content?.caption || '';
  const mediaUrl = msg.content?.URL;

  let mediaType: string | undefined;
  if (msg.messageType) {
    const type = msg.messageType.toLowerCase();
    if (type.includes('image')) mediaType = 'image';
    else if (type.includes('video')) mediaType = 'video';
    else if (type.includes('audio')) mediaType = 'audio';
    else if (type.includes('document')) mediaType = 'document';
  }

  return { text, mediaType, mediaUrl };
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

async function fetchMessages(groupId: string, limit: number = 100): Promise<UazapiMessage[]> {
  console.log(`\n📥 Buscando últimas ${limit} mensagens do grupo...`);

  const data = await uazapiFetch('/message/find', {
    method: 'POST',
    body: JSON.stringify({
      chatid: groupId,
      limit,
      offset: 0,
    }),
  }) as UazapiMessagesResponse;

  return data.messages || [];
}

async function importMessages(messages: UazapiMessage[]): Promise<{ imported: number; skipped: number }> {
  let imported = 0;
  let skipped = 0;

  for (const msg of messages) {
    const messageId = msg.messageid;

    if (!messageId) {
      skipped++;
      continue;
    }

    const authorName = msg.senderName || msg.sender?.split('@')[0] || 'WhatsApp';
    const { text, mediaType, mediaUrl } = extractMessageContent(msg);

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

    // Converte timestamp (já vem em milliseconds)
    let whatsappTimestamp: Date | undefined;
    if (msg.messageTimestamp) {
      whatsappTimestamp = new Date(msg.messageTimestamp);
    }

    const title = generateTitle(text || `Mídia de ${authorName}`);

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
    process.stdout.write(`\r  Importando: ${imported} mensagens...`);
  }

  console.log('');

  return { imported, skipped };
}

async function main() {
  console.log('🚀 Iniciando importação de mensagens UAZAPI\n');

  if (!UAZAPI_TOKEN) {
    console.error('❌ UAZAPI_TOKEN não configurado!');
    process.exit(1);
  }

  try {
    let groupId = AVISOS_GROUP_ID;

    if (!groupId) {
      groupId = await findAvisosGroup() || '';
      if (!groupId) {
        process.exit(1);
      }
    }

    const messages = await fetchMessages(groupId, 100);
    console.log(`  Encontradas: ${messages.length} mensagens`);

    if (messages.length === 0) {
      console.log('\n⚠️ Nenhuma mensagem encontrada no grupo.');
      process.exit(0);
    }

    console.log('\n📝 Importando para o banco de dados...');
    const { imported, skipped } = await importMessages(messages);

    console.log('\n✅ Importação concluída!');
    console.log(`   Importadas: ${imported}`);
    console.log(`   Ignoradas (duplicadas ou vazias): ${skipped}`);

    console.log('\n📌 Próximo passo: Configure o webhook na UAZAPI');
    console.log(`   URL do webhook: https://braza.app.br/api/webhook/uazapi`);

  } catch (error) {
    console.error('\n❌ Erro:', error);
    process.exit(1);
  } finally {
    await prisma.$disconnect();
  }
}

main();
