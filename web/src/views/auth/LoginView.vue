<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const nick = ref('')
const password = ref('')
const showPassword = ref(false)

const isFormValid = computed(() => {
  return nick.value.trim().length >= 3 && password.value.length >= 6
})

async function handleLogin() {
  if (!isFormValid.value) return

  const success = await authStore.login(nick.value.trim(), password.value)

  if (success) {
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  }
}

function goToRegister() {
  router.push('/register')
}

function goToForgotPassword() {
  router.push('/forgot-password')
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
        Entre na sua conta para continuar
      </p>
    </div>

    <!-- Login Form Card -->
    <div class="w-full max-w-md mx-auto">
      <div class="card">
        <form @submit.prevent="handleLogin" class="space-y-6">
          <!-- Nick Field -->
          <div>
            <label for="nick" class="label">Nick</label>
            <input
              id="nick"
              v-model="nick"
              type="text"
              autocomplete="username"
              placeholder="Seu nick no jogo"
              class="input"
              :class="{ 'input-error': authStore.error }"
            />
          </div>

          <!-- Password Field -->
          <div>
            <label for="password" class="label">Senha</label>
            <div class="relative">
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="Sua senha"
                class="input pr-12"
                :class="{ 'input-error': authStore.error }"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300 p-1"
              >
                <svg v-if="showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Error Message -->
          <div v-if="authStore.error" class="text-red-400 text-sm text-center bg-red-500/10 rounded-lg p-3">
            {{ authStore.error }}
          </div>

          <!-- Forgot Password Link -->
          <div class="text-right">
            <button
              type="button"
              @click="goToForgotPassword"
              class="text-sm text-primary-400 hover:text-primary-300 transition-colors"
            >
              Esqueceu a senha?
            </button>
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            :disabled="!isFormValid || authStore.loading"
            class="btn-primary w-full py-3 text-base font-semibold"
          >
            <span v-if="authStore.loading" class="flex items-center justify-center gap-2">
              <svg class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Entrando...
            </span>
            <span v-else>Entrar</span>
          </button>
        </form>

        <!-- Register Link -->
        <div class="mt-6 text-center">
          <p class="text-gray-400 text-sm">
            Ainda não tem conta?
            <button
              @click="goToRegister"
              class="text-primary-400 hover:text-primary-300 font-medium transition-colors"
            >
              Cadastre-se
            </button>
          </p>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="mt-8 text-center text-gray-500 text-xs sm:text-sm">
      <p>Made in Braza &copy; {{ new Date().getFullYear() }}</p>
    </div>
  </div>
</template>
