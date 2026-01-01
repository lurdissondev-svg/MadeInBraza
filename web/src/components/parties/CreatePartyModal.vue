<script setup lang="ts">
import { ref, watch } from 'vue'
import { usePartiesStore } from '@/stores/parties'
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
const maxMembers = ref(5)
const isSubmitting = ref(false)
const error = ref<string | null>(null)

watch(() => props.show, (newValue) => {
  if (newValue) {
    // Reset form when opening
    name.value = ''
    description.value = ''
    maxMembers.value = 5
    error.value = null
  }
})

function close() {
  emit('update:show', false)
}

async function handleSubmit() {
  if (!name.value.trim()) {
    error.value = 'Digite um nome para a party'
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
        maxMembers: maxMembers.value
      })
    } else {
      success = await partiesStore.createGlobalParty({
        name: name.value.trim(),
        description: description.value.trim() || null,
        maxMembers: maxMembers.value
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
        <div class="relative bg-dark-700 rounded-xl shadow-xl w-full max-w-md max-h-[90vh] overflow-auto">
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
                class="input min-h-[80px] resize-none"
                placeholder="Descrição opcional..."
                :disabled="isSubmitting"
                maxlength="200"
              />
            </div>

            <!-- Max Members -->
            <div>
              <label for="maxMembers" class="label">Máximo de Membros</label>
              <div class="flex items-center gap-3">
                <input
                  id="maxMembers"
                  v-model.number="maxMembers"
                  type="range"
                  class="flex-1 h-2 bg-dark-600 rounded-lg appearance-none cursor-pointer accent-primary-500"
                  min="2"
                  max="6"
                  :disabled="isSubmitting"
                />
                <span class="text-gray-100 font-medium w-6 text-center">{{ maxMembers }}</span>
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
                :disabled="isSubmitting || !name.trim()"
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
