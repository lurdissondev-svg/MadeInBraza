import { Router } from 'express';
import { getMembers, getPendingUsers, approveUser, rejectUser, banUser, promoteUser, demoteUser, getBannedUsers, unbanUser, getUserProfile, updateUserRole } from '../controllers/users.js';
import { authenticate, requireApproved, requireLeader } from '../middleware/auth.js';

export const usersRouter = Router();

usersRouter.use(authenticate);
usersRouter.use(requireApproved);

// Available to all approved members
usersRouter.get('/members', getMembers);
usersRouter.get('/:id/profile', getUserProfile);

// Leader only routes
usersRouter.get('/pending', requireLeader, getPendingUsers);
usersRouter.get('/banned', requireLeader, getBannedUsers);
usersRouter.post('/:id/approve', requireLeader, approveUser);
usersRouter.post('/:id/reject', requireLeader, rejectUser);
usersRouter.post('/:id/ban', requireLeader, banUser);
usersRouter.post('/:id/unban', requireLeader, unbanUser);
usersRouter.post('/:id/promote', requireLeader, promoteUser);
usersRouter.post('/:id/demote', requireLeader, demoteUser);
usersRouter.put('/:id/role', requireLeader, updateUserRole);
