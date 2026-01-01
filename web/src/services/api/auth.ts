import apiClient from './client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  StatusResponse,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ChangePasswordRequest,
  SuccessResponse
} from '@/types'

export const authApi = {
  /**
   * Login with nick and password
   */
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/login', data)
    return response.data
  },

  /**
   * Register a new user
   */
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/register', data)
    return response.data
  },

  /**
   * Check current auth status
   */
  status: async (): Promise<StatusResponse> => {
    const response = await apiClient.get<StatusResponse>('/auth/status')
    return response.data
  },

  /**
   * Reset password (forgot password)
   */
  forgotPassword: async (data: ForgotPasswordRequest): Promise<ForgotPasswordResponse> => {
    const response = await apiClient.post<ForgotPasswordResponse>('/auth/forgot-password', data)
    return response.data
  },

  /**
   * Change password (requires auth)
   */
  changePassword: async (data: ChangePasswordRequest): Promise<SuccessResponse> => {
    const response = await apiClient.put<SuccessResponse>('/auth/change-password', data)
    return response.data
  }
}
