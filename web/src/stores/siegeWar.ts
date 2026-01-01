import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  SiegeWar,
  SWUserResponse,
  SubmitSWResponseRequest,
  AvailableShare,
  SWResponseItem,
  SWResponseUser,
  SWResponsesSummary,
  SiegeWarHistoryItem
} from '@/types'
import { siegeWarApi } from '@/services/api/siegeWar'

export const useSiegeWarStore = defineStore('siegeWar', () => {
  // State
  const siegeWar = ref<SiegeWar | null>(null)
  const userResponse = ref<SWUserResponse | null>(null)
  const responses = ref<SWResponseItem[]>([])
  const notResponded = ref<SWResponseUser[]>([])
  const availableShares = ref<AvailableShare[]>([])
  const summary = ref<SWResponsesSummary | null>(null)
  const history = ref<SiegeWarHistoryItem[]>([])

  const loading = ref(false)
  const submitting = ref(false)
  const loadingHistory = ref(false)
  const error = ref<string | null>(null)

  // Actions
  async function fetchCurrentSiegeWar(): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      const data = await siegeWarApi.getCurrentSiegeWar()
      siegeWar.value = data.siegeWar
      userResponse.value = data.userResponse
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar Siege War'
      return false
    } finally {
      loading.value = false
    }
  }

  async function createSiegeWar(): Promise<boolean> {
    loading.value = true
    error.value = null

    try {
      siegeWar.value = await siegeWarApi.createSiegeWar()
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao criar Siege War'
      return false
    } finally {
      loading.value = false
    }
  }

  async function submitResponse(data: SubmitSWResponseRequest): Promise<boolean> {
    if (!siegeWar.value) return false

    submitting.value = true
    error.value = null

    try {
      userResponse.value = await siegeWarApi.submitResponse(siegeWar.value.id, data)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao enviar resposta'
      return false
    } finally {
      submitting.value = false
    }
  }

  async function fetchResponses(): Promise<boolean> {
    if (!siegeWar.value) return false

    loading.value = true
    error.value = null

    try {
      const data = await siegeWarApi.getResponses(siegeWar.value.id)
      responses.value = data.responses
      notResponded.value = data.notResponded
      availableShares.value = data.availableShares
      summary.value = data.summary
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar respostas'
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchAvailableShares(): Promise<boolean> {
    if (!siegeWar.value) return false

    try {
      availableShares.value = await siegeWarApi.getAvailableShares(siegeWar.value.id)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar contas disponíveis'
      return false
    }
  }

  async function closeSiegeWar(): Promise<boolean> {
    if (!siegeWar.value) return false

    loading.value = true
    error.value = null

    try {
      siegeWar.value = await siegeWarApi.closeSiegeWar(siegeWar.value.id)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao fechar Siege War'
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchHistory(): Promise<boolean> {
    loadingHistory.value = true
    error.value = null

    try {
      history.value = await siegeWarApi.getHistory()
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Erro ao carregar histórico'
      return false
    } finally {
      loadingHistory.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    siegeWar,
    userResponse,
    responses,
    notResponded,
    availableShares,
    summary,
    history,
    loading,
    submitting,
    loadingHistory,
    error,
    // Actions
    fetchCurrentSiegeWar,
    createSiegeWar,
    submitResponse,
    fetchResponses,
    fetchAvailableShares,
    closeSiegeWar,
    fetchHistory,
    clearError
  }
})
