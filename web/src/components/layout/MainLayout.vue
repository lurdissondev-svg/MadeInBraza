<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BottomNav from './BottomNav.vue'
import Sidebar from './Sidebar.vue'
import AppHeader from './AppHeader.vue'

const route = useRoute()

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    home: 'Início',
    events: 'Eventos',
    parties: 'PTs',
    'siege-war': 'Siege War',
    channels: 'Chat',
    chat: 'Chat',
    members: 'Membros',
    'member-profile': 'Perfil do Membro',
    profile: 'Meu Perfil',
    'admin-pending': 'Pendentes',
    'admin-banned': 'Banidos'
  }
  return titles[route.name as string] || 'Made in Braza'
})

const showBackButton = computed(() => {
  const routesWithBack = ['chat', 'member-profile']
  return routesWithBack.includes(route.name as string)
})
</script>

<template>
  <div class="min-h-screen bg-dark-900 flex">
    <!-- Sidebar (desktop only) -->
    <Sidebar class="hidden lg:flex" />

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col min-h-screen lg:ml-64">
      <!-- Header -->
      <AppHeader
        :title="pageTitle"
        :show-back="showBackButton"
      />

      <!-- Page Content -->
      <main class="flex-1 overflow-auto pb-20 lg:pb-6">
        <div class="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8">
          <slot />
        </div>
      </main>

      <!-- Bottom Navigation (mobile/tablet only) -->
      <BottomNav class="lg:hidden" />
    </div>
  </div>
</template>
