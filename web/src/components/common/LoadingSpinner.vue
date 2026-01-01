<script setup lang="ts">
defineProps<{
  size?: 'sm' | 'md' | 'lg'
}>()
</script>

<template>
  <div
    class="inline-block animate-spin rounded-full border-2 border-solid border-current border-r-transparent text-primary-500"
    :class="{
      'w-4 h-4': size === 'sm',
      'w-8 h-8': size === 'md' || !size,
      'w-12 h-12': size === 'lg'
    }"
    role="status"
  >
    <span class="sr-only">Carregando...</span>
  </div>
</template>
