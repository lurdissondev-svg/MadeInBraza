<script setup lang="ts">
import { computed, ref } from 'vue'
import { Role, PlayerClass, PlayerClassAbbreviations } from '@/types'
import type { Member } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps<{
  member: Member
  isLeader: boolean
  currentUserId: string | undefined
  promotingId: string | null
  demotingId: string | null
  updatingRoleId: string | null
  banningId: string | null
}>()

const emit = defineEmits<{
  click: []
  promote: []
  demote: []
  updateRole: [role: Role]
  ban: []
}>()

const isMemberLeader = computed(() => props.member.role === Role.LEADER)
const isMemberCounselor = computed(() => props.member.role === Role.COUNSELOR)
const isMemberRegular = computed(() => props.member.role === Role.MEMBER)
const isSelf = computed(() => props.member.id === props.currentUserId)

// Can change role if leader and not self
const canChangeRole = computed(() => props.isLeader && !isSelf.value)
const canBan = computed(() => props.isLeader && !isMemberLeader.value && !isSelf.value)

const isPromoting = computed(() => props.promotingId === props.member.id)
const isDemoting = computed(() => props.demotingId === props.member.id)
const isUpdatingRole = computed(() => props.updatingRoleId === props.member.id)
const isBanning = computed(() => props.banningId === props.member.id)
const isAnyActionInProgress = computed(() => isPromoting.value || isDemoting.value || isUpdatingRole.value || isBanning.value)

// Role menu state
const showRoleMenu = ref(false)

// Track if avatar image failed to load
const avatarError = ref(false)

const avatarUrl = computed(() => {
  if (avatarError.value) return null
  const url = props.member.avatarUrl
  if (!url) return null
  const baseUrl = import.meta.env.VITE_API_URL?.replace('/api', '') || ''
  return `${baseUrl}${url}`
})

function handleAvatarError() {
  avatarError.value = true
}

function toggleRoleMenu(event: Event) {
  event.stopPropagation()
  showRoleMenu.value = !showRoleMenu.value
}

function closeRoleMenu() {
  showRoleMenu.value = false
}

function setRole(role: Role, event: Event) {
  event.stopPropagation()
  if (role !== props.member.role) {
    emit('updateRole', role)
  }
  closeRoleMenu()
}

// Role display info
const roleInfo = computed(() => {
  switch (props.member.role) {
    case Role.LEADER:
      return { label: 'Líder', emoji: '👑', class: 'text-primary-400', bgClass: 'bg-primary-500/20 text-primary-400' }
    case Role.COUNSELOR:
      return { label: 'Conselheiro', emoji: '⭐', class: 'text-amber-400', bgClass: 'bg-amber-500/20 text-amber-400' }
    default:
      return { label: 'Membro', emoji: '', class: 'text-gray-100', bgClass: 'bg-dark-500 text-gray-400' }
  }
})
</script>

<template>
  <div
    class="card cursor-pointer transition-colors hover:bg-dark-600"
    :class="isMemberLeader ? 'border-l-4 border-l-primary-500 bg-primary-500/5' : isMemberCounselor ? 'border-l-4 border-l-amber-500 bg-amber-500/5' : ''"
    @click="emit('click')"
  >
    <div class="flex items-center gap-3">
      <!-- Avatar -->
      <div
        class="w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-lg overflow-hidden"
        :class="avatarUrl ? '' : isMemberLeader ? 'bg-primary-600' : isMemberCounselor ? 'bg-amber-600' : 'bg-dark-500'"
      >
        <img
          v-if="avatarUrl"
          :src="avatarUrl"
          alt="Avatar"
          loading="lazy"
          decoding="async"
          class="w-full h-full object-cover"
          @error="handleAvatarError"
        />
        <span v-else>{{ member.nick.charAt(0).toUpperCase() }}</span>
      </div>

      <!-- Info -->
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2">
          <span
            class="font-semibold truncate"
            :class="roleInfo.class"
          >
            {{ member.nick }}
          </span>
          <span v-if="roleInfo.emoji" class="text-sm">{{ roleInfo.emoji }}</span>
          <span v-if="isSelf" class="text-xs text-gray-500">(você)</span>
        </div>
        <span class="text-sm text-gray-400">
          {{ PlayerClassAbbreviations[member.playerClass as PlayerClass] }}
        </span>
      </div>

      <!-- Actions (for leaders) -->
      <div v-if="canChangeRole || canBan" class="flex items-center gap-1">
        <!-- Role change button with dropdown -->
        <div v-if="canChangeRole" class="relative">
          <button
            @click="toggleRoleMenu"
            :disabled="isAnyActionInProgress"
            class="p-2 rounded-lg hover:bg-dark-500 transition-colors disabled:opacity-50 flex items-center gap-1"
            :title="'Alterar cargo'"
          >
            <LoadingSpinner v-if="isUpdatingRole" size="sm" />
            <template v-else>
              <svg class="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <svg class="w-3 h-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </template>
          </button>

          <!-- Role dropdown menu -->
          <Transition name="fade">
            <div
              v-if="showRoleMenu"
              class="absolute right-0 top-full mt-1 w-40 bg-dark-700 border border-dark-500 rounded-lg shadow-xl z-50 overflow-hidden"
              @click.stop
            >
              <div class="py-1">
                <button
                  v-if="!isMemberLeader"
                  @click="(e) => setRole(Role.LEADER, e)"
                  class="w-full px-3 py-2 text-left text-sm hover:bg-dark-600 flex items-center gap-2"
                >
                  <span>👑</span>
                  <span class="text-primary-400">Líder</span>
                </button>
                <button
                  v-if="!isMemberCounselor"
                  @click="(e) => setRole(Role.COUNSELOR, e)"
                  class="w-full px-3 py-2 text-left text-sm hover:bg-dark-600 flex items-center gap-2"
                >
                  <span>⭐</span>
                  <span class="text-amber-400">Conselheiro</span>
                </button>
                <button
                  v-if="!isMemberRegular"
                  @click="(e) => setRole(Role.MEMBER, e)"
                  class="w-full px-3 py-2 text-left text-sm hover:bg-dark-600 flex items-center gap-2"
                >
                  <span>👤</span>
                  <span class="text-gray-300">Membro</span>
                </button>
              </div>
            </div>
          </Transition>
        </div>

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
          :class="roleInfo.bgClass"
        >
          {{ roleInfo.label }}
        </span>
      </div>
    </div>
  </div>

  <!-- Click outside to close menu -->
  <Teleport to="body">
    <div
      v-if="showRoleMenu"
      class="fixed inset-0 z-40"
      @click="closeRoleMenu"
    />
  </Teleport>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
