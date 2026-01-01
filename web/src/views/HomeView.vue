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
      <!-- Welcome Card -->
      <div class="card">
        <div class="flex items-center gap-4">
          <div class="w-14 h-14 sm:w-16 sm:h-16 rounded-full bg-primary-500/20 flex items-center justify-center flex-shrink-0">
            <span class="text-2xl sm:text-3xl font-bold text-primary-400">
              {{ authStore.user?.nick?.charAt(0).toUpperCase() }}
            </span>
          </div>
          <div class="min-w-0">
            <h2 class="text-xl sm:text-2xl font-bold text-gray-100 truncate">
              Olá, {{ authStore.user?.nick }}!
            </h2>
            <p class="text-gray-400 text-sm sm:text-base">
              {{ userClassName }} • Made in Braza
            </p>
          </div>
        </div>
      </div>

      <!-- Announcements Section -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-lg font-semibold text-gray-100">Avisos</h3>
          <div class="flex items-center gap-2">
            <button
              @click="handleRefresh"
              class="p-2 rounded-lg text-gray-400 hover:text-gray-200 hover:bg-dark-600 transition-colors"
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
              class="btn btn-primary text-sm"
            >
              <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
        <div v-else-if="announcementsStore.error" class="card bg-red-900/20 border border-red-500/30">
          <p class="text-red-400 text-center">{{ announcementsStore.error }}</p>
          <button @click="handleRefresh" class="btn btn-secondary mt-3 mx-auto block">
            Tentar novamente
          </button>
        </div>

        <!-- Empty State -->
        <div v-else-if="announcementsStore.announcements.length === 0" class="card text-center py-8">
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

      <!-- Quick Actions -->
      <div>
        <h3 class="text-lg font-semibold text-gray-100 mb-3">Acesso Rápido</h3>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <router-link
            to="/siege-war"
            class="card flex flex-col items-center justify-center py-6 hover:bg-dark-600 transition-colors"
          >
            <svg class="w-8 h-8 text-primary-400 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="text-sm font-medium text-gray-300">Siege War</span>
          </router-link>

          <router-link
            to="/events"
            class="card flex flex-col items-center justify-center py-6 hover:bg-dark-600 transition-colors"
          >
            <svg class="w-8 h-8 text-primary-400 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <span class="text-sm font-medium text-gray-300">Eventos</span>
          </router-link>

          <router-link
            to="/parties"
            class="card flex flex-col items-center justify-center py-6 hover:bg-dark-600 transition-colors"
          >
            <svg class="w-8 h-8 text-primary-400 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <span class="text-sm font-medium text-gray-300">Parties</span>
          </router-link>

          <router-link
            to="/channels"
            class="card flex flex-col items-center justify-center py-6 hover:bg-dark-600 transition-colors"
          >
            <svg class="w-8 h-8 text-primary-400 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span class="text-sm font-medium text-gray-300">Chat</span>
          </router-link>
        </div>
      </div>
    </div>

    <!-- Create Announcement Modal -->
    <CreateAnnouncementModal
      v-model:show="showCreateModal"
    />
  </MainLayout>
</template>
