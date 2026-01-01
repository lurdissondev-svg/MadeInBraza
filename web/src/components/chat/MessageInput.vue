<script setup lang="ts">
import { ref } from 'vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

defineProps<{
  disabled?: boolean
  sending?: boolean
  uploading?: boolean
}>()

const emit = defineEmits<{
  send: [content: string]
  sendMedia: [file: File]
}>()

const message = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

function handleSend() {
  const content = message.value.trim()
  if (content) {
    emit('send', content)
    message.value = ''
  }
}

function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

function openFilePicker() {
  fileInput.value?.click()
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    emit('sendMedia', file)
    // Reset input
    target.value = ''
  }
}
</script>

<template>
  <div class="flex items-end gap-2 p-3 bg-dark-800 border-t border-dark-600">
    <!-- Hidden file input -->
    <input
      ref="fileInput"
      type="file"
      accept="image/*,video/*"
      class="hidden"
      @change="handleFileChange"
    />

    <!-- Attach button -->
    <button
      @click="openFilePicker"
      :disabled="disabled || sending || uploading"
      class="p-2 rounded-full hover:bg-dark-600 transition-colors disabled:opacity-50"
    >
      <svg class="w-6 h-6 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
      </svg>
    </button>

    <!-- Message input -->
    <div class="flex-1">
      <textarea
        v-model="message"
        @keydown="handleKeyDown"
        :disabled="disabled || sending || uploading"
        placeholder="Digite sua mensagem..."
        rows="1"
        class="w-full px-4 py-2 bg-dark-600 border border-dark-500 rounded-3xl text-gray-100 placeholder-gray-500 resize-none focus:outline-none focus:border-primary-500 disabled:opacity-50"
      />
    </div>

    <!-- Send button -->
    <button
      @click="handleSend"
      :disabled="!message.trim() || disabled || sending || uploading"
      class="p-2 rounded-full bg-primary-500 text-white hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:hover:bg-primary-500"
    >
      <LoadingSpinner v-if="sending || uploading" size="sm" />
      <svg v-else class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
      </svg>
    </button>
  </div>
</template>
