const TOKEN_KEY = 'hmdp-token'
export const tokenStorage = { get: () => localStorage.getItem(TOKEN_KEY), set: (token: string) => localStorage.setItem(TOKEN_KEY, token), clear: () => localStorage.removeItem(TOKEN_KEY) }
