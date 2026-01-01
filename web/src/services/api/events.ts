import apiClient from './client'
import type {
  EventsResponse,
  Event,
  CreateEventRequest
} from '@/types'

export const eventsApi = {
  async getEvents(): Promise<Event[]> {
    const response = await apiClient.get<EventsResponse>('/events')
    return response.data.events
  },

  async createEvent(data: CreateEventRequest): Promise<Event> {
    const response = await apiClient.post<{ event: Event }>('/events', data)
    return response.data.event
  },

  async deleteEvent(id: string): Promise<void> {
    await apiClient.delete(`/events/${id}`)
  },

  async joinEvent(id: string): Promise<Event> {
    const response = await apiClient.post<{ event: Event }>(`/events/${id}/join`)
    return response.data.event
  },

  async leaveEvent(id: string): Promise<Event> {
    const response = await apiClient.post<{ event: Event }>(`/events/${id}/leave`)
    return response.data.event
  }
}
