<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { usePartiesStore } from '@/stores/parties'
import { PlayerClass, PlayerClassNames } from '@/types'
import type { SlotRequest } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps<{
  show: boolean
  eventId?: string
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const partiesStore = usePartiesStore()

const name = ref('')
const description = ref('')
const isSubmitting = ref(false)
const error = ref<string | null>(null)
const creatorSlotClass = ref<PlayerClass | 'FREE' | null>(null)

// Slot counts per class (including FREE)
type SlotKey = PlayerClass | 'FREE'
const slotCounts = ref<Record<SlotKey, number>>(
  [...Object.values(PlayerClass), 'FREE' as const].reduce((acc, pc) => {
    acc[pc] = 0
    return acc
  }, {} as Record<SlotKey, number>)
)

// All slot options (classes + FREE)
const allSlotOptions = computed(() => [
  ...Object.values(PlayerClass),
  'FREE' as const
])

// Display name for slot (including FREE)
function getSlotDisplayName(slot: SlotKey): string {
  if (slot === 'FREE') return 'Livre'
  return PlayerClassNames[slot]
}

// Computed total slots
const totalSlots = computed(() => {
  return Object.values(slotCounts.value).reduce((sum, count) => sum + count, 0)
})

// Available classes for creator to choose (only classes with slots > 0)
const availableClasses = computed(() => {
  return Object.entries(slotCounts.value)
    .filter(([_, count]) => count > 0)
    .map(([key]) => key as SlotKey)
})

// Build slots array for API
const slots = computed<SlotRequest[]>(() => {
  return Object.entries(slotCounts.value)
    .filter(([_, count]) => count > 0)
    .map(([key, count]) => ({
      playerClass: key as PlayerClass | 'FREE',
      count
    }))
})

// Reset creator slot class if their chosen class is no longer available
watch(availableClasses, (newClasses) => {
  if (creatorSlotClass.value && !newClasses.includes(creatorSlotClass.value)) {
    creatorSlotClass.value = null
  }
})

watch(() => props.show, (newValue) => {
  if (newValue) {
    // Reset form when opening
    name.value = ''
    description.value = ''
    error.value = null
    creatorSlotClass.value = null
    // Reset all slot counts
    Object.keys(slotCounts.value).forEach(key => {
      slotCounts.value[key as SlotKey] = 0
    })
  }
})

function close() {
  emit('update:show', false)
}

function incrementSlot(slotKey: SlotKey) {
  if (slotCounts.value[slotKey] < 6 && totalSlots.value < 6) {
    slotCounts.value[slotKey]++
  }
}

function decrementSlot(slotKey: SlotKey) {
  if (slotCounts.value[slotKey] > 0) {
    slotCounts.value[slotKey]--
  }
}

async function handleSubmit() {
  if (!name.value.trim()) {
    error.value = 'Digite um nome para a party'
    return
  }

  if (totalSlots.value < 2 || totalSlots.value > 6) {
    error.value = 'Selecione entre 2 e 6 vagas'
    return
  }

  if (!creatorSlotClass.value) {
    error.value = 'Selecione sua classe na party'
    return
  }

  isSubmitting.value = true
  error.value = null

  try {
    let success: boolean

    if (props.eventId) {
      success = await partiesStore.createEventParty(props.eventId, {
        name: name.value.trim(),
        description: description.value.trim() || null,
        slots: slots.value,
        creatorSlotClass: creatorSlotClass.value
      })
    } else {
      success = await partiesStore.createGlobalParty({
        name: name.value.trim(),
        description: description.value.trim() || null,
        slots: slots.value,
        creatorSlotClass: creatorSlotClass.value
      })
    }

    if (success) {
      close()
    } else {
      error.value = partiesStore.error || 'Erro ao criar party'
    }
  } catch (err) {
    error.value = 'Erro ao criar party'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="show"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/60 backdrop-blur-sm"
          @click="close"
        />

        <!-- Modal -->
        <div class="relative bg-dark-700 rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-auto">
          <!-- Header -->
          <div class="flex items-center justify-between p-4 border-b border-dark-600">
            <h2 class="text-lg font-semibold text-gray-100">Nova Party</h2>
            <button
              @click="close"
              class="p-1 text-gray-400 hover:text-gray-200 rounded-lg hover:bg-dark-600 transition-colors"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Form -->
          <form @submit.prevent="handleSubmit" class="p-4 space-y-4">
            <!-- Error -->
            <div v-if="error" class="p-3 rounded-lg bg-red-900/20 border border-red-500/30 text-red-400 text-sm">
              {{ error }}
            </div>

            <!-- Name -->
            <div>
              <label for="name" class="label">Nome da Party *</label>
              <input
                id="name"
                v-model="name"
                type="text"
                class="input"
                placeholder="Ex: Party de Farm"
                :disabled="isSubmitting"
                maxlength="50"
              />
            </div>

            <!-- Description -->
            <div>
              <label for="description" class="label">Descrição</label>
              <textarea
                id="description"
                v-model="description"
                class="input min-h-[60px] resize-none"
                placeholder="Descrição opcional..."
                :disabled="isSubmitting"
                maxlength="200"
              />
            </div>

            <!-- Class Slots -->
            <div>
              <div class="flex items-center justify-between mb-2">
                <label class="label mb-0">Vagas por Classe *</label>
                <span class="text-sm text-gray-400">Total: {{ totalSlots }}</span>
              </div>

              <div class="grid grid-cols-2 gap-2">
                <div
                  v-for="slotKey in allSlotOptions"
                  :key="slotKey"
                  class="flex items-center justify-between p-2 rounded-lg"
                  :class="[
                    slotCounts[slotKey] > 0 ? 'ring-1 ring-primary-500' : '',
                    slotKey === 'FREE' ? 'bg-amber-900/30' : 'bg-dark-600'
                  ]"
                >
                  <span class="text-sm" :class="slotKey === 'FREE' ? 'text-amber-300 font-medium' : 'text-gray-200'">
                    {{ getSlotDisplayName(slotKey) }}
                  </span>
                  <div class="flex items-center gap-1">
                    <button
                      type="button"
                      @click="decrementSlot(slotKey)"
                      :disabled="isSubmitting || slotCounts[slotKey] === 0"
                      class="w-6 h-6 flex items-center justify-center rounded bg-dark-500 text-gray-300 hover:bg-dark-400 disabled:opacity-30 disabled:cursor-not-allowed"
                    >
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
                      </svg>
                    </button>
                    <span class="w-6 text-center text-sm font-medium text-gray-100">{{ slotCounts[slotKey] }}</span>
                    <button
                      type="button"
                      @click="incrementSlot(slotKey)"
                      :disabled="isSubmitting || slotCounts[slotKey] >= 6 || totalSlots >= 6"
                      class="w-6 h-6 flex items-center justify-center rounded bg-dark-500 text-gray-300 hover:bg-dark-400 disabled:opacity-30 disabled:cursor-not-allowed"
                    >
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
              <p class="text-xs text-amber-400/80 mt-2">* "Livre" permite qualquer classe entrar</p>
            </div>

            <!-- Creator's Class Selection -->
            <div v-if="availableClasses.length > 0">
              <label class="label">Sua vaga na Party *</label>
              <p class="text-xs text-gray-400 mb-2">Escolha qual classe você vai ocupar</p>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="slotKey in availableClasses"
                  :key="slotKey"
                  type="button"
                  @click="creatorSlotClass = slotKey"
                  :disabled="isSubmitting"
                  class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
                  :class="creatorSlotClass === slotKey
                    ? 'bg-primary-500 text-white'
                    : slotKey === 'FREE' ? 'bg-amber-900/50 text-amber-300 hover:bg-amber-900/70' : 'bg-dark-600 text-gray-300 hover:bg-dark-500'"
                >
                  {{ getSlotDisplayName(slotKey) }}
                </button>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex gap-3 pt-2">
              <button
                type="button"
                @click="close"
                class="btn btn-secondary flex-1"
                :disabled="isSubmitting"
              >
                Cancelar
              </button>
              <button
                type="submit"
                class="btn btn-primary flex-1"
                :disabled="isSubmitting || !name.trim() || totalSlots < 2 || !creatorSlotClass"
              >
                <LoadingSpinner v-if="isSubmitting" size="sm" class="mr-2" />
                {{ isSubmitting ? 'Criando...' : 'Criar Party' }}
              </button>
            </div>
          </form>
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
