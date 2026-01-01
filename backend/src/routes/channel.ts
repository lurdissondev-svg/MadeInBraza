import { Router } from 'express';
import { authenticate } from '../middleware/auth.js';
import { uploadMedia } from '../middleware/upload.js';
import {
  getChannels,
  getChannelMessages,
  sendMessage,
  sendMediaMessage,
  createDefaultChannels,
  getChannelMembers,
} from '../controllers/channel.js';

const router = Router();

// All routes require authentication
router.use(authenticate);

// GET /channels - List accessible channels
router.get('/', getChannels);

// POST /channels/setup - Create default channels (GENERAL, LEADERS) - Leaders only
router.post('/setup', createDefaultChannels);

// GET /channels/:channelId/messages - Get channel messages
router.get('/:channelId/messages', getChannelMessages);

// POST /channels/:channelId/messages - Send text message to channel
router.post('/:channelId/messages', sendMessage);

// POST /channels/:channelId/messages/media - Send media message to channel
router.post('/:channelId/messages/media', uploadMedia, sendMediaMessage);

// GET /channels/:channelId/members - Get channel members
router.get('/:channelId/members', getChannelMembers);

// POST /channels/:channelId/read - Mark channel as read (stub - no-op for now)
router.post('/:channelId/read', (_req, res) => {
  res.json({ success: true });
});

export default router;
