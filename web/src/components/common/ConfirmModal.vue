<script setup lang="ts">
const props = defineProps<{
  show: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  confirm: []
  cancel: []
}>()

function close() {
  emit('update:show', false)
  emit('cancel')
}

function handleConfirm() {
  emit('update:show', false)
  emit('confirm')
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
        <div class="relative bg-dark-700 rounded-xl shadow-xl w-full max-w-sm overflow-hidden">
          <!-- Header -->
          <div class="p-4 border-b border-dark-600">
            <h2 class="text-lg font-semibold text-gray-100">{{ title }}</h2>
          </div>

          <!-- Content -->
          <div class="p-4">
            <p class="text-gray-300">{{ message }}</p>
          </div>

          <!-- Actions -->
          <div class="flex gap-3 p-4 border-t border-dark-600">
            <button
              @click="close"
              class="btn btn-secondary flex-1"
            >
              {{ cancelText || 'Cancelar' }}
            </button>
            <button
              @click="handleConfirm"
              class="btn flex-1"
              :class="danger ? 'bg-red-500 hover:bg-red-600 text-white' : 'btn-primary'"
            >
              {{ confirmText || 'Confirmar' }}
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
