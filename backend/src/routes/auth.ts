import { Router } from 'express';
import { register, login, checkStatus, registerFcmToken, changePassword } from '../controllers/auth.js';
import { authenticate } from '../middleware/auth.js';

export const authRouter = Router();

authRouter.post('/register', register);
authRouter.post('/login', login);
authRouter.get('/status', authenticate, checkStatus);
authRouter.post('/fcm-token', authenticate, registerFcmToken);
authRouter.put('/change-password', authenticate, changePassword);
