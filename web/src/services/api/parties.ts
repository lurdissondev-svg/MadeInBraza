import apiClient from './client'
import type {
  PartiesResponse,
  Party,
  CreatePartyRequest,
  JoinPartyRequest
} from '@/types'

export const partiesApi = {
  async getGlobalParties(): Promise<Party[]> {
    const response = await apiClient.get<PartiesResponse>('/parties')
    return response.data.parties
  },

  async getPartiesByEvent(eventId: string): Promise<Party[]> {
    const response = await apiClient.get<PartiesResponse>(`/parties/event/${eventId}`)
    return response.data.parties
  },

  async createGlobalParty(data: CreatePartyRequest): Promise<Party> {
    const response = await apiClient.post<{ party: Party }>('/parties', data)
    return response.data.party
  },

  async createEventParty(eventId: string, data: CreatePartyRequest): Promise<Party> {
    const response = await apiClient.post<{ party: Party }>(`/parties/event/${eventId}`, data)
    return response.data.party
  },

  async deleteParty(id: string): Promise<void> {
    await apiClient.delete(`/parties/${id}`)
  },

  async joinParty(id: string, data: JoinPartyRequest): Promise<Party> {
    const response = await apiClient.post<{ party: Party }>(`/parties/${id}/join`, data)
    return response.data.party
  },

  async leaveParty(id: string): Promise<Party> {
    const response = await apiClient.post<{ party: Party }>(`/parties/${id}/leave`)
    return response.data.party
  }
}
