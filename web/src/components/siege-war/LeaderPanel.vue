<script setup lang="ts">
import { SWResponseType, SWTag, PlayerClassAbbreviations, PlayerClassNames } from '@/types'
import type { SWResponseItem, SWResponseUser, SWResponsesSummary } from '@/types'

defineProps<{
  summary: SWResponsesSummary | null
  responses: SWResponseItem[]
  notResponded: SWResponseUser[]
  isActive: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

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
  [SWResponseType.CONFIRMED]: 'text-primary-400',
  [SWResponseType.SHARED]: 'text-purple-400',
  [SWResponseType.PILOT]: 'text-cyan-400',
  [SWResponseType.ABSENT]: 'text-red-400'
}

const responseLabels: Record<SWResponseType, string> = {
  [SWResponseType.CONFIRMED]: 'CONFIRMADO',
  [SWResponseType.SHARED]: 'SHARED',
  [SWResponseType.PILOT]: 'PILOTO',
  [SWResponseType.ABSENT]: 'AUSENTE'
}

function handleClose() {
  if (confirm('Tem certeza que deseja fechar este Siege War?')) {
    emit('close')
  }
}
</script>

<template>
  <div class="card">
    <div class="flex items-center justify-between mb-6">
      <h3 class="text-lg font-semibold text-gray-100">Painel do Líder</h3>
      <button
        v-if="isActive"
        @click="handleClose"
        class="flex items-center gap-2 px-3 py-1.5 text-sm text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-lg transition-colors"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
        </svg>
        Fechar SW
      </button>
    </div>

    <!-- Summary -->
    <div v-if="summary" class="grid grid-cols-3 sm:grid-cols-6 gap-4 mb-6">
      <div class="text-center">
        <p class="text-2xl font-bold text-gray-100">{{ summary.total }}</p>
        <p class="text-xs text-gray-400">Total</p>
      </div>
      <div class="text-center">
        <p class="text-2xl font-bold text-primary-400">{{ summary.responded }}</p>
        <p class="text-xs text-gray-400">Resp.</p>
      </div>
      <div class="text-center">
        <p class="text-2xl font-bold text-primary-400">{{ summary.confirmed }}</p>
        <p class="text-xs text-gray-400">Conf.</p>
      </div>
      <div class="text-center">
        <p class="text-2xl font-bold text-purple-400">{{ summary.shared }}</p>
        <p class="text-xs text-gray-400">Comp.</p>
      </div>
      <div class="text-center">
        <p class="text-2xl font-bold text-cyan-400">{{ summary.pilots }}</p>
        <p class="text-xs text-gray-400">Pilot</p>
      </div>
      <div class="text-center">
        <p class="text-2xl font-bold text-red-400">{{ summary.absent }}</p>
        <p class="text-xs text-gray-400">Aus.</p>
      </div>
    </div>

    <!-- Not Responded -->
    <div v-if="notResponded.length > 0" class="mb-6">
      <p class="text-sm font-medium text-red-400 mb-2">Não responderam ({{ notResponded.length }}):</p>
      <p class="text-sm text-gray-400">
        {{ notResponded.map(u => u.nick).join(', ') }}
      </p>
    </div>

    <!-- Responses List -->
    <div v-if="responses.length > 0">
      <p class="text-sm font-medium text-gray-300 mb-3">Respostas:</p>
      <div class="space-y-3">
        <div
          v-for="response in responses"
          :key="response.id"
          class="p-3 rounded-lg border"
          :class="{
            'bg-primary-500/10 border-primary-500/30': response.responseType === SWResponseType.CONFIRMED,
            'bg-purple-500/10 border-purple-500/30': response.responseType === SWResponseType.SHARED,
            'bg-cyan-500/10 border-cyan-500/30': response.responseType === SWResponseType.PILOT,
            'bg-red-500/10 border-red-500/30': response.responseType === SWResponseType.ABSENT
          }"
        >
          <!-- Header -->
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-2">
              <svg
                class="w-4 h-4"
                :class="responseColors[response.responseType]"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  v-if="response.responseType === SWResponseType.CONFIRMED"
                  stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"
                />
                <path
                  v-else-if="response.responseType === SWResponseType.SHARED"
                  stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"
                />
                <path
                  v-else-if="response.responseType === SWResponseType.PILOT"
                  stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                />
                <path
                  v-else
                  stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"
                />
              </svg>
              <span class="font-medium text-gray-100">{{ response.user.nick }}</span>
              <span class="text-sm text-gray-400">({{ PlayerClassAbbreviations[response.user.playerClass] }})</span>
            </div>
            <span
              v-if="response.tag"
              class="px-2 py-0.5 rounded text-xs text-white"
              :class="tagColors[response.tag]"
            >
              {{ tagNames[response.tag] }}
            </span>
          </div>

          <!-- Response Type -->
          <p class="text-sm font-medium mb-1" :class="responseColors[response.responseType]">
            {{ responseLabels[response.responseType] }}
          </p>

          <!-- Details -->
          <div class="text-sm text-gray-400">
            <template v-if="response.responseType === SWResponseType.CONFIRMED">
              Vai participar com sua própria conta
            </template>
            <template v-else-if="response.responseType === SWResponseType.SHARED">
              <p>Deixando conta para pilotagem:</p>
              <p>ID: {{ response.gameId || '-' }}</p>
              <p>Senha: {{ response.password || '***' }}</p>
              <p>Classe: {{ response.sharedClass ? PlayerClassNames[response.sharedClass] : '-' }}</p>
            </template>
            <template v-else-if="response.responseType === SWResponseType.PILOT">
              <p>Vai pilotar conta de outro membro:</p>
              <p>
                Pilotando: {{ response.pilotingFor?.nick || '-' }}
                <span v-if="response.pilotingFor?.playerClass">({{ PlayerClassAbbreviations[response.pilotingFor.playerClass] }})</span>
              </p>
              <p>Classe preferida: {{ response.preferredClass ? PlayerClassNames[response.preferredClass] : '-' }}</p>
            </template>
            <template v-else>
              Não vai participar desta SW
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
