import { request } from './http'
import type { ScrollResult } from '@/types/common'
import type { Blog, UserDTO } from '@/types/domain'
export const blogApi = {
  hot: (current = 1) => request<Blog[]>({ url: '/blog/hot', params: { current } }), get: (id: number) => request<Blog>({ url: `/blog/${id}` }),
  create: (data: Blog) => request<number>({ url: '/blog', method: 'post', data }), like: (id: number) => request<void>({ url: `/blog/like/${id}`, method: 'put' }),
  likes: (id: number) => request<UserDTO[]>({ url: `/blog/likes/${id}` }), mine: (current = 1) => request<Blog[]>({ url: '/blog/of/me', params: { current } }),
  ofUser: (id: number, current = 1) => request<Blog[]>({ url: '/blog/of/user', params: { id, current } }),
  feed: (lastId: number, offset = 0) => request<ScrollResult<Blog> | null>({ url: '/blog/of/follow', params: { lastId, offset } }),
  upload: (file: File) => { const data = new FormData(); data.append('file', file); return request<string>({ url: '/upload/blog', method: 'post', data }) },
  removeImage: (name: string) => request<void>({ url: '/upload/blog/delete', params: { name } }),
}
