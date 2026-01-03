import apiClient from './client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  StatusResponse,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ChangePasswordRequest,
  SuccessResponse,
  RequestResetRequest,
  RequestResetResponse,
  VerifyResetTokenRequest,
  VerifyResetTokenResponse,
  ResetPasswordRequest,
  ResetPasswordResponse,
  UpdateEmailRequest,
  UpdateEmailResponse
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
   * Reset password (forgot password) - Legacy, generates random password
   */
  forgotPassword: async (data: ForgotPasswordRequest): Promise<ForgotPasswordResponse> => {
    const response = await apiClient.post<ForgotPasswordResponse>('/auth/forgot-password', data)
    return response.data
  },

  /**
   * Request password reset via email
   */
  requestPasswordReset: async (data: RequestResetRequest): Promise<RequestResetResponse> => {
    const response = await apiClient.post<RequestResetResponse>('/auth/request-reset', data)
    return response.data
  },

  /**
   * Verify reset token is valid
   */
  verifyResetToken: async (data: VerifyResetTokenRequest): Promise<VerifyResetTokenResponse> => {
    const response = await apiClient.post<VerifyResetTokenResponse>('/auth/verify-reset-token', data)
    return response.data
  },

  /**
   * Reset password with token
   */
  resetPassword: async (data: ResetPasswordRequest): Promise<ResetPasswordResponse> => {
    const response = await apiClient.post<ResetPasswordResponse>('/auth/reset-password', data)
    return response.data
  },

  /**
   * Change password (requires auth)
   */
  changePassword: async (data: ChangePasswordRequest): Promise<SuccessResponse> => {
    const response = await apiClient.put<SuccessResponse>('/auth/change-password', data)
    return response.data
  },

  /**
   * Update email (requires auth)
   */
  updateEmail: async (data: UpdateEmailRequest): Promise<UpdateEmailResponse> => {
    const response = await apiClient.put<UpdateEmailResponse>('/auth/update-email', data)
    return response.data
  }
}
