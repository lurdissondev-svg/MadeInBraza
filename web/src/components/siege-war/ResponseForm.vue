<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { SWResponseType, SWTag, PlayerClass, PlayerClassNames, PlayerClassAbbreviations } from '@/types'
import type { SWUserResponse, AvailableShare, SubmitSWResponseRequest } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps<{
  userResponse: SWUserResponse | null
  availableShares: AvailableShare[]
  isActive: boolean
  submitting: boolean
}>()

const emit = defineEmits<{
  submit: [data: SubmitSWResponseRequest]
  loadShares: []
}>()

// Form state
const selectedTag = ref<SWTag | null>(null)
const selectedResponseType = ref<SWResponseType | null>(null)
const gameId = ref('')
const password = ref('')
const selectedSharedClass = ref<PlayerClass | null>(null)
const selectedPilotFor = ref<AvailableShare | null>(null)
const selectedPreferredClass = ref<PlayerClass | null>(null)

// Initialize form with existing response
watch(() => props.userResponse, (response) => {
  if (response) {
    selectedTag.value = response.tag
    selectedResponseType.value = response.responseType
    if (response.responseType === SWResponseType.SHARED) {
      gameId.value = response.gameId || ''
      selectedSharedClass.value = response.sharedClass
    }
    if (response.responseType === SWResponseType.PILOT) {
      selectedPreferredClass.value = response.preferredClass
    }
  }
}, { immediate: true })

// Load shares when PILOT is selected
watch(() => selectedResponseType.value, (type) => {
  if (type === SWResponseType.PILOT) {
    emit('loadShares')
  }
})

const tagOptions = [
  { value: SWTag.ATTACK, label: 'ATAQUE' },
  { value: SWTag.DEFENSE, label: 'DEFESA' },
  { value: SWTag.ACADEMY, label: 'ACADEMY' }
]

const responseOptions = [
  {
    value: SWResponseType.CONFIRMED,
    label: 'Estarei lá com certeza!',
    description: 'Vou participar com minha conta'
  },
  {
    value: SWResponseType.SHARED,
    label: 'Vou deixar Shared!',
    description: 'Vou disponibilizar minha conta para outro pilotar'
  },
  {
    value: SWResponseType.PILOT,
    label: 'Vou de Piloto!',
    description: 'Vou pilotar a conta de outro membro'
  }
]

const canSubmit = computed(() => {
  if (!selectedTag.value || !selectedResponseType.value) return false

  if (selectedResponseType.value === SWResponseType.SHARED) {
    return gameId.value.trim() && password.value.trim() && selectedSharedClass.value
  }

  if (selectedResponseType.value === SWResponseType.PILOT) {
    return selectedPilotFor.value && selectedPreferredClass.value
  }

  return true
})

function handleSubmit() {
  if (!canSubmit.value) return

  const data: SubmitSWResponseRequest = {
    responseType: selectedResponseType.value!,
    tag: selectedTag.value
  }

  if (selectedResponseType.value === SWResponseType.SHARED) {
    data.gameId = gameId.value.trim()
    data.password = password.value.trim()
    data.sharedClass = selectedSharedClass.value
  }

  if (selectedResponseType.value === SWResponseType.PILOT) {
    data.pilotingForId = selectedPilotFor.value?.userId
    data.preferredClass = selectedPreferredClass.value
  }

  emit('submit', data)
}
</script>

