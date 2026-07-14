import { describe, expect, it } from 'vitest'
import { unwrapResult } from './http'
import { ApiError } from '@/types/common'

describe('Result response handling', () => {
  it('returns data for a successful response', () => expect(unwrapResult({ success: true, errorMsg: null, data: 3, total: null })).toBe(3))
  it('throws an ApiError for an application failure returned with HTTP 200', () => expect(() => unwrapResult({ success: false, errorMsg: '库存不足', data: null, total: null }, 200)).toThrow(ApiError))
})
