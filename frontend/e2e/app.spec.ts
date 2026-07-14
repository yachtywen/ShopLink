import { expect, test } from '@playwright/test'

const ok = (data: unknown) => ({ success: true, errorMsg: null, data, total: null })
async function mockApi(page: import('@playwright/test').Page) {
  let statusCalls = 0
  let meAuthorization = ''
  await page.route((url) => url.pathname.startsWith('/api/'), async (route) => {
    const path = new URL(route.request().url()).pathname.replace('/api', '')
    if (path === '/user/me') meAuthorization = route.request().headers().authorization || ''
    const json = path === '/shop-type/list' ? ok([{ id: 1, name: 'Food', icon: '', sort: 1 }])
      : path === '/blog/hot' ? ok([{ id: 1, title: 'E2E blog', content: 'E2E fixture', liked: 2, comments: 0, images: '' }])
      : path === '/user/login' ? ok('e2e-token')
      : path === '/user/me' ? ok({ id: 1, nickName: 'E2E User', icon: '' })
      : path.startsWith('/voucher-order/status/') ? ok({ orderId: 1, statusCode: ++statusCalls > 1 ? 13 : 10, statusText: statusCalls > 1 ? 'Paid' : 'Queued', payTime: null, createTime: '2026-01-01T00:00:00' })
      : ok([])
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(json) })
  })
  return { authorization: () => meAuthorization }
}
test('renders public dashboard and navigates guest to login', async ({ page }) => { await mockApi(page); await page.goto('/'); await expect(page.getByTestId('home-title')).toBeVisible(); await expect(page.getByTestId('blog-card')).toHaveCount(1); await page.goto('/blogs/new'); await expect(page.getByTestId('login-title')).toBeVisible() })
test('logs in with a mocked API and sends raw token to later requests', async ({ page }) => { const api = await mockApi(page); await page.goto('/login'); await page.locator('#phone-input').fill('13800138000'); await page.locator('#code-input').fill('123456'); await page.locator('#login-submit').click(); await expect(page.getByTestId('user-name')).toHaveText('E2E User'); await expect.poll(api.authorization).toBe('e2e-token') })
test('polls the async seckill status to a terminal state', async ({ page }) => { await page.addInitScript(() => localStorage.setItem('hmdp-token', 'e2e-token')); await mockApi(page); await page.goto('/orders/1'); await expect(page.getByTestId('order-status')).toHaveAttribute('data-status-code', '13', { timeout: 4_000 }) })
