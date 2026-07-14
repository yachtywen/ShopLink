import { onBeforeUnmount, ref } from 'vue'
import { voucherApi } from '@/api/voucher'
import type { VoucherOrderStatusResult } from '@/types/domain'
const terminal = new Set([13, 14, 15])
export const isTerminalOrderStatus = (statusCode: number) => terminal.has(statusCode)
export function useOrderPolling() {
  const order = ref<VoucherOrderStatusResult | null>(null); const polling = ref(false); let timer: number | undefined
  const stop = () => { if (timer) window.clearInterval(timer); timer = undefined; polling.value = false }
  const refresh = async (id: number) => { order.value = await voucherApi.status(id); if (isTerminalOrderStatus(order.value.statusCode)) stop() }
  const start = async (id: number) => { stop(); polling.value = true; await refresh(id); if (polling.value) timer = window.setInterval(() => void refresh(id).catch(stop), 1000) }
  onBeforeUnmount(stop)
  return { order, polling, start, refresh, stop }
}
