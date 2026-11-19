export function normalizeTags(rawTags: unknown): string[] {
  if (!rawTags) return []
  if (Array.isArray(rawTags)) return rawTags.map((tag) => String(tag).trim()).filter(Boolean)

  const text = String(rawTags).trim()
  if (!text) return []
  try {
    const parsed = JSON.parse(text)
    return Array.isArray(parsed)
      ? parsed.map((tag) => String(tag).trim()).filter(Boolean)
      : [String(parsed).trim()].filter(Boolean)
  } catch {
    return text.split(/[,，]/).map((tag) => tag.trim()).filter(Boolean)
  }
}

function parseDate(value: string | Date): Date {
  if (value instanceof Date) return value
  const isoDate = new Date(value)
  if (!Number.isNaN(isoDate.getTime())) return isoDate
  return new Date(value.replace(/-/g, '/'))
}

export function formatDateTime(value?: string | Date | null): string {
  if (!value) return '长期有效'
  const date = parseDate(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (number: number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function formatTime(value?: string): string {
  if (!value) return ''
  const date = parseDate(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (number: number) => String(number).padStart(2, '0')
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}
