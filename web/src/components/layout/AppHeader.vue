<script setup lang="ts">
import { useRouter } from 'vue-router'

interface Props {
  title: string
  showBack?: boolean
}

defineProps<Props>()

const router = useRouter()

function goBack() {
  router.back()
}
</script>

<template>
  <header class="sticky top-0 z-30 bg-dark-800 border-b border-dark-600 safe-top">
    <div class="flex items-center h-14 px-4 sm:px-6 lg:px-8">
      <!-- Back Button -->
      <button
        v-if="showBack"
        @click="goBack"
        class="mr-3 p-1.5 rounded-lg text-gray-400 hover:text-gray-200 hover:bg-dark-700 transition-colors touch-target"
      >
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>

      <!-- Title -->
      <h1 class="text-lg font-semibold text-gray-100 truncate">
        {{ title }}
      </h1>

      <!-- Spacer -->
      <div class="flex-1"></div>

      <!-- Optional: Right side actions slot -->
      <slot name="actions"></slot>
    </div>
  </header>
</template>
