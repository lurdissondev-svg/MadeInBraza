<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const nick = ref('')
const password = ref('')
const showPassword = ref(false)
const stayLoggedIn = ref(false)

const isFormValid = computed(() => {
  return nick.value.trim().length >= 3 && password.value.length >= 6
})

async function handleLogin() {
  if (!isFormValid.value) return

  const success = await authStore.login(nick.value.trim(), password.value, stayLoggedIn.value)

  if (success) {
    // Wait for Vue reactivity to process state changes
    await nextTick()
    const redirect = route.query.redirect as string
    // Use replace to avoid back button returning to login
    await router.replace(redirect || '/')
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
  <div class="min-h-screen bg-black flex flex-col justify-center items-center px-6 py-8">
    <!-- Logo -->
    <img
      src="/braza_logo.png"
      alt="Made in Braza"
      class="w-28 h-28 sm:w-32 sm:h-32 mb-12"
    />

    <!-- Login Form -->
    <form @submit.prevent="handleLogin" class="w-full max-w-sm space-y-4">
      <!-- Nick Field -->
      <div class="input-outlined">
        <input
          id="nick"
          v-model="nick"
          type="text"
          autocomplete="username"
          placeholder=" "
          class="input-outlined-field"
          :class="{ 'border-red-500': authStore.error }"
        />
        <label for="nick" class="input-outlined-label">Nick</label>
      </div>

      <!-- Password Field -->
      <div class="input-outlined">
        <input
          id="password"
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="current-password"
          placeholder=" "
          class="input-outlined-field pr-12"
          :class="{ 'border-red-500': authStore.error }"
        />
        <label for="password" class="input-outlined-label">Senha</label>
        <button
          type="button"
          @click="showPassword = !showPassword"
          class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white p-1"
        >
          <span class="text-lg">{{ showPassword ? '🙈' : '👁' }}</span>
        </button>
      </div>

      <!-- Error Message -->
      <p v-if="authStore.error" class="text-red-400 text-sm">
        {{ authStore.error }}
      </p>

      <!-- Forgot Password Link -->
      <div class="text-right">
        <button
          type="button"
          @click="goToForgotPassword"
          class="text-sm text-white hover:text-gray-300 transition-colors"
        >
          Esqueceu a senha?
        </button>
      </div>

      <!-- Stay Logged In Checkbox -->
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          v-model="stayLoggedIn"
          class="checkbox-custom"
        />
        <span class="text-white text-sm">Manter conectado</span>
      </label>

      <!-- Submit Button -->
      <button
        type="submit"
        :disabled="!isFormValid || authStore.loading"
        class="w-full py-3 bg-white text-black font-semibold rounded-lg hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
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

      <!-- Register Link -->
      <div class="text-center pt-4">
        <button
          type="button"
          @click="goToRegister"
          class="text-white hover:text-gray-300 font-medium transition-colors"
        >
          Criar Conta
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
/* Outlined Input Style - igual Material Design do Android */
.input-outlined {
  position: relative;
}

.input-outlined-field {
  width: 100%;
  padding: 1rem 1rem;
  background-color: transparent;
  border: 1px solid #666;
  border-radius: 0.5rem;
  color: white;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.input-outlined-field:focus {
  outline: none;
  border-color: white;
}

.input-outlined-field::placeholder {
  color: transparent;
}

.input-outlined-label {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  font-size: 1rem;
  pointer-events: none;
  transition: all 0.2s ease;
  background-color: black;
  padding: 0 0.25rem;
}

.input-outlined-field:focus + .input-outlined-label,
.input-outlined-field:not(:placeholder-shown) + .input-outlined-label {
  top: 0;
  font-size: 0.75rem;
  color: white;
}

/* Custom Checkbox - roxo quando marcado */
.checkbox-custom {
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 0.25rem;
  border: 2px solid #666;
  background-color: transparent;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  position: relative;
  transition: all 0.2s ease;
}

.checkbox-custom:checked {
  background-color: #8b5cf6;
  border-color: #8b5cf6;
}

.checkbox-custom:checked::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 1px;
  width: 6px;
  height: 10px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.checkbox-custom:focus {
  outline: none;
  border-color: #8b5cf6;
  box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.3);
}
</style>
