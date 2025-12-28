import { Router } from 'express';
import { getEvents, createEvent, deleteEvent, joinEvent, leaveEvent } from '../controllers/events.js';
import { authenticate, requireApproved, requireLeader } from '../middleware/auth.js';

export const eventsRouter = Router();

eventsRouter.use(authenticate);
eventsRouter.use(requireApproved);

// All approved members can view events and join/leave
eventsRouter.get('/', getEvents);
eventsRouter.post('/:id/join', joinEvent);
eventsRouter.post('/:id/leave', leaveEvent);

// Only leaders can create and delete events
eventsRouter.post('/', requireLeader, createEvent);
eventsRouter.delete('/:id', requireLeader, deleteEvent);
