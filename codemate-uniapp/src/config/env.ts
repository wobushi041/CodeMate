export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api').replace(/\/$/, '')

export const WS_BASE_URL = (import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8091/ws/chat').replace(/\/$/, '')
