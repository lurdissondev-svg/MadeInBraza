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
  const notificationSound = ref<HTMLAudioElement | null>(null)
  const userInteracted = ref(false)
  const previousUnreadCount = ref(0)

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
      // Pre-load the audio
      notificationSound.value.load()
    } catch (e) {
      console.warn('Could not initialize notification sound:', e)
    }
  }

  // Play notification sound
  async function playSound() {
    if (!soundEnabled.value || !userInteracted.value) {
      console.log('Sound blocked:', { soundEnabled: soundEnabled.value, userInteracted: userInteracted.value })
      return
    }

    try {
      if (!notificationSound.value) {
        initAudio()
      }

      if (notificationSound.value) {
        notificationSound.value.currentTime = 0
        await notificationSound.value.play()
        console.log('Sound played!')
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

  // Toggle sound
  function toggleSound() {
    soundEnabled.value = !soundEnabled.value
    localStorage.setItem(STORAGE_KEY, String(soundEnabled.value))
  }

  // Mark user as having interacted (enables sound)
  function markUserInteracted() {
    if (userInteracted.value) return
    userInteracted.value = true
    initAudio()
    console.log('User interaction registered, sound enabled')
  }

  // Watch for changes in unread count
  watch(totalUnreadCount, (newCount, oldCount) => {
    console.log('Unread count changed:', oldCount, '->', newCount)

    // Update title
    updateDocumentTitle()

    // Play sound if count increased (and not initial load)
    const prevCount = oldCount ?? 0
    if (newCount > prevCount && previousUnreadCount.value > 0) {
      playSound()
    }

    previousUnreadCount.value = newCount
  }, { immediate: true })

  // Initialize on first run
  function init() {
    previousUnreadCount.value = totalUnreadCount.value
    updateDocumentTitle()
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
    toggleSound,
    markUserInteracted,
    init
  }
})
