import { request } from './http'
import type { LoginForm, UserDTO, UserInfo } from '@/types/domain'
export const userApi = {
  sendCode: (phone: string) => request<void>({ url: '/user/code', method: 'post', params: { phone } }),
  login: (data: LoginForm) => request<string>({ url: '/user/login', method: 'post', data }),
  me: () => request<UserDTO>({ url: '/user/me' }), getById: (id: number) => request<UserDTO | null>({ url: `/user/${id}` }),
  getInfo: (id: number) => request<UserInfo | null>({ url: `/user/info/${id}` }), sign: () => request<void>({ url: '/user/sign', method: 'post' }), signCount: () => request<number>({ url: '/user/sign/count' }),
}
