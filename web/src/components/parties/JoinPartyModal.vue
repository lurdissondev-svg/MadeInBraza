<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { Party, PartySlot } from '@/types'
import { PlayerClassNames, PlayerClassAbbreviations } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps<{
  show: boolean
  party: Party | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  join: [partyId: string, slotId: string]
}>()

const selectedSlotId = ref<string | null>(null)
const isSubmitting = ref(false)

// Group available slots by class (null = FREE slot)
const availableSlotsByClass = computed(() => {
  if (!props.party) return []

  const slotsByClass = new Map<string | null, PartySlot[]>()

  props.party.slots
    .filter(slot => !slot.filledBy)
    .forEach(slot => {
      const key = slot.playerClass // null for FREE slots
      const existing = slotsByClass.get(key) || []
      existing.push(slot)
      slotsByClass.set(key, existing)
    })

  return Array.from(slotsByClass.entries()).map(([playerClass, slots]) => ({
    playerClass,
    isFreeSlot: playerClass === null,
    slots,
    count: slots.length
  }))
})

watch(() => props.show, (newValue) => {
  if (newValue) {
    selectedSlotId.value = null
    isSubmitting.value = false
  }
})

function close() {
  emit('update:show', false)
}

function selectSlot(slotId: string) {
  selectedSlotId.value = slotId
}

async function handleJoin() {
  if (!props.party || !selectedSlotId.value) return

  isSubmitting.value = true
  emit('join', props.party.id, selectedSlotId.value)
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
            <p class="text-sm text-gray-300 mb-4">Selecione a vaga que deseja ocupar:</p>

            <!-- Available Slots by Class -->
            <div class="space-y-2">
              <template v-for="group in availableSlotsByClass" :key="group.playerClass ?? 'FREE'">
                <button
                  v-for="(slot, index) in group.slots"
                  :key="slot.id"
                  @click="selectSlot(slot.id)"
                  class="w-full flex items-center justify-between p-3 rounded-lg transition-colors"
                  :class="[
                    selectedSlotId === slot.id
                      ? 'bg-primary-500/20 ring-1 ring-primary-500'
                      : group.isFreeSlot ? 'bg-amber-500/10 hover:bg-amber-500/20' : 'bg-dark-600 hover:bg-dark-500'
                  ]"
                  :disabled="isSubmitting"
                >
                  <div class="flex items-center gap-3">
                    <div
                      class="w-10 h-10 rounded-full flex items-center justify-center text-sm font-medium"
                      :class="group.isFreeSlot ? 'bg-amber-500/20 text-amber-400' : 'bg-dark-500 text-gray-300'"
                    >
                      {{ group.isFreeSlot ? 'LIVRE' : PlayerClassAbbreviations[group.playerClass as keyof typeof PlayerClassAbbreviations] }}
                    </div>
                    <span :class="group.isFreeSlot ? 'text-amber-300' : 'text-gray-200'">
                      {{ group.isFreeSlot ? 'Livre (qualquer classe)' : PlayerClassNames[group.playerClass as keyof typeof PlayerClassNames] }}
                      <span v-if="group.count > 1" class="text-gray-500">({{ index + 1 }}/{{ group.count }})</span>
                    </span>
                  </div>
                  <svg
                    v-if="selectedSlotId === slot.id"
                    class="w-5 h-5 text-primary-400"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                  </svg>
                </button>
              </template>
            </div>

            <!-- No slots available -->
            <div v-if="availableSlotsByClass.length === 0" class="text-center py-8 text-gray-400">
              Não há vagas disponíveis nesta party.
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
              Cancelar
            </button>
            <button
              @click="handleJoin"
              class="btn btn-primary flex-1"
              :disabled="isSubmitting || !selectedSlotId"
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
