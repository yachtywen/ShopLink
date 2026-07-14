import { defineStore } from 'pinia'
import { userApi } from '@/api/user'
import type { LoginForm, UserDTO } from '@/types/domain'
import { tokenStorage } from '@/utils/token'

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: tokenStorage.get() || '', user: null as UserDTO | null, initialized: false }),
  getters: { isLoggedIn: (state) => Boolean(state.token) },
  actions: {
    async login(form: LoginForm) { this.token = await userApi.login(form); tokenStorage.set(this.token); await this.fetchMe() },
    async fetchMe() { if (!this.token) return; this.user = await userApi.me(); this.initialized = true },
    clear() { this.token = ''; this.user = null; this.initialized = true; tokenStorage.clear() },
  },
})
