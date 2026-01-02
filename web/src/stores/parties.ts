import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Party, CreatePartyRequest, UpdatePartyRequest } from '@/types'
import { partiesApi } from '@/services/api/parties'

export const usePartiesStore = defineStore('parties', () => {
  // State
  const globalParties = ref<Party[]>([])
  const eventParties = ref<Map<string, Party[]>>(new Map())
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Actions
  async function fetchGlobalParties(): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      globalParties.value = await partiesApi.getGlobalParties()
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar parties'
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchEventParties(eventId: string): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const parties = await partiesApi.getPartiesByEvent(eventId)
      eventParties.value.set(eventId, parties)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar parties do evento'
      return false
    } finally {
      loading.value = false
    }
  }

  async function createGlobalParty(data: CreatePartyRequest): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const party = await partiesApi.createGlobalParty(data)
      globalParties.value.unshift(party)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao criar party'
      return false
    } finally {
      loading.value = false
    }
  }

  async function createEventParty(eventId: string, data: CreatePartyRequest): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const party = await partiesApi.createEventParty(eventId, data)
      const parties = eventParties.value.get(eventId) || []
      parties.unshift(party)
      eventParties.value.set(eventId, parties)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao criar party'
      return false
    } finally {
      loading.value = false
    }
  }

  async function updateParty(id: string, data: UpdatePartyRequest, eventId?: string): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const updatedParty = await partiesApi.updateParty(id, data)

      // Update in global parties
      const globalIndex = globalParties.value.findIndex(p => p.id === id)
      if (globalIndex !== -1) {
        globalParties.value[globalIndex] = updatedParty
      }

      // Update in event parties if applicable
      if (eventId) {
        const parties = eventParties.value.get(eventId) || []
        const eventIndex = parties.findIndex(p => p.id === id)
        if (eventIndex !== -1) {
          parties[eventIndex] = updatedParty
          eventParties.value.set(eventId, [...parties])
        }
      }

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao atualizar party'
      return false
    } finally {
      loading.value = false
    }
  }

  async function deleteParty(id: string, eventId?: string): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      await partiesApi.deleteParty(id)

      // Remove from global parties
      globalParties.value = globalParties.value.filter(p => p.id !== id)

      // Remove from event parties if applicable
      if (eventId) {
        const parties = eventParties.value.get(eventId) || []
        eventParties.value.set(eventId, parties.filter(p => p.id !== id))
      }

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao deletar party'
      return false
    } finally {
      loading.value = false
    }
  }

  async function joinParty(id: string, slotId: string, selectedClass?: string, eventId?: string): Promise<boolean> {
    error.value = null

    try {
      const updatedParty = await partiesApi.joinParty(id, { slotId, selectedClass })

      // Update in global parties
      const globalIndex = globalParties.value.findIndex(p => p.id === id)
      if (globalIndex !== -1) {
        globalParties.value[globalIndex] = updatedParty
      }

      // Update in event parties if applicable
      if (eventId) {
        const parties = eventParties.value.get(eventId) || []
        const eventIndex = parties.findIndex(p => p.id === id)
        if (eventIndex !== -1) {
          parties[eventIndex] = updatedParty
          eventParties.value.set(eventId, [...parties])
        }
      }

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao entrar na party'
      return false
    }
  }

  async function leaveParty(id: string, eventId?: string): Promise<boolean> {
    error.value = null

    try {
      const updatedParty = await partiesApi.leaveParty(id)

      // Update in global parties
      const globalIndex = globalParties.value.findIndex(p => p.id === id)
      if (globalIndex !== -1) {
        globalParties.value[globalIndex] = updatedParty
      }

      // Update in event parties if applicable
      if (eventId) {
        const parties = eventParties.value.get(eventId) || []
        const eventIndex = parties.findIndex(p => p.id === id)
        if (eventIndex !== -1) {
          parties[eventIndex] = updatedParty
          eventParties.value.set(eventId, [...parties])
        }
      }

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao sair da party'
      return false
    }
  }

  function getEventParties(eventId: string): Party[] {
    return eventParties.value.get(eventId) || []
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    globalParties,
    eventParties,
    loading,
    error,
    // Actions
    fetchGlobalParties,
    fetchEventParties,
    createGlobalParty,
    createEventParty,
    updateParty,
    deleteParty,
    joinParty,
    leaveParty,
    getEventParties,
    clearError
  }
})
