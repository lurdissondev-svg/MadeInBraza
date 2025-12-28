import multer from 'multer';
import path from 'path';
import { v4 as uuidv4 } from 'uuid';
import fs from 'fs';
import { AppError } from './errorHandler.js';

// Tipos de arquivo permitidos
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const ALLOWED_VIDEO_TYPES = ['video/mp4', 'video/quicktime', 'video/webm'];
const ALLOWED_TYPES = [...ALLOWED_IMAGE_TYPES, ...ALLOWED_VIDEO_TYPES];

// Limite de 25MB
const MAX_FILE_SIZE = 25 * 1024 * 1024;

// Pasta base para uploads
const UPLOADS_DIR = path.join(process.cwd(), 'uploads', 'channels');

// Garantir que a pasta existe
if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

// Configuração do storage
const storage = multer.diskStorage({
  destination: (req, _file, cb) => {
    const channelId = req.params.channelId;
    const channelDir = path.join(UPLOADS_DIR, channelId);

    // Criar pasta do canal se não existir
    if (!fs.existsSync(channelDir)) {
      fs.mkdirSync(channelDir, { recursive: true });
    }

    cb(null, channelDir);
  },
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const uniqueName = `${uuidv4()}${ext}`;
    cb(null, uniqueName);
  }
});

// Filtro de arquivos
const fileFilter = (
  _req: Express.Request,
  file: Express.Multer.File,
  cb: multer.FileFilterCallback
) => {
  if (ALLOWED_TYPES.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new AppError(400, 'Tipo de arquivo não permitido. Use imagens (JPEG, PNG, WebP, GIF) ou vídeos (MP4, MOV, WebM).'));
  }
};

// Middleware de upload
export const uploadMedia = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: MAX_FILE_SIZE
  }
}).single('file');

// Helper para determinar o tipo de mídia
export function getMediaType(mimetype: string): 'image' | 'video' {
  return ALLOWED_IMAGE_TYPES.includes(mimetype) ? 'image' : 'video';
}

// Helper para construir URL relativa
export function getMediaUrl(channelId: string, filename: string): string {
  return `/uploads/channels/${channelId}/${filename}`;
}
