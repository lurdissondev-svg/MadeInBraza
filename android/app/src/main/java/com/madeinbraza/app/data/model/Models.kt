package com.madeinbraza.app.data.model

enum class PlayerClass {
    ASSASSIN, BRAWLER, ATALANTA, PIKEMAN, FIGHTER,
    MECHANIC, KNIGHT, PRIESTESS, SHAMAN, MAGE, ARCHER
}

enum class UserStatus {
    PENDING, APPROVED, BANNED
}

enum class Role {
    LEADER, MEMBER
}

data class User(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass,
    val status: UserStatus,
    val role: Role,
    val avatarUrl: String? = null
)

data class AuthResponse(
    val user: User,
    val token: String
)

data class RegisterRequest(
    val nick: String,
    val password: String,
    val playerClass: PlayerClass
)

data class LoginRequest(
    val nick: String,
    val password: String
)

data class FcmTokenRequest(
    val fcmToken: String
)

data class StatusResponse(
    val user: User
)

data class ApiError(
    val error: String
)

data class PendingUser(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass,
    val createdAt: String
)

data class PendingUsersResponse(
    val users: List<PendingUser>
)

data class ApproveUserResponse(
    val user: User
)

// Chat models
data class MessageUser(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass,
    val role: Role,
    val avatarUrl: String? = null
)

data class ChatMessage(
    val id: String,
    val content: String,
    val createdAt: String,
    val user: MessageUser
)

data class MessagesResponse(
    val messages: List<ChatMessage>
)

data class SendMessageRequest(
    val content: String
)

data class SendMessageResponse(
    val message: ChatMessage
)

// Event models
data class EventCreator(
    val id: String,
    val nick: String
)

data class EventParticipant(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass
)

data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val eventDate: String,
    val maxParticipants: Int?,
    val requiredClasses: List<PlayerClass>,
    val createdAt: String,
    val createdBy: EventCreator,
    val participants: List<EventParticipant>
) {
    val isFull: Boolean
        get() = maxParticipants != null && participants.size >= maxParticipants

    val availableSlots: Int?
        get() = maxParticipants?.let { it - participants.size }
}

data class EventsResponse(
    val events: List<Event>
)

data class CreateEventRequest(
    val title: String,
    val description: String?,
    val eventDate: String,
    val maxParticipants: Int?,
    val requiredClasses: List<PlayerClass>?
)

data class CreateEventResponse(
    val event: Event
)

data class SuccessResponse(
    val success: Boolean
)

// Member models
data class Member(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass,
    val role: Role,
    val createdAt: String,
    val avatarUrl: String? = null
)

data class MembersResponse(
    val members: List<Member>
)

// Profile models
data class ProfileStats(
    val messagesCount: Int,
    val eventsParticipated: Int
)

data class Profile(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass,
    val status: UserStatus,
    val role: Role,
    val createdAt: String,
    val stats: ProfileStats,
    val avatarUrl: String? = null
)

data class ProfileResponse(
    val profile: Profile
)

data class UpdateProfileRequest(
    val nick: String? = null,
    val playerClass: PlayerClass? = null
)

data class UpdateProfileResponse(
    val user: User
)

data class AvatarUploadResponse(
    val avatarUrl: String,
    val user: User
)

data class AvatarDeleteResponse(
    val message: String,
    val user: User
)

// Banned users models
data class BannedUser(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass,
    val createdAt: String
)

data class BannedUsersResponse(
    val users: List<BannedUser>
)

// Member profile models
data class MemberProfile(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass,
    val role: Role,
    val status: UserStatus,
    val createdAt: String,
    val approvedAt: String?,
    val avatarUrl: String? = null
)

data class MemberProfileResponse(
    val user: MemberProfile
)

// Party models
data class PartyCreator(
    val id: String,
    val nick: String
)

data class PartyMember(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass?,
    val joinedAt: String
)

data class PartySlotUser(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass?
)

data class PartySlot(
    val id: String,
    val playerClass: PlayerClass,
    val filledBy: PartySlotUser?
)

data class Party(
    val id: String,
    val name: String,
    val description: String? = null,
    val isClosed: Boolean,
    val createdAt: String,
    val createdBy: PartyCreator,
    val slots: List<PartySlot>,
    val members: List<PartyMember> // Backwards compatibility
) {
    val totalSlots: Int
        get() = slots.size

    val filledSlots: Int
        get() = slots.count { it.filledBy != null }

    val isFull: Boolean
        get() = filledSlots >= totalSlots

    val availableSlots: Int
        get() = totalSlots - filledSlots
}

data class PartiesResponse(
    val parties: List<Party>
)

data class SlotRequest(
    val playerClass: PlayerClass,
    val count: Int
)

data class CreatePartyRequest(
    val name: String,
    val description: String? = null,
    val slots: List<SlotRequest>
)

data class CreatePartyResponse(
    val party: Party
)

data class JoinPartyRequest(
    val slotId: String
)

data class JoinPartyResponse(
    val party: Party
)

