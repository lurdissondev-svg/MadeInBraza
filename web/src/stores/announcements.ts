import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Announcement, CreateAnnouncementRequest } from '@/types'
import { announcementsApi } from '@/services/api/announcements'

const LAST_READ_KEY = 'braza_announcements_last_read'

export const useAnnouncementsStore = defineStore('announcements', () => {
  // State
  const announcements = ref<Announcement[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const lastReadAt = ref<string | null>(localStorage.getItem(LAST_READ_KEY))

  // Computed: Count of unread announcements
  const unreadCount = computed(() => {
    if (!lastReadAt.value) {
      // First time user - all announcements are "new"
      return announcements.value.length
    }

    const lastReadTime = new Date(lastReadAt.value).getTime()

    return announcements.value.filter(a => {
      const announcementTime = new Date(a.createdAt).getTime()
      return announcementTime > lastReadTime
    }).length
  })

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

  // Mark all announcements as read
  function markAsRead() {
    lastReadAt.value = new Date().toISOString()
    localStorage.setItem(LAST_READ_KEY, lastReadAt.value)
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    announcements,
    loading,
    error,
    lastReadAt,
    // Computed
    unreadCount,
    // Actions
    fetchAnnouncements,
    createAnnouncement,
    deleteAnnouncement,
    markAsRead,
    clearError
  }
})
