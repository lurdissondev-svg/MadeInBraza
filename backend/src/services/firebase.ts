import * as admin from 'firebase-admin';
import * as path from 'path';
import * as fs from 'fs';

let messaging: admin.messaging.Messaging | null = null;

try {
  const serviceAccountPath = path.join(__dirname, '../../firebase-service-account.json');

  if (fs.existsSync(serviceAccountPath)) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccountPath),
    });
    messaging = admin.messaging();
    console.log('Firebase initialized successfully');
  } else {
    console.warn('Firebase service account file not found, notifications disabled');
  }
} catch (error) {
  console.error('Failed to initialize Firebase:', error);
}

export { messaging };

interface NotificationPayload {
  title: string;
  body: string;
  data?: Record<string, string>;
}

export async function sendNotificationToUser(
  fcmToken: string,
  notification: NotificationPayload
): Promise<boolean> {
  if (!messaging) {
    console.warn('Firebase not initialized, skipping notification');
    return false;
  }
  try {
    await messaging.send({
      token: fcmToken,
      notification: {
        title: notification.title,
        body: notification.body,
      },
      data: notification.data,
      android: {
        priority: 'high',
        notification: {
          channelId: 'braza_notifications',
        },
      },
    });
    return true;
  } catch (error) {
    console.error('Error sending notification:', error);
    return false;
  }
}

export async function sendNotificationToMultiple(
  fcmTokens: string[],
  notification: NotificationPayload
): Promise<{ success: number; failure: number }> {
  if (!messaging) {
    console.warn('Firebase not initialized, skipping multicast notification');
    return { success: 0, failure: fcmTokens.length };
  }

  if (fcmTokens.length === 0) {
    return { success: 0, failure: 0 };
  }

  try {
    const response = await messaging.sendEachForMulticast({
      tokens: fcmTokens,
      notification: {
        title: notification.title,
        body: notification.body,
      },
      data: notification.data,
      android: {
        priority: 'high',
        notification: {
          channelId: 'braza_notifications',
        },
      },
    });

    return {
      success: response.successCount,
      failure: response.failureCount,
    };
  } catch (error) {
    console.error('Error sending multicast notification:', error);
    return { success: 0, failure: fcmTokens.length };
  }
}
