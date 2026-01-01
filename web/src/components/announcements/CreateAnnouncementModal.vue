<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAnnouncementsStore } from '@/stores/announcements'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const announcementsStore = useAnnouncementsStore()

const title = ref('')
const content = ref('')
const isSubmitting = ref(false)
const error = ref<string | null>(null)

watch(() => props.show, (newValue) => {
  if (newValue) {
    // Reset form when opening
    title.value = ''
    content.value = ''
    error.value = null
  }
})

function close() {
  emit('update:show', false)
}

async function handleSubmit() {
  if (!title.value.trim() || !content.value.trim()) {
    error.value = 'Preencha todos os campos'
    return
  }

  isSubmitting.value = true
  error.value = null

  try {
    const success = await announcementsStore.createAnnouncement({
      title: title.value.trim(),
      content: content.value.trim()
    })

    if (success) {
      close()
    } else {
      error.value = announcementsStore.error || 'Erro ao criar aviso'
    }
  } catch (err) {
    error.value = 'Erro ao criar aviso'
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
            <h2 class="text-lg font-semibold text-gray-100">Novo Aviso</h2>
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
              <label for="title" class="label">Título</label>
              <input
                id="title"
                v-model="title"
                type="text"
                class="input"
                placeholder="Título do aviso"
                :disabled="isSubmitting"
                maxlength="100"
              />
            </div>

            <!-- Content -->
            <div>
              <label for="content" class="label">Conteúdo</label>
              <textarea
                id="content"
                v-model="content"
                class="input min-h-[120px] resize-none"
                placeholder="Escreva o conteúdo do aviso..."
                :disabled="isSubmitting"
                maxlength="1000"
              />
              <p class="text-xs text-gray-500 mt-1 text-right">
                {{ content.length }}/1000
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
                :disabled="isSubmitting || !title.trim() || !content.trim()"
              >
                <LoadingSpinner v-if="isSubmitting" size="sm" class="mr-2" />
                {{ isSubmitting ? 'Criando...' : 'Criar Aviso' }}
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
