import { Router } from 'express';
import { getProfile, updateProfile, uploadUserAvatar, deleteUserAvatar } from '../controllers/profile.js';
import { authenticate, requireApproved } from '../middleware/auth.js';
import { uploadAvatar } from '../middleware/upload.js';

export const profileRouter = Router();

profileRouter.use(authenticate);
profileRouter.use(requireApproved);

profileRouter.get('/', getProfile);
profileRouter.put('/', updateProfile);

// Avatar routes
profileRouter.post('/avatar', uploadAvatar, uploadUserAvatar);
profileRouter.delete('/avatar', deleteUserAvatar);
