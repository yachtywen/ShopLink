import { request } from './http'
import type { Shop, ShopType, ShopTypeQuery } from '@/types/domain'
export const shopApi = {
  types: () => request<ShopType[]>({ url: '/shop-type/list' }), get: (id: number) => request<Shop>({ url: `/shop/${id}` }),
  byType: (params: ShopTypeQuery) => request<Shop[]>({ url: '/shop/of/type', params }), byName: (name?: string, current = 1) => request<Shop[]>({ url: '/shop/of/name', params: { name, current } }),
  create: (data: Shop) => request<number>({ url: '/shop', method: 'post', data }), update: (data: Shop) => request<void>({ url: '/shop', method: 'put', data }),
}
