<script setup lang="ts">
import { computed, ref } from 'vue'
import { Role } from '@/types'
import type { ChannelMessage } from '@/types'

const props = defineProps<{
  message: ChannelMessage
  isCurrentUser: boolean
}>()

const isLeader = computed(() => props.message.user.role === Role.LEADER)

// Track if avatar image failed to load
const avatarError = ref(false)

const avatarUrl = computed(() => {
  if (avatarError.value) return null
  const url = props.message.user.avatarUrl
  if (!url) return null
  const baseUrl = import.meta.env.VITE_API_URL?.replace('/api', '') || ''
  return `${baseUrl}${url}`
})

function handleAvatarError() {
  avatarError.value = true
}

const bubbleBackground = computed(() => {
  if (props.isCurrentUser) {
    return '#9c27b0'  // primary-500 (purple)
  }
  if (isLeader.value) {
    return 'rgba(156, 39, 176, 0.2)'  // primary-500/20
  }
  return '#374151'  // dark-600
})

const textColor = computed(() => {
  if (props.isCurrentUser) {
    return '#ffffff'  // White text for own messages
  }
  if (isLeader.value) {
    return '#f3f4f6'  // gray-100
  }
  return '#e5e7eb'  // gray-200
})

// Check if media is an image (by type or file extension)
const isImage = computed(() => {
  if (!props.message.mediaUrl) return false

  // Check by mediaType
  if (props.message.mediaType === 'image') return true
  if (props.message.mediaType?.startsWith('image/')) return true

  // Check by file extension as fallback
  const url = props.message.mediaUrl.toLowerCase()
  const fileName = props.message.fileName?.toLowerCase() || ''
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.svg']
  return imageExtensions.some(ext => url.includes(ext) || fileName.endsWith(ext))
})

// Check if media is a video (by type or file extension)
const isVideo = computed(() => {
  if (!props.message.mediaUrl) return false

  // Check by mediaType
  if (props.message.mediaType === 'video') return true
  if (props.message.mediaType?.startsWith('video/')) return true

  // Check by file extension as fallback
  const url = props.message.mediaUrl.toLowerCase()
  const fileName = props.message.fileName?.toLowerCase() || ''
  const videoExtensions = ['.mp4', '.webm', '.ogg', '.mov', '.avi']
  return videoExtensions.some(ext => url.includes(ext) || fileName.endsWith(ext))
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

function openFullImage() {
  if (props.message.mediaUrl) {
    window.open(getMediaUrl(props.message.mediaUrl), '_blank')
  }
}
</script>

<template>
  <div
    class="flex gap-2"
    :class="isCurrentUser ? 'flex-row-reverse' : 'flex-row'"
  >
    <!-- Avatar (only for other users) -->
    <div v-if="!isCurrentUser" class="flex-shrink-0">
      <div
        class="w-8 h-8 rounded-full flex items-center justify-center overflow-hidden"
        :class="avatarUrl ? '' : 'bg-dark-500'"
      >
        <img
          v-if="avatarUrl"
          :src="avatarUrl"
          alt="Avatar"
          loading="lazy"
          decoding="async"
          class="w-full h-full object-cover"
          @error="handleAvatarError"
        />
        <span v-else class="text-sm font-bold text-white">
          {{ message.user.nick.charAt(0).toUpperCase() }}
        </span>
      </div>
    </div>

    <div class="flex flex-col" :class="isCurrentUser ? 'items-end' : 'items-start'">
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
        isCurrentUser ? 'rounded-tr-sm' : 'rounded-tl-sm',
        message.mediaUrl ? 'p-1' : 'px-4 py-2'
      ]"
      :style="{ backgroundColor: bubbleBackground, color: textColor }"
    >
      <!-- Media content -->
      <template v-if="message.mediaUrl">
        <!-- Image (including GIF) -->
        <div v-if="isImage" class="rounded-xl overflow-hidden cursor-pointer" @click="openFullImage">
          <img
            :src="getMediaUrl(message.mediaUrl)"
            :alt="message.fileName || 'Imagem'"
            class="max-h-72 w-full object-contain bg-dark-700 hover:opacity-90 transition-opacity"
          />
        </div>

        <!-- Video -->
        <div
          v-else-if="isVideo"
          class="relative rounded-xl overflow-hidden bg-dark-700"
        >
          <video
            :src="getMediaUrl(message.mediaUrl)"
            class="max-h-72 w-full"
            controls
          />
        </div>

        <!-- Other file types -->
        <a
          v-else
          :href="getMediaUrl(message.mediaUrl)"
          target="_blank"
          class="flex items-center gap-2 px-3 py-2 text-sm hover:opacity-80"
          :style="{ color: textColor }"
        >
          <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <span class="truncate">{{ message.fileName || 'Arquivo' }}</span>
        </a>

        <!-- File name for images/videos -->
        <p
          v-if="(isImage || isVideo) && message.fileName"
          class="text-xs px-2 py-1 opacity-70 truncate"
          :style="{ color: textColor }"
        >
          {{ message.fileName }}
        </p>
      </template>

      <!-- Text content -->
      <p
        v-if="message.content"
        class="whitespace-pre-wrap break-words"
        :class="message.mediaUrl ? 'px-3 py-2' : ''"
        :style="{ color: textColor }"
      >
        {{ message.content }}
      </p>
    </div>
    </div>
  </div>
</template>
