<script setup lang="ts">
import { computed } from 'vue'
import type { Announcement } from '@/types'

const props = defineProps<{
  announcement: Announcement
  canDelete?: boolean
}>()

const emit = defineEmits<{
  delete: [id: string]
}>()

const isWhatsApp = computed(() => !!props.announcement.whatsappMessageId)

const authorName = computed(() => {
  if (props.announcement.whatsappAuthor) {
    return props.announcement.whatsappAuthor
  }
  return props.announcement.createdBy?.nick || 'Sistema'
})

const formattedDate = computed(() => {
  const dateStr = props.announcement.whatsappTimestamp || props.announcement.createdAt
  const date = new Date(dateStr)
  return date.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
})

// Check if media is an image (by type or file extension)
const isImage = computed(() => {
  if (!props.announcement.mediaUrl) return false

  // Check by mediaType (supports both 'image' and 'image/jpeg' formats)
  const mediaType = props.announcement.mediaType?.toLowerCase()
  if (mediaType === 'image' || mediaType?.startsWith('image/')) return true

  // Check by file extension as fallback
  const url = props.announcement.mediaUrl.toLowerCase()
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.svg']
  return imageExtensions.some(ext => url.includes(ext))
})

// Check if media is a video (by type or file extension)
const isVideo = computed(() => {
  if (!props.announcement.mediaUrl) return false

  // Check by mediaType (supports both 'video' and 'video/mp4' formats)
  const mediaType = props.announcement.mediaType?.toLowerCase()
  if (mediaType === 'video' || mediaType?.startsWith('video/')) return true

  // Check by file extension as fallback
  const url = props.announcement.mediaUrl.toLowerCase()
  const videoExtensions = ['.mp4', '.webm', '.ogg', '.mov', '.avi']
  return videoExtensions.some(ext => url.includes(ext))
})

function handleDelete() {
  emit('delete', props.announcement.id)
}

// Build full URL for relative media paths
function getMediaUrl(mediaUrl: string): string {
  if (mediaUrl.startsWith('http://') || mediaUrl.startsWith('https://')) {
    return mediaUrl
  }
  const baseUrl = import.meta.env.VITE_API_URL?.replace('/api', '') || ''
  return `${baseUrl}${mediaUrl}`
}

function openMedia() {
  if (props.announcement.mediaUrl) {
    window.open(getMediaUrl(props.announcement.mediaUrl), '_blank')
  }
}
</script>

<template>
  <div class="card">
    <div class="flex items-start justify-between gap-3">
      <div class="flex-1 min-w-0">
        <!-- Header -->
        <div class="flex items-center gap-2 mb-2">
          <!-- WhatsApp Badge -->
          <span
            v-if="isWhatsApp"
            class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-500/20 text-green-400"
          >
            <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
            </svg>
            WhatsApp
          </span>
          <span v-else class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-primary-500/20 text-primary-400">
            App
          </span>
          <span class="text-xs text-gray-500">{{ formattedDate }}</span>
        </div>

        <!-- Title -->
        <h4 class="font-semibold text-gray-100 mb-1">{{ announcement.title }}</h4>

        <!-- Content -->
        <p class="text-gray-400 text-sm whitespace-pre-wrap">{{ announcement.content }}</p>

        <!-- Media Preview -->
        <div v-if="announcement.mediaUrl" class="mt-3">
          <!-- Image Preview (including GIF) -->
          <img
            v-if="isImage"
            :src="getMediaUrl(announcement.mediaUrl)"
            :alt="announcement.title"
            class="rounded-lg max-w-full max-h-64 object-contain cursor-pointer hover:opacity-90 transition-opacity"
            @click="openMedia"
          />
          <!-- Video Preview -->
          <video
            v-else-if="isVideo"
            :src="getMediaUrl(announcement.mediaUrl)"
            controls
            class="rounded-lg max-h-64 w-full"
          />
          <!-- Other files (documents, etc.) -->
          <a
            v-else
            :href="getMediaUrl(announcement.mediaUrl)"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center text-primary-400 hover:text-primary-300 text-sm"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Ver arquivo
          </a>
        </div>

        <!-- Author -->
        <p class="text-xs text-gray-500 mt-2">
          Por: {{ authorName }}
        </p>
      </div>

      <!-- Delete Button -->
      <button
        v-if="canDelete && !isWhatsApp"
        @click="handleDelete"
        class="p-2 text-gray-500 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors flex-shrink-0"
        title="Deletar aviso"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
      </button>
    </div>
  </div>
</template>
