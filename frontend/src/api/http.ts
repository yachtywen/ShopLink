import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { tokenStorage } from '@/utils/token'
import { ApiError, type Result } from '@/types/common'

const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api', timeout: 15_000 })
let onUnauthorized: (() => void) | undefined
export function setUnauthorizedHandler(handler: () => void) { onUnauthorized = handler }
export function unwrapResult<T>(body: Result<T>, status?: number): T {
  if (!body.success) throw new ApiError(body.errorMsg || '请求失败', status)
  return body.data
}
http.interceptors.request.use((config) => { const token = tokenStorage.get(); if (token) config.headers.authorization = token; return config })
http.interceptors.response.use((response) => {
  const body = response.data as Result<unknown>
  if (typeof body?.success === 'boolean') {
    try { return unwrapResult(body, response.status) } catch (error) { ElMessage.error((error as ApiError).message); return Promise.reject(error) }
  }
  return response.data
}, (error: AxiosError<{ errorMsg?: string }>) => {
  const status = error.response?.status
  const message = status === 401 ? '登录已失效，请重新登录' : error.response?.data?.errorMsg || error.message || '网络请求失败'
  if (status === 401) { tokenStorage.clear(); onUnauthorized?.() }
  ElMessage.error(message)
  return Promise.reject(new ApiError(message, status))
})
export function request<T>(config: AxiosRequestConfig): Promise<T> { return http.request<unknown, T>(config) }
