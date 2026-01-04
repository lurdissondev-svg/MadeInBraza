import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { useChannelsStore } from './channels'
import { useAnnouncementsStore } from './announcements'
import { useMembersStore } from './members'
import { useAuthStore } from './auth'

const STORAGE_KEY = 'braza_notifications_enabled'

export const useNotificationsStore = defineStore('notifications', () => {
  // State
  const soundEnabled = ref(localStorage.getItem(STORAGE_KEY) !== 'false')
  const lastTotalUnread = ref(0)
  const pollingInterval = ref<ReturnType<typeof setInterval> | null>(null)
  const notificationSound = ref<HTMLAudioElement | null>(null)
  const userInteracted = ref(false)

  // Other stores
  const channelsStore = useChannelsStore()
  const announcementsStore = useAnnouncementsStore()
  const membersStore = useMembersStore()
  const authStore = useAuthStore()

  // Computed: Total unread count across all features
  const totalUnreadCount = computed(() => {
    let total = 0

    // Chat unread count
    total += channelsStore.totalUnreadCount

    // Announcements unread count
    total += announcementsStore.unreadCount

    // Pending approvals (only for leaders)
    if (authStore.isLeader) {
      total += membersStore.pendingCount
    }

    return total
  })

  // Initialize audio
  function initAudio() {
    if (notificationSound.value) return

    try {
      // Use base URL for the sound file (handles /web/ prefix)
      const baseUrl = import.meta.env.BASE_URL || '/'
      notificationSound.value = new Audio(`${baseUrl}sounds/notification.mp3`)
      notificationSound.value.volume = 0.7
      notificationSound.value.preload = 'auto'
    } catch (e) {
      console.warn('Could not initialize notification sound:', e)
    }
  }

  // Play notification sound
  async function playSound() {
    if (!soundEnabled.value || !userInteracted.value) return

    try {
      if (!notificationSound.value) {
        initAudio()
      }

      if (notificationSound.value) {
        notificationSound.value.currentTime = 0
        await notificationSound.value.play()
      }
    } catch (e) {
      console.warn('Could not play notification sound:', e)
    }
  }

  // Update document title with unread count
  function updateDocumentTitle() {
    const baseTitle = 'Made in Braza'
    const count = totalUnreadCount.value

    if (count > 0) {
      document.title = `(${count > 99 ? '99+' : count}) ${baseTitle}`
    } else {
      document.title = baseTitle
    }
  }

  // Check for new content and trigger notifications
  async function checkForNewContent() {
    if (!authStore.user) return

    const previousTotal = lastTotalUnread.value

    // Refresh data from all stores (channels won't recalculate unread on every call)
    await Promise.all([
      channelsStore.fetchChannels(),
      announcementsStore.fetchAnnouncements(),
      authStore.isLeader ? membersStore.fetchPendingUsers() : Promise.resolve()
    ])

    const currentTotal = totalUnreadCount.value

    // Play sound if count increased
    if (currentTotal > previousTotal && previousTotal >= 0) {
      playSound()
    }

    lastTotalUnread.value = currentTotal
    updateDocumentTitle()
  }

  // Start polling for new content
  function startPolling(intervalMs = 30000) {
    if (pollingInterval.value) return

    // Initial check
    checkForNewContent()

    // Set up interval
    pollingInterval.value = setInterval(() => {
      checkForNewContent()
    }, intervalMs)
  }

  // Stop polling
  function stopPolling() {
    if (pollingInterval.value) {
      clearInterval(pollingInterval.value)
      pollingInterval.value = null
    }
  }

  // Toggle sound
  function toggleSound() {
    soundEnabled.value = !soundEnabled.value
    localStorage.setItem(STORAGE_KEY, String(soundEnabled.value))
  }

  // Mark user as having interacted (enables sound)
  function markUserInteracted() {
    userInteracted.value = true
    initAudio()
  }

  // Watch for changes in unread count and update title
  watch(totalUnreadCount, () => {
    updateDocumentTitle()
  })

  // Cleanup
  function cleanup() {
    stopPolling()
    if (notificationSound.value) {
      notificationSound.value = null
    }
  }

  return {
    // State
    soundEnabled,
    userInteracted,
    // Computed
    totalUnreadCount,
    // Actions
    playSound,
    updateDocumentTitle,
    checkForNewContent,
    startPolling,
    stopPolling,
    toggleSound,
    markUserInteracted,
    cleanup
  }
})