// Siege War models
enum class SWResponseType {
    CONFIRMED, SHARED, PILOT, ABSENT
}

enum class SWTag {
    ATTACK, DEFENSE, ACADEMY
}

data class SiegeWar(
    val id: String,
    val weekStart: String,
    val weekEnd: String,
    val isActive: Boolean
)

data class PilotingForUser(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass
)

data class SWUserResponse(
    val id: String,
    val responseType: SWResponseType,
    val tag: SWTag?,
    val gameId: String?,
    val sharedClass: PlayerClass?,
    val pilotingFor: PilotingForUser?,
    val preferredClass: PlayerClass?
)

data class CurrentSiegeWarResponse(
    val siegeWar: SiegeWar?,
    val userResponse: SWUserResponse?
)

data class SubmitSWResponseRequest(
    val responseType: SWResponseType,
    val tag: SWTag? = null,
    val gameId: String? = null,
    val password: String? = null,
    val sharedClass: PlayerClass? = null,
    val pilotingForId: String? = null,
    val preferredClass: PlayerClass? = null
)

data class SubmitSWResponseResponse(
    val response: SWUserResponse
)

data class AvailableShare(
    val userId: String,
    val nick: String,
    val sharedClass: PlayerClass?
)

data class AvailableSharesResponse(
    val availableShares: List<AvailableShare>
)

data class SWResponseUser(
    val id: String,
    val nick: String,
    val playerClass: PlayerClass
)

data class SWResponseItem(
    val id: String,
    val user: SWResponseUser,
    val responseType: SWResponseType,
    val tag: SWTag?,
    val gameId: String?,
    val password: String?,
    val sharedClass: PlayerClass?,
    val pilotingFor: PilotingForUser?,
    val preferredClass: PlayerClass?,
    val createdAt: String
)

data class SWResponsesSummary(
    val total: Int,
    val responded: Int,
    val confirmed: Int,
    val shared: Int,
    val pilots: Int,
    val absent: Int
)

data class SWResponsesResponse(
    val responses: List<SWResponseItem>,
    val notResponded: List<SWResponseUser>,
    val availableShares: List<AvailableShare>,
    val summary: SWResponsesSummary
)

data class CreateSiegeWarResponse(
    val siegeWar: SiegeWar
)

// Siege War History models
data class SWHistoryResponseItem(
    val id: String,
    val user: SWResponseUser,
    val responseType: SWResponseType,
    val tag: SWTag?,
    val sharedClass: PlayerClass?,
    val pilotingFor: PilotingForUser?,
    val preferredClass: PlayerClass?,
    val createdAt: String
)

data class SiegeWarHistoryItem(
    val id: String,
    val weekStart: String,
    val weekEnd: String,
    val isActive: Boolean,
    val responses: List<SWHistoryResponseItem>,
    val summary: SWResponsesSummary
)

data class SiegeWarHistoryResponse(
    val siegeWars: List<SiegeWarHistoryItem>
)

// Channel models
enum class ChannelType {
    GENERAL, LEADERS, EVENT, PARTY
}

data class ChannelEvent(
    val id: String,
    val title: String
)

data class ChannelParty(
    val id: String,
    val name: String
)

data class ChannelMessageCount(
    val messages: Int
)

data class Channel(
    val id: String,
    val type: ChannelType,
    val name: String,
    val eventId: String? = null,
    val event: ChannelEvent? = null,
    val partyId: String? = null,
    val party: ChannelParty? = null,
    val createdAt: String,
    val _count: ChannelMessageCount? = null
)

data class ChannelMessage(
    val id: String,
    val content: String?,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val fileSize: Int? = null,
    val createdAt: String,
    val user: MessageUser
)

data class SendChannelMessageRequest(
    val content: String
)

data class ChannelMember(
    val id: String,
    val nick: String,
    val playerClass: String,
    val role: String
)

data class ChannelMembersResponse(
    val members: List<ChannelMember>,
    val count: Int
)

// Announcement models
data class AnnouncementCreator(
    val id: String,
    val nick: String
)

data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: AnnouncementCreator? = null,  // Nullable para mensagens do WhatsApp
    val whatsappMessageId: String? = null,
    val whatsappAuthor: String? = null,
    val whatsappTimestamp: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null
) {
    // Retorna o autor (seja do app ou do WhatsApp)
    val authorName: String
        get() {
            createdBy?.nick?.let { return it }
            return whatsappAuthor ?: "Desconhecido"
        }

    // Indica se é uma mensagem do WhatsApp
    val isFromWhatsApp: Boolean
        get() = whatsappMessageId != null
}

data class AnnouncementsResponse(
    val announcements: List<Announcement>
)

data class CreateAnnouncementRequest(
    val title: String,
    val content: String
)

data class CreateAnnouncementResponse(
    val announcement: Announcement
)

// Change password models
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

// Forgot password models
data class ForgotPasswordRequest(
    val nick: String
)

data class ForgotPasswordResponse(
    val message: String,
    val newPassword: String? = null
)
