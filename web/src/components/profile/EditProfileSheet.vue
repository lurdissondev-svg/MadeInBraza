<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useProfileStore } from '@/stores/profile'
import { useAuthStore } from '@/stores/auth'
import { PlayerClass, PlayerClassNames } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const profileStore = useProfileStore()
const authStore = useAuthStore()

const emit = defineEmits<{
  close: []
}>()

const nick = ref('')
const playerClass = ref<PlayerClass>(PlayerClass.ASSASSIN)

const playerClassOptions = Object.entries(PlayerClassNames).map(([key, name]) => ({
  value: key as PlayerClass,
  label: name
}))

onMounted(() => {
  if (authStore.user) {
    nick.value = authStore.user.nick
    playerClass.value = authStore.user.playerClass
  }
})

async function handleSubmit() {
  const success = await profileStore.updateProfile({
    nick: nick.value,
    playerClass: playerClass.value
  })
  if (success) {
    emit('close')
  }
}
</script>

<template>
  <!-- Backdrop -->
  <div
    class="fixed inset-0 bg-black/50 z-40"
    @click="emit('close')"
  />

  <!-- Sheet -->
  <div class="fixed inset-x-0 bottom-0 z-50 animate-slide-up">
    <div class="bg-dark-700 rounded-t-2xl max-h-[80vh] overflow-hidden flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between p-4 border-b border-dark-600">
        <h3 class="text-lg font-semibold text-gray-100">Editar Perfil</h3>
        <button
          @click="emit('close')"
          class="p-2 hover:bg-dark-600 rounded-lg transition-colors"
        >
          <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Content -->
      <form @submit.prevent="handleSubmit" class="flex-1 overflow-y-auto p-4 space-y-4">
        <!-- Error -->
        <div v-if="profileStore.error" class="bg-red-900/20 border border-red-500/30 rounded-lg p-3">
          <p class="text-red-400 text-sm">{{ profileStore.error }}</p>
        </div>

        <!-- Nick -->
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-2">
            Nick
          </label>
          <input
            v-model="nick"
            type="text"
            required
            minlength="3"
            maxlength="20"
            class="w-full px-4 py-3 bg-dark-600 border border-dark-500 rounded-lg text-gray-100 placeholder-gray-500 focus:outline-none focus:border-primary-500"
            placeholder="Seu nick no jogo"
          />
        </div>

        <!-- Player Class -->
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-2">
            Classe
          </label>
          <select
            v-model="playerClass"
            required
            class="w-full px-4 py-3 bg-dark-600 border border-dark-500 rounded-lg text-gray-100 focus:outline-none focus:border-primary-500"
          >
            <option
              v-for="option in playerClassOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>
        </div>

        <!-- Submit button -->
        <button
          type="submit"
          :disabled="profileStore.updating"
          class="w-full btn btn-primary py-3 flex items-center justify-center gap-2"
        >
          <LoadingSpinner v-if="profileStore.updating" size="sm" />
          <span>{{ profileStore.updating ? 'Salvando...' : 'Salvar Alterações' }}</span>
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
@keyframes slide-up {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.animate-slide-up {
  animation: slide-up 0.3s ease-out;
}
</style>
