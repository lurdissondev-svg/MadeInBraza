// Enums
export enum PlayerClass {
  ASSASSIN = 'ASSASSIN',
  BRAWLER = 'BRAWLER',
  ATALANTA = 'ATALANTA',
  PIKEMAN = 'PIKEMAN',
  FIGHTER = 'FIGHTER',
  MECHANIC = 'MECHANIC',
  KNIGHT = 'KNIGHT',
  PRIESTESS = 'PRIESTESS',
  SHAMAN = 'SHAMAN',
  MAGE = 'MAGE',
  ARCHER = 'ARCHER'
}

export enum UserStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  BANNED = 'BANNED',
  REJECTED = 'REJECTED'
}

export enum Role {
  LEADER = 'LEADER',
  COUNSELOR = 'COUNSELOR',
  MEMBER = 'MEMBER'
}

export enum SWResponseType {
  CONFIRMED = 'CONFIRMED',
  SHARED = 'SHARED',
  PILOT = 'PILOT',
  ABSENT = 'ABSENT'
}

export enum SWTag {
  ATTACK = 'ATTACK',
  DEFENSE = 'DEFENSE',
  ACADEMY = 'ACADEMY'
}

export enum ChannelType {
  GENERAL = 'GENERAL',
  LEADERS = 'LEADERS',
  EVENT = 'EVENT',
  PARTY = 'PARTY'
}

// Player class display names (original names without translation)
export const PlayerClassNames: Record<PlayerClass, string> = {
  [PlayerClass.ASSASSIN]: 'Assassin',
  [PlayerClass.BRAWLER]: 'Brawler',
  [PlayerClass.ATALANTA]: 'Atalanta',
  [PlayerClass.PIKEMAN]: 'Pikeman',
  [PlayerClass.FIGHTER]: 'Fighter',
  [PlayerClass.MECHANIC]: 'Mechanic',
  [PlayerClass.KNIGHT]: 'Knight',
  [PlayerClass.PRIESTESS]: 'Priestess',
  [PlayerClass.SHAMAN]: 'Shaman',
  [PlayerClass.MAGE]: 'Mage',
  [PlayerClass.ARCHER]: 'Archer'
}

export const PlayerClassAbbreviations: Record<PlayerClass, string> = {
  [PlayerClass.ASSASSIN]: 'ASS',
  [PlayerClass.BRAWLER]: 'BS',
  [PlayerClass.ATALANTA]: 'ATA',
  [PlayerClass.PIKEMAN]: 'PIKE',
  [PlayerClass.FIGHTER]: 'FIGHT',
  [PlayerClass.MECHANIC]: 'MECH',
  [PlayerClass.KNIGHT]: 'KNT',
  [PlayerClass.PRIESTESS]: 'PRS',
  [PlayerClass.SHAMAN]: 'SHA',
  [PlayerClass.MAGE]: 'MAGE',
  [PlayerClass.ARCHER]: 'ARC'
}

// User types
export interface User {
  id: string
  nick: string
  email: string | null
  playerClass: PlayerClass
  status: UserStatus
  role: Role
  avatarUrl: string | null
}

export interface AuthResponse {
  user: User
  token: string
}

export interface RegisterRequest {
  nick: string
  password: string
  playerClass: PlayerClass
  email?: string
}

export interface LoginRequest {
  nick: string
  password: string
}

export interface StatusResponse {
  user: User
}

export interface ApiError {
  error: string
}

// Pending users
export interface PendingUser {
  id: string
  nick: string
  playerClass: PlayerClass
  createdAt: string
}

export interface PendingUsersResponse {
  users: PendingUser[]
}

// Banned users
export interface BannedUser {
  id: string
  nick: string
  playerClass: PlayerClass
  createdAt: string
}

export interface BannedUsersResponse {
  users: BannedUser[]
}

// Members
export interface Member {
  id: string
  nick: string
  playerClass: PlayerClass
  role: Role
  avatarUrl: string | null
  createdAt: string
}

export interface MembersResponse {
  members: Member[]
}

export interface MemberProfile {
  id: string
  nick: string
  playerClass: PlayerClass
  role: Role
  status: UserStatus
  avatarUrl: string | null
  createdAt: string
  approvedAt: string | null
}

export interface MemberProfileResponse {
  user: MemberProfile
}

// Profile
export interface ProfileStats {
  messagesCount: number
  eventsParticipated: number
}

export interface Profile {
  id: string
  nick: string
  playerClass: PlayerClass
  status: UserStatus
  role: Role
  avatarUrl: string | null
  createdAt: string
  stats: ProfileStats
}

export interface ProfileResponse {
  profile: Profile
}

export interface UpdateProfileRequest {
  nick?: string
  playerClass?: PlayerClass
}

// Chat / Messages
export interface MessageUser {
  id: string
  nick: string
  playerClass: PlayerClass
  role: Role
  avatarUrl: string | null
}

export interface ChatMessage {
  id: string
  content: string
  createdAt: string
  user: MessageUser
}

export interface ChannelMessage {
  id: string
  content: string | null
  mediaUrl: string | null
  mediaType: string | null
  fileName: string | null
  fileSize: number | null
  createdAt: string
  editedAt: string | null
  user: MessageUser
}

// Events
export interface EventCreator {
  id: string
  nick: string
}

export interface EventParticipant {
  id: string
  nick: string
  playerClass: PlayerClass
}

export interface Event {
  id: string
  title: string
  description: string | null
  eventDate: string
  maxParticipants: number | null
  requiredClasses: PlayerClass[]
  createdAt: string
  createdBy: EventCreator
  participants: EventParticipant[]
}

export interface EventsResponse {
  events: Event[]
}

