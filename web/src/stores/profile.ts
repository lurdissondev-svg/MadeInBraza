import { defineStore } from 'pinia'
import { ref } from 'vue'
import { profileApi } from '@/services/api/profile'
import { useAuthStore } from './auth'
import type { Profile, UpdateProfileRequest } from '@/types'

export const useProfileStore = defineStore('profile', () => {
  // State
  const profile = ref<Profile | null>(null)
  const loading = ref(false)
  const updating = ref(false)
  const changingPassword = ref(false)
  const error = ref<string | null>(null)
  const successMessage = ref<string | null>(null)

  // Actions
  async function fetchProfile() {
    loading.value = true
    error.value = null
    try {
      profile.value = await profileApi.getProfile()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao carregar perfil'
      console.error('Error fetching profile:', e)
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(data: UpdateProfileRequest): Promise<boolean> {
    updating.value = true
    error.value = null
    try {
      profile.value = await profileApi.updateProfile(data)

      // Also update auth store user data
      const authStore = useAuthStore()
      if (authStore.user) {
        if (data.nick) authStore.user.nick = data.nick
        if (data.playerClass) authStore.user.playerClass = data.playerClass
      }

      successMessage.value = 'Perfil atualizado com sucesso!'
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao atualizar perfil'
      console.error('Error updating profile:', e)
      return false
    } finally {
      updating.value = false
    }
  }

  async function changePassword(currentPassword: string, newPassword: string): Promise<boolean> {
    changingPassword.value = true
    error.value = null
    try {
      await profileApi.changePassword(currentPassword, newPassword)
      successMessage.value = 'Senha alterada com sucesso!'
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message || 'Erro ao alterar senha'
      console.error('Error changing password:', e)
      return false
    } finally {
      changingPassword.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearSuccessMessage() {
    successMessage.value = null
  }

  return {
    // State
    profile,
    loading,
    updating,
    changingPassword,
    error,
    successMessage,

    // Actions
    fetchProfile,
    updateProfile,
    changePassword,
    clearError,
    clearSuccessMessage
  }
})
