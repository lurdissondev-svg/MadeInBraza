import { Router } from 'express';
import { getPartiesByEvent, createParty, deleteParty, joinParty, leaveParty } from '../controllers/parties.js';
import { authenticate, requireApproved } from '../middleware/auth.js';

export const partiesRouter = Router();

partiesRouter.use(authenticate);
partiesRouter.use(requireApproved);

// Get all parties for an event
partiesRouter.get('/event/:eventId', getPartiesByEvent);

// Create a new party for an event
partiesRouter.post('/event/:eventId', createParty);

// Delete a party (creator or leader only)
partiesRouter.delete('/:partyId', deleteParty);

// Join a party
partiesRouter.post('/:partyId/join', joinParty);

// Leave a party
partiesRouter.post('/:partyId/leave', leaveParty);
