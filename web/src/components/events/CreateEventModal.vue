<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useEventsStore } from '@/stores/events'
import { PlayerClass, PlayerClassNames } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const eventsStore = useEventsStore()

const title = ref('')
const description = ref('')
const eventDate = ref('')
const eventTime = ref('')
const maxParticipants = ref<number | null>(null)
const selectedClasses = ref<PlayerClass[]>([])
const isSubmitting = ref(false)
const error = ref<string | null>(null)

const allClasses = Object.values(PlayerClass)

const minDate = computed(() => {
  const now = new Date()
  return now.toISOString().split('T')[0]
})

watch(() => props.show, (newValue) => {
  if (newValue) {
    // Reset form when opening
    title.value = ''
    description.value = ''
    eventDate.value = ''
    eventTime.value = ''
    maxParticipants.value = null
    selectedClasses.value = []
    error.value = null
  }
})

function close() {
  emit('update:show', false)
}

function toggleClass(playerClass: PlayerClass) {
  const index = selectedClasses.value.indexOf(playerClass)
  if (index === -1) {
    selectedClasses.value.push(playerClass)
  } else {
    selectedClasses.value.splice(index, 1)
  }
}

function selectAllClasses() {
  if (selectedClasses.value.length === allClasses.length) {
    selectedClasses.value = []
  } else {
    selectedClasses.value = [...allClasses]
  }
}

async function handleSubmit() {
  if (!title.value.trim() || !eventDate.value || !eventTime.value) {
    error.value = 'Preencha todos os campos obrigatórios'
    return
  }

  isSubmitting.value = true
  error.value = null

  try {
    const dateTime = new Date(`${eventDate.value}T${eventTime.value}`)

    const success = await eventsStore.createEvent({
      title: title.value.trim(),
      description: description.value.trim() || null,
      eventDate: dateTime.toISOString(),
      maxParticipants: maxParticipants.value || null,
      requiredClasses: selectedClasses.value.length > 0 ? selectedClasses.value : undefined
    })

    if (success) {
      close()
    } else {
      error.value = eventsStore.error || 'Erro ao criar evento'
    }
  } catch (err) {
    error.value = 'Erro ao criar evento'
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
            <h2 class="text-lg font-semibold text-gray-100">Novo Evento</h2>
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

            <!-- Title -->
            <div>
              <label for="title" class="label">Título *</label>
              <input
                id="title"
                v-model="title"
                type="text"
                class="input"
                placeholder="Nome do evento"
                :disabled="isSubmitting"
                maxlength="100"
              />
            </div>

            <!-- Description -->
            <div>
              <label for="description" class="label">Descrição</label>
              <textarea
                id="description"
                v-model="description"
                class="input min-h-[80px] resize-none"
                placeholder="Descrição opcional..."
                :disabled="isSubmitting"
                maxlength="500"
              />
            </div>

            <!-- Date and Time -->
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label for="date" class="label">Data *</label>
                <input
                  id="date"
                  v-model="eventDate"
                  type="date"
                  class="input"
                  :min="minDate"
                  :disabled="isSubmitting"
                />
              </div>
              <div>
                <label for="time" class="label">Horário *</label>
                <input
                  id="time"
                  v-model="eventTime"
                  type="time"
                  class="input"
                  :disabled="isSubmitting"
                />
              </div>
            </div>

            <!-- Max Participants -->
            <div>
              <label for="maxParticipants" class="label">Máximo de Participantes</label>
              <input
                id="maxParticipants"
                v-model.number="maxParticipants"
                type="number"
                class="input"
                placeholder="Sem limite"
                min="1"
                max="100"
                :disabled="isSubmitting"
              />
            </div>

            <!-- Required Classes -->
            <div>
              <div class="flex items-center justify-between mb-2">
                <label class="label mb-0">Classes Permitidas</label>
                <button
                  type="button"
                  @click="selectAllClasses"
                  class="text-xs text-primary-400 hover:text-primary-300"
                >
                  {{ selectedClasses.length === allClasses.length ? 'Desmarcar todas' : 'Selecionar todas' }}
                </button>
              </div>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="playerClass in allClasses"
                  :key="playerClass"
                  type="button"
                  @click="toggleClass(playerClass)"
                  class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
                  :class="selectedClasses.includes(playerClass)
                    ? 'bg-primary-500 text-dark-900'
                    : 'bg-dark-600 text-gray-300 hover:bg-dark-500'"
                  :disabled="isSubmitting"
                >
                  {{ PlayerClassNames[playerClass] }}
                </button>
              </div>
              <p class="text-xs text-gray-500 mt-1">
                {{ selectedClasses.length === 0 ? 'Todas as classes podem participar' : `${selectedClasses.length} classe(s) selecionada(s)` }}
              </p>
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
                :disabled="isSubmitting || !title.trim() || !eventDate || !eventTime"
              >
                <LoadingSpinner v-if="isSubmitting" size="sm" class="mr-2" />
                {{ isSubmitting ? 'Criando...' : 'Criar Evento' }}
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
