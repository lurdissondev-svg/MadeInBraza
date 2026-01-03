<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMembersStore } from '@/stores/members'
import MainLayout from '@/components/layout/MainLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import MemberCard from '@/components/members/MemberCard.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import type { Member } from '@/types'
import { Role } from '@/types'

const router = useRouter()
const membersStore = useMembersStore()

const memberToPromote = ref<Member | null>(null)
const memberToDemote = ref<Member | null>(null)
const memberToBan = ref<Member | null>(null)
const memberToUpdateRole = ref<{ member: Member; role: Role } | null>(null)

onMounted(() => {
  membersStore.fetchMembers()
})

function handleMemberClick(member: Member) {
  router.push(`/members/${member.id}`)
}

async function handlePromote() {
  if (memberToPromote.value) {
    await membersStore.promoteMember(memberToPromote.value.id)
    memberToPromote.value = null
  }
}

async function handleDemote() {
  if (memberToDemote.value) {
    await membersStore.demoteMember(memberToDemote.value.id)
    memberToDemote.value = null
  }
}

async function handleBan() {
  if (memberToBan.value) {
    await membersStore.banMember(memberToBan.value.id)
    memberToBan.value = null
  }
}

function handleUpdateRole(member: Member, role: Role) {
  memberToUpdateRole.value = { member, role }
}

async function confirmUpdateRole() {
  if (memberToUpdateRole.value) {
    await membersStore.updateMemberRole(memberToUpdateRole.value.member.id, memberToUpdateRole.value.role)
    memberToUpdateRole.value = null
  }
}

function getRoleName(role: Role): string {
  switch (role) {
    case Role.LEADER: return 'Líder'
    case Role.COUNSELOR: return 'Conselheiro'
    default: return 'Membro'
  }
}
</script>

<template>
  <MainLayout>
    <div class="space-y-4">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-bold text-gray-100">Membros da Guilda</h2>
        <div class="text-sm text-gray-400">
          {{ membersStore.members.length }} membros
        </div>
      </div>

      <!-- Stats -->
      <div class="flex flex-wrap gap-4 text-sm">
        <div class="flex items-center gap-2">
          <span class="text-primary-400">👑 {{ membersStore.leaderCount }}</span>
          <span class="text-gray-500">líderes</span>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-amber-400">⭐ {{ membersStore.counselorCount }}</span>
          <span class="text-gray-500">conselheiros</span>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-gray-300">{{ membersStore.memberCount }}</span>
          <span class="text-gray-500">membros</span>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="membersStore.loading && membersStore.members.length === 0" class="flex justify-center py-12">
        <LoadingSpinner />
      </div>

      <!-- Error -->
      <div v-else-if="membersStore.error && membersStore.members.length === 0" class="card bg-red-900/20 border border-red-500/30">
        <p class="text-red-400">{{ membersStore.error }}</p>
        <button
          @click="membersStore.fetchMembers()"
          class="mt-4 btn btn-primary"
        >
          Tentar novamente
        </button>
      </div>

      <!-- Empty -->
      <div v-else-if="membersStore.members.length === 0" class="card text-center py-8">
        <svg class="w-16 h-16 mx-auto text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
        <p class="text-gray-400">Nenhum membro encontrado</p>
      </div>

      <!-- Members List -->
      <div v-else class="space-y-2">
        <MemberCard
          v-for="member in membersStore.sortedMembers"
          :key="member.id"
          :member="member"
          :is-leader="membersStore.isLeader"
          :current-user-id="membersStore.currentUserId"
          :promoting-id="membersStore.promotingId"
          :demoting-id="membersStore.demotingId"
          :updating-role-id="membersStore.updatingRoleId"
          :banning-id="membersStore.banningId"
          @click="handleMemberClick(member)"
          @promote="memberToPromote = member"
          @demote="memberToDemote = member"
          @update-role="(role) => handleUpdateRole(member, role)"
          @ban="memberToBan = member"
        />
      </div>
    </div>

    <!-- Error toast -->
    <div
      v-if="membersStore.error && membersStore.members.length > 0"
      class="fixed bottom-20 left-4 right-4 bg-red-500/90 text-white px-4 py-3 rounded-lg flex items-center justify-between z-50"
    >
      <span>{{ membersStore.error }}</span>
      <button @click="membersStore.clearError()" class="ml-4 font-bold">OK</button>
    </div>

    <!-- Promote Dialog (legacy) -->
    <ConfirmDialog
      v-if="memberToPromote"
      title="Promover a Líder"
      :message="`Tem certeza que deseja promover ${memberToPromote.nick} a líder?`"
      confirm-text="Promover"
      @confirm="handlePromote"
      @cancel="memberToPromote = null"
    />

    <!-- Demote Dialog (legacy) -->
    <ConfirmDialog
      v-if="memberToDemote"
      title="Rebaixar para Membro"
      :message="`Tem certeza que deseja rebaixar ${memberToDemote.nick} para membro?`"
      confirm-text="Rebaixar"
      @confirm="handleDemote"
      @cancel="memberToDemote = null"
    />

    <!-- Update Role Dialog -->
    <ConfirmDialog
      v-if="memberToUpdateRole"
      :title="`Alterar cargo para ${getRoleName(memberToUpdateRole.role)}`"
      :message="`Tem certeza que deseja alterar o cargo de ${memberToUpdateRole.member.nick} para ${getRoleName(memberToUpdateRole.role)}?`"
      confirm-text="Confirmar"
      @confirm="confirmUpdateRole"
      @cancel="memberToUpdateRole = null"
    />

    <!-- Ban Dialog -->
    <ConfirmDialog
      v-if="memberToBan"
      title="Banir Membro"
      :message="`Tem certeza que deseja banir ${memberToBan.nick}? Esta ação pode ser revertida.`"
      confirm-text="Banir"
      confirm-variant="danger"
      @confirm="handleBan"
      @cancel="memberToBan = null"
    />
  </MainLayout>
</template>
