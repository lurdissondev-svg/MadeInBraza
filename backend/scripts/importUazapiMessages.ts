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
  id: string;
  name?: string;
  subject?: string;
}

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

async function uazapiFetch(endpoint: string, options?: RequestInit) {
  const response = await fetch(`${UAZAPI_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${UAZAPI_TOKEN}`,
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
  return data.groups || (data as unknown as UazapiGroup[]) || [];
}

async function findAvisosGroup(): Promise<string | null> {
  const groups = await listGroups();

  console.log('\n📋 Grupos encontrados:');
  groups.forEach((g, i) => {
    const name = g.name || g.subject || 'Sem nome';
    console.log(`  ${i + 1}. ${name} (${g.id})`);
  });

  // Procura por grupo com nome contendo "avisos" (case insensitive)
  const avisosGroup = groups.find((g) => {
    const name = (g.name || g.subject || '').toLowerCase();
    return name.includes('avisos') || name.includes('aviso');
  });

  if (avisosGroup) {
    console.log(`\n✅ Grupo AVISOS encontrado: ${avisosGroup.name || avisosGroup.subject} (${avisosGroup.id})`);
    return avisosGroup.id;
  }

  console.log('\n❌ Grupo AVISOS não encontrado automaticamente.');
  console.log('   Configure UAZAPI_AVISOS_GROUP_ID manualmente com um dos IDs acima.');
  return null;
}

function extractMessageContent(message: UazapiMessage['message']): { text: string; mediaType?: string; mediaUrl?: string } {
  if (!message) return { text: '' };

  if (message.conversation) {
    return { text: message.conversation };
  }

  if (message.extendedTextMessage?.text) {
    return { text: message.extendedTextMessage.text };
  }

  if (message.imageMessage) {
    return {
      text: message.imageMessage.caption || '',
      mediaType: 'image',
      mediaUrl: message.imageMessage.url,
    };
  }

  if (message.videoMessage) {
    return {
      text: message.videoMessage.caption || '',
      mediaType: 'video',
      mediaUrl: message.videoMessage.url,
    };
  }

  if (message.documentMessage) {
    return {
      text: message.documentMessage.caption || message.documentMessage.fileName || '',
      mediaType: 'document',
      mediaUrl: message.documentMessage.url,
    };
  }

  if (message.audioMessage) {
    return {
      text: '[Áudio]',
      mediaType: 'audio',
      mediaUrl: message.audioMessage.url,
    };
  }

  return { text: '' };
}

function generateTitle(content: string): string {
  const maxLength = 100;

  if (content.length <= maxLength) {
    return content;
  }

  const truncated = content.substring(0, maxLength);
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
  }) as { messages?: UazapiMessage[] };

  return data.messages || (data as unknown as UazapiMessage[]) || [];
}

async function importMessages(messages: UazapiMessage[]): Promise<{ imported: number; skipped: number }> {
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

  console.log(''); // Nova linha após o progresso

  return { imported, skipped };
}

async function main() {
  console.log('🚀 Iniciando importação de mensagens UAZAPI\n');

  if (!UAZAPI_TOKEN) {
    console.error('❌ UAZAPI_TOKEN não configurado!');
    console.log('\nConfigurações necessárias:');
    console.log('  UAZAPI_URL: URL da instância UAZAPI (padrão: https://lucas.uazapi.com)');
    console.log('  UAZAPI_TOKEN: Token de autenticação da UAZAPI');
    console.log('  UAZAPI_AVISOS_GROUP_ID: ID do grupo AVISOS (opcional, será detectado automaticamente)');
    process.exit(1);
  }

  try {
    // Encontra o grupo AVISOS
    let groupId = AVISOS_GROUP_ID;

    if (!groupId) {
      groupId = await findAvisosGroup() || '';
      if (!groupId) {
        process.exit(1);
      }
    }

    // Busca mensagens
    const messages = await fetchMessages(groupId, 100);
    console.log(`  Encontradas: ${messages.length} mensagens`);

    if (messages.length === 0) {
      console.log('\n⚠️ Nenhuma mensagem encontrada no grupo.');
      process.exit(0);
    }

    // Importa mensagens
    console.log('\n📝 Importando para o banco de dados...');
    const { imported, skipped } = await importMessages(messages);

    console.log('\n✅ Importação concluída!');
    console.log(`   Importadas: ${imported}`);
    console.log(`   Ignoradas (duplicadas ou vazias): ${skipped}`);

    // Mostra instrução para configurar o webhook
    console.log('\n📌 Próximo passo: Configure o webhook na UAZAPI');
    console.log(`   URL do webhook: http://braza.lurdisson.com.br/api/webhook/uazapi`);
    console.log(`   Eventos: messages`);
    console.log(`   Filtros: isGroupYes (opcional)`);

  } catch (error) {
    console.error('\n❌ Erro:', error);
    process.exit(1);
  } finally {
    await prisma.$disconnect();
  }
}

main();
