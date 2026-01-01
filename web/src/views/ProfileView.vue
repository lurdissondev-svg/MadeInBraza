<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useProfileStore } from '@/stores/profile'
import MainLayout from '@/components/layout/MainLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EditProfileSheet from '@/components/profile/EditProfileSheet.vue'
import ChangePasswordSheet from '@/components/profile/ChangePasswordSheet.vue'
import { PlayerClass, PlayerClassNames, Role } from '@/types'

const router = useRouter()
const authStore = useAuthStore()
const profileStore = useProfileStore()

const showEditProfile = ref(false)
const showChangePassword = ref(false)

onMounted(() => {
  profileStore.fetchProfile()
})

// Watch for success messages and auto-dismiss
watch(() => profileStore.successMessage, (message) => {
  if (message) {
    setTimeout(() => {
      profileStore.clearSuccessMessage()
    }, 3000)
  }
})

function formatDate(isoDate: string): string {
  try {
    const date = new Date(isoDate)
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    })
  } catch {
    return '-'
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/web/login')
}
</script>

<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Loading -->
      <div v-if="profileStore.loading && !profileStore.profile" class="flex justify-center py-12">
        <LoadingSpinner />
      </div>

      <template v-else>
        <!-- Profile Card -->
        <div class="card">
          <div class="flex flex-col items-center text-center">
            <div
              class="w-24 h-24 rounded-full flex items-center justify-center mb-4"
              :class="authStore.isLeader ? 'bg-primary-500' : 'bg-primary-500/20'"
            >
              <span
                class="text-4xl font-bold"
                :class="authStore.isLeader ? 'text-white' : 'text-primary-400'"
              >
                {{ authStore.user?.nick?.charAt(0).toUpperCase() }}
              </span>
            </div>

            <div class="flex items-center gap-2 mb-1">
              <h2 class="text-2xl font-bold text-gray-100">
                {{ authStore.user?.nick }}
              </h2>
              <span v-if="authStore.isLeader" class="text-xl">👑</span>
            </div>

            <p class="text-gray-400">
              {{ PlayerClassNames[authStore.user?.playerClass as PlayerClass] }}
            </p>

            <span
              v-if="authStore.isLeader"
              class="mt-3 px-4 py-1.5 bg-primary-500/20 text-primary-400 rounded-full text-sm font-medium"
            >
              Líder da Guilda
            </span>
          </div>
        </div>

        <!-- Stats Card -->
        <div v-if="profileStore.profile?.stats" class="card">
          <h3 class="text-lg font-semibold text-gray-100 mb-4">Estatísticas</h3>

          <div class="grid grid-cols-2 gap-4">
            <div class="bg-dark-600 rounded-lg p-4 text-center">
              <p class="text-2xl font-bold text-primary-400">
                {{ profileStore.profile.stats.messagesCount }}
              </p>
              <p class="text-sm text-gray-400">Mensagens</p>
            </div>
            <div class="bg-dark-600 rounded-lg p-4 text-center">
              <p class="text-2xl font-bold text-primary-400">
                {{ profileStore.profile.stats.eventsParticipated }}
              </p>
              <p class="text-sm text-gray-400">Eventos</p>
            </div>
          </div>
        </div>

        <!-- Info Card -->
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-100 mb-4">Informações</h3>

          <div class="space-y-3">
            <div class="flex justify-between items-center py-2 border-b border-dark-600">
              <span class="text-gray-400">Status</span>
              <span class="text-green-400">Ativo</span>
            </div>
            <div class="flex justify-between items-center py-2 border-b border-dark-600">
              <span class="text-gray-400">Cargo</span>
              <span class="text-gray-100">
                {{ authStore.user?.role === Role.LEADER ? 'Líder' : 'Membro' }}
              </span>
            </div>
            <div v-if="profileStore.profile?.createdAt" class="flex justify-between items-center py-2">
              <span class="text-gray-400">Membro desde</span>
              <span class="text-gray-100">
                {{ formatDate(profileStore.profile.createdAt) }}
              </span>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="space-y-3">
          <button
            @click="showEditProfile = true"
            class="btn-secondary w-full py-3 flex items-center justify-center gap-2"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            Editar Perfil
          </button>

          <button
            @click="showChangePassword = true"
            class="btn-secondary w-full py-3 flex items-center justify-center gap-2"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
            </svg>
            Alterar Senha
          </button>

          <!-- Leader actions -->
          <template v-if="authStore.isLeader">
            <router-link
              to="/web/admin/pending"
              class="btn-secondary w-full py-3 flex items-center justify-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
              Membros Pendentes
            </router-link>

            <router-link
              to="/web/admin/banned"
              class="btn-secondary w-full py-3 flex items-center justify-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
              </svg>
              Usuários Banidos
            </router-link>
          </template>

          <button
            @click="handleLogout"
            class="btn-ghost w-full py-3 text-red-400 hover:text-red-300 hover:bg-red-500/10 flex items-center justify-center gap-2"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Sair da Conta
          </button>
        </div>
      </template>
    </div>

    <!-- Success toast -->
    <div
      v-if="profileStore.successMessage"
      class="fixed bottom-20 left-4 right-4 bg-green-500/90 text-white px-4 py-3 rounded-lg flex items-center justify-between z-50"
    >
      <span>{{ profileStore.successMessage }}</span>
      <button @click="profileStore.clearSuccessMessage()" class="ml-4 font-bold">OK</button>
    </div>

    <!-- Error toast -->
    <div
      v-if="profileStore.error"
      class="fixed bottom-20 left-4 right-4 bg-red-500/90 text-white px-4 py-3 rounded-lg flex items-center justify-between z-50"
    >
      <span>{{ profileStore.error }}</span>
      <button @click="profileStore.clearError()" class="ml-4 font-bold">OK</button>
    </div>

    <!-- Edit Profile Sheet -->
    <EditProfileSheet
      v-if="showEditProfile"
      @close="showEditProfile = false"
    />

    <!-- Change Password Sheet -->
    <ChangePasswordSheet
      v-if="showChangePassword"
      @close="showChangePassword = false"
    />
  </MainLayout>
</template>
