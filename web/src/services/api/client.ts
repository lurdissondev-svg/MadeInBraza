import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://braza.app.br/api'
const TOKEN_KEY = 'braza_auth_token'

// Create axios instance
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Token management
export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
}

export const setToken = (token: string, persistent: boolean = true): void => {
  if (persistent) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
  }
}

export const removeToken = (): void => {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}

export const hasToken = (): boolean => {
  return !!getToken()
}

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// Response interceptor - handle errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    // Handle 401 Unauthorized
    if (error.response?.status === 401) {
      removeToken()
      // Redirect to login if not already there
      if (window.location.pathname !== '/web/login') {
        window.location.href = '/web/login'
      }
    }

    // Extract error message
    const errorData = error.response?.data as { error?: string } | undefined
    const message = errorData?.error || error.message || 'Erro desconhecido'

    return Promise.reject(new Error(message))
  }
)

export default apiClient
