const imageBaseUrl = import.meta.env.VITE_IMAGE_BASE_URL || '/imgs'
export function toImageUrl(path?: string): string { if (!path) return ''; if (/^https?:\/\//.test(path)) return path; return `${imageBaseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}` }
export function splitImages(images?: string): string[] { return (images || '').split(',').map((item) => item.trim()).filter(Boolean) }
export function joinImages(images: string[]): string { return images.filter(Boolean).join(',') }
