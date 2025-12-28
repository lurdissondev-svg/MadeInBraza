import { Router } from 'express';
import { getMessages, sendMessage } from '../controllers/chat.js';
import { authenticate, requireApproved } from '../middleware/auth.js';

export const chatRouter = Router();

chatRouter.use(authenticate);
chatRouter.use(requireApproved);

chatRouter.get('/messages', getMessages);
chatRouter.post('/messages', sendMessage);
