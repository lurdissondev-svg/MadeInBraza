import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Event, CreateEventRequest } from '@/types'
import { eventsApi } from '@/services/api/events'

export const useEventsStore = defineStore('events', () => {
  // State
  const events = ref<Event[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Actions
  async function fetchEvents(): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      events.value = await eventsApi.getEvents()
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar eventos'
      return false
    } finally {
      loading.value = false
    }
  }

  async function createEvent(data: CreateEventRequest): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const event = await eventsApi.createEvent(data)
      events.value.unshift(event)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao criar evento'
      return false
    } finally {
      loading.value = false
    }
  }

  async function deleteEvent(id: string): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      await eventsApi.deleteEvent(id)
      events.value = events.value.filter(e => e.id !== id)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao deletar evento'
      return false
    } finally {
      loading.value = false
    }
  }

  async function joinEvent(id: string): Promise<boolean> {
    error.value = null

    try {
      const updatedEvent = await eventsApi.joinEvent(id)
      const index = events.value.findIndex(e => e.id === id)
      if (index !== -1) {
        events.value[index] = updatedEvent
      }
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao entrar no evento'
      return false
    }
  }

  async function leaveEvent(id: string): Promise<boolean> {
    error.value = null

    try {
      const updatedEvent = await eventsApi.leaveEvent(id)
      const index = events.value.findIndex(e => e.id === id)
      if (index !== -1) {
        events.value[index] = updatedEvent
      }
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao sair do evento'
      return false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    events,
    loading,
    error,
    // Actions
    fetchEvents,
    createEvent,
    deleteEvent,
    joinEvent,
    leaveEvent,
    clearError
  }
})
