<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useSiegeWarStore } from '@/stores/siegeWar'
import MainLayout from '@/components/layout/MainLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import SiegeWarHeader from '@/components/siege-war/SiegeWarHeader.vue'
import ResponseForm from '@/components/siege-war/ResponseForm.vue'
import LeaderPanel from '@/components/siege-war/LeaderPanel.vue'
import HistoryTab from '@/components/siege-war/HistoryTab.vue'
import type { SubmitSWResponseRequest } from '@/types'

const authStore = useAuthStore()
const siegeWarStore = useSiegeWarStore()

type TabId = 'response' | 'responses' | 'history'
const activeTab = ref<TabId>('response')

const isLeader = computed(() =>
  authStore.user?.role === 'LEADER'
)

const tabs = computed(() => {
  const baseTabs: { id: TabId; label: string }[] = [
    { id: 'response', label: 'Minha Resposta' },
    { id: 'history', label: 'Histórico' }
  ]

  if (isLeader.value) {
    baseTabs.splice(1, 0, { id: 'responses', label: 'Respostas' })
  }

  return baseTabs
})

onMounted(async () => {
  await siegeWarStore.fetchCurrentSiegeWar()
  siegeWarStore.fetchHistory()
})

// Load responses when leader tab is selected
watch(activeTab, (tab) => {
  if (tab === 'responses' && siegeWarStore.siegeWar) {
    siegeWarStore.fetchResponses()
  }
})

async function handleCreateSiegeWar() {
  await siegeWarStore.createSiegeWar()
}

async function handleSubmitResponse(data: SubmitSWResponseRequest) {
  const success = await siegeWarStore.submitResponse(data)
  if (success) {
    // Optionally show toast or feedback
  }
}

async function handleLoadShares() {
  await siegeWarStore.fetchAvailableShares()
}

async function handleCloseSiegeWar() {
  await siegeWarStore.closeSiegeWar()
}
</script>

<template>
  <MainLayout>
    <div class="space-y-4">
      <!-- Loading -->
      <div v-if="siegeWarStore.loading && !siegeWarStore.siegeWar" class="flex justify-center py-12">
        <LoadingSpinner />
      </div>

      <!-- Error -->
      <div v-else-if="siegeWarStore.error" class="card bg-red-900/20 border border-red-500/30">
        <p class="text-red-400">{{ siegeWarStore.error }}</p>
        <button
          @click="siegeWarStore.fetchCurrentSiegeWar()"
          class="mt-4 btn btn-primary"
        >
          Tentar novamente
        </button>
      </div>

      <!-- No Siege War -->
      <template v-else-if="!siegeWarStore.siegeWar">
        <div class="card text-center py-8">
          <svg class="w-16 h-16 mx-auto text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
          <p class="text-gray-400 mb-4">Nenhum Siege War ativo no momento</p>
          <button
            v-if="isLeader"
            @click="handleCreateSiegeWar"
            :disabled="siegeWarStore.loading"
            class="btn btn-primary"
          >
            <LoadingSpinner v-if="siegeWarStore.loading" size="sm" class="mr-2" />
            Criar Novo Siege War
          </button>
        </div>

        <!-- Show history even when no active SW -->
        <div class="mt-6">
          <h3 class="text-lg font-semibold text-gray-100 mb-4">Histórico</h3>
          <HistoryTab
            :history="siegeWarStore.history"
            :loading="siegeWarStore.loadingHistory"
          />
        </div>
      </template>

      <!-- Active Siege War -->
      <template v-else>
        <!-- Header -->
        <SiegeWarHeader :siege-war="siegeWarStore.siegeWar" />

        <!-- Tabs -->
        <div class="flex border-b border-dark-600">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            class="flex-1 py-3 text-sm font-medium transition-colors relative"
            :class="activeTab === tab.id
              ? 'text-primary-400'
              : 'text-gray-400 hover:text-gray-200'"
          >
            {{ tab.label }}
            <div
              v-if="activeTab === tab.id"
              class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary-500"
            />
          </button>
        </div>

        <!-- Tab Content -->
        <div class="mt-4">
          <!-- My Response Tab -->
          <div v-if="activeTab === 'response'">
            <ResponseForm
              :user-response="siegeWarStore.userResponse"
              :available-shares="siegeWarStore.availableShares"
              :is-active="siegeWarStore.siegeWar.isActive"
              :submitting="siegeWarStore.submitting"
              @submit="handleSubmitResponse"
              @load-shares="handleLoadShares"
            />
          </div>

          <!-- Responses Tab (Leaders only) -->
          <div v-else-if="activeTab === 'responses'">
            <div v-if="siegeWarStore.loading" class="flex justify-center py-12">
              <LoadingSpinner />
            </div>
            <LeaderPanel
              v-else
              :summary="siegeWarStore.summary"
              :responses="siegeWarStore.responses"
              :not-responded="siegeWarStore.notResponded"
              :is-active="siegeWarStore.siegeWar.isActive"
              @close="handleCloseSiegeWar"
            />
          </div>

          <!-- History Tab -->
          <div v-else-if="activeTab === 'history'">
            <HistoryTab
              :history="siegeWarStore.history"
              :loading="siegeWarStore.loadingHistory"
            />
          </div>
        </div>
      </template>
    </div>
  </MainLayout>
</template>
