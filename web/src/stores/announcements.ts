import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Announcement, CreateAnnouncementRequest } from '@/types'
import { announcementsApi } from '@/services/api/announcements'

export const useAnnouncementsStore = defineStore('announcements', () => {
  // State
  const announcements = ref<Announcement[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Actions
  async function fetchAnnouncements(): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      announcements.value = await announcementsApi.getAnnouncements()
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar avisos'
      return false
    } finally {
      loading.value = false
    }
  }

  async function createAnnouncement(data: CreateAnnouncementRequest): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const announcement = await announcementsApi.createAnnouncement(data)
      announcements.value.unshift(announcement)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao criar aviso'
      return false
    } finally {
      loading.value = false
    }
  }

  async function deleteAnnouncement(id: string): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      await announcementsApi.deleteAnnouncement(id)
      announcements.value = announcements.value.filter(a => a.id !== id)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao deletar aviso'
      return false
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    announcements,
    loading,
    error,
    // Actions
    fetchAnnouncements,
    createAnnouncement,
    deleteAnnouncement,
    clearError
  }
})
