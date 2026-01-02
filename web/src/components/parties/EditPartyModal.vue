<script setup lang="ts">
import { ref, watch } from 'vue'
import { usePartiesStore } from '@/stores/parties'
import type { Party } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps<{
  show: boolean
  party: Party | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const partiesStore = usePartiesStore()

const name = ref('')
const description = ref('')
const isSubmitting = ref(false)
const error = ref<string | null>(null)

watch(() => props.show, (newValue) => {
  if (newValue && props.party) {
    name.value = props.party.name
    description.value = props.party.description || ''
    error.value = null
  }
})

function close() {
  emit('update:show', false)
}

async function handleSubmit() {
  if (!props.party) return

  if (!name.value.trim()) {
    error.value = 'Digite um nome para a party'
    return
  }

  isSubmitting.value = true
  error.value = null

  try {
    const success = await partiesStore.updateParty(props.party.id, {
      name: name.value.trim(),
      description: description.value.trim() || null
    })

    if (success) {
      close()
    } else {
      error.value = partiesStore.error || 'Erro ao atualizar party'
    }
  } catch (err) {
    error.value = 'Erro ao atualizar party'
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
        <div class="relative bg-dark-700 rounded-xl shadow-xl w-full max-w-lg overflow-hidden">
          <!-- Header -->
          <div class="flex items-center justify-between p-4 border-b border-dark-600">
            <h2 class="text-lg font-semibold text-gray-100">Editar Party</h2>
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
              <label for="edit-name" class="label">Nome da Party *</label>
              <input
                id="edit-name"
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
              <label for="edit-description" class="label">Descricao</label>
              <textarea
                id="edit-description"
                v-model="description"
                class="input min-h-[80px] resize-none"
                placeholder="Descricao opcional..."
                :disabled="isSubmitting"
                maxlength="200"
              />
            </div>

            <!-- Info about slots -->
            <div class="p-3 rounded-lg bg-dark-600 text-gray-400 text-sm">
              <p class="flex items-center gap-2">
                <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                As vagas da party nao podem ser alteradas apos a criacao.
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
                :disabled="isSubmitting || !name.trim()"
              >
                <LoadingSpinner v-if="isSubmitting" size="sm" class="mr-2" />
                {{ isSubmitting ? 'Salvando...' : 'Salvar' }}
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
