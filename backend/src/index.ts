import express from 'express';
import cors from 'cors';
import path from 'path';
import { authRouter } from './routes/auth.js';
import { usersRouter } from './routes/users.js';
import { chatRouter } from './routes/chat.js';
import { eventsRouter } from './routes/events.js';
import { profileRouter } from './routes/profile.js';
import { partiesRouter } from './routes/parties.js';
import siegeWarRouter from './routes/siegeWar.js';
import channelRouter from './routes/channel.js';
import { announcementsRouter } from './routes/announcements.js';
import { webhookRouter } from './routes/webhook.js';
import { errorHandler } from './middleware/errorHandler.js';
import { startSiegeWarCron } from './services/siegeWarCron.js';

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Serve uploaded files statically
app.use('/uploads', express.static(path.join(process.cwd(), 'uploads')));

// Serve public files (privacy policy, etc.)
app.use(express.static(path.join(process.cwd(), 'public')));

// Serve Vue.js web app
const webDistPath = path.join(process.cwd(), 'web', 'dist');
app.use('/web', express.static(webDistPath));

// SPA fallback for Vue Router history mode
app.get('/web/*', (_req, res) => {
  res.sendFile(path.join(webDistPath, 'index.html'));
});

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.use('/api/auth', authRouter);
app.use('/api/users', usersRouter);
app.use('/api/chat', chatRouter);
app.use('/api/events', eventsRouter);
app.use('/api/profile', profileRouter);
app.use('/api/parties', partiesRouter);
app.use('/api/siege-war', siegeWarRouter);
app.use('/api/channels', channelRouter);
app.use('/api/announcements', announcementsRouter);
app.use('/api/webhook', webhookRouter);
app.use('/webhook', webhookRouter); // Rota alternativa sem /api

app.use(errorHandler);

app.listen(PORT, () => {
  console.log(`Braza Backend running on port ${PORT}`);
  startSiegeWarCron();
});
