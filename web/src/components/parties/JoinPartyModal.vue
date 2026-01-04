<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { Party, PartySlot } from '@/types'
import { PlayerClass, PlayerClassNames, PlayerClassAbbreviations } from '@/types'
import { useAuthStore } from '@/stores/auth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps<{
  show: boolean
  party: Party | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  join: [partyId: string]
}>()

const authStore = useAuthStore()
const isSubmitting = ref(false)

// Get user's player class
const userClass = computed(() => authStore.user?.playerClass)

// Group available slots by class (null = FREE slot)
const availableSlotsByClass = computed(() => {
  if (!props.party) return new Map<PlayerClass | null, PartySlot[]>()

  const slotsByClass = new Map<PlayerClass | null, PartySlot[]>()

  props.party.slots
    .filter(slot => !slot.filledBy)
    .forEach(slot => {
      const key = slot.playerClass // null for FREE slots
      const existing = slotsByClass.get(key) || []
      existing.push(slot)
      slotsByClass.set(key, existing)
    })

  return slotsByClass
})

// Check if user can join this party
const hasSlotForUserClass = computed(() => {
  if (!userClass.value) return false
  return availableSlotsByClass.value.has(userClass.value)
})

const hasFreeSlot = computed(() => {
  return availableSlotsByClass.value.has(null)
})

const canJoin = computed(() => {
  return hasSlotForUserClass.value || hasFreeSlot.value
})

watch(() => props.show, (newValue) => {
  if (newValue) {
    isSubmitting.value = false
  }
})

function close() {
  emit('update:show', false)
}

async function handleJoin() {
  if (!props.party || !canJoin.value) return

  isSubmitting.value = true
  emit('join', props.party.id)
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="show && party"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/60 backdrop-blur-sm"
          @click="close"
        />

        <!-- Modal -->
        <div class="relative bg-dark-700 rounded-xl shadow-xl w-full max-w-md max-h-[90vh] overflow-auto">
          <!-- Header -->
          <div class="flex items-center justify-between p-4 border-b border-dark-600">
            <div>
              <h2 class="text-lg font-semibold text-gray-100">Entrar na Party</h2>
              <p class="text-sm text-gray-400">{{ party.name }}</p>
            </div>
            <button
              @click="close"
              class="p-1 text-gray-400 hover:text-gray-200 rounded-lg hover:bg-dark-600 transition-colors"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Content -->
          <div class="p-4">
            <!-- User's Class Info -->
            <div class="flex items-center gap-3 p-3 bg-dark-600 rounded-lg mb-4">
              <div class="w-12 h-12 rounded-full bg-primary-500/20 flex items-center justify-center">
                <span class="text-primary-400 font-bold text-sm">
                  {{ userClass ? PlayerClassAbbreviations[userClass] : '?' }}
                </span>
              </div>
              <div>
                <p class="text-sm text-gray-400">Sua classe</p>
                <p class="text-gray-200 font-medium">
                  {{ userClass ? PlayerClassNames[userClass] : 'Desconhecida' }}
                </p>
              </div>
            </div>

            <!-- Can Join - Show available slots -->
            <div v-if="canJoin" class="space-y-3">
              <p class="text-sm text-gray-300">Vagas disponíveis para sua classe:</p>

              <!-- Slots for user's class -->
              <div
                v-if="hasSlotForUserClass && userClass"
                class="flex items-center gap-3 p-3 bg-green-500/10 rounded-lg border border-green-500/30"
              >
                <div class="w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center">
                  <span class="text-green-400 font-medium text-sm">
                    {{ PlayerClassAbbreviations[userClass] }}
                  </span>
                </div>
                <div class="flex-1">
                  <p class="text-green-300 font-medium">{{ PlayerClassNames[userClass] }}</p>
                  <p class="text-xs text-green-400/70">
                    {{ availableSlotsByClass.get(userClass)?.length }} vaga(s) disponível(is)
                  </p>
                </div>
                <svg class="w-5 h-5 text-green-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                </svg>
              </div>

              <!-- Free slots available -->
              <div
                v-else-if="hasFreeSlot"
                class="flex items-center gap-3 p-3 bg-amber-500/10 rounded-lg border border-amber-500/30"
              >
                <div class="w-10 h-10 rounded-full bg-amber-500/20 flex items-center justify-center">
                  <span class="text-amber-400 font-medium text-xs">LIVRE</span>
                </div>
                <div class="flex-1">
                  <p class="text-amber-300 font-medium">Vaga Livre</p>
                  <p class="text-xs text-amber-400/70">
                    {{ availableSlotsByClass.get(null)?.length }} vaga(s) disponível(is)
                  </p>
                </div>
                <svg class="w-5 h-5 text-amber-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                </svg>
              </div>

              <p class="text-xs text-gray-500 text-center">
                O sistema encontrará automaticamente a vaga adequada para você.
              </p>
            </div>

            <!-- Cannot Join - No slots for user's class -->
            <div v-else class="text-center py-4">
              <div class="w-16 h-16 mx-auto mb-3 rounded-full bg-red-500/10 flex items-center justify-center">
                <svg class="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </div>
              <p class="text-red-400 font-medium mb-2">Não há vagas para sua classe</p>
              <p class="text-sm text-gray-400">
                Esta party não possui vagas disponíveis para
                <span class="text-gray-300">{{ userClass ? PlayerClassNames[userClass] : 'sua classe' }}</span>.
              </p>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex gap-3 p-4 border-t border-dark-600">
            <button
              type="button"
              @click="close"
              class="btn btn-secondary flex-1"
              :disabled="isSubmitting"
            >
              {{ canJoin ? 'Cancelar' : 'Fechar' }}
            </button>
            <button
              v-if="canJoin"
              @click="handleJoin"
              class="btn btn-primary flex-1"
              :disabled="isSubmitting"
            >
              <LoadingSpinner v-if="isSubmitting" size="sm" class="mr-2" />
              {{ isSubmitting ? 'Entrando...' : 'Entrar' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active > div:last-child,
.modal-leave-active > div:last-child {
  transition: transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from > div:last-child,
.modal-leave-to > div:last-child {
  transform: scale(0.95);
}
</style>
