<script setup lang="ts">
import { computed, ref } from 'vue'
import { Role } from '@/types'
import type { ChannelMessage } from '@/types'

const props = defineProps<{
  message: ChannelMessage
  isCurrentUser: boolean
}>()

const emit = defineEmits<{
  delete: [messageId: string]
  edit: [messageId: string, content: string]
}>()

const isLeader = computed(() => props.message.user.role === Role.LEADER)

// Edit mode state
const isEditing = ref(false)
const editContent = ref('')
const showActions = ref(false)

function startEdit() {
  editContent.value = props.message.content || ''
  isEditing.value = true
  showActions.value = false
}

function cancelEdit() {
  isEditing.value = false
  editContent.value = ''
}

function saveEdit() {
  if (editContent.value.trim()) {
    emit('edit', props.message.id, editContent.value.trim())
  }
  isEditing.value = false
}

function confirmDelete() {
  if (confirm('Tem certeza que deseja excluir esta mensagem?')) {
    emit('delete', props.message.id)
  }
  showActions.value = false
}

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
        class="w-10 h-10 rounded-full flex items-center justify-center overflow-hidden"
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
      <!-- Header (nick, leader tag, time, actions) -->
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
        <span v-if="message.editedAt" class="text-xs text-gray-500 italic">
          (editado)
        </span>
        <!-- Actions menu for own messages -->
        <div v-if="isCurrentUser && !isEditing" class="relative ml-auto">
          <button
            @click="showActions = !showActions"
            class="p-1 hover:bg-dark-500 rounded transition-colors"
          >
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z" />
            </svg>
          </button>
          <!-- Dropdown menu -->
          <div
            v-if="showActions"
            class="absolute right-0 mt-1 w-32 bg-dark-600 border border-dark-500 rounded-lg shadow-lg z-10"
          >
            <button
              v-if="message.content"
              @click="startEdit"
              class="w-full px-3 py-2 text-left text-sm text-gray-200 hover:bg-dark-500 flex items-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              Editar
            </button>
            <button
              @click="confirmDelete"
              class="w-full px-3 py-2 text-left text-sm text-red-400 hover:bg-dark-500 flex items-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              Excluir
            </button>
          </div>
        </div>
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

      <!-- Text content - Edit mode -->
      <div v-if="isEditing" class="px-3 py-2">
        <textarea
          v-model="editContent"
          class="w-full bg-dark-700 border border-dark-500 rounded-lg px-3 py-2 text-sm text-white resize-none focus:outline-none focus:border-primary-500"
          rows="2"
          @keydown.enter.exact.prevent="saveEdit"
          @keydown.escape="cancelEdit"
        ></textarea>
        <div class="flex gap-2 mt-2">
          <button
            @click="saveEdit"
            class="px-3 py-1 text-xs bg-primary-500 text-white rounded hover:bg-primary-600 transition-colors"
          >
            Salvar
          </button>
          <button
            @click="cancelEdit"
            class="px-3 py-1 text-xs bg-dark-500 text-gray-300 rounded hover:bg-dark-400 transition-colors"
          >
            Cancelar
          </button>
        </div>
      </div>

      <!-- Text content - View mode -->
      <p
        v-else-if="message.content"
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
