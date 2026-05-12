import { httpClient } from './http';
import type {
  ApiMessage,
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  PasswordResetRequest,
  ResetPasswordRequest,
} from '../types/auth';

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const response = await httpClient.post<AuthResponse>('/api/auth/login', request);
  return response.data;
}

export async function changePassword(request: ChangePasswordRequest): Promise<ApiMessage> {
  const response = await httpClient.post<ApiMessage>('/api/auth/change-password', request);
  return response.data;
}

export async function requestPasswordReset(request: PasswordResetRequest): Promise<ApiMessage> {
  const response = await httpClient.post<ApiMessage>('/api/auth/password-reset/request', request);
  return response.data;
}

export async function resetPassword(request: ResetPasswordRequest): Promise<ApiMessage> {
  const response = await httpClient.post<ApiMessage>('/api/auth/password-reset/confirm', request);
  return response.data;
}

