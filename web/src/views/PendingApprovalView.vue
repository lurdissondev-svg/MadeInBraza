<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function checkStatus() {
  await authStore.checkStatus()

  if (authStore.isApproved) {
    router.push('/')
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-dark-900 flex flex-col justify-center px-4 py-8 sm:px-6 lg:px-8">
    <div class="w-full max-w-md mx-auto text-center">
      <!-- Icon -->
      <div class="w-24 h-24 mx-auto bg-yellow-500/20 rounded-full flex items-center justify-center mb-6">
        <svg class="w-12 h-12 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>

      <!-- Title -->
      <h1 class="text-2xl sm:text-3xl font-bold text-gray-100 mb-4">
        Aguardando Aprovação
      </h1>

      <!-- Message -->
      <div class="card mb-6">
        <p class="text-gray-300 mb-4">
          Olá, <span class="text-primary-400 font-semibold">{{ authStore.user?.nick }}</span>!
        </p>
        <p class="text-gray-400 text-sm">
          Sua conta foi criada com sucesso e está aguardando aprovação de um líder da guilda.
        </p>
        <p class="text-gray-400 text-sm mt-3">
          Assim que for aprovado, você terá acesso completo ao sistema.
        </p>
      </div>

      <!-- Actions -->
      <div class="space-y-3">
        <button
          @click="checkStatus"
          class="btn-primary w-full py-3"
        >
          <span class="flex items-center justify-center gap-2">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Verificar Status
          </span>
        </button>

        <button
          @click="handleLogout"
          class="btn-ghost w-full py-3"
        >
          Sair
        </button>
      </div>
    </div>

    <!-- Footer -->
    <div class="mt-8 text-center text-gray-500 text-xs sm:text-sm">
      <p>Made in Braza &copy; {{ new Date().getFullYear() }}</p>
    </div>
  </div>
</template>
