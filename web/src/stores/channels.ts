import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Channel, ChannelMessage, ChannelMember } from '@/types'
import { channelsApi } from '@/services/api/channels'

// localStorage keys
const LAST_READ_PREFIX = 'braza_last_read_'

export const useChannelsStore = defineStore('channels', () => {
  // State
  const channels = ref<Channel[]>([])
  const currentChannel = ref<Channel | null>(null)
  const messages = ref<ChannelMessage[]>([])
  const members = ref<ChannelMember[]>([])
  const unreadCounts = ref<Record<string, number>>({})
  const firstUnreadMessageId = ref<string | null>(null)

  const loading = ref(false)
  const loadingMessages = ref(false)
  const loadingMembers = ref(false)
  const sending = ref(false)
  const uploading = ref(false)
  const error = ref<string | null>(null)

  // Helper: Get last read timestamp for a channel
  function getLastReadTimestamp(channelId: string): number {
    const stored = localStorage.getItem(LAST_READ_PREFIX + channelId)
    return stored ? parseInt(stored, 10) : 0
  }

  // Helper: Set last read timestamp for a channel
  function setLastReadTimestamp(channelId: string, timestamp: number) {
    localStorage.setItem(LAST_READ_PREFIX + channelId, timestamp.toString())
  }

  // Computed: Total unread count across all channels
  const totalUnreadCount = computed(() => {
    return Object.values(unreadCounts.value).reduce((sum, count) => sum + count, 0)
  })

  // Computed
  const sortedChannels = computed(() => {
    return [...channels.value].sort((a, b) => {
      // Leaders channel first, then by type, then by name
      if (a.type === 'LEADERS' && b.type !== 'LEADERS') return -1
      if (b.type === 'LEADERS' && a.type !== 'LEADERS') return 1
      if (a.type === 'GENERAL' && b.type !== 'GENERAL') return -1
      if (b.type === 'GENERAL' && a.type !== 'GENERAL') return 1
      return a.name.localeCompare(b.name)
    })
  })

  // Actions
  async function fetchChannels(): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      channels.value = await channelsApi.getChannels()
      // Calculate unread counts for all channels
      await calculateAllUnreadCounts()
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar canais'
      return false
    } finally {
      loading.value = false
    }
  }

  // Calculate unread counts for all channels
  async function calculateAllUnreadCounts(): Promise<void> {
    const counts: Record<string, number> = {}

    for (const channel of channels.value) {
      try {
        const channelMessages = await channelsApi.getChannelMessages(channel.id)
        const lastRead = getLastReadTimestamp(channel.id)

        // Count messages created after last read timestamp
        const unreadCount = channelMessages.filter(msg => {
          const msgTime = new Date(msg.createdAt).getTime()
          return msgTime > lastRead
        }).length

        counts[channel.id] = Math.min(unreadCount, 99) // Cap at 99
      } catch {
        counts[channel.id] = 0
      }
    }

    unreadCounts.value = counts
  }

  // Calculate unread count for a single channel
  async function calculateUnreadCount(channelId: string): Promise<number> {
    try {
      const channelMessages = await channelsApi.getChannelMessages(channelId)
      const lastRead = getLastReadTimestamp(channelId)

      const unreadCount = channelMessages.filter(msg => {
        const msgTime = new Date(msg.createdAt).getTime()
        return msgTime > lastRead
      }).length

      unreadCounts.value[channelId] = Math.min(unreadCount, 99)
      return unreadCount
    } catch {
      return 0
    }
  }

  async function setupDefaultChannels(): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      await channelsApi.setupDefaultChannels()
      await fetchChannels()
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao criar canais'
      return false
    } finally {
      loading.value = false
    }
  }

  async function openChannel(channel: Channel): Promise<boolean> {
    currentChannel.value = channel
    messages.value = []
    return fetchMessages()
  }

  function closeChannel() {
    currentChannel.value = null
    messages.value = []
    members.value = []
  }

  async function fetchMessages(): Promise<boolean> {
    if (!currentChannel.value) return false

    loadingMessages.value = true
    error.value = null

    try {
      const channelId = currentChannel.value.id
      const lastRead = getLastReadTimestamp(channelId)

      messages.value = await channelsApi.getChannelMessages(channelId)

      // Find first unread message (for separator)
      firstUnreadMessageId.value = null
      for (const msg of messages.value) {
        const msgTime = new Date(msg.createdAt).getTime()
        if (msgTime > lastRead) {
          firstUnreadMessageId.value = msg.id
          break
        }
      }

      // Mark as read: update last read timestamp to now
      const now = Date.now()
      setLastReadTimestamp(channelId, now)

      // Call API (even if stub) and reset unread count
      await channelsApi.markAsRead(channelId)
      unreadCounts.value[channelId] = 0

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar mensagens'
      return false
    } finally {
      loadingMessages.value = false
    }
  }

  async function sendMessage(content: string): Promise<boolean> {
    if (!currentChannel.value || !content.trim()) return false

    sending.value = true
    error.value = null

    try {
      const message = await channelsApi.sendMessage(currentChannel.value.id, content)
      messages.value.push(message)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao enviar mensagem'
      return false
    } finally {
      sending.value = false
    }
  }

  async function sendMediaMessage(file: File, content?: string): Promise<boolean> {
    if (!currentChannel.value) return false

    uploading.value = true
    error.value = null

    try {
      const message = await channelsApi.sendMediaMessage(
        currentChannel.value.id,
        file,
        content
      )
      messages.value.push(message)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao enviar mídia'
      return false
    } finally {
      uploading.value = false
    }
  }

  async function fetchMembers(): Promise<boolean> {
    if (!currentChannel.value) return false

    loadingMembers.value = true
    error.value = null

    try {
      members.value = await channelsApi.getChannelMembers(currentChannel.value.id)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar membros'
      return false
    } finally {
      loadingMembers.value = false
    }
  }

  function addMessage(message: ChannelMessage) {
    // Add message if not already present
    if (!messages.value.find(m => m.id === message.id)) {
      messages.value.push(message)
    }
  }

  function incrementUnread(channelId: string) {
    unreadCounts.value[channelId] = (unreadCounts.value[channelId] || 0) + 1
  }

  async function deleteMessage(messageId: string): Promise<boolean> {
    if (!currentChannel.value) return false

    error.value = null

    try {
      await channelsApi.deleteMessage(currentChannel.value.id, messageId)
      messages.value = messages.value.filter(m => m.id !== messageId)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao excluir mensagem'
      return false
    }
  }

  async function editMessage(messageId: string, content: string): Promise<boolean> {
    if (!currentChannel.value || !content.trim()) return false

    error.value = null

    try {
      const updatedMessage = await channelsApi.editMessage(currentChannel.value.id, messageId, content)
      const index = messages.value.findIndex(m => m.id === messageId)
      if (index !== -1) {
        messages.value[index] = updatedMessage
      }
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao editar mensagem'
      return false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    channels,
    currentChannel,
    messages,
    members,
    unreadCounts,
    firstUnreadMessageId,
    loading,
    loadingMessages,
    loadingMembers,
    sending,
    uploading,
    error,
    // Computed
    sortedChannels,
    totalUnreadCount,
    // Actions
    fetchChannels,
    setupDefaultChannels,
    openChannel,
    closeChannel,
    fetchMessages,
    sendMessage,
    sendMediaMessage,
    fetchMembers,
    addMessage,
    incrementUnread,
    calculateUnreadCount,
    calculateAllUnreadCounts,
    deleteMessage,
    editMessage,
    clearError
  }
})
