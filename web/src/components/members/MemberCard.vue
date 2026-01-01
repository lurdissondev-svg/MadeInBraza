<script setup lang="ts">
import { computed } from 'vue'
import { Role, PlayerClass, PlayerClassAbbreviations } from '@/types'
import type { Member } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps<{
  member: Member
  isLeader: boolean
  currentUserId: string | undefined
  promotingId: string | null
  demotingId: string | null
  banningId: string | null
}>()

const emit = defineEmits<{
  click: []
  promote: []
  demote: []
  ban: []
}>()

const isMemberLeader = computed(() => props.member.role === Role.LEADER)
const isSelf = computed(() => props.member.id === props.currentUserId)
const canPromote = computed(() => props.isLeader && !isMemberLeader.value && !isSelf.value)
const canDemote = computed(() => props.isLeader && isMemberLeader.value && !isSelf.value)
const canBan = computed(() => props.isLeader && !isMemberLeader.value && !isSelf.value)

const isPromoting = computed(() => props.promotingId === props.member.id)
const isDemoting = computed(() => props.demotingId === props.member.id)
const isBanning = computed(() => props.banningId === props.member.id)

const avatarUrl = computed(() => {
  const url = props.member.avatarUrl
  if (!url) return null
  const baseUrl = import.meta.env.VITE_API_URL?.replace('/api', '') || ''
  return `${baseUrl}${url}`
})
</script>

<template>
  <div
    class="card cursor-pointer transition-colors hover:bg-dark-600"
    :class="isMemberLeader ? 'border-l-4 border-l-primary-500 bg-primary-500/5' : ''"
    @click="emit('click')"
  >
    <div class="flex items-center gap-3">
      <!-- Avatar -->
      <div
        class="w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-lg overflow-hidden"
        :class="avatarUrl ? '' : (isMemberLeader ? 'bg-primary-500' : 'bg-dark-500')"
      >
        <img
          v-if="avatarUrl"
          :src="avatarUrl"
          alt="Avatar"
          class="w-full h-full object-cover"
        />
        <span v-else>{{ member.nick.charAt(0).toUpperCase() }}</span>
      </div>

      <!-- Info -->
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2">
          <span
            class="font-semibold truncate"
            :class="isMemberLeader ? 'text-primary-400' : 'text-gray-100'"
          >
            {{ member.nick }}
          </span>
          <span v-if="isMemberLeader" class="text-sm">👑</span>
          <span v-if="isSelf" class="text-xs text-gray-500">(você)</span>
        </div>
        <span class="text-sm text-gray-400">
          {{ PlayerClassAbbreviations[member.playerClass as PlayerClass] }}
        </span>
      </div>

      <!-- Actions (for leaders) -->
      <div v-if="canPromote || canDemote || canBan" class="flex items-center gap-1">
        <!-- Promote button -->
        <button
          v-if="canPromote"
          @click.stop="emit('promote')"
          :disabled="isPromoting"
          class="p-2 rounded-lg hover:bg-dark-500 transition-colors disabled:opacity-50"
          title="Promover a líder"
        >
          <LoadingSpinner v-if="isPromoting" size="sm" />
          <svg v-else class="w-5 h-5 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
          </svg>
        </button>

        <!-- Demote button -->
        <button
          v-if="canDemote"
          @click.stop="emit('demote')"
          :disabled="isDemoting"
          class="p-2 rounded-lg hover:bg-dark-500 transition-colors disabled:opacity-50"
          title="Rebaixar para membro"
        >
          <LoadingSpinner v-if="isDemoting" size="sm" />
          <svg v-else class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
          </svg>
        </button>

        <!-- Ban button -->
        <button
          v-if="canBan"
          @click.stop="emit('ban')"
          :disabled="isBanning"
          class="p-2 rounded-lg hover:bg-dark-500 transition-colors disabled:opacity-50"
          title="Banir membro"
        >
          <LoadingSpinner v-if="isBanning" size="sm" />
          <svg v-else class="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
          </svg>
        </button>
      </div>

      <!-- Role badge (for non-leaders viewing) -->
      <div v-else class="flex items-center">
        <span
          class="px-2 py-1 text-xs rounded-full"
          :class="isMemberLeader ? 'bg-primary-500/20 text-primary-400' : 'bg-dark-500 text-gray-400'"
        >
          {{ isMemberLeader ? 'Líder' : 'Membro' }}
        </span>
      </div>
    </div>
  </div>
</template>
