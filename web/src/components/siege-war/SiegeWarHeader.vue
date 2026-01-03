<script setup lang="ts">
import type { SiegeWar } from '@/types'

defineProps<{
  siegeWar: SiegeWar
}>()

function formatDate(weekEnd: string): string {
  try {
    // weekEnd is already Sunday 23:59:59
    const date = new Date(weekEnd)
    const dayName = date.toLocaleDateString('pt-BR', { weekday: 'long' })
    const dateStr = date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
    return `${dayName.charAt(0).toUpperCase() + dayName.slice(1)}, ${dateStr}`
  } catch {
    return weekEnd
  }
}
</script>

<template>
  <div class="bg-purple-800 rounded-xl p-6 mb-6">
    <h2 class="text-2xl font-bold text-white mb-3">{{ formatDate(siegeWar.weekEnd) }}</h2>
    <span
      class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium"
      :class="siegeWar.isActive ? 'bg-white/20 text-white border border-white/50' : 'bg-red-500 text-white'"
    >
      {{ siegeWar.isActive ? 'ATIVO' : 'FECHADO' }}
    </span>
  </div>
</template>
