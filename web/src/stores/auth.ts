import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { UserStatus, Role } from '@/types'
import { authApi } from '@/services/api/auth'
import { setToken, removeToken, hasToken } from '@/services/api/client'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const initialized = ref(false)

  // Getters
  const isAuthenticated = computed(() => hasToken() && user.value !== null)
  const isLeader = computed(() => user.value?.role === Role.LEADER)
  const isPending = computed(() => user.value?.status === UserStatus.PENDING)
  const isApproved = computed(() => user.value?.status === UserStatus.APPROVED)
  const isBanned = computed(() => user.value?.status === UserStatus.BANNED)

  // Actions
  async function login(nick: string, password: string, stayLoggedIn: boolean = true): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.login({ nick, password })
      setToken(response.token, stayLoggedIn)
      user.value = response.user
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao fazer login'
      return false
    } finally {
      loading.value = false
    }
  }

  async function register(nick: string, password: string, playerClass: string): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.register({
        nick,
        password,
        playerClass: playerClass as any
      })
      setToken(response.token)
      user.value = response.user
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao registrar'
      return false
    } finally {
      loading.value = false
    }
  }

  async function checkStatus(): Promise<boolean> {
    if (!hasToken()) {
      initialized.value = true
      return false
    }

    loading.value = true
    error.value = null

    try {
      const response = await authApi.status()
      user.value = response.user
      initialized.value = true
      return true
    } catch (err) {
      // Token inválido ou expirado
      removeToken()
      user.value = null
      initialized.value = true
      return false
    } finally {
      loading.value = false
    }
  }

  function logout() {
    removeToken()
    user.value = null
    error.value = null
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    user,
    loading,
    error,
    initialized,
    // Getters
    isAuthenticated,
    isLeader,
    isPending,
    isApproved,
    isBanned,
    // Actions
    login,
    register,
    checkStatus,
    logout,
    clearError
  }
})
