<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authApi } from '@/services/api/auth'

const router = useRouter()
const route = useRoute()

const token = ref('')
const nick = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const loading = ref(false)
const verifying = ref(true)
const error = ref<string | null>(null)
const success = ref(false)
const tokenValid = ref(false)

const passwordsMatch = computed(() => {
  return newPassword.value === confirmPassword.value
})

const isFormValid = computed(() => {
  return (
    newPassword.value.length >= 6 &&
    passwordsMatch.value &&
    tokenValid.value
  )
})

onMounted(async () => {
  // Get token from URL
  token.value = (route.query.token as string) || ''

  if (!token.value) {
    error.value = 'Token nao fornecido'
    verifying.value = false
    return
  }

  try {
    const response = await authApi.verifyResetToken({ token: token.value })
    if (response.valid) {
      tokenValid.value = true
      nick.value = response.nick
    } else {
      error.value = 'Token invalido ou expirado'
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Token invalido ou expirado'
  } finally {
    verifying.value = false
  }
})

async function handleResetPassword() {
  if (!isFormValid.value) return

  loading.value = true
  error.value = null

  try {
    await authApi.resetPassword({
      token: token.value,
      newPassword: newPassword.value
    })
    success.value = true
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Erro ao resetar senha'
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
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
        Criar nova senha
      </p>
    </div>

    <!-- Form Card -->
    <div class="w-full max-w-md mx-auto">
      <div class="card">
        <!-- Loading State -->
        <div v-if="verifying" class="text-center py-8">
          <svg class="animate-spin h-10 w-10 text-primary-500 mx-auto" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-4 text-gray-400">Verificando link...</p>
        </div>

        <!-- Success State -->
        <div v-else-if="success" class="text-center space-y-4">
          <div class="w-16 h-16 mx-auto bg-green-500/20 rounded-full flex items-center justify-center">
            <svg class="w-8 h-8 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h2 class="text-xl font-semibold text-gray-100">Senha Alterada!</h2>
          <p class="text-gray-400">
            Sua senha foi alterada com sucesso. Agora voce pode fazer login.
          </p>

          <button
            @click="goToLogin"
            class="btn-primary w-full py-3"
          >
            Ir para Login
          </button>
        </div>

        <!-- Invalid Token State -->
        <div v-else-if="!tokenValid && error" class="text-center space-y-4">
          <div class="w-16 h-16 mx-auto bg-red-500/20 rounded-full flex items-center justify-center">
            <svg class="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <h2 class="text-xl font-semibold text-gray-100">Link Invalido</h2>
          <p class="text-gray-400">
            {{ error }}
          </p>
          <p class="text-sm text-gray-500">
            Solicite um novo link de recuperacao
          </p>

          <button
            @click="goToForgotPassword"
            class="btn-primary w-full py-3"
          >
            Solicitar Novo Link
          </button>
        </div>

        <!-- Form State -->
        <form v-else @submit.prevent="handleResetPassword" class="space-y-5">
          <!-- User Info -->
          <div class="bg-dark-800 rounded-lg p-4 text-center">
            <p class="text-sm text-gray-400">Criando nova senha para</p>
            <p class="text-lg font-semibold text-primary-400">{{ nick }}</p>
          </div>

          <!-- New Password Field -->
          <div>
            <label for="newPassword" class="label">Nova Senha</label>
            <div class="relative">
              <input
                id="newPassword"
                v-model="newPassword"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="Digite sua nova senha"
                class="input pr-12"
                :class="{ 'input-error': error }"
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
            <p class="mt-1 text-xs text-gray-500">Minimo 6 caracteres</p>
          </div>

          <!-- Confirm Password Field -->
          <div>
            <label for="confirmPassword" class="label">Confirmar Senha</label>
            <div class="relative">
              <input
                id="confirmPassword"
                v-model="confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="Confirme sua nova senha"
                class="input pr-12"
                :class="{
                  'input-error': (confirmPassword && !passwordsMatch) || error
                }"
              />
              <button
                type="button"
                @click="showConfirmPassword = !showConfirmPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300 p-1"
              >
                <svg v-if="showConfirmPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
              </button>
            </div>
            <p v-if="confirmPassword && !passwordsMatch" class="mt-1 text-xs text-red-400">
              As senhas nao coincidem
            </p>
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
              Alterando...
            </span>
            <span v-else>Alterar Senha</span>
          </button>
        </form>
      </div>
    </div>

    <!-- Footer -->
    <div class="mt-8 text-center text-gray-500 text-xs sm:text-sm">
      <p>Made in Braza &copy; {{ new Date().getFullYear() }}</p>
    </div>
  </div>
</template>
