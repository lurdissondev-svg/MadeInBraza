import apiClient from './client'
import type {
  CurrentSiegeWarResponse,
  SiegeWar,
  SWUserResponse,
  SubmitSWResponseRequest,
  AvailableShare,
  SWResponsesResponse,
  SiegeWarHistoryItem
} from '@/types'

export const siegeWarApi = {
  async getCurrentSiegeWar(): Promise<CurrentSiegeWarResponse> {
    const response = await apiClient.get<CurrentSiegeWarResponse>('/siege-war/current')
    return response.data
  },

  async createSiegeWar(): Promise<SiegeWar> {
    const response = await apiClient.post<{ siegeWar: SiegeWar }>('/siege-war')
    return response.data.siegeWar
  },

  async submitResponse(siegeWarId: string, data: SubmitSWResponseRequest): Promise<SWUserResponse> {
    const response = await apiClient.post<{ response: SWUserResponse }>(
      `/siege-war/${siegeWarId}/respond`,
      data
    )
    return response.data.response
  },

  async getResponses(siegeWarId: string): Promise<SWResponsesResponse> {
    const response = await apiClient.get<SWResponsesResponse>(
      `/siege-war/${siegeWarId}/responses`
    )
    return response.data
  },

  async getAvailableShares(siegeWarId: string): Promise<AvailableShare[]> {
    const response = await apiClient.get<{ availableShares: AvailableShare[] }>(
      `/siege-war/${siegeWarId}/available-shares`
    )
    return response.data.availableShares
  },

  async closeSiegeWar(siegeWarId: string): Promise<SiegeWar> {
    const response = await apiClient.post<{ siegeWar: SiegeWar }>(
      `/siege-war/${siegeWarId}/close`
    )
    return response.data.siegeWar
  },

  async getHistory(): Promise<SiegeWarHistoryItem[]> {
    const response = await apiClient.get<{ siegeWars: SiegeWarHistoryItem[] }>(
      '/siege-war/history'
    )
    return response.data.siegeWars
  }
}
