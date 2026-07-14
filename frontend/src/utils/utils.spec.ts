import { describe, expect, it } from 'vitest'
import { joinImages, splitImages, toImageUrl } from './image'
import { tokenStorage } from './token'

describe('tokenStorage', () => {
  it('persists and clears a raw backend token', () => { tokenStorage.set('abc-token'); expect(tokenStorage.get()).toBe('abc-token'); tokenStorage.clear(); expect(tokenStorage.get()).toBeNull() })
})
describe('image helpers', () => {
  it('converts comma separated backend image paths', () => { expect(splitImages(' /blogs/a.jpg,,/blogs/b.jpg ')).toEqual(['/blogs/a.jpg', '/blogs/b.jpg']); expect(joinImages(['/blogs/a.jpg', '', '/blogs/b.jpg'])).toBe('/blogs/a.jpg,/blogs/b.jpg') })
  it('creates an image server url', () => expect(toImageUrl('/blogs/a.jpg')).toContain('/imgs/blogs/a.jpg'))
})
