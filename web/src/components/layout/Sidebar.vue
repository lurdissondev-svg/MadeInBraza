<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { PlayerClassNames } from '@/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

interface NavItem {
  name: string
  route: string
  icon: string
  label: string
  leaderOnly?: boolean
}

const mainNavItems: NavItem[] = [
  { name: 'home', route: '/', icon: 'home', label: 'Início' },
  { name: 'siege-war', route: '/siege-war', icon: 'sword', label: 'Siege War' },
  { name: 'events', route: '/events', icon: 'calendar', label: 'Eventos' },
  { name: 'parties', route: '/parties', icon: 'users-group', label: 'Parties' },
  { name: 'channels', route: '/channels', icon: 'chat', label: 'Canais' },
  { name: 'members', route: '/members', icon: 'users', label: 'Membros' }
]

const adminNavItems: NavItem[] = [
  { name: 'admin-pending', route: '/admin/pending', icon: 'clock', label: 'Pendentes', leaderOnly: true },
  { name: 'admin-banned', route: '/admin/banned', icon: 'ban', label: 'Banidos', leaderOnly: true }
]

const filteredAdminItems = computed(() => {
  if (!authStore.isLeader) return []
  return adminNavItems
})

const userClassName = computed(() => {
  if (!authStore.user?.playerClass) return ''
  return PlayerClassNames[authStore.user.playerClass]
})

function isActive(routeName: string): boolean {
  return route.name === routeName
}

function navigateTo(path: string) {
  router.push(path)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <aside class="fixed left-0 top-0 bottom-0 w-64 bg-dark-800 border-r border-dark-600 flex flex-col z-40">
    <!-- Logo -->
    <div class="p-6 border-b border-dark-600">
      <h1 class="text-xl font-bold text-primary-500">Made in Braza</h1>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto py-4">
      <!-- Main Navigation -->
      <div class="px-3 space-y-1">
        <button
          v-for="item in mainNavItems"
          :key="item.name"
          @click="navigateTo(item.route)"
          class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-left transition-colors"
          :class="isActive(item.name)
            ? 'bg-primary-500/20 text-primary-400'
            : 'text-gray-300 hover:bg-dark-700 hover:text-gray-100'"
        >
          <!-- Icons -->
          <svg v-if="item.icon === 'home'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>

          <svg v-else-if="item.icon === 'sword'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>

          <svg v-else-if="item.icon === 'calendar'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>

          <svg v-else-if="item.icon === 'users-group'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>

          <svg v-else-if="item.icon === 'chat'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>

          <svg v-else-if="item.icon === 'users'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>

          <span class="font-medium">{{ item.label }}</span>
        </button>
      </div>

      <!-- Admin Section -->
      <div v-if="filteredAdminItems.length > 0" class="mt-6 px-3">
        <p class="px-3 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
          Administração
        </p>
        <div class="space-y-1">
          <button
            v-for="item in filteredAdminItems"
            :key="item.name"
            @click="navigateTo(item.route)"
            class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-left transition-colors"
            :class="isActive(item.name)
              ? 'bg-primary-500/20 text-primary-400'
              : 'text-gray-300 hover:bg-dark-700 hover:text-gray-100'"
          >
            <svg v-if="item.icon === 'clock'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>

            <svg v-else-if="item.icon === 'ban'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
            </svg>

            <span class="font-medium">{{ item.label }}</span>
          </button>
        </div>
      </div>
    </nav>

    <!-- User Section -->
    <div class="p-4 border-t border-dark-600">
      <div class="flex items-center gap-3 mb-3">
        <div class="w-10 h-10 rounded-full bg-primary-500/20 flex items-center justify-center">
          <span class="text-primary-400 font-semibold">
            {{ authStore.user?.nick?.charAt(0).toUpperCase() }}
          </span>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm font-medium text-gray-100 truncate">
            {{ authStore.user?.nick }}
          </p>
          <p class="text-xs text-gray-400 truncate">
            {{ userClassName }}
          </p>
        </div>
      </div>

      <div class="flex gap-2">
        <button
          @click="navigateTo('/profile')"
          class="flex-1 btn-ghost text-sm py-2"
        >
          Perfil
        </button>
        <button
          @click="handleLogout"
          class="flex-1 btn-ghost text-sm py-2 text-red-400 hover:text-red-300 hover:bg-red-500/10"
        >
          Sair
        </button>
      </div>
    </div>
  </aside>
</template>
