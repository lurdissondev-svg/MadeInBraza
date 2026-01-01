<script setup lang="ts">
import { computed } from 'vue'
import { Role } from '@/types'
import type { ChannelMessage } from '@/types'

const props = defineProps<{
  message: ChannelMessage
  isCurrentUser: boolean
}>()

const isLeader = computed(() => props.message.user.role === Role.LEADER)

const bubbleClasses = computed(() => {
  if (props.isCurrentUser) {
    return 'bg-primary-500 text-white'
  }
  if (isLeader.value) {
    return 'bg-primary-500/20 text-gray-100'
  }
  return 'bg-dark-600 text-gray-200'
})

function formatTime(isoDate: string): string {
  try {
    const date = new Date(isoDate)
    return date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

function getMediaUrl(mediaUrl: string): string {
  // Media URL is like "/uploads/channels/{channelId}/{filename}"
  // Base URL needs to be prepended
  const baseUrl = import.meta.env.VITE_API_URL?.replace('/api', '') || ''
  return `${baseUrl}${mediaUrl}`
}
</script>

<template>
  <div
    class="flex flex-col"
    :class="isCurrentUser ? 'items-end' : 'items-start'"
  >
    <!-- Header (nick, leader tag, time) -->
    <div class="flex items-center gap-2 px-1 mb-1">
      <span
        v-if="!isCurrentUser"
        class="text-sm font-semibold"
        :class="isLeader ? 'text-primary-400' : 'text-gray-300'"
      >
        {{ message.user.nick }}
      </span>
      <span
        v-if="!isCurrentUser && isLeader"
        class="text-xs text-primary-400"
      >
        [LIDER]
      </span>
      <span class="text-xs text-gray-500">
        {{ formatTime(message.createdAt) }}
      </span>
    </div>

    <!-- Message bubble -->
    <div
      class="max-w-[280px] rounded-2xl overflow-hidden"
      :class="[
        bubbleClasses,
        isCurrentUser ? 'rounded-tr-sm' : 'rounded-tl-sm',
        message.mediaUrl ? 'p-1' : 'px-4 py-2'
      ]"
    >
      <!-- Media content -->
      <template v-if="message.mediaUrl">
        <div v-if="message.mediaType === 'image'" class="rounded-xl overflow-hidden">
          <img
            :src="getMediaUrl(message.mediaUrl)"
            :alt="message.fileName || 'Imagem'"
            class="max-h-72 w-full object-contain bg-dark-700"
          />
        </div>

        <div
          v-else-if="message.mediaType === 'video'"
          class="relative rounded-xl overflow-hidden bg-dark-700"
        >
          <video
            :src="getMediaUrl(message.mediaUrl)"
            class="max-h-72 w-full"
            controls
          />
        </div>

        <!-- File name -->
        <p
          v-if="message.fileName"
          class="text-xs px-2 py-1 opacity-70 truncate"
        >
          {{ message.fileName }}
        </p>
      </template>

      <!-- Text content -->
      <p
        v-if="message.content"
        class="whitespace-pre-wrap break-words"
        :class="message.mediaUrl ? 'px-3 py-2' : ''"
      >
        {{ message.content }}
      </p>
    </div>
  </div>
</template>
