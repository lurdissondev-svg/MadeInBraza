<script setup lang="ts">
import { computed } from 'vue'
import type { Party } from '@/types'
import { PlayerClassAbbreviations } from '@/types'

const props = defineProps<{
  party: Party
  currentUserId?: string
  canDelete?: boolean
}>()

const emit = defineEmits<{
  join: [id: string]
  leave: [id: string]
  delete: [id: string]
}>()

const isParticipating = computed(() => {
  if (!props.currentUserId) return false
  return props.party.members.some(m => m.id === props.currentUserId)
})

const isFull = computed(() => {
  return props.party.members.length >= props.party.maxMembers
})

const memberCount = computed(() => {
  return `${props.party.members.length}/${props.party.maxMembers}`
})

function handleToggleParticipation() {
  if (isParticipating.value) {
    emit('leave', props.party.id)
  } else {
    emit('join', props.party.id)
  }
}

function handleDelete() {
  emit('delete', props.party.id)
}
</script>

<template>
  <div class="card flex flex-col">
    <div class="flex items-start justify-between gap-2 mb-3">
      <div class="min-w-0">
        <div class="flex items-center gap-2 mb-1">
          <h4 class="font-semibold text-gray-100 truncate">{{ party.name }}</h4>
          <span
            v-if="party.isClosed"
            class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-red-500/20 text-red-400"
          >
            Fechada
          </span>
        </div>
        <p class="text-xs text-gray-500">Por: {{ party.createdBy.nick }}</p>
      </div>
      <button
        v-if="canDelete"
        @click="handleDelete"
        class="p-1.5 text-gray-500 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors flex-shrink-0"
        title="Deletar party"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
      </button>
    </div>

    <!-- Description -->
    <p v-if="party.description" class="text-gray-400 text-sm mb-3 line-clamp-2">{{ party.description }}</p>

    <!-- Members Progress -->
    <div class="mb-3">
      <div class="flex items-center justify-between text-xs text-gray-400 mb-1">
        <span>Membros</span>
        <span>{{ memberCount }}</span>
      </div>
      <div class="h-2 bg-dark-600 rounded-full overflow-hidden">
        <div
          class="h-full transition-all duration-300"
          :class="isFull ? 'bg-red-500' : 'bg-primary-500'"
          :style="{ width: `${(party.members.length / party.maxMembers) * 100}%` }"
        />
      </div>
    </div>

    <!-- Members List -->
    <div v-if="party.members.length > 0" class="flex flex-wrap gap-1 mb-3">
      <span
        v-for="member in party.members"
        :key="member.id"
        class="inline-flex items-center px-2 py-0.5 rounded text-xs bg-dark-600 text-gray-300"
        :class="{ 'bg-primary-500/20 text-primary-400': member.id === currentUserId }"
      >
        {{ member.nick }}
        <span v-if="member.playerClass" class="ml-1 text-gray-500">{{ PlayerClassAbbreviations[member.playerClass] }}</span>
      </span>
    </div>

    <!-- Spacer -->
    <div class="flex-1" />

    <!-- Join/Leave Button -->
    <button
      @click="handleToggleParticipation"
      class="btn w-full text-sm"
      :class="isParticipating ? 'btn-secondary' : 'btn-primary'"
      :disabled="!isParticipating && (isFull || party.isClosed)"
    >
      {{ isParticipating ? 'Sair da Party' : (isFull ? 'Party Lotada' : 'Entrar') }}
    </button>
  </div>
</template>
