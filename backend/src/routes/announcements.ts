import { Router } from 'express';
import {
  getAnnouncements,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement
} from '../controllers/announcements.js';
import { authenticate, requireApproved, requireLeader } from '../middleware/auth.js';

export const announcementsRouter = Router();

// All routes require authentication and approved status
announcementsRouter.use(authenticate);
announcementsRouter.use(requireApproved);

// List all announcements (any member)
announcementsRouter.get('/', getAnnouncements);

// Create announcement (leaders only)
announcementsRouter.post('/', requireLeader, createAnnouncement);

// Update announcement (leaders only)
announcementsRouter.put('/:id', requireLeader, updateAnnouncement);

// Delete announcement (leaders only)
announcementsRouter.delete('/:id', requireLeader, deleteAnnouncement);
