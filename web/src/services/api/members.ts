import apiClient from './client'
import type { Member, MemberProfile, PendingUser, BannedUser } from '@/types'

interface MembersResponse {
  members: Member[]
}

interface MemberProfileResponse {
  user: MemberProfile
}

interface PendingUsersResponse {
  users: PendingUser[]
}

interface BannedUsersResponse {
  users: BannedUser[]
}

export const membersApi = {
  // Get all approved members
  async getMembers(): Promise<Member[]> {
    const response = await apiClient.get<MembersResponse>('/users/members')
    return response.data.members
  },

  // Get member profile by ID
  async getMemberProfile(memberId: string): Promise<MemberProfile> {
    const response = await apiClient.get<MemberProfileResponse>(`/users/${memberId}/profile`)
    return response.data.user
  },

  // Get pending users (awaiting approval)
  async getPendingUsers(): Promise<PendingUser[]> {
    const response = await apiClient.get<PendingUsersResponse>('/users/pending')
    return response.data.users
  },

  // Approve pending user
  async approveUser(userId: string): Promise<void> {
    await apiClient.post(`/users/${userId}/approve`)
  },

  // Reject pending user
  async rejectUser(userId: string): Promise<void> {
    await apiClient.post(`/users/${userId}/reject`)
  },

  // Get banned users
  async getBannedUsers(): Promise<BannedUser[]> {
    const response = await apiClient.get<BannedUsersResponse>('/users/banned')
    return response.data.users
  },

  // Ban a member
  async banMember(memberId: string): Promise<void> {
    await apiClient.post(`/users/${memberId}/ban`)
  },

  // Unban a user
  async unbanUser(userId: string): Promise<void> {
    await apiClient.post(`/users/${userId}/unban`)
  },

  // Promote member to leader
  async promoteMember(memberId: string): Promise<void> {
    await apiClient.post(`/users/${memberId}/promote`)
  },

  // Demote leader to member
  async demoteMember(memberId: string): Promise<void> {
    await apiClient.post(`/users/${memberId}/demote`)
  }
}
