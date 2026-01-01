import apiClient from './client'
import type { Channel, ChannelMessage, ChannelMember } from '@/types'

export interface SendMessageRequest {
  content: string
}

export interface ChannelsResponse {
  channels: Channel[]
}

export interface ChannelMembersResponse {
  members: ChannelMember[]
}

export const channelsApi = {
  async getChannels(): Promise<Channel[]> {
    const response = await apiClient.get<ChannelsResponse>('/channels')
    return response.data.channels
  },

  async setupDefaultChannels(): Promise<void> {
    await apiClient.post('/channels/setup')
  },

  async getChannelMessages(channelId: string, limit = 50, before?: string): Promise<ChannelMessage[]> {
    const params: Record<string, string | number> = { limit }
    if (before) params.before = before
    const response = await apiClient.get<ChannelMessage[]>(
      `/channels/${channelId}/messages`,
      { params }
    )
    return response.data
  },

  async sendMessage(channelId: string, content: string): Promise<ChannelMessage> {
    const response = await apiClient.post<ChannelMessage>(
      `/channels/${channelId}/messages`,
      { content }
    )
    return response.data
  },

  async sendMediaMessage(channelId: string, file: File, content?: string): Promise<ChannelMessage> {
    const formData = new FormData()
    formData.append('file', file)
    if (content) formData.append('content', content)

    const response = await apiClient.post<ChannelMessage>(
      `/channels/${channelId}/messages/media`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    )
    return response.data
  },

  async getChannelMembers(channelId: string): Promise<ChannelMember[]> {
    const response = await apiClient.get<ChannelMembersResponse>(
      `/channels/${channelId}/members`
    )
    return response.data.members
  },

  async markAsRead(channelId: string): Promise<void> {
    await apiClient.post(`/channels/${channelId}/read`)
  }
}
