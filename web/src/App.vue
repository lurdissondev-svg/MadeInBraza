<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationsStore } from '@/stores/notifications'

const router = useRouter()
const authStore = useAuthStore()
const notificationsStore = useNotificationsStore()
const isNavigating = ref(false)

// Enable sound on first user interaction
function handleUserInteraction() {
  notificationsStore.markUserInteracted()
  // Remove listeners after first interaction
  document.removeEventListener('click', handleUserInteraction)
  document.removeEventListener('keydown', handleUserInteraction)
  document.removeEventListener('touchstart', handleUserInteraction)
}

onMounted(() => {
  router.beforeEach(() => {
    isNavigating.value = true
  })

  router.afterEach(() => {
    isNavigating.value = false
  })

  // Listen for user interaction to enable sound
  document.addEventListener('click', handleUserInteraction)
  document.addEventListener('keydown', handleUserInteraction)
  document.addEventListener('touchstart', handleUserInteraction)
})

// Watch for user changes - start/stop polling
watch(
  () => authStore.user,
  (user) => {
    if (user) {
      // Start polling when user is loaded (every 30 seconds)
      notificationsStore.startPolling(30000)
    } else {
      // Stop polling when user logs out
      notificationsStore.stopPolling()
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  notificationsStore.cleanup()
  document.removeEventListener('click', handleUserInteraction)
  document.removeEventListener('keydown', handleUserInteraction)
  document.removeEventListener('touchstart', handleUserInteraction)
})
</script>

<template>
  <!-- Global Navigation Loading Indicator -->
  <div
    v-if="isNavigating"
    class="fixed top-0 left-0 right-0 z-[100] h-1 bg-primary-500/30"
  >
    <div class="h-full bg-primary-500 animate-loading-bar" />
  </div>

  <Suspense>
    <template #default>
      <RouterView />
    </template>
    <template #fallback>
      <div class="min-h-screen bg-dark-900 flex items-center justify-center">
        <div class="flex flex-col items-center gap-4">
          <div class="w-10 h-10 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
          <span class="text-gray-400 text-sm">Carregando...</span>
        </div>
      </div>
    </template>
  </Suspense>
</template>

<style>
@keyframes loading-bar {
  0% {
    width: 0%;
    margin-left: 0%;
  }
  50% {
    width: 30%;
    margin-left: 35%;
  }
  100% {
    width: 0%;
    margin-left: 100%;
  }
}

.animate-loading-bar {
  animation: loading-bar 1s ease-in-out infinite;
}
</style>
