import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { membersApi } from '@/services/api/members'
import { useAuthStore } from './auth'
import type { Member, MemberProfile, PendingUser, BannedUser } from '@/types'
import { Role } from '@/types'

export const useMembersStore = defineStore('members', () => {
  // State
  const members = ref<Member[]>([])
  const pendingUsers = ref<PendingUser[]>([])
  const bannedUsers = ref<BannedUser[]>([])
  const selectedMember = ref<MemberProfile | null>(null)

  const loading = ref(false)
  const loadingPending = ref(false)
  const loadingBanned = ref(false)
  const loadingProfile = ref(false)
  const error = ref<string | null>(null)

  // Action loading states
  const promotingId = ref<string | null>(null)
  const demotingId = ref<string | null>(null)
  const updatingRoleId = ref<string | null>(null)
  const banningId = ref<string | null>(null)
  const unbanningId = ref<string | null>(null)
  const approvingId = ref<string | null>(null)
  const rejectingId = ref<string | null>(null)

  // Computed
  const authStore = useAuthStore()

  const isLeader = computed(() => authStore.user?.role === Role.LEADER)
  const currentUserId = computed(() => authStore.user?.id)

  const sortedMembers = computed(() => {
    return [...members.value].sort((a, b) => {
      // Leaders first, then counselors, then members
      const roleOrder = { [Role.LEADER]: 0, [Role.COUNSELOR]: 1, [Role.MEMBER]: 2 }
      const orderA = roleOrder[a.role] ?? 2
      const orderB = roleOrder[b.role] ?? 2
      if (orderA !== orderB) return orderA - orderB
      // Then by nick
      return a.nick.localeCompare(b.nick)
    })
  })

  const leaderCount = computed(() =>
    members.value.filter(m => m.role === Role.LEADER).length
  )

  const counselorCount = computed(() =>
    members.value.filter(m => m.role === Role.COUNSELOR).length
  )

  const memberCount = computed(() =>
    members.value.filter(m => m.role === Role.MEMBER).length
  )

  const pendingCount = computed(() => pendingUsers.value.length)

  // Actions
  async function fetchMembers() {
    loading.value = true
    error.value = null
    try {
      members.value = await membersApi.getMembers()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao carregar membros'
      console.error('Error fetching members:', e)
    } finally {
      loading.value = false
    }
  }

  async function fetchMemberProfile(memberId: string) {
    loadingProfile.value = true
    error.value = null
    try {
      selectedMember.value = await membersApi.getMemberProfile(memberId)
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao carregar perfil'
      console.error('Error fetching member profile:', e)
    } finally {
      loadingProfile.value = false
    }
  }

  async function fetchPendingUsers() {
    loadingPending.value = true
    error.value = null
    try {
      pendingUsers.value = await membersApi.getPendingUsers()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao carregar pendentes'
      console.error('Error fetching pending users:', e)
    } finally {
      loadingPending.value = false
    }
  }

  async function fetchBannedUsers() {
    loadingBanned.value = true
    error.value = null
    try {
      bannedUsers.value = await membersApi.getBannedUsers()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao carregar banidos'
      console.error('Error fetching banned users:', e)
    } finally {
      loadingBanned.value = false
    }
  }

  async function approveUser(userId: string): Promise<boolean> {
    approvingId.value = userId
    error.value = null
    try {
      await membersApi.approveUser(userId)
      pendingUsers.value = pendingUsers.value.filter(u => u.id !== userId)
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao aprovar usuário'
      console.error('Error approving user:', e)
      return false
    } finally {
      approvingId.value = null
    }
  }

  async function rejectUser(userId: string): Promise<boolean> {
    rejectingId.value = userId
    error.value = null
    try {
      await membersApi.rejectUser(userId)
      pendingUsers.value = pendingUsers.value.filter(u => u.id !== userId)
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao rejeitar usuário'
      console.error('Error rejecting user:', e)
      return false
    } finally {
      rejectingId.value = null
    }
  }

  async function promoteMember(memberId: string): Promise<boolean> {
    promotingId.value = memberId
    error.value = null
    try {
      await membersApi.promoteMember(memberId)
      // Update local state
      const member = members.value.find(m => m.id === memberId)
      if (member) {
        member.role = Role.LEADER
      }
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao promover membro'
      console.error('Error promoting member:', e)
      return false
    } finally {
      promotingId.value = null
    }
  }

  async function demoteMember(memberId: string): Promise<boolean> {
    demotingId.value = memberId
    error.value = null
    try {
      await membersApi.demoteMember(memberId)
      // Update local state
      const member = members.value.find(m => m.id === memberId)
      if (member) {
        member.role = Role.MEMBER
      }
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao rebaixar membro'
      console.error('Error demoting member:', e)
      return false
    } finally {
      demotingId.value = null
    }
  }

  async function updateMemberRole(memberId: string, role: Role): Promise<boolean> {
    updatingRoleId.value = memberId
    error.value = null
    try {
      const newRole = await membersApi.updateMemberRole(memberId, role)
      // Update local state
      const member = members.value.find(m => m.id === memberId)
      if (member) {
        member.role = newRole
      }
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao atualizar cargo'
      console.error('Error updating member role:', e)
      return false
    } finally {
      updatingRoleId.value = null
    }
  }

  async function banMember(memberId: string): Promise<boolean> {
    banningId.value = memberId
    error.value = null
    try {
      await membersApi.banMember(memberId)
      // Remove from members list
      members.value = members.value.filter(m => m.id !== memberId)
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao banir membro'
      console.error('Error banning member:', e)
      return false
    } finally {
      banningId.value = null
    }
  }

  async function unbanUser(userId: string): Promise<boolean> {
    unbanningId.value = userId
    error.value = null
    try {
      await membersApi.unbanUser(userId)
      bannedUsers.value = bannedUsers.value.filter(u => u.id !== userId)
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao desbanir usuário'
      console.error('Error unbanning user:', e)
      return false
    } finally {
      unbanningId.value = null
    }
  }

  function clearError() {
    error.value = null
  }

  function clearSelectedMember() {
    selectedMember.value = null
  }

  return {
    // State
    members,
    pendingUsers,
    bannedUsers,
    selectedMember,
    loading,
    loadingPending,
    loadingBanned,
    loadingProfile,
    error,
    promotingId,
    demotingId,
    updatingRoleId,
    banningId,
    unbanningId,
    approvingId,
    rejectingId,

    // Computed
    isLeader,
    currentUserId,
    sortedMembers,
    leaderCount,
    counselorCount,
    memberCount,
    pendingCount,

    // Actions
    fetchMembers,
    fetchMemberProfile,
    fetchPendingUsers,
    fetchBannedUsers,
    approveUser,
    rejectUser,
    promoteMember,
    demoteMember,
    updateMemberRole,
    banMember,
    unbanUser,
    clearError,
    clearSelectedMember
  }
})
