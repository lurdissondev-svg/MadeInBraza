<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useEventsStore } from '@/stores/events'
import MainLayout from '@/components/layout/MainLayout.vue'
import EventCard from '@/components/events/EventCard.vue'
import CreateEventModal from '@/components/events/CreateEventModal.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const authStore = useAuthStore()
const eventsStore = useEventsStore()

const showCreateModal = ref(false)

const upcomingEvents = computed(() => {
  const now = new Date()
  return eventsStore.events
    .filter(e => new Date(e.eventDate) >= now)
    .sort((a, b) => new Date(a.eventDate).getTime() - new Date(b.eventDate).getTime())
})

const pastEvents = computed(() => {
  const now = new Date()
  return eventsStore.events
    .filter(e => new Date(e.eventDate) < now)
    .sort((a, b) => new Date(b.eventDate).getTime() - new Date(a.eventDate).getTime())
})

onMounted(() => {
  eventsStore.fetchEvents()
})

function handleRefresh() {
  eventsStore.fetchEvents()
}

async function handleDelete(id: string) {
  if (confirm('Tem certeza que deseja deletar este evento?')) {
    await eventsStore.deleteEvent(id)
  }
}

async function handleJoin(id: string) {
  await eventsStore.joinEvent(id)
}

async function handleLeave(id: string) {
  await eventsStore.leaveEvent(id)
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
            :disabled="eventsStore.loading"
          >
            <svg
              class="w-5 h-5"
              :class="{ 'animate-spin': eventsStore.loading }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
        <button
          v-if="authStore.isLeader"
          @click="showCreateModal = true"
          class="btn btn-primary text-sm"
        >
          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          Novo Evento
        </button>
      </div>

      <!-- Loading State -->
      <div v-if="eventsStore.loading && eventsStore.events.length === 0" class="flex justify-center py-8">
        <LoadingSpinner />
      </div>

      <!-- Error State -->
      <div v-else-if="eventsStore.error" class="card bg-red-900/20 border border-red-500/30">
        <p class="text-red-400 text-center">{{ eventsStore.error }}</p>
        <button @click="handleRefresh" class="btn btn-secondary mt-3 mx-auto block">
          Tentar novamente
        </button>
      </div>

      <!-- Empty State -->
      <div v-else-if="eventsStore.events.length === 0" class="card text-center py-8">
        <svg class="w-12 h-12 mx-auto text-gray-500 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <p class="text-gray-400">Nenhum evento disponível</p>
      </div>

      <!-- Events List -->
      <template v-else>
        <!-- Upcoming Events -->
        <div v-if="upcomingEvents.length > 0">
          <h3 class="text-sm font-medium text-gray-400 uppercase tracking-wide mb-3">Próximos Eventos</h3>
          <div class="space-y-3">
            <EventCard
              v-for="event in upcomingEvents"
              :key="event.id"
              :event="event"
              :current-user-id="authStore.user?.id"
              :can-delete="authStore.isLeader"
              @join="handleJoin"
              @leave="handleLeave"
              @delete="handleDelete"
            />
          </div>
        </div>

        <!-- Past Events -->
        <div v-if="pastEvents.length > 0">
          <h3 class="text-sm font-medium text-gray-400 uppercase tracking-wide mb-3">Eventos Passados</h3>
          <div class="space-y-3">
            <EventCard
              v-for="event in pastEvents"
              :key="event.id"
              :event="event"
              :current-user-id="authStore.user?.id"
              :can-delete="authStore.isLeader"
              :is-past="true"
              @delete="handleDelete"
            />
          </div>
        </div>
      </template>
    </div>

    <!-- Create Event Modal -->
    <CreateEventModal v-model:show="showCreateModal" />
  </MainLayout>
</template>
