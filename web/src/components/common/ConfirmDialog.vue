<script setup lang="ts">
defineProps<{
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  confirmVariant?: 'primary' | 'danger'
  loading?: boolean
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<template>
  <!-- Backdrop -->
  <div
    class="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4"
    @click.self="emit('cancel')"
  >
    <!-- Dialog -->
    <div class="bg-dark-700 rounded-xl max-w-sm w-full p-6 space-y-4 animate-scale-in">
      <h3 class="text-lg font-semibold text-gray-100">{{ title }}</h3>
      <p class="text-gray-400">{{ message }}</p>

      <div class="flex gap-3 pt-2">
        <button
          @click="emit('cancel')"
          :disabled="loading"
          class="flex-1 btn bg-dark-600 hover:bg-dark-500 text-gray-300 disabled:opacity-50"
        >
          {{ cancelText || 'Cancelar' }}
        </button>
        <button
          @click="emit('confirm')"
          :disabled="loading"
          class="flex-1 btn disabled:opacity-50"
          :class="confirmVariant === 'danger' ? 'bg-red-500 hover:bg-red-600 text-white' : 'btn-primary'"
        >
          {{ confirmText || 'Confirmar' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes scale-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.animate-scale-in {
  animation: scale-in 0.2s ease-out;
}
</style>
