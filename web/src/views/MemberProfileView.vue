<script setup lang="ts">
import { onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMembersStore } from '@/stores/members'
import MainLayout from '@/components/layout/MainLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import { Role, PlayerClass, PlayerClassNames } from '@/types'

const route = useRoute()
const router = useRouter()
const membersStore = useMembersStore()

const memberId = computed(() => route.params.id as string)

onMounted(() => {
  if (memberId.value) {
    membersStore.fetchMemberProfile(memberId.value)
  }
})

onUnmounted(() => {
  membersStore.clearSelectedMember()
})

function formatDate(isoDate: string): string {
  try {
    const date = new Date(isoDate)
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    })
  } catch {
    return '-'
  }
}

function goBack() {
  router.push('/members')
}
</script>

<template>
  <MainLayout>
    <div class="space-y-4">
      <!-- Header with back button -->
      <div class="flex items-center gap-3">
        <button
          @click="goBack"
          class="p-2 hover:bg-dark-600 rounded-lg transition-colors"
        >
          <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h2 class="text-xl font-bold text-gray-100">Perfil do Membro</h2>
      </div>

      <!-- Loading -->
      <div v-if="membersStore.loadingProfile" class="flex justify-center py-12">
        <LoadingSpinner />
      </div>

      <!-- Error -->
      <div v-else-if="membersStore.error" class="card bg-red-900/20 border border-red-500/30">
        <p class="text-red-400">{{ membersStore.error }}</p>
        <button
          @click="membersStore.fetchMemberProfile(memberId)"
          class="mt-4 btn btn-primary"
        >
          Tentar novamente
        </button>
      </div>

      <!-- Profile -->
      <template v-else-if="membersStore.selectedMember">
        <div class="card">
          <!-- Avatar and Basic Info -->
          <div class="flex items-center gap-4 mb-6">
            <div
              class="w-20 h-20 rounded-full flex items-center justify-center text-white font-bold text-2xl"
              :class="membersStore.selectedMember.role === Role.LEADER ? 'bg-primary-500' : 'bg-dark-500'"
            >
              {{ membersStore.selectedMember.nick.charAt(0).toUpperCase() }}
            </div>

            <div class="flex-1">
              <div class="flex items-center gap-2">
                <h3
                  class="text-2xl font-bold"
                  :class="membersStore.selectedMember.role === Role.LEADER ? 'text-primary-400' : 'text-gray-100'"
                >
                  {{ membersStore.selectedMember.nick }}
                </h3>
                <span v-if="membersStore.selectedMember.role === Role.LEADER" class="text-xl">👑</span>
              </div>
              <span
                class="inline-block mt-1 px-3 py-1 text-sm rounded-full"
                :class="membersStore.selectedMember.role === Role.LEADER
                  ? 'bg-primary-500/20 text-primary-400'
                  : 'bg-dark-500 text-gray-400'"
              >
                {{ membersStore.selectedMember.role === Role.LEADER ? 'Líder' : 'Membro' }}
              </span>
            </div>
          </div>

          <!-- Details -->
          <div class="space-y-4">
            <div class="flex justify-between items-center py-3 border-b border-dark-600">
              <span class="text-gray-400">Classe</span>
              <span class="text-gray-100 font-medium">
                {{ PlayerClassNames[membersStore.selectedMember.playerClass as PlayerClass] }}
              </span>
            </div>

            <div class="flex justify-between items-center py-3 border-b border-dark-600">
              <span class="text-gray-400">Membro desde</span>
              <span class="text-gray-100 font-medium">
                {{ formatDate(membersStore.selectedMember.createdAt) }}
              </span>
            </div>

            <div v-if="membersStore.selectedMember.approvedAt" class="flex justify-between items-center py-3 border-b border-dark-600">
              <span class="text-gray-400">Aprovado em</span>
              <span class="text-gray-100 font-medium">
                {{ formatDate(membersStore.selectedMember.approvedAt) }}
              </span>
            </div>
          </div>
        </div>
      </template>

      <!-- Not found -->
      <div v-else class="card text-center py-8">
        <svg class="w-16 h-16 mx-auto text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
        <p class="text-gray-400">Membro não encontrado</p>
        <button @click="goBack" class="mt-4 btn btn-primary">
          Voltar
        </button>
      </div>
    </div>
  </MainLayout>
</template>
