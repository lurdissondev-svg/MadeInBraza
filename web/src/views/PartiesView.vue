<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePartiesStore } from '@/stores/parties'
import type { Party } from '@/types'
import MainLayout from '@/components/layout/MainLayout.vue'
import PartyCard from '@/components/parties/PartyCard.vue'
import CreatePartyModal from '@/components/parties/CreatePartyModal.vue'
import JoinPartyModal from '@/components/parties/JoinPartyModal.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const authStore = useAuthStore()
const partiesStore = usePartiesStore()

const showCreateModal = ref(false)
const showJoinModal = ref(false)
const selectedParty = ref<Party | null>(null)

onMounted(() => {
  partiesStore.fetchGlobalParties()
})

function handleRefresh() {
  partiesStore.fetchGlobalParties()
}

async function handleDelete(id: string) {
  if (confirm('Tem certeza que deseja deletar esta party?')) {
    await partiesStore.deleteParty(id)
  }
}

function handleJoinClick(party: Party) {
  selectedParty.value = party
  showJoinModal.value = true
}

async function handleJoin(partyId: string, slotId: string) {
  const success = await partiesStore.joinParty(partyId, slotId)
  if (success) {
    showJoinModal.value = false
    selectedParty.value = null
  }
}

async function handleLeave(id: string) {
  await partiesStore.leaveParty(id)
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <button
            @click="handleRefresh"
            class="p-2 rounded-lg text-gray-400 hover:text-gray-200 hover:bg-dark-600 transition-colors"
            :disabled="partiesStore.loading"
          >
            <svg
              class="w-5 h-5"
              :class="{ 'animate-spin': partiesStore.loading }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
        <button
          @click="showCreateModal = true"
          class="btn btn-primary text-sm"
        >
          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          Nova Party
        </button>
      </div>

      <!-- Loading State -->
      <div v-if="partiesStore.loading && partiesStore.globalParties.length === 0" class="flex justify-center py-8">
        <LoadingSpinner />
      </div>

      <!-- Error State -->
      <div v-else-if="partiesStore.error" class="card bg-red-900/20 border border-red-500/30">
        <p class="text-red-400 text-center">{{ partiesStore.error }}</p>
        <button @click="handleRefresh" class="btn btn-secondary mt-3 mx-auto block">
          Tentar novamente
        </button>
      </div>

      <!-- Empty State -->
      <div v-else-if="partiesStore.globalParties.length === 0" class="card text-center py-8">
        <svg class="w-12 h-12 mx-auto text-gray-500 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
        <p class="text-gray-400">Nenhuma party disponível</p>
        <p class="text-gray-500 text-sm mt-1">Crie uma party para começar!</p>
      </div>

      <!-- Parties Grid -->
      <div v-else class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <PartyCard
          v-for="party in partiesStore.globalParties"
          :key="party.id"
          :party="party"
          :current-user-id="authStore.user?.id"
          :can-delete="authStore.isLeader || party.createdBy.id === authStore.user?.id"
          @join="handleJoinClick"
          @leave="handleLeave"
          @delete="handleDelete"
        />
      </div>
    </div>

    <!-- Create Party Modal -->
    <CreatePartyModal v-model:show="showCreateModal" />

    <!-- Join Party Modal -->
    <JoinPartyModal
      v-model:show="showJoinModal"
      :party="selectedParty"
      @join="handleJoin"
    />
  </MainLayout>
</template>
