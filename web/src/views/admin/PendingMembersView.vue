<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMembersStore } from '@/stores/members'
import MainLayout from '@/components/layout/MainLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { PlayerClass, PlayerClassAbbreviations } from '@/types'
import type { PendingUser } from '@/types'

const membersStore = useMembersStore()

const userToApprove = ref<PendingUser | null>(null)
const userToReject = ref<PendingUser | null>(null)

onMounted(() => {
  membersStore.fetchPendingUsers()
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

async function handleApprove() {
  if (userToApprove.value) {
    await membersStore.approveUser(userToApprove.value.id)
    userToApprove.value = null
  }
}

async function handleReject() {
  if (userToReject.value) {
    await membersStore.rejectUser(userToReject.value.id)
    userToReject.value = null
  }
}
</script>

<template>
  <MainLayout>
    <div class="space-y-4">
      <h2 class="text-xl font-bold text-gray-100">Membros Pendentes</h2>

      <!-- Loading -->
      <div v-if="membersStore.loadingPending && membersStore.pendingUsers.length === 0" class="flex justify-center py-12">
        <LoadingSpinner />
      </div>

      <!-- Error -->
      <div v-else-if="membersStore.error && membersStore.pendingUsers.length === 0" class="card bg-red-900/20 border border-red-500/30">
        <p class="text-red-400">{{ membersStore.error }}</p>
        <button
          @click="membersStore.fetchPendingUsers()"
          class="mt-4 btn btn-primary"
        >
          Tentar novamente
        </button>
      </div>

      <!-- Empty -->
      <div v-else-if="membersStore.pendingUsers.length === 0" class="card text-center py-8">
        <svg class="w-16 h-16 mx-auto text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p class="text-gray-400">Nenhum membro pendente de aprovação</p>
      </div>

      <!-- Pending Users List -->
      <div v-else class="space-y-2">
        <p class="text-sm text-gray-400">
          {{ membersStore.pendingUsers.length }} usuário(s) aguardando aprovação
        </p>

        <div
          v-for="user in membersStore.pendingUsers"
          :key="user.id"
          class="card"
        >
          <div class="flex items-center gap-3">
            <!-- Avatar -->
            <div class="w-12 h-12 rounded-full bg-yellow-500/20 flex items-center justify-center text-yellow-400 font-bold text-lg">
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
                <span>{{ formatDate(user.createdAt) }}</span>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex items-center gap-2">
              <button
                @click="userToReject = user"
                :disabled="membersStore.rejectingId === user.id"
                class="p-2 rounded-lg hover:bg-red-500/20 transition-colors disabled:opacity-50"
                title="Rejeitar"
              >
                <LoadingSpinner v-if="membersStore.rejectingId === user.id" size="sm" />
                <svg v-else class="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
              <button
                @click="userToApprove = user"
                :disabled="membersStore.approvingId === user.id"
                class="p-2 rounded-lg hover:bg-green-500/20 transition-colors disabled:opacity-50"
                title="Aprovar"
              >
                <LoadingSpinner v-if="membersStore.approvingId === user.id" size="sm" />
                <svg v-else class="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Error toast -->
    <div
      v-if="membersStore.error && membersStore.pendingUsers.length > 0"
      class="fixed bottom-20 left-4 right-4 bg-red-500/90 text-white px-4 py-3 rounded-lg flex items-center justify-between z-50"
    >
      <span>{{ membersStore.error }}</span>
      <button @click="membersStore.clearError()" class="ml-4 font-bold">OK</button>
    </div>

    <!-- Approve Dialog -->
    <ConfirmDialog
      v-if="userToApprove"
      title="Aprovar Membro"
      :message="`Tem certeza que deseja aprovar ${userToApprove.nick}?`"
      confirm-text="Aprovar"
      @confirm="handleApprove"
      @cancel="userToApprove = null"
    />

    <!-- Reject Dialog -->
    <ConfirmDialog
      v-if="userToReject"
      title="Rejeitar Solicitação"
      :message="`Tem certeza que deseja rejeitar a solicitação de ${userToReject.nick}?`"
      confirm-text="Rejeitar"
      confirm-variant="danger"
      @confirm="handleReject"
      @cancel="userToReject = null"
    />
  </MainLayout>
</template>
