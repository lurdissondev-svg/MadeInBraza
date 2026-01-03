<script setup lang="ts">
import { ref } from 'vue'
import { SWResponseType, SWTag, PlayerClassAbbreviations } from '@/types'
import type { SiegeWarHistoryItem } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

defineProps<{
  history: SiegeWarHistoryItem[]
  loading: boolean
}>()

const expandedIds = ref<Set<string>>(new Set())

function toggleExpand(id: string) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

function formatDate(weekEnd: string): string {
  try {
    // weekEnd is already Sunday 23:59:59
    const date = new Date(weekEnd)
    return `Domingo, ${date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })}`
  } catch {
    return weekEnd
  }
}

const tagColors: Record<SWTag, string> = {
  [SWTag.ATTACK]: 'bg-red-500',
  [SWTag.DEFENSE]: 'bg-blue-500',
  [SWTag.ACADEMY]: 'bg-green-500'
}

const tagNames: Record<SWTag, string> = {
  [SWTag.ATTACK]: 'ATAQUE',
  [SWTag.DEFENSE]: 'DEFESA',
  [SWTag.ACADEMY]: 'ACADEMY'
}

const responseColors: Record<SWResponseType, string> = {
  [SWResponseType.CONFIRMED]: 'bg-primary-500/20 text-primary-400',
  [SWResponseType.SHARED]: 'bg-purple-500/20 text-purple-400',
  [SWResponseType.PILOT]: 'bg-cyan-500/20 text-cyan-400',
  [SWResponseType.ABSENT]: 'bg-red-500/20 text-red-400'
}

const responseLabels: Record<SWResponseType, string> = {
  [SWResponseType.CONFIRMED]: 'CONFIRMADO',
  [SWResponseType.SHARED]: 'SHARED',
  [SWResponseType.PILOT]: 'PILOTO',
  [SWResponseType.ABSENT]: 'AUSENTE'
}

function getResponseIcon(type: SWResponseType): string {
  switch (type) {
    case SWResponseType.CONFIRMED: return 'M5 13l4 4L19 7'
    case SWResponseType.SHARED: return 'M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z'
    case SWResponseType.PILOT: return 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z'
    case SWResponseType.ABSENT: return 'M6 18L18 6M6 6l12 12'
  }
}
</script>

<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <LoadingSpinner />
    </div>

    <!-- Empty -->
    <div v-else-if="history.length === 0" class="text-center py-12">
      <p class="text-gray-400">Nenhum histórico disponível</p>
    </div>

    <!-- History List -->
    <div v-else class="space-y-4">
      <div
        v-for="sw in history"
        :key="sw.id"
        class="card"
      >
        <!-- Header (clickable) -->
        <button
          @click="toggleExpand(sw.id)"
          class="w-full text-left"
        >
          <div class="flex items-center justify-between mb-4">
            <h4 class="font-semibold text-gray-100">{{ formatDate(sw.weekEnd) }}</h4>
            <svg
              class="w-5 h-5 text-gray-400 transition-transform"
              :class="{ 'rotate-180': expandedIds.has(sw.id) }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </div>

          <!-- Summary Chips -->
          <div class="flex flex-wrap gap-3">
            <div class="text-center">
              <span class="text-lg font-bold text-primary-400">{{ sw.summary.responded }}</span>
              <span class="text-xs text-gray-400 ml-1">Resp.</span>
            </div>
            <div class="text-center">
              <span class="text-lg font-bold text-primary-400">{{ sw.summary.confirmed }}</span>
              <span class="text-xs text-gray-400 ml-1">Conf.</span>
            </div>
            <div class="text-center">
              <span class="text-lg font-bold text-purple-400">{{ sw.summary.shared }}</span>
              <span class="text-xs text-gray-400 ml-1">Comp.</span>
            </div>
            <div class="text-center">
              <span class="text-lg font-bold text-cyan-400">{{ sw.summary.pilots }}</span>
              <span class="text-xs text-gray-400 ml-1">Pilot</span>
            </div>
            <div class="text-center">
              <span class="text-lg font-bold text-red-400">{{ sw.summary.absent }}</span>
              <span class="text-xs text-gray-400 ml-1">Aus.</span>
            </div>
          </div>
        </button>

        <!-- Expanded Content -->
        <div v-if="expandedIds.has(sw.id)" class="mt-4 pt-4 border-t border-dark-600">
          <div v-if="sw.responses.length === 0" class="text-sm text-gray-400">
            Nenhuma resposta registrada
          </div>
          <div v-else class="space-y-2">
            <div
              v-for="response in sw.responses"
              :key="response.id"
              class="flex items-center justify-between py-2"
            >
              <div class="flex items-center gap-2">
                <svg
                  class="w-4 h-4"
                  :class="responseColors[response.responseType].split(' ')[1]"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="getResponseIcon(response.responseType)" />
                </svg>
                <span class="text-gray-200">{{ response.user.nick }}</span>
                <span class="text-sm text-gray-500">({{ PlayerClassAbbreviations[response.user.playerClass] }})</span>
              </div>
              <div class="flex items-center gap-2">
                <span
                  v-if="response.tag"
                  class="px-2 py-0.5 rounded text-xs text-white"
                  :class="tagColors[response.tag]"
                >
                  {{ tagNames[response.tag] }}
                </span>
                <span
                  class="px-2 py-0.5 rounded text-xs font-medium"
                  :class="responseColors[response.responseType]"
                >
                  {{ responseLabels[response.responseType] }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
