import { describe, expect, it } from 'vitest'
import { isTerminalOrderStatus } from './useOrderPolling'
describe('order status terminal detection', () => { it('only stops at final states', () => { expect(isTerminalOrderStatus(10)).toBe(false); expect(isTerminalOrderStatus(12)).toBe(false); expect(isTerminalOrderStatus(13)).toBe(true); expect(isTerminalOrderStatus(15)).toBe(true) }) })
