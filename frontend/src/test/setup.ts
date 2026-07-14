import { afterEach } from 'vitest'
Object.defineProperty(window, 'scrollTo', { value: () => undefined, writable: true })
afterEach(() => localStorage.clear())
