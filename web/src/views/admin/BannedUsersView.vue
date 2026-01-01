<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMembersStore } from '@/stores/members'
import MainLayout from '@/components/layout/MainLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { PlayerClass, PlayerClassAbbreviations } from '@/types'
import type { BannedUser } from '@/types'

const membersStore = useMembersStore()

const userToUnban = ref<BannedUser | null>(null)

onMounted(() => {
  membersStore.fetchBannedUsers()
})

function formatDate(isoDate: string): string {
  try {
    const date = new Date(isoDate)
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    })
  } catch {
    return '-'
  }
}

async function handleUnban() {
  if (userToUnban.value) {
    await membersStore.unbanUser(userToUnban.value.id)
    userToUnban.value = null
  }
}
</script>

<template>
  <MainLayout>
    <div class="space-y-4">
      <h2 class="text-xl font-bold text-gray-100">Usuários Banidos</h2>

      <!-- Loading -->
      <div v-if="membersStore.loadingBanned && membersStore.bannedUsers.length === 0" class="flex justify-center py-12">
        <LoadingSpinner />
      </div>

      <!-- Error -->
      <div v-else-if="membersStore.error && membersStore.bannedUsers.length === 0" class="card bg-red-900/20 border border-red-500/30">
        <p class="text-red-400">{{ membersStore.error }}</p>
        <button
          @click="membersStore.fetchBannedUsers()"
          class="mt-4 btn btn-primary"
        >
          Tentar novamente
        </button>
      </div>

      <!-- Empty -->
      <div v-else-if="membersStore.bannedUsers.length === 0" class="card text-center py-8">
        <svg class="w-16 h-16 mx-auto text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p class="text-gray-400">Nenhum usuário banido</p>
      </div>

      <!-- Banned Users List -->
      <div v-else class="space-y-2">
        <p class="text-sm text-gray-400">
          {{ membersStore.bannedUsers.length }} usuário(s) banido(s)
        </p>

        <div
          v-for="user in membersStore.bannedUsers"
          :key="user.id"
          class="card border-l-4 border-l-red-500"
        >
          <div class="flex items-center gap-3">
            <!-- Avatar -->
            <div class="w-12 h-12 rounded-full bg-red-500/20 flex items-center justify-center text-red-400 font-bold text-lg">
              {{ user.nick.charAt(0).toUpperCase() }}
            </div>

            <!-- Info -->
            <div class="flex-1 min-w-0">
              <span class="font-semibold text-gray-100 truncate block">
                {{ user.nick }}
              </span>
              <div class="flex items-center gap-2 text-sm text-gray-400">
                <span>{{ PlayerClassAbbreviations[user.playerClass as PlayerClass] }}</span>
                <span>•</span>
                <span>Banido em {{ formatDate(user.createdAt) }}</span>
              </div>
            </div>

            <!-- Unban Action -->
            <button
              @click="userToUnban = user"
              :disabled="membersStore.unbanningId === user.id"
              class="px-4 py-2 rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors disabled:opacity-50 flex items-center gap-2"
            >
              <LoadingSpinner v-if="membersStore.unbanningId === user.id" size="sm" />
              <template v-else>
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>Desbanir</span>
              </template>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Error toast -->
    <div
      v-if="membersStore.error && membersStore.bannedUsers.length > 0"
      class="fixed bottom-20 left-4 right-4 bg-red-500/90 text-white px-4 py-3 rounded-lg flex items-center justify-between z-50"
    >
      <span>{{ membersStore.error }}</span>
      <button @click="membersStore.clearError()" class="ml-4 font-bold">OK</button>
    </div>

    <!-- Unban Dialog -->
    <ConfirmDialog
      v-if="userToUnban"
      title="Desbanir Usuário"
      :message="`Tem certeza que deseja desbanir ${userToUnban.nick}? O usuário poderá solicitar entrada novamente.`"
      confirm-text="Desbanir"
      @confirm="handleUnban"
      @cancel="userToUnban = null"
    />
  </MainLayout>
</template>
