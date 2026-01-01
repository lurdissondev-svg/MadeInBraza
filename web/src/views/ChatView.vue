<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useChannelsStore } from '@/stores/channels'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import MessageBubble from '@/components/chat/MessageBubble.vue'
import MessageInput from '@/components/chat/MessageInput.vue'
import MembersSheet from '@/components/chat/MembersSheet.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const channelsStore = useChannelsStore()

const messagesContainer = ref<HTMLElement | null>(null)
const showMembersSheet = ref(false)
let pollingInterval: ReturnType<typeof setInterval> | null = null

const channelId = computed(() => route.params.id as string)

const currentUserId = computed(() => authStore.user?.id)

onMounted(async () => {
  if (channelId.value) {
    // Find or fetch channel info
    if (channelsStore.channels.length === 0) {
      await channelsStore.fetchChannels()
    }

    const channel = channelsStore.channels.find(c => c.id === channelId.value)
    if (channel) {
      await channelsStore.openChannel(channel)
      scrollToBottom()
      startPolling()
    } else {
      // Channel not found, go back
      router.push('/channels')
    }
  }
})

onUnmounted(() => {
  stopPolling()
  channelsStore.closeChannel()
})

// Watch for new messages and scroll
watch(() => channelsStore.messages.length, () => {
  nextTick(() => scrollToBottom())
})

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function startPolling() {
  // Poll for new messages every 10 seconds
  pollingInterval = setInterval(() => {
    if (channelsStore.currentChannel) {
      channelsStore.fetchMessages()
    }
  }, 10000)
}

function stopPolling() {
  if (pollingInterval) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
}

async function handleSendMessage(content: string) {
  const success = await channelsStore.sendMessage(content)
  if (success) {
    scrollToBottom()
  }
}

async function handleSendMedia(file: File) {
  const success = await channelsStore.sendMediaMessage(file)
  if (success) {
    scrollToBottom()
  }
}

function handleShowMembers() {
  channelsStore.fetchMembers()
  showMembersSheet.value = true
}

function goBack() {
  router.push('/channels')
}
</script>

<template>
  <div class="h-screen flex flex-col bg-dark-800">
    <!-- Header -->
    <header class="bg-dark-700 border-b border-dark-600 px-4 py-3 flex items-center gap-3 shrink-0">
      <button
        @click="goBack"
        class="p-2 hover:bg-dark-600 rounded-lg transition-colors"
      >
        <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>

      <h1 class="flex-1 font-semibold text-gray-100 truncate">
        {{ channelsStore.currentChannel?.name || 'Chat' }}
      </h1>

      <button
        @click="handleShowMembers"
        class="p-2 hover:bg-dark-600 rounded-lg transition-colors"
      >
        <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      </button>
    </header>

    <!-- Messages -->
    <div
      ref="messagesContainer"
      class="flex-1 overflow-y-auto p-4 space-y-4"
    >
      <!-- Loading -->
      <div v-if="channelsStore.loadingMessages && channelsStore.messages.length === 0" class="flex justify-center py-12">
        <LoadingSpinner />
      </div>

      <!-- Empty -->
      <div v-else-if="channelsStore.messages.length === 0" class="flex items-center justify-center h-full">
        <p class="text-gray-400">Nenhuma mensagem ainda. Seja o primeiro a enviar!</p>
      </div>

      <!-- Messages list -->
      <template v-else>
        <MessageBubble
          v-for="message in channelsStore.messages"
          :key="message.id"
          :message="message"
          :is-current-user="message.user.id === currentUserId"
        />
      </template>
    </div>

    <!-- Input -->
    <MessageInput
      :disabled="!channelsStore.currentChannel"
      :sending="channelsStore.sending"
      :uploading="channelsStore.uploading"
      @send="handleSendMessage"
      @send-media="handleSendMedia"
    />

    <!-- Error toast -->
    <div
      v-if="channelsStore.error"
      class="fixed bottom-20 left-4 right-4 bg-red-500/90 text-white px-4 py-3 rounded-lg flex items-center justify-between"
    >
      <span>{{ channelsStore.error }}</span>
      <button @click="channelsStore.clearError()" class="ml-4 font-bold">OK</button>
    </div>

    <!-- Members Sheet -->
    <MembersSheet
      v-if="showMembersSheet && channelsStore.currentChannel"
      :channel-name="channelsStore.currentChannel.name"
      :members="channelsStore.members"
      :loading="channelsStore.loadingMembers"
      @close="showMembersSheet = false"
    />
  </div>
</template>
