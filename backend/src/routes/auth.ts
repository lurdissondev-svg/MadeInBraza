import { Router } from 'express';
import {
  register,
  login,
  checkStatus,
  registerFcmToken,
  changePassword,
  forgotPassword,
  requestPasswordReset,
  verifyResetToken,
  resetPasswordWithToken,
  updateEmail,
} from '../controllers/auth.js';
import { authenticate } from '../middleware/auth.js';

export const authRouter = Router();

authRouter.post('/register', register);
authRouter.post('/login', login);
authRouter.get('/status', authenticate, checkStatus);
authRouter.post('/fcm-token', authenticate, registerFcmToken);
authRouter.put('/change-password', authenticate, changePassword);
authRouter.put('/update-email', authenticate, updateEmail);

// Password reset
authRouter.post('/forgot-password', forgotPassword); // Legacy - generates random password
authRouter.post('/request-reset', requestPasswordReset); // New - sends email with link
authRouter.post('/verify-reset-token', verifyResetToken); // Verify token is valid
authRouter.post('/reset-password', resetPasswordWithToken); // Reset password with token
