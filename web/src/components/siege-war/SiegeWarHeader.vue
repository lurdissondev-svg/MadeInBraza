<script setup lang="ts">
import type { SiegeWar } from '@/types'

defineProps<{
  siegeWar: SiegeWar
}>()

function formatDate(weekEnd: string): string {
  try {
    const date = new Date(weekEnd)
    // Go back to Sunday (weekEnd is Monday early morning)
    const day = date.getDay()
    const daysToSubtract = day === 0 ? 0 : day
    date.setDate(date.getDate() - daysToSubtract)

    const dayName = date.toLocaleDateString('pt-BR', { weekday: 'long' })
    const dateStr = date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
    return `${dayName.charAt(0).toUpperCase() + dayName.slice(1)}, ${dateStr}`
  } catch {
    return weekEnd
  }
}
</script>

<template>
  <div class="bg-primary-700 rounded-xl p-6 mb-6">
    <h2 class="text-2xl font-bold text-white mb-3">{{ formatDate(siegeWar.weekEnd) }}</h2>
    <span
      class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium"
      :class="siegeWar.isActive ? 'bg-primary-500/30 text-primary-100 border border-primary-400' : 'bg-red-500 text-white'"
    >
      {{ siegeWar.isActive ? 'ATIVO' : 'FECHADO' }}
    </span>
  </div>
</template>
