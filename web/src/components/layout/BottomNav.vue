<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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

const navItems: NavItem[] = [
  { name: 'home', route: '/', icon: 'home', label: 'Início' },
  { name: 'siege-war', route: '/siege-war', icon: 'sword', label: 'SW' },
  { name: 'parties', route: '/parties', icon: 'party', label: 'PTs' },
  { name: 'members', route: '/members', icon: 'users', label: 'Membros', leaderOnly: true },
  { name: 'profile', route: '/profile', icon: 'user', label: 'Perfil' }
]

const filteredNavItems = computed(() => {
  return navItems.filter(item => {
    if (item.leaderOnly && !authStore.isLeader) return false
    return true
  })
})

function isActive(routeName: string): boolean {
  return route.name === routeName
}

function navigateTo(path: string) {
  router.push(path)
}
</script>

<template>
  <nav class="fixed bottom-0 left-0 right-0 bg-dark-800 border-t border-dark-600 safe-bottom z-50">
    <div class="flex justify-around items-center h-16">
      <button
        v-for="item in filteredNavItems"
        :key="item.name"
        @click="navigateTo(item.route)"
        class="flex flex-col items-center justify-center flex-1 h-full px-2 transition-colors touch-target"
        :class="isActive(item.name) ? 'text-primary-500' : 'text-gray-400 hover:text-gray-300'"
      >
        <!-- Icons -->
        <svg v-if="item.icon === 'home'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>

        <svg v-else-if="item.icon === 'sword'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>

        <svg v-else-if="item.icon === 'party'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>

        <svg v-else-if="item.icon === 'users'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>

        <svg v-else-if="item.icon === 'user'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>

        <span class="text-xs mt-1 font-medium">{{ item.label }}</span>
      </button>
    </div>
  </nav>
</template>
