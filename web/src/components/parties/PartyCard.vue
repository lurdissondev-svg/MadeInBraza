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
  join: [party: Party]
  leave: [id: string]
  delete: [id: string]
}>()

const isParticipating = computed(() => {
  if (!props.currentUserId) return false
  return props.party.slots.some(s => s.filledBy?.id === props.currentUserId)
})

const totalSlots = computed(() => props.party.slots.length)
const filledSlots = computed(() => props.party.slots.filter(s => s.filledBy).length)

const isFull = computed(() => filledSlots.value >= totalSlots.value)

const memberCount = computed(() => `${filledSlots.value}/${totalSlots.value}`)

// Group slots by class for display
const slotsByClass = computed(() => {
  const groups = new Map<string, { total: number; filled: number; members: { nick: string; id: string }[] }>()

  props.party.slots.forEach(slot => {
    const existing = groups.get(slot.playerClass) || { total: 0, filled: 0, members: [] }
    existing.total++
    if (slot.filledBy) {
      existing.filled++
      existing.members.push({ nick: slot.filledBy.nick, id: slot.filledBy.id })
    }
    groups.set(slot.playerClass, existing)
  })

  return Array.from(groups.entries()).map(([playerClass, data]) => ({
    playerClass,
    abbr: PlayerClassAbbreviations[playerClass as keyof typeof PlayerClassAbbreviations],
    ...data
  }))
})

function handleJoin() {
  emit('join', props.party)
}

function handleLeave() {
  emit('leave', props.party.id)
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
        <span>Vagas</span>
        <span>{{ memberCount }}</span>
      </div>
      <div class="h-2 bg-dark-600 rounded-full overflow-hidden">
        <div
          class="h-full transition-all duration-300"
          :class="isFull ? 'bg-red-500' : 'bg-primary-500'"
          :style="{ width: `${(filledSlots / totalSlots) * 100}%` }"
        />
      </div>
    </div>

    <!-- Slots by Class -->
    <div class="flex flex-wrap gap-1.5 mb-3">
      <div
        v-for="group in slotsByClass"
        :key="group.playerClass"
        class="flex items-center gap-1 px-2 py-1 rounded text-xs bg-dark-600"
        :class="group.filled === group.total ? 'opacity-50' : ''"
      >
        <span class="font-medium text-gray-300">{{ group.abbr }}</span>
        <span class="text-gray-500">{{ group.filled }}/{{ group.total }}</span>
      </div>
    </div>

    <!-- Members List (filled slots) -->
    <div v-if="filledSlots > 0" class="flex flex-wrap gap-1 mb-3">
      <template v-for="group in slotsByClass" :key="group.playerClass">
        <span
          v-for="member in group.members"
          :key="member.id"
          class="inline-flex items-center px-2 py-0.5 rounded text-xs bg-dark-600 text-gray-300"
          :class="{ 'bg-primary-500/20 text-primary-400': member.id === currentUserId }"
        >
          {{ member.nick }}
          <span class="ml-1 text-gray-500">{{ group.abbr }}</span>
        </span>
      </template>
    </div>

    <!-- Spacer -->
    <div class="flex-1" />

    <!-- Join/Leave Button -->
    <button
      v-if="isParticipating"
      @click="handleLeave"
      class="btn btn-secondary w-full text-sm"
    >
      Sair da Party
    </button>
    <button
      v-else
      @click="handleJoin"
      class="btn btn-primary w-full text-sm"
      :disabled="isFull || party.isClosed"
    >
      {{ isFull ? 'Party Lotada' : 'Entrar' }}
    </button>
  </div>
</template>
