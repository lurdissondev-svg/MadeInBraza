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
  fileURL?: string;
  mimetype?: string;
  transcription?: string;
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
  _remoteJid?: string
): Promise<{ url: string; mimetype: string } | null> {
  if (!UAZAPI_URL || !UAZAPI_TOKEN) {
    console.error('[UAZAPI Media] UAZAPI_URL or UAZAPI_TOKEN not configured');
    return null;
  }

  try {
    // Remove owner prefix if present (e.g., "556581062401:3EB074786CD363571051CD" -> "3EB074786CD363571051CD")
    const cleanMessageId = messageId.includes(':') ? messageId.split(':').pop()! : messageId;
    console.log('[UAZAPI Media] Requesting media for message:', cleanMessageId);

    // Call UAZAPI download endpoint to get cached URL
    const response = await fetch(`${UAZAPI_URL}/message/download`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': UAZAPI_TOKEN,
      },
      body: JSON.stringify({
        id: cleanMessageId,
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

    if (!data.fileURL) {
      console.error('[UAZAPI Media] No fileURL in response');
      return null;
    }

    // Download the file from UAZAPI's cached URL
    console.log('[UAZAPI Media] Downloading from:', data.fileURL);
    const fileResponse = await fetch(data.fileURL);
    if (!fileResponse.ok) {
      console.error('[UAZAPI Media] Failed to download file:', fileResponse.status);
      return null;
    }

    const mimetype = data.mimetype || 'application/octet-stream';
    const extension = getExtensionFromMimetype(mimetype);
    const filename = `${randomUUID()}.${extension}`;
    const filepath = path.join(UPLOADS_DIR, filename);

    // Save to local file
    const buffer = Buffer.from(await fileResponse.arrayBuffer());
    fs.writeFileSync(filepath, buffer);

    const publicUrl = `${BASE_URL}/uploads/media/${filename}`;
    console.log('[UAZAPI Media] Saved locally to:', publicUrl);

    return { url: publicUrl, mimetype };
  } catch (error) {
    console.error('[UAZAPI Media] Error downloading media:', error);
    return null;
  }
}
