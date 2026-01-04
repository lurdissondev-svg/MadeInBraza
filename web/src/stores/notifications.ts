import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { useChannelsStore } from './channels'
import { useAnnouncementsStore } from './announcements'
import { useMembersStore } from './members'
import { useAuthStore } from './auth'
import { usePartiesStore } from './parties'

const STORAGE_KEY = 'braza_notifications_enabled'
const LAST_PARTY_CHECK_KEY = 'braza_last_party_check'

export const useNotificationsStore = defineStore('notifications', () => {
  // State
  const soundEnabled = ref(localStorage.getItem(STORAGE_KEY) !== 'false')
  const lastTotalUnread = ref(-1) // -1 means first check hasn't happened yet
  const pollingInterval = ref<ReturnType<typeof setInterval> | null>(null)
  const notificationSound = ref<HTMLAudioElement | null>(null)
  const userInteracted = ref(false)

  // Track new parties count
  const newPartiesCount = ref(0)
  const lastKnownPartyIds = ref<Set<string>>(new Set())
  const currentUserId = ref<string | null>(null) // Track current user to detect account switches

  // Other stores
  const channelsStore = useChannelsStore()
  const announcementsStore = useAnnouncementsStore()
  const membersStore = useMembersStore()
  const authStore = useAuthStore()
  const partiesStore = usePartiesStore()

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

    // New parties count
    total += newPartiesCount.value

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

  // Check for new parties
  async function checkForNewParties(): Promise<number> {
    try {
      await partiesStore.fetchGlobalParties()
      const currentParties = partiesStore.globalParties
      const currentPartyIds = new Set(currentParties.map(p => p.id))

      // First check - just initialize the known parties
      if (lastKnownPartyIds.value.size === 0) {
        lastKnownPartyIds.value = currentPartyIds
        // Load saved party IDs from localStorage for this user
        const savedIds = localStorage.getItem(`${LAST_PARTY_CHECK_KEY}_${authStore.user?.id}`)
        if (savedIds) {
          try {
            const parsed = JSON.parse(savedIds)
            lastKnownPartyIds.value = new Set(parsed)
          } catch {
            // Ignore parse errors
          }
        }
        return 0
      }

      // Find new parties (parties that exist now but weren't known before)
      let newCount = 0
      for (const partyId of currentPartyIds) {
        if (!lastKnownPartyIds.value.has(partyId)) {
          newCount++
        }
      }

      // Update known parties
      lastKnownPartyIds.value = currentPartyIds

      // Save to localStorage for this user
      localStorage.setItem(
        `${LAST_PARTY_CHECK_KEY}_${authStore.user?.id}`,
        JSON.stringify([...currentPartyIds])
      )

      return newCount
    } catch (e) {
      console.warn('[Notifications] Error checking parties:', e)
      return 0
    }
  }

  // Reset notification state (used when switching accounts)
  function resetState() {
    console.log('[Notifications] Resetting notification state')
    lastTotalUnread.value = -1
    newPartiesCount.value = 0
    lastKnownPartyIds.value = new Set()
    document.title = 'Made in Braza'
  }

  // Check for new content and trigger notifications
  async function checkForNewContent() {
    if (!authStore.user) {
      console.log('[Notifications] No user, skipping check')
      return
    }

    // Detect account switch - reset state if user changed
    if (currentUserId.value !== null && currentUserId.value !== authStore.user.id) {
      console.log('[Notifications] Account switched, resetting state')
      resetState()
    }
    currentUserId.value = authStore.user.id

    const previousTotal = lastTotalUnread.value
    console.log('[Notifications] Checking for new content, previousTotal:', previousTotal)

    // Check for new parties first
    const newParties = await checkForNewParties()
    if (newParties > 0) {
      console.log('[Notifications] Found', newParties, 'new parties!')
      newPartiesCount.value += newParties
    }

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
      pending: authStore.isLeader ? membersStore.pendingCount : 0,
      newParties: newPartiesCount.value
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
    // Reset state when stopping (e.g., on logout)
    resetState()
  }

  // Clear new parties count (called when user views parties page)
  function clearNewPartiesCount() {
    newPartiesCount.value = 0

    // Update known party IDs with current parties to prevent re-notification
    const currentParties = partiesStore.globalParties
    if (currentParties.length > 0) {
      const currentPartyIds = new Set(currentParties.map(p => p.id))
      lastKnownPartyIds.value = currentPartyIds

      // Save to localStorage for persistence
      if (authStore.user?.id) {
        localStorage.setItem(
          `${LAST_PARTY_CHECK_KEY}_${authStore.user.id}`,
          JSON.stringify([...currentPartyIds])
        )
      }
    }

    updateDocumentTitle()
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
      newParties: newPartiesCount.value,
      knownPartyIds: [...lastKnownPartyIds.value],
      currentUserId: currentUserId.value,
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
    newPartiesCount,
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
    clearNewPartiesCount,
    resetState,
    cleanup
  }
})
