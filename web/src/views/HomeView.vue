<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAnnouncementsStore } from '@/stores/announcements'
import { PlayerClassNames } from '@/types'
import MainLayout from '@/components/layout/MainLayout.vue'
import AnnouncementCard from '@/components/announcements/AnnouncementCard.vue'
import CreateAnnouncementModal from '@/components/announcements/CreateAnnouncementModal.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const authStore = useAuthStore()
const announcementsStore = useAnnouncementsStore()

const showCreateModal = ref(false)

const userClassName = computed(() => {
  if (!authStore.user?.playerClass) return ''
  return PlayerClassNames[authStore.user.playerClass]
})

const userRole = computed(() => {
  return authStore.isLeader ? 'Líder' : 'Membro'
})

onMounted(() => {
  announcementsStore.fetchAnnouncements()
})

function handleRefresh() {
  announcementsStore.fetchAnnouncements()
}

async function handleDelete(id: string) {
  if (confirm('Tem certeza que deseja deletar este aviso?')) {
    await announcementsStore.deleteAnnouncement(id)
  }
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Welcome Card - igual Android -->
      <div class="bg-dark-700 rounded-xl p-4">
        <h2 class="text-xl font-semibold text-white mb-2">
          Bem-vindo, {{ authStore.user?.nick }}!
        </h2>
        <p class="text-gray-300">
          Classe: {{ userClassName }}
        </p>
        <p class="text-gray-300">
          Cargo: {{ userRole }}
        </p>
      </div>

      <!-- Avisos Section -->
      <div>
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-white">Avisos</h3>
          <div class="flex items-center gap-2">
            <button
              @click="handleRefresh"
              class="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-dark-600 transition-colors"
              :disabled="announcementsStore.loading"
            >
              <svg
                class="w-5 h-5"
                :class="{ 'animate-spin': announcementsStore.loading }"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
            <button
              v-if="authStore.isLeader"
              @click="showCreateModal = true"
              class="px-4 py-2 bg-white text-black font-medium rounded-lg hover:bg-gray-200 transition-colors flex items-center gap-2"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              Novo
            </button>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="announcementsStore.loading && announcementsStore.announcements.length === 0" class="flex justify-center py-8">
          <LoadingSpinner />
        </div>

        <!-- Error State -->
        <div v-else-if="announcementsStore.error" class="bg-red-900/20 border border-red-500/30 rounded-xl p-4">
          <p class="text-red-400 text-center">{{ announcementsStore.error }}</p>
          <button @click="handleRefresh" class="mt-3 mx-auto block px-4 py-2 bg-dark-600 text-white rounded-lg hover:bg-dark-500">
            Tentar novamente
          </button>
        </div>

        <!-- Empty State -->
        <div v-else-if="announcementsStore.announcements.length === 0" class="bg-dark-700 rounded-xl text-center py-8">
          <svg class="w-12 h-12 mx-auto text-gray-500 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z" />
          </svg>
          <p class="text-gray-400">Nenhum aviso disponível</p>
        </div>

        <!-- Announcements List -->
        <div v-else class="space-y-3">
          <AnnouncementCard
            v-for="announcement in announcementsStore.announcements"
            :key="announcement.id"
            :announcement="announcement"
            :can-delete="authStore.isLeader"
            @delete="handleDelete"
          />
        </div>
      </div>
    </div>

    <!-- Create Announcement Modal -->
    <CreateAnnouncementModal
      v-model:show="showCreateModal"
    />
  </MainLayout>
</template>
