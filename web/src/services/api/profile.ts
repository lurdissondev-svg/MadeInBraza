import apiClient from './client'
import type { Profile, ProfileResponse, UpdateProfileRequest } from '@/types'

interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export const profileApi = {
  // Get current user's full profile with stats
  async getProfile(): Promise<Profile> {
    const response = await apiClient.get<ProfileResponse>('/profile')
    return response.data.profile
  },

  // Update profile (nick, playerClass)
  async updateProfile(data: UpdateProfileRequest): Promise<Profile> {
    const response = await apiClient.put<ProfileResponse>('/profile', data)
    return response.data.profile
  },

  // Change password
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    const request: ChangePasswordRequest = { currentPassword, newPassword }
    await apiClient.put('/auth/change-password', request)
  }
}
