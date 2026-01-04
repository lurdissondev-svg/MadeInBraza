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
  const lastTotalUnread = ref(-1) // -1 means first check hasn't happened yet
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
    console.log('[Notifications] playSound called', {
      soundEnabled: soundEnabled.value,
      userInteracted: userInteracted.value,
      hasAudio: !!notificationSound.value
    })

    if (!soundEnabled.value) {
      console.log('[Notifications] Sound disabled by user')
      return
    }

    if (!userInteracted.value) {
      console.log('[Notifications] User has not interacted yet - sound blocked by browser')
      return
    }

    try {
      if (!notificationSound.value) {
        initAudio()
      }

      if (notificationSound.value) {
        notificationSound.value.currentTime = 0
        console.log('[Notifications] Playing sound...')
        await notificationSound.value.play()
        console.log('[Notifications] Sound played successfully')
      }
    } catch (e) {
      console.warn('[Notifications] Could not play notification sound:', e)
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
    if (!authStore.user) {
      console.log('[Notifications] No user, skipping check')
      return
    }

    const previousTotal = lastTotalUnread.value
    console.log('[Notifications] Checking for new content, previousTotal:', previousTotal)

    // Refresh data from all stores (force recalculate unread counts)
    await Promise.all([
      channelsStore.fetchChannels(true), // Force recalculate unread counts
      announcementsStore.fetchAnnouncements(),
      authStore.isLeader ? membersStore.fetchPendingUsers() : Promise.resolve()
    ])

    const currentTotal = totalUnreadCount.value
    console.log('[Notifications] After fetch - currentTotal:', currentTotal, {
      channels: channelsStore.totalUnreadCount,
      announcements: announcementsStore.unreadCount,
      pending: authStore.isLeader ? membersStore.pendingCount : 0
    })

    // Play sound if count increased (skip first check to avoid sound on page load)
    // -1 means first check, don't play sound
    // >= 0 means subsequent checks, play sound if increased
    if (previousTotal >= 0 && currentTotal > previousTotal) {
      console.log('[Notifications] New content detected! Playing sound...')
      playSound()
    } else if (previousTotal === -1) {
      console.log('[Notifications] First check - initializing count, not playing sound')
    }

    lastTotalUnread.value = currentTotal
    updateDocumentTitle()
  }

  // Start polling for new content
  function startPolling(intervalMs = 30000) {
    if (pollingInterval.value) {
      console.log('[Notifications] Polling already running')
      return
    }

    console.log('[Notifications] Starting polling every', intervalMs, 'ms')

    // Initial check
    checkForNewContent()

    // Set up interval
    pollingInterval.value = setInterval(() => {
      console.log('[Notifications] Polling tick...')
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
    console.log('[Notifications] User interacted - enabling sound')
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

  // Debug helper - exposed to window for troubleshooting
  function getDebugState() {
    return {
      soundEnabled: soundEnabled.value,
      userInteracted: userInteracted.value,
      hasAudio: !!notificationSound.value,
      lastTotalUnread: lastTotalUnread.value,
      currentTotal: totalUnreadCount.value,
      channels: channelsStore.totalUnreadCount,
      announcements: announcementsStore.unreadCount,
      pending: authStore.isLeader ? membersStore.pendingCount : 0,
      pollingActive: !!pollingInterval.value
    }
  }

  // Test sound manually
  async function testSound() {
    console.log('[Notifications] Manual test - forcing sound play')
    // Temporarily bypass checks
    if (!notificationSound.value) {
      initAudio()
    }
    if (notificationSound.value) {
      try {
        notificationSound.value.currentTime = 0
        await notificationSound.value.play()
        console.log('[Notifications] Test sound played!')
        return true
      } catch (e) {
        console.error('[Notifications] Test sound failed:', e)
        return false
      }
    }
    return false
  }

  // Expose debug helpers to window
  if (typeof window !== 'undefined') {
    (window as any).brazaNotifications = {
      getState: getDebugState,
      testSound,
      forceCheck: checkForNewContent
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
