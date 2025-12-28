import { Router } from 'express';
import { authenticate } from '../middleware/auth.js';
import {
  getCurrentSiegeWar,
  submitResponse,
  getResponses,
  getAvailableShares,
  createSiegeWar,
  closeSiegeWar,
  getSiegeWarHistory,
} from '../controllers/siegeWar.js';

const router = Router();

// All routes require authentication
router.use(authenticate);

// Get current active Siege War and user's response
router.get('/current', getCurrentSiegeWar);

// Get Siege War history
router.get('/history', getSiegeWarHistory);

// Create new Siege War (Leaders only)
router.post('/', createSiegeWar);

// Submit/update response to a Siege War
router.post('/:siegeWarId/respond', submitResponse);

// Get all responses for a Siege War (Leaders only)
router.get('/:siegeWarId/responses', getResponses);

// Get available shared accounts for piloting
router.get('/:siegeWarId/available-shares', getAvailableShares);

// Close a Siege War (Leaders only)
router.post('/:siegeWarId/close', closeSiegeWar);

export default router;
