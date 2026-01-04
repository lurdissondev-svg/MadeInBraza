package com.madeinbraza.app.data.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.madeinbraza.app.MainActivity
import com.madeinbraza.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrazaMessagingService : FirebaseMessagingService() {

    companion object {
        // Channel IDs for different notification types
        const val CHANNEL_MESSAGES = "braza_messages"
        const val CHANNEL_EVENTS = "braza_events"
        const val CHANNEL_GENERAL = "braza_general"
        const val CHANNEL_UPDATES = "braza_updates"
        const val CHANNEL_PARTIES = "braza_parties"

        // Notification types from backend
        const val TYPE_CHANNEL_MESSAGE = "channel_message"
        const val TYPE_EVENT = "event"
        const val TYPE_ANNOUNCEMENT = "announcement"
        const val TYPE_PARTY = "party"
        const val TYPE_SIEGE_WAR = "siege_war"
        const val TYPE_APP_UPDATE = "app_update"
        const val TYPE_PENDING_APPROVAL = "pending_approval"

        // Stable notification IDs to prevent duplicates
        private const val NOTIFICATION_ID_PARTY = 100
        private const val NOTIFICATION_ID_ANNOUNCEMENT = 200
        private const val NOTIFICATION_ID_PENDING = 300
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token will be registered when user logs in
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        createNotificationChannels()

        val data = message.data
        val notificationType = data["type"] ?: "general"

        when (notificationType) {
            TYPE_CHANNEL_MESSAGE -> handleChannelMessageNotification(data)
            TYPE_EVENT -> handleEventNotification(data)
            TYPE_ANNOUNCEMENT -> handleAnnouncementNotification(data)
            TYPE_PARTY -> handlePartyNotification(data)
            TYPE_SIEGE_WAR -> handleSiegeWarNotification(data)
            TYPE_APP_UPDATE -> handleAppUpdateNotification(data)
            TYPE_PENDING_APPROVAL -> handlePendingApprovalNotification(data)
            else -> handleGeneralNotification(message)
        }
    }

    private fun handleChannelMessageNotification(data: Map<String, String>) {
        val channelId = data["channelId"]
        val channelName = data["channelName"] ?: "Canal"
        val senderName = data["senderName"] ?: "Alguém"
        val messageContent = data["message"] ?: ""
        val isMedia = data["isMedia"] == "true"

        val title = "#$channelName"
        val body = if (isMedia) {
            "$senderName enviou uma mídia"
        } else {
            "$senderName: $messageContent"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "channel")
            channelId?.let { putExtra("channelId", it) }
            putExtra("channelName", channelName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            channelId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setColor(Color.parseColor("#FF9800"))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Group notifications by channel
        val notificationId = channelId?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun handleEventNotification(data: Map<String, String>) {
        val eventId = data["eventId"]
        val eventTitle = data["eventTitle"] ?: "Evento"
        val action = data["action"] ?: "update" // join, leave, new, reminder

        val title = when (action) {
            "new" -> "Novo evento criado"
            "reminder" -> "Lembrete de evento"
            "join" -> "Alguém entrou no evento"
            "leave" -> "Alguém saiu do evento"
            else -> "Atualização de evento"
        }
        val body = eventTitle

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "events")
            eventId?.let { putExtra("eventId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            eventId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setColor(Color.parseColor("#4CAF50"))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(eventId?.hashCode() ?: System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun handleAnnouncementNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Novo Anúncio"
        val content = data["content"] ?: ""
        val authorName = data["authorName"]

        val body = if (authorName != null) {
            "Por $authorName: $content"
        } else {
            content
        }

        showSimpleNotification(
            title = title,
            body = body,
            navigateTo = "home",
            channelId = CHANNEL_GENERAL
        )
    }

    private fun handlePartyNotification(data: Map<String, String>) {
        val partyName = data["partyName"] ?: "Party"
        val action = data["action"] ?: "update"
        val partyId = data["partyId"]

        val title = when (action) {
            "new" -> "Nova party criada"
            "join" -> "Alguém entrou na party"
            "leave" -> "Alguém saiu da party"
            else -> "Atualização de party"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "parties")
            partyId?.let { putExtra("partyId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID_PARTY,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_PARTIES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(partyName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(partyName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setColor(Color.parseColor("#4CAF50")) // Green for parties

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use stable ID to replace existing party notifications
        notificationManager.notify(NOTIFICATION_ID_PARTY, notificationBuilder.build())
    }

    private fun handleSiegeWarNotification(data: Map<String, String>) {
        val action = data["action"] ?: "update"

        val title = when (action) {
            "new" -> "Siege War aberta!"
            "close" -> "Siege War encerrada"
            "response" -> "Nova resposta na Siege War"
            else -> "Atualização de Siege War"
        }
        val body = data["message"] ?: "Verifique sua resposta"

        showSimpleNotification(
            title = title,
            body = body,
            navigateTo = "siege_war",
            channelId = CHANNEL_EVENTS
        )
    }

    private fun handleAppUpdateNotification(data: Map<String, String>) {
        val version = data["version"] ?: "nova"
        val releaseName = data["releaseName"] ?: "Nova versão"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "update")
            putExtra("version", version)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            "app_update".hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚀 Atualização v$version")
            .setContentText("Toque para atualizar o app")
            .setStyle(NotificationCompat.BigTextStyle().bigText(releaseName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setColor(Color.parseColor("#2196F3"))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify("app_update".hashCode(), notificationBuilder.build())
    }

    private fun handlePendingApprovalNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Nova solicitação de entrada"
        val body = data["body"] ?: "Um novo usuário aguarda aprovação"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "pending_members")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            "pending_approval".hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setColor(Color.parseColor("#FFC107")) // Yellow color for pending

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun handleGeneralNotification(message: RemoteMessage) {
        val notification = message.notification ?: return
        val data = message.data

        showSimpleNotification(
            title = notification.title ?: "Braza",
            body = notification.body ?: "",
            navigateTo = data["navigateTo"] ?: "home",
            channelId = CHANNEL_GENERAL
        )
    }

    private fun showSimpleNotification(
        title: String,
        body: String,
        navigateTo: String,
        channelId: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", navigateTo)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#FF9800"))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Messages channel - high importance for chat messages
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Mensagens",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de mensagens nos canais"
                enableLights(true)
                lightColor = Color.parseColor("#FF9800")
                enableVibration(true)
            }

            // Events channel - default importance
            val eventsChannel = NotificationChannel(
                CHANNEL_EVENTS,
                "Eventos e Siege War",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificações de eventos e Siege War"
                enableLights(true)
                lightColor = Color.parseColor("#4CAF50")
            }

            // General channel - low importance
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "Geral",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificações gerais do Braza"
            }

            // Updates channel - high importance for app updates
            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "Atualizações",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de atualizações do app"
                enableLights(true)
                lightColor = Color.parseColor("#2196F3")
                enableVibration(true)
            }

            // Parties channel - high importance for party notifications
            val partiesChannel = NotificationChannel(
                CHANNEL_PARTIES,
                "Parties",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de parties (PTs)"
                enableLights(true)
                lightColor = Color.parseColor("#4CAF50")
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, eventsChannel, generalChannel, updatesChannel, partiesChannel)
            )
        }
    }
}
