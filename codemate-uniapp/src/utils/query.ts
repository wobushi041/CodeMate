export function decodeQueryValue(value?: string): string {
  if (!value) return ''
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

export function decodeQueryJson<T>(value: string | undefined, fallback: T): T {
  try {
    const parsed = JSON.parse(decodeQueryValue(value || ''))
    return parsed as T
  } catch {
    return fallback
  }
}
