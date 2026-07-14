import { request } from './http'
import type { SeckillOrderSubmitResult, Voucher, VoucherOrderStatusResult } from '@/types/domain'
export const voucherApi = {
  list: (shopId: number) => request<Voucher[]>({ url: `/voucher/list/${shopId}` }), create: (data: Voucher) => request<number>({ url: '/voucher', method: 'post', data }),
  createSeckill: (data: Voucher) => request<number>({ url: '/voucher/seckill', method: 'post', data }), seckill: (id: number) => request<SeckillOrderSubmitResult>({ url: `/voucher-order/seckill/${id}`, method: 'post' }),
  status: (id: number) => request<VoucherOrderStatusResult>({ url: `/voucher-order/status/${id}` }), mockPay: (id: number, success: boolean) => request<string>({ url: `/voucher-order/mock-pay/${id}/${success}`, method: 'post' }),
}
