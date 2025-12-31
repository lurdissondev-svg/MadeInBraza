import { PrismaClient } from '@prisma/client';
import fs from 'fs';
import path from 'path';
import { randomUUID } from 'crypto';

const prisma = new PrismaClient();

const UAZAPI_URL = process.env.UAZAPI_URL || '';
const UAZAPI_TOKEN = process.env.UAZAPI_TOKEN || '';
const UPLOADS_DIR = path.join(process.cwd(), 'uploads', 'media');
const BASE_URL = process.env.BASE_URL || 'https://braza.lurdisson.com.br';

// Ensure uploads/media directory exists
if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

interface DownloadMediaResponse {
  mimetype?: string;
  base64?: string;
  data?: string;
  error?: string;
}

function getExtensionFromMimetype(mimetype: string): string {
  const mimeMap: Record<string, string> = {
    'image/jpeg': 'jpg',
    'image/jpg': 'jpg',
    'image/png': 'png',
    'image/gif': 'gif',
    'image/webp': 'webp',
    'video/mp4': 'mp4',
    'video/3gpp': '3gp',
    'audio/ogg': 'ogg',
    'audio/mpeg': 'mp3',
    'audio/mp4': 'm4a',
    'application/pdf': 'pdf',
  };
  return mimeMap[mimetype] || 'bin';
}

async function downloadAndSaveMedia(
  messageId: string,
  remoteJid: string
): Promise<{ url: string; mimetype: string } | null> {
  if (!UAZAPI_URL || !UAZAPI_TOKEN) {
    console.error('[Fix Media] UAZAPI_URL or UAZAPI_TOKEN not configured');
    return null;
  }

  try {
    console.log('[Fix Media] Downloading media for message:', messageId);

    const response = await fetch(`${UAZAPI_URL}/message/downloadMediaMessage`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${UAZAPI_TOKEN}`,
      },
      body: JSON.stringify({
        messageId,
        remoteJid,
      }),
    });

    if (!response.ok) {
      console.error('[Fix Media] Download failed:', response.status, response.statusText);
      return null;
    }

    const data = (await response.json()) as DownloadMediaResponse;

    if (data.error) {
      console.error('[Fix Media] API error:', data.error);
      return null;
    }

    const base64Data = data.base64 || data.data;
    if (!base64Data) {
      console.error('[Fix Media] No base64 data in response');
      return null;
    }

    const mimetype = data.mimetype || 'application/octet-stream';
    const extension = getExtensionFromMimetype(mimetype);
    const filename = `${randomUUID()}.${extension}`;
    const filepath = path.join(UPLOADS_DIR, filename);

    const cleanBase64 = base64Data.replace(/^data:[^;]+;base64,/, '');
    const buffer = Buffer.from(cleanBase64, 'base64');
    fs.writeFileSync(filepath, buffer);

    const publicUrl = `${BASE_URL}/uploads/media/${filename}`;
    console.log('[Fix Media] Saved media to:', publicUrl);

    return { url: publicUrl, mimetype };
  } catch (error) {
    console.error('[Fix Media] Error downloading media:', error);
    return null;
  }
}

async function main() {
  const AVISOS_GROUP_ID = process.env.UAZAPI_AVISOS_GROUP_ID || '';

  if (!AVISOS_GROUP_ID) {
    console.error('UAZAPI_AVISOS_GROUP_ID not set');
    process.exit(1);
  }

  console.log('Finding announcements with WhatsApp media URLs...');

  const announcements = await prisma.announcement.findMany({
    where: {
      whatsappMessageId: { not: null },
      mediaUrl: { not: null },
      mediaType: { not: null },
    },
  });

  console.log(`Found ${announcements.length} announcements with media`);

  for (const ann of announcements) {
    // Skip if already a local URL
    if (ann.mediaUrl?.startsWith(BASE_URL)) {
      console.log(`Skipping ${ann.id} - already has local URL`);
      continue;
    }

    console.log(`Processing announcement ${ann.id}...`);

    const result = await downloadAndSaveMedia(
      ann.whatsappMessageId!,
      AVISOS_GROUP_ID
    );

    if (result) {
      await prisma.announcement.update({
        where: { id: ann.id },
        data: { mediaUrl: result.url },
      });
      console.log(`Updated announcement ${ann.id} with new URL: ${result.url}`);
    } else {
      console.error(`Failed to download media for announcement ${ann.id}`);
    }
  }

  console.log('Done!');
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect());
