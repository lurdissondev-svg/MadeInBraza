import { Router } from 'express';
import { handleUazapiWebhook, importMessagesFromGroup } from '../controllers/uazapiWebhook.js';
import { handleGitHubWebhook } from '../controllers/githubWebhook.js';
import { authenticate, requireLeader } from '../middleware/auth.js';

export const webhookRouter = Router();

// Webhook público para UAZAPI (sem autenticação)
webhookRouter.post('/uazapi', handleUazapiWebhook);

// Endpoint protegido para importar histórico (apenas líderes)
webhookRouter.post('/uazapi/import', authenticate, requireLeader, importMessagesFromGroup);

// Webhook para GitHub Releases (notificação de atualização do app)
webhookRouter.post('/github', handleGitHubWebhook);
