<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAnnouncementsStore } from '@/stores/announcements'
import { useEventsStore } from '@/stores/events'
import { PlayerClassNames } from '@/types'
import MainLayout from '@/components/layout/MainLayout.vue'
import AnnouncementCard from '@/components/announcements/AnnouncementCard.vue'
import CreateAnnouncementModal from '@/components/announcements/CreateAnnouncementModal.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()
const authStore = useAuthStore()
const announcementsStore = useAnnouncementsStore()
const eventsStore = useEventsStore()

const showCreateModal = ref(false)

const userClassName = computed(() => {
  if (!authStore.user?.playerClass) return ''
  return PlayerClassNames[authStore.user.playerClass]
})

const userRole = computed(() => {
  return authStore.isLeader ? 'Líder' : 'Membro'
})

// Get upcoming events (limit to 3)
const upcomingEvents = computed(() => {
  const now = new Date()
  return eventsStore.events
    .filter(e => new Date(e.eventDate) >= now)
    .slice(0, 3)
})

onMounted(() => {
  announcementsStore.fetchAnnouncements()
  eventsStore.fetchEvents()
})

function handleRefresh() {
  announcementsStore.fetchAnnouncements()
  eventsStore.fetchEvents()
}

async function handleDelete(id: string) {
  if (confirm('Tem certeza que deseja deletar este aviso?')) {
    await announcementsStore.deleteAnnouncement(id)
  }
}

function formatEventDate(isoDate: string): string {
  try {
    const date = new Date(isoDate)
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return '-'
  }
}

function isParticipating(eventId: string): boolean {
  const event = eventsStore.events.find(e => e.id === eventId)
  return event?.participants.some(p => p.id === authStore.user?.id) ?? false
}

async function toggleParticipation(eventId: string) {
  if (isParticipating(eventId)) {
    await eventsStore.leaveEvent(eventId)
  } else {
    await eventsStore.joinEvent(eventId)
  }
}

function navigateToEvents() {
  router.push('/events')
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

      <!-- Events Widget -->
      <div class="bg-dark-700 rounded-xl p-4">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-lg font-semibold text-white flex items-center gap-2">
            <svg class="w-5 h-5 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            Eventos
          </h3>
          <button
            @click="navigateToEvents"
            class="text-primary-400 text-sm hover:text-primary-300 transition-colors"
          >
            Ver todos
          </button>
        </div>

        <!-- Loading -->
        <div v-if="eventsStore.loading && upcomingEvents.length === 0" class="flex justify-center py-4">
          <LoadingSpinner />
        </div>

        <!-- Empty State -->
        <div v-else-if="upcomingEvents.length === 0" class="text-center py-4">
          <p class="text-gray-400 text-sm">Nenhum evento futuro</p>
        </div>

        <!-- Events List -->
        <div v-else class="space-y-2">
          <div
            v-for="event in upcomingEvents"
            :key="event.id"
            class="bg-dark-600 rounded-lg p-3 flex items-center justify-between"
          >
            <div class="flex-1 min-w-0">
              <p class="font-medium text-white truncate">{{ event.title }}</p>
              <p class="text-xs text-gray-400">
                {{ formatEventDate(event.eventDate) }}
                <span class="text-gray-500 ml-2">
                  {{ event.participants.length }}{{ event.maxParticipants ? '/' + event.maxParticipants : '' }} participantes
                </span>
              </p>
            </div>
            <button
              @click.stop="toggleParticipation(event.id)"
              class="ml-3 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
              :class="isParticipating(event.id)
                ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30'
                : 'bg-primary-500/20 text-primary-400 hover:bg-primary-500/30'"
            >
              {{ isParticipating(event.id) ? 'Sair' : 'Entrar' }}
            </button>
          </div>
        </div>
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
