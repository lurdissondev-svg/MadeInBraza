<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterView, useRouter } from 'vue-router'

const router = useRouter()
const isNavigating = ref(false)

onMounted(() => {
  router.beforeEach(() => {
    isNavigating.value = true
  })

  router.afterEach(() => {
    isNavigating.value = false
  })
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
