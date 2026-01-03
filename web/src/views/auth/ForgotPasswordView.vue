<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/services/api/auth'

const router = useRouter()

const nick = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const isFormValid = computed(() => {
  return nick.value.trim().length >= 3
})

async function handleForgotPassword() {
  if (!isFormValid.value) return

  loading.value = true
  error.value = null
  successMessage.value = null

  try {
    // Email-based recovery only (secure method)
    const response = await authApi.requestPasswordReset({ nick: nick.value.trim() })
    successMessage.value = response.message
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Erro ao recuperar senha'
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-dark-900 flex flex-col justify-center px-4 py-8 sm:px-6 lg:px-8">
    <!-- Logo/Header -->
    <div class="text-center mb-8">
      <h1 class="text-3xl sm:text-4xl font-bold text-primary-500">
        Made in Braza
      </h1>
      <p class="mt-2 text-gray-400 text-sm sm:text-base">
        Recupere sua senha
      </p>
    </div>

    <!-- Form Card -->
    <div class="w-full max-w-md mx-auto">
      <div class="card">
        <!-- Success State - Email Sent -->
        <div v-if="successMessage" class="text-center space-y-4">
          <div class="w-16 h-16 mx-auto bg-blue-500/20 rounded-full flex items-center justify-center">
            <svg class="w-8 h-8 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </div>
          <h2 class="text-xl font-semibold text-gray-100">Verifique seu Email!</h2>
          <p class="text-gray-400">
            {{ successMessage }}
          </p>
          <p class="text-sm text-gray-500">
            O link expira em 1 hora
          </p>

          <button
            @click="goToLogin"
            class="btn-primary w-full py-3"
          >
            Ir para Login
          </button>
        </div>

        <!-- Form State -->
        <form v-else @submit.prevent="handleForgotPassword" class="space-y-6">
          <div class="text-center mb-4">
            <p class="text-sm text-gray-400">
              Um link de recuperacao sera enviado para seu email cadastrado
            </p>
          </div>

          <div>
            <label for="nick" class="label">Nick</label>
            <input
              id="nick"
              v-model="nick"
              type="text"
              autocomplete="username"
              placeholder="Seu nick no jogo"
              class="input"
              :class="{ 'input-error': error }"
            />
          </div>

          <!-- Error Message -->
          <div v-if="error" class="text-red-400 text-sm text-center bg-red-500/10 rounded-lg p-3">
            {{ error }}
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            :disabled="!isFormValid || loading"
            class="btn-primary w-full py-3 text-base font-semibold"
          >
            <span v-if="loading" class="flex items-center justify-center gap-2">
              <svg class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Enviando...
            </span>
            <span v-else>Enviar Link de Recuperacao</span>
          </button>

          <!-- Back to Login -->
          <div class="text-center">
            <button
              type="button"
              @click="goToLogin"
              class="text-sm text-gray-400 hover:text-gray-300 transition-colors"
            >
              &larr; Voltar para login
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Footer -->
    <div class="mt-8 text-center text-gray-500 text-xs sm:text-sm">
      <p>Made in Braza &copy; {{ new Date().getFullYear() }}</p>
    </div>
  </div>
</template>
