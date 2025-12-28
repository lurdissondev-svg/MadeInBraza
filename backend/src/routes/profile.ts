import { Router } from 'express';
import { getProfile, updateProfile } from '../controllers/profile.js';
import { authenticate, requireApproved } from '../middleware/auth.js';

export const profileRouter = Router();

profileRouter.use(authenticate);
profileRouter.use(requireApproved);

profileRouter.get('/', getProfile);
profileRouter.put('/', updateProfile);
