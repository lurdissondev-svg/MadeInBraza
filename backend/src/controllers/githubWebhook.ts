import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import crypto from 'crypto';
import { sendNotificationToMultiple } from '../services/firebase.js';

const prisma = new PrismaClient();

// GitHub webhook secret (configure in environment)
const GITHUB_WEBHOOK_SECRET = process.env.GITHUB_WEBHOOK_SECRET || '';

function verifyGitHubSignature(payload: string, signature: string | undefined): boolean {
  if (!GITHUB_WEBHOOK_SECRET || !signature) {
    // If no secret is configured, allow all requests (for testing)
    return !GITHUB_WEBHOOK_SECRET;
  }

  const hmac = crypto.createHmac('sha256', GITHUB_WEBHOOK_SECRET);
  const digest = 'sha256=' + hmac.update(payload).digest('hex');

  try {
    return crypto.timingSafeEqual(Buffer.from(digest), Buffer.from(signature));
  } catch {
    return false;
  }
}

export async function handleGitHubWebhook(req: Request, res: Response) {
  try {
    const event = req.headers['x-github-event'];
    const signature = req.headers['x-hub-signature-256'] as string | undefined;
    const payload = JSON.stringify(req.body);

    // Verify signature if secret is configured
    if (GITHUB_WEBHOOK_SECRET && !verifyGitHubSignature(payload, signature)) {
      console.warn('GitHub webhook signature verification failed');
      return res.status(401).json({ error: 'Invalid signature' });
    }

    // Only process release events
    if (event !== 'release') {
      return res.status(200).json({ message: 'Event ignored', event });
    }

    const { action, release } = req.body;

    // Only process when a release is published
    if (action !== 'published') {
      return res.status(200).json({ message: 'Action ignored', action });
    }

    const tagName = release?.tag_name || 'nova versão';
    const releaseName = release?.name || tagName;
    const releaseBody = release?.body || '';
    const version = tagName.replace(/^v/i, '');

    console.log(`New release published: ${releaseName} (${tagName})`);

    // Get all users with FCM tokens
    const usersWithTokens = await prisma.user.findMany({
      where: {
        fcmToken: { not: null },
        status: 'APPROVED'
      },
      select: {
        fcmToken: true
      }
    });

    const fcmTokens = usersWithTokens
      .map(u => u.fcmToken)
      .filter((token): token is string => token !== null);

    if (fcmTokens.length === 0) {
      console.log('No users with FCM tokens to notify');
      return res.status(200).json({ message: 'No users to notify' });
    }

    // Send push notification to all users
    const result = await sendNotificationToMultiple(fcmTokens, {
      title: `🚀 Atualização disponível: v${version}`,
      body: releaseBody
        ? releaseBody.substring(0, 100) + (releaseBody.length > 100 ? '...' : '')
        : 'Uma nova versão do app está disponível!',
      data: {
        type: 'app_update',
        version: version,
        tagName: tagName,
        releaseName: releaseName
      }
    });

    console.log(`Update notification sent: ${result.success} success, ${result.failure} failed`);

    return res.status(200).json({
      message: 'Release notification sent',
      version,
      notified: result.success,
      failed: result.failure
    });

  } catch (error) {
    console.error('Error handling GitHub webhook:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