<template>
  <div class="space-y-6">
    <!-- TAG Selection -->
    <div class="card">
      <h3 class="font-medium text-gray-100 mb-2">TAG *</h3>
      <p class="text-sm text-gray-400 mb-4">Selecione seu time</p>
      <div class="space-y-2">
        <label
          v-for="option in tagOptions"
          :key="option.value"
          class="flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-colors"
          :class="selectedTag === option.value ? 'bg-primary-500/20 border border-primary-500' : 'bg-dark-600 border border-transparent hover:bg-dark-500'"
        >
          <input
            type="radio"
            :value="option.value"
            v-model="selectedTag"
            class="w-4 h-4 text-primary-500 border-gray-600 focus:ring-primary-500 focus:ring-offset-dark-700"
            :disabled="!isActive"
          />
          <span class="text-gray-200">{{ option.label }}</span>
        </label>
      </div>
    </div>

    <!-- Response Type Selection -->
    <div class="card">
      <h3 class="font-medium text-gray-100 mb-2">Confirmado ou Shared? *</h3>
      <p class="text-sm text-gray-400 mb-4">Como você vai participar?</p>
      <div class="space-y-2">
        <label
          v-for="option in responseOptions"
          :key="option.value"
          class="flex items-start gap-3 p-3 rounded-lg cursor-pointer transition-colors"
          :class="selectedResponseType === option.value ? 'bg-primary-500/20 border border-primary-500' : 'bg-dark-600 border border-transparent hover:bg-dark-500'"
        >
          <input
            type="radio"
            :value="option.value"
            v-model="selectedResponseType"
            class="w-4 h-4 mt-1 text-primary-500 border-gray-600 focus:ring-primary-500 focus:ring-offset-dark-700"
            :disabled="!isActive"
          />
          <div>
            <span class="text-gray-200 block">{{ option.label }}</span>
            <span class="text-sm text-gray-400">{{ option.description }}</span>
          </div>
        </label>
      </div>
    </div>

    <!-- SHARED Fields -->
    <div v-if="selectedResponseType === SWResponseType.SHARED" class="card">
      <h3 class="font-medium text-gray-100 mb-2">Dados da Conta Compartilhada</h3>
      <p class="text-sm text-gray-400 mb-4">Informe os dados para outro membro pilotar</p>
      <div class="space-y-4">
        <div>
          <label for="gameId" class="label">ID do Jogo *</label>
          <input
            id="gameId"
            v-model="gameId"
            type="text"
            class="input"
            placeholder="Digite o ID da conta"
            :disabled="!isActive"
          />
        </div>
        <div>
          <label for="password" class="label">Senha *</label>
          <input
            id="password"
            v-model="password"
            type="text"
            class="input"
            placeholder="Digite a senha"
            :disabled="!isActive"
          />
        </div>
        <div>
          <label class="label mb-2">Classe da conta *</label>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="pc in Object.values(PlayerClass)"
              :key="pc"
              type="button"
              @click="selectedSharedClass = pc"
              class="px-3 py-1.5 rounded-lg text-sm transition-colors"
              :class="selectedSharedClass === pc ? 'bg-primary-500 text-white' : 'bg-dark-600 text-gray-300 hover:bg-dark-500'"
              :disabled="!isActive"
            >
              {{ PlayerClassAbbreviations[pc] }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- PILOT Fields -->
    <div v-if="selectedResponseType === SWResponseType.PILOT" class="card">
      <h3 class="font-medium text-gray-100 mb-2">Selecionar Conta para Pilotar</h3>
      <p class="text-sm text-gray-400 mb-4">Escolha uma conta disponível</p>

      <div v-if="availableShares.length === 0" class="text-gray-400 text-sm">
        Nenhuma conta disponível para pilotagem no momento
      </div>
      <div v-else class="space-y-2 mb-6">
        <label
          v-for="share in availableShares"
          :key="share.userId"
          class="flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-colors"
          :class="selectedPilotFor?.userId === share.userId ? 'bg-primary-500/20 border border-primary-500' : 'bg-dark-600 border border-transparent hover:bg-dark-500'"
        >
          <input
            type="radio"
            :value="share"
            v-model="selectedPilotFor"
            class="w-4 h-4 text-primary-500 border-gray-600 focus:ring-primary-500 focus:ring-offset-dark-700"
            :disabled="!isActive"
          />
          <div>
            <span class="text-gray-200">{{ share.nick }}</span>
            <span v-if="share.sharedClass" class="text-sm text-gray-400 ml-2">
              ({{ PlayerClassNames[share.sharedClass] }})
            </span>
          </div>
        </label>
      </div>

      <h4 class="font-medium text-gray-100 mb-2">Classe Preferida *</h4>
      <p class="text-sm text-gray-400 mb-4">Qual classe você prefere pilotar?</p>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="pc in Object.values(PlayerClass)"
          :key="pc"
          type="button"
          @click="selectedPreferredClass = pc"
          class="px-3 py-1.5 rounded-lg text-sm transition-colors"
          :class="selectedPreferredClass === pc ? 'bg-primary-500 text-white' : 'bg-dark-600 text-gray-300 hover:bg-dark-500'"
          :disabled="!isActive"
        >
          {{ PlayerClassAbbreviations[pc] }}
        </button>
      </div>
    </div>

    <!-- Current Response Card -->
    <div v-if="userResponse" class="card bg-green-900/20 border border-green-500/30">
      <div class="flex items-center gap-3">
        <svg class="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        <div>
          <p class="text-sm text-green-300">Sua resposta atual:</p>
          <p class="font-medium text-green-400">
            {{ userResponse.responseType === 'CONFIRMED' ? 'Confirmado' :
               userResponse.responseType === 'SHARED' ? 'Compartilhando conta' :
               userResponse.responseType === 'PILOT' ? `Pilotando: ${userResponse.pilotingFor?.nick || 'outro membro'}` :
               'Ausente' }}
          </p>
          <p v-if="userResponse.tag" class="text-sm text-green-300/70">
            Time: {{ userResponse.tag === 'ATTACK' ? 'ATAQUE' : userResponse.tag === 'DEFENSE' ? 'DEFESA' : 'ACADEMY' }}
          </p>
        </div>
      </div>
    </div>

    <!-- Submit Button -->
    <button
      @click="handleSubmit"
      class="btn btn-primary w-full"
      :disabled="!canSubmit || submitting || !isActive"
    >
      <LoadingSpinner v-if="submitting" size="sm" class="mr-2" />
      {{ submitting ? 'Enviando...' : 'ENVIAR' }}
    </button>
  </div>
</template>
