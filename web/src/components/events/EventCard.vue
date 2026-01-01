<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Event } from '@/types'
import { PlayerClassAbbreviations } from '@/types'

const props = defineProps<{
  event: Event
  currentUserId?: string
  canDelete?: boolean
  isPast?: boolean
}>()

const emit = defineEmits<{
  join: [id: string]
  leave: [id: string]
  delete: [id: string]
}>()

const showParticipants = ref(false)

const isParticipating = computed(() => {
  if (!props.currentUserId) return false
  return props.event.participants.some(p => p.id === props.currentUserId)
})

const isFull = computed(() => {
  if (!props.event.maxParticipants) return false
  return props.event.participants.length >= props.event.maxParticipants
})

const participantCount = computed(() => {
  const current = props.event.participants.length
  const max = props.event.maxParticipants
  return max ? `${current}/${max}` : `${current}`
})

const formattedDate = computed(() => {
  const date = new Date(props.event.eventDate)
  return date.toLocaleDateString('pt-BR', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

const requiredClassesText = computed(() => {
  if (!props.event.requiredClasses || props.event.requiredClasses.length === 0) {
    return 'Todas as classes'
  }
  return props.event.requiredClasses.map(c => PlayerClassAbbreviations[c] || c).join(', ')
})

function handleToggleParticipation() {
  if (isParticipating.value) {
    emit('leave', props.event.id)
  } else {
    emit('join', props.event.id)
  }
}

function handleDelete() {
  emit('delete', props.event.id)
}
</script>

<template>
  <div class="card" :class="{ 'opacity-60': isPast }">
    <div class="flex items-start justify-between gap-3">
      <div class="flex-1 min-w-0">
        <!-- Header -->
        <div class="flex items-center gap-2 mb-2">
          <span
            v-if="isPast"
            class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-500/20 text-gray-400"
          >
            Encerrado
          </span>
          <span
            v-else-if="isFull"
            class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-500/20 text-red-400"
          >
            Lotado
          </span>
          <span class="text-xs text-gray-500">{{ formattedDate }}</span>
        </div>

        <!-- Title -->
        <h4 class="font-semibold text-gray-100 mb-1">{{ event.title }}</h4>

        <!-- Description -->
        <p v-if="event.description" class="text-gray-400 text-sm mb-2">{{ event.description }}</p>

        <!-- Info -->
        <div class="flex flex-wrap gap-3 text-xs text-gray-500">
          <span class="flex items-center gap-1">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            {{ participantCount }} participantes
          </span>
          <span class="flex items-center gap-1">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            {{ requiredClassesText }}
          </span>
        </div>

        <!-- Participants Toggle -->
        <button
          v-if="event.participants.length > 0"
          @click="showParticipants = !showParticipants"
          class="mt-2 text-xs text-primary-400 hover:text-primary-300 flex items-center gap-1"
        >
          <svg
            class="w-4 h-4 transition-transform"
            :class="{ 'rotate-180': showParticipants }"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
          {{ showParticipants ? 'Ocultar' : 'Ver' }} participantes
        </button>

        <!-- Participants List -->
        <div v-if="showParticipants && event.participants.length > 0" class="mt-2 flex flex-wrap gap-1">
          <span
            v-for="participant in event.participants"
            :key="participant.id"
            class="inline-flex items-center px-2 py-0.5 rounded text-xs bg-dark-600 text-gray-300"
            :class="{ 'bg-primary-500/20 text-primary-400': participant.id === currentUserId }"
          >
            {{ participant.nick }}
            <span class="ml-1 text-gray-500">{{ PlayerClassAbbreviations[participant.playerClass] }}</span>
          </span>
        </div>

        <!-- Created By -->
        <p class="text-xs text-gray-500 mt-2">
          Por: {{ event.createdBy.nick }}
        </p>
      </div>

      <!-- Actions -->
      <div class="flex flex-col items-end gap-2 flex-shrink-0">
        <!-- Delete Button -->
        <button
          v-if="canDelete"
          @click="handleDelete"
          class="p-2 text-gray-500 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
          title="Deletar evento"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>

        <!-- Join/Leave Button -->
        <button
          v-if="!isPast"
          @click="handleToggleParticipation"
          class="btn text-sm"
          :class="isParticipating ? 'btn-secondary' : 'btn-primary'"
          :disabled="!isParticipating && isFull"
        >
          {{ isParticipating ? 'Sair' : 'Participar' }}
        </button>
      </div>
    </div>
  </div>
</template>
