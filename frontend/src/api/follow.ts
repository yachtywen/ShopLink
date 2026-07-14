import { request } from './http'
import type { UserDTO } from '@/types/domain'
export const followApi = {
  change: (id: number, isFollow: boolean) => request<void>({ url: `/follow/${id}/${isFollow}`, method: 'put' }),
  isFollowing: (id: number) => request<boolean>({ url: `/follow/or/not/${id}` }), commons: (id: number) => request<UserDTO[]>({ url: `/follow/common/${id}` }),
}