export interface CreateEventRequest {
  title: string
  description?: string | null
  eventDate: string
  maxParticipants?: number | null
  requiredClasses?: PlayerClass[]
}

// Parties
export interface PartyCreator {
  id: string
  nick: string
}

export interface PartyMember {
  id: string
  nick: string
  playerClass: PlayerClass | null
  joinedAt: string
}

export interface PartySlotUser {
  id: string
  nick: string
  playerClass: PlayerClass | null
}

export interface PartySlot {
  id: string
  playerClass: PlayerClass | null // null = "FREE" slot (any class can join)
  filledAsClass: PlayerClass | null // Class chosen when filling a FREE slot
  filledBy: PartySlotUser | null
}

export interface Party {
  id: string
  name: string
  description: string | null
  isClosed: boolean
  createdAt: string
  createdBy: PartyCreator
  slots: PartySlot[]
  members: PartyMember[] // Backwards compatibility
}

export interface PartiesResponse {
  parties: Party[]
}

export interface SlotRequest {
  playerClass: PlayerClass | 'FREE' // 'FREE' = any class can join
  count: number
}

export interface CreatePartyRequest {
  name: string
  description?: string | null
  slots: SlotRequest[]
  creatorSlotClass?: PlayerClass | 'FREE' | null // Optional for LEADER/COUNSELOR
}

export interface JoinPartyRequest {
  slotId: string
  selectedClass?: string // Required for FREE slots
}

export interface UpdatePartyRequest {
  name: string
  description?: string | null
}

// Siege War
export interface SiegeWar {
  id: string
  weekStart: string
  weekEnd: string
  isActive: boolean
}

export interface PilotingForUser {
  id: string
  nick: string
  playerClass: PlayerClass
}

export interface SWUserResponse {
  id: string
  responseType: SWResponseType
  tag: SWTag | null
  gameId: string | null
  sharedClass: PlayerClass | null
  pilotingFor: PilotingForUser | null
  preferredClass: PlayerClass | null
}

export interface CurrentSiegeWarResponse {
  siegeWar: SiegeWar | null
  userResponse: SWUserResponse | null
}

export interface SubmitSWResponseRequest {
  responseType: SWResponseType
  tag?: SWTag | null
  gameId?: string | null
  password?: string | null
  sharedClass?: PlayerClass | null
  pilotingForId?: string | null
  preferredClass?: PlayerClass | null
}

export interface AvailableShare {
  userId: string
  nick: string
  sharedClass: PlayerClass | null
}

export interface AvailableSharesResponse {
  availableShares: AvailableShare[]
}

export interface SWResponseUser {
  id: string
  nick: string
  playerClass: PlayerClass
}

export interface SWResponseItem {
  id: string
  user: SWResponseUser
  responseType: SWResponseType
  tag: SWTag | null
  gameId: string | null
  password: string | null
  sharedClass: PlayerClass | null
  pilotingFor: PilotingForUser | null
  preferredClass: PlayerClass | null
  createdAt: string
}

export interface SWResponsesSummary {
  total: number
  responded: number
  confirmed: number
  shared: number
  pilots: number
  absent: number
}

export interface SWResponsesResponse {
  responses: SWResponseItem[]
  notResponded: SWResponseUser[]
  availableShares: AvailableShare[]
  summary: SWResponsesSummary
}

export interface SWHistoryResponseItem {
  id: string
  user: SWResponseUser
  responseType: SWResponseType
  tag: SWTag | null
  sharedClass: PlayerClass | null
  pilotingFor: PilotingForUser | null
  preferredClass: PlayerClass | null
  createdAt: string
}

export interface SiegeWarHistoryItem {
  id: string
  weekStart: string
  weekEnd: string
  isActive: boolean
  responses: SWHistoryResponseItem[]
  summary: SWResponsesSummary
}

export interface SiegeWarHistoryResponse {
  siegeWars: SiegeWarHistoryItem[]
}

// Channels
export interface ChannelEvent {
  id: string
  title: string
}

export interface ChannelParty {
  id: string
  name: string
}

export interface Channel {
  id: string
  type: ChannelType
  name: string
  eventId: string | null
  event: ChannelEvent | null
  partyId: string | null
  party: ChannelParty | null
  createdAt: string
  _count?: { messages: number }
}

export interface ChannelMember {
  id: string
  nick: string
  playerClass: string
  role: string
}

export interface ChannelMembersResponse {
  members: ChannelMember[]
  count: number
}

// Announcements
export interface AnnouncementCreator {
  id: string
  nick: string
}

export interface Announcement {
  id: string
  title: string
  content: string
  createdAt: string
  updatedAt: string
  createdBy: AnnouncementCreator | null
  whatsappMessageId: string | null
  whatsappAuthor: string | null
  whatsappTimestamp: string | null
  mediaUrl: string | null
  mediaType: string | null
}

export interface AnnouncementsResponse {
  announcements: Announcement[]
}

export interface CreateAnnouncementRequest {
  title: string
  content: string
}

// Password
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface ForgotPasswordRequest {
  nick: string
}

export interface ForgotPasswordResponse {
  message: string
  newPassword?: string
}

export interface RequestResetRequest {
  nick: string
}

export interface RequestResetResponse {
  message: string
}

export interface VerifyResetTokenRequest {
  token: string
}

export interface VerifyResetTokenResponse {
  valid: boolean
  nick: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

export interface ResetPasswordResponse {
  message: string
}

export interface UpdateEmailRequest {
  email: string
}

export interface UpdateEmailResponse {
  success: boolean
  email: string
}

// Generic responses
export interface SuccessResponse {
  success: boolean
}
