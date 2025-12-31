import fs from 'fs';
import path from 'path';
import { randomUUID } from 'crypto';

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

export async function downloadAndSaveMedia(
  messageId: string,
  remoteJid: string
): Promise<{ url: string; mimetype: string } | null> {
  if (!UAZAPI_URL || !UAZAPI_TOKEN) {
    console.error('[UAZAPI Media] UAZAPI_URL or UAZAPI_TOKEN not configured');
    return null;
  }

  try {
    console.log('[UAZAPI Media] Downloading media for message:', messageId);

    // Call UAZAPI download endpoint
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
      console.error('[UAZAPI Media] Download failed:', response.status, response.statusText);
      return null;
    }

    const data = (await response.json()) as DownloadMediaResponse;

    if (data.error) {
      console.error('[UAZAPI Media] API error:', data.error);
      return null;
    }

    const base64Data = data.base64 || data.data;
    if (!base64Data) {
      console.error('[UAZAPI Media] No base64 data in response');
      return null;
    }

    const mimetype = data.mimetype || 'application/octet-stream';
    const extension = getExtensionFromMimetype(mimetype);
    const filename = `${randomUUID()}.${extension}`;
    const filepath = path.join(UPLOADS_DIR, filename);

    // Remove data URL prefix if present
    const cleanBase64 = base64Data.replace(/^data:[^;]+;base64,/, '');

    // Save to file
    const buffer = Buffer.from(cleanBase64, 'base64');
    fs.writeFileSync(filepath, buffer);

    const publicUrl = `${BASE_URL}/uploads/media/${filename}`;
    console.log('[UAZAPI Media] Saved media to:', publicUrl);

    return { url: publicUrl, mimetype };
  } catch (error) {
    console.error('[UAZAPI Media] Error downloading media:', error);
    return null;
  }
}
