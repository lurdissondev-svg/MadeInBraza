import apiClient from './client'
import type {
  AnnouncementsResponse,
  Announcement,
  CreateAnnouncementRequest
} from '@/types'

export const announcementsApi = {
  async getAnnouncements(): Promise<Announcement[]> {
    const response = await apiClient.get<AnnouncementsResponse>('/announcements')
    return response.data.announcements
  },

  async createAnnouncement(data: CreateAnnouncementRequest): Promise<Announcement> {
    const response = await apiClient.post<{ announcement: Announcement }>('/announcements', data)
    return response.data.announcement
  },

  async deleteAnnouncement(id: string): Promise<void> {
    await apiClient.delete(`/announcements/${id}`)
  }
}
