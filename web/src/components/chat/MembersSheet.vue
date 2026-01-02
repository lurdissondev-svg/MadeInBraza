<script setup lang="ts">
import { Role, PlayerClass, PlayerClassAbbreviations } from '@/types'
import type { ChannelMember } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

defineProps<{
  channelName: string
  members: ChannelMember[]
  loading: boolean
}>()

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <!-- Backdrop -->
  <div
    class="fixed inset-0 bg-black/50 z-40"
    @click="emit('close')"
  />

  <!-- Sheet -->
  <div class="fixed inset-x-0 bottom-0 z-50 animate-slide-up">
    <div class="bg-dark-700 rounded-t-2xl max-h-[70vh] overflow-hidden flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between p-4 border-b border-dark-600">
        <div>
          <h3 class="text-lg font-semibold text-gray-100">Membros</h3>
          <p class="text-sm text-gray-400">{{ channelName }}</p>
        </div>
        <button
          @click="emit('close')"
          class="p-2 hover:bg-dark-600 rounded-lg transition-colors"
        >
          <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-4">
        <!-- Loading -->
        <div v-if="loading" class="flex justify-center py-8">
          <LoadingSpinner />
        </div>

        <!-- Empty -->
        <div v-else-if="members.length === 0" class="text-center py-8">
          <p class="text-gray-400">Nenhum membro encontrado</p>
        </div>

        <!-- Members list -->
        <div v-else class="space-y-2">
          <p class="text-sm text-gray-400 mb-3">
            {{ members.length }} {{ members.length === 1 ? 'membro' : 'membros' }}
          </p>

          <div
            v-for="member in members"
            :key="member.id"
            class="flex items-center gap-3 p-3 rounded-lg"
            :class="member.role === Role.LEADER ? 'bg-primary-500/10' : 'bg-dark-600'"
          >
            <!-- Avatar -->
            <div
              class="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold"
              :class="'bg-dark-500'"
            >
              {{ member.nick.charAt(0).toUpperCase() }}
            </div>

            <!-- Info -->
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <span
                  class="font-medium"
                  :class="member.role === Role.LEADER ? 'text-primary-400' : 'text-gray-100'"
                >
                  {{ member.nick }}
                </span>
                <svg
                  v-if="member.role === Role.LEADER"
                  class="w-4 h-4 text-primary-400"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                </svg>
              </div>
              <span class="text-sm text-gray-400">
                {{ PlayerClassAbbreviations[member.playerClass as PlayerClass] }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes slide-up {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.animate-slide-up {
  animation: slide-up 0.3s ease-out;
}
</style>
