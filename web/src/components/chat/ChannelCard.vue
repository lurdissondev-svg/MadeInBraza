<script setup lang="ts">
import { ChannelType } from '@/types'
import type { Channel } from '@/types'

defineProps<{
  channel: Channel
  unreadCount?: number
}>()

const emit = defineEmits<{
  click: []
  showMembers: []
}>()

const typeLabels: Record<ChannelType, string> = {
  [ChannelType.GENERAL]: 'Geral',
  [ChannelType.LEADERS]: 'Lideres',
  [ChannelType.EVENT]: 'Evento',
  [ChannelType.PARTY]: 'Party'
}

const typeColors: Record<ChannelType, string> = {
  [ChannelType.GENERAL]: 'bg-dark-600',
  [ChannelType.LEADERS]: 'bg-primary-500/20 border-primary-500/30',
  [ChannelType.EVENT]: 'bg-blue-500/20 border-blue-500/30',
  [ChannelType.PARTY]: 'bg-purple-500/20 border-purple-500/30'
}
</script>

<template>
  <div
    @click="emit('click')"
    class="card cursor-pointer transition-all hover:scale-[1.02] border"
    :class="typeColors[channel.type]"
  >
    <div class="flex items-center gap-4">
      <!-- Icon with unread badge -->
      <div class="relative">
        <div class="w-10 h-10 rounded-full bg-primary-500/20 flex items-center justify-center">
          <svg
            v-if="channel.type === ChannelType.GENERAL"
            class="w-5 h-5 text-primary-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          <svg
            v-else-if="channel.type === ChannelType.LEADERS"
            class="w-5 h-5 text-primary-400"
            fill="currentColor"
            viewBox="0 0 24 24"
          >
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
          </svg>
          <svg
            v-else-if="channel.type === ChannelType.EVENT"
            class="w-5 h-5 text-blue-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <svg
            v-else
            class="w-5 h-5 text-purple-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
        </div>
        <!-- Unread badge -->
        <div
          v-if="unreadCount && unreadCount > 0"
          class="absolute -top-1 -right-1 min-w-5 h-5 px-1.5 rounded-full bg-red-500 text-white text-xs font-bold flex items-center justify-center"
        >
          {{ unreadCount > 99 ? '99+' : unreadCount }}
        </div>
      </div>

      <!-- Channel info -->
      <div class="flex-1 min-w-0">
        <h4
          class="font-semibold truncate"
          :class="unreadCount && unreadCount > 0 ? 'text-primary-400 font-bold' : 'text-gray-100'"
        >
          {{ channel.name }}
        </h4>
        <p class="text-sm text-gray-400">{{ typeLabels[channel.type] }}</p>
        <p v-if="channel._count?.messages" class="text-xs text-gray-500">
          {{ channel._count.messages }} mensagens
        </p>
      </div>

      <!-- Members button -->
      <button
        @click.stop="emit('showMembers')"
        class="p-2 hover:bg-dark-500 rounded-lg transition-colors"
      >
        <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      </button>

      <!-- Arrow -->
      <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
      </svg>
    </div>
  </div>
</template>
