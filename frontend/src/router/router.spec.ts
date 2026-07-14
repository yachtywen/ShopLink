import { describe, expect, it } from 'vitest'
import router from './index'
describe('route access control', () => {
  it('redirects a guest away from a protected page', async () => { localStorage.removeItem('hmdp-token'); await router.push('/blogs/new'); await router.isReady(); expect(router.currentRoute.value.name).toBe('login') })
  it('keeps a logged in visitor out of login page', async () => { localStorage.setItem('hmdp-token', 'demo-token'); await router.push('/login'); expect(router.currentRoute.value.name).toBe('home') })
})
