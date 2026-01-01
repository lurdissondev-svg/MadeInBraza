<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useChannelsStore } from '@/stores/channels'
import MainLayout from '@/components/layout/MainLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ChannelCard from '@/components/chat/ChannelCard.vue'
import MembersSheet from '@/components/chat/MembersSheet.vue'
import type { Channel } from '@/types'

const router = useRouter()
const channelsStore = useChannelsStore()

const showMembersSheet = ref(false)
const selectedChannel = ref<Channel | null>(null)

onMounted(() => {
  channelsStore.fetchChannels()
})

function handleChannelClick(channel: Channel) {
  router.push(`/web/channels/${channel.id}`)
}

function handleShowMembers(channel: Channel) {
  selectedChannel.value = channel
  channelsStore.openChannel(channel).then(() => {
    channelsStore.fetchMembers()
    showMembersSheet.value = true
  })
}

function closeMembersSheet() {
  showMembersSheet.value = false
  selectedChannel.value = null
}
</script>

<template>
  <MainLayout>
    <div class="space-y-4">
      <h2 class="text-xl font-bold text-gray-100">Canais</h2>

      <!-- Loading -->
      <div v-if="channelsStore.loading && channelsStore.channels.length === 0" class="flex justify-center py-12">
        <LoadingSpinner />
      </div>

      <!-- Error -->
      <div v-else-if="channelsStore.error" class="card bg-red-900/20 border border-red-500/30">
        <p class="text-red-400">{{ channelsStore.error }}</p>
        <button
          @click="channelsStore.fetchChannels()"
          class="mt-4 btn btn-primary"
        >
          Tentar novamente
        </button>
      </div>

      <!-- Empty -->
      <template v-else-if="channelsStore.channels.length === 0">
        <div class="card text-center py-8">
          <svg class="w-16 h-16 mx-auto text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8h2a2 2 0 012 2v6a2 2 0 01-2 2h-2v4l-4-4H9a1.994 1.994 0 01-1.414-.586m0 0L11 14h4a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2v4l.586-.586z" />
          </svg>
          <p class="text-gray-400 mb-4">Nenhum canal disponivel</p>
          <button
            @click="channelsStore.setupDefaultChannels()"
            :disabled="channelsStore.loading"
            class="btn btn-primary"
          >
            <LoadingSpinner v-if="channelsStore.loading" size="sm" class="mr-2" />
            Criar Canais Padrao
          </button>
        </div>
      </template>

      <!-- Channels List -->
      <div v-else class="space-y-3">
        <ChannelCard
          v-for="channel in channelsStore.sortedChannels"
          :key="channel.id"
          :channel="channel"
          :unread-count="channelsStore.unreadCounts[channel.id]"
          @click="handleChannelClick(channel)"
          @show-members="handleShowMembers(channel)"
        />
      </div>
    </div>

    <!-- Members Sheet -->
    <MembersSheet
      v-if="showMembersSheet && selectedChannel"
      :channel-name="selectedChannel.name"
      :members="channelsStore.members"
      :loading="channelsStore.loadingMembers"
      @close="closeMembersSheet"
    />
  </MainLayout>
</template>
