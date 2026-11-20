import { API_BASE_URL } from '@/config/env'
import { clearLoginStorage, getToken } from '@/utils/storage'
import { redirectToLogin } from '@/utils/navigation'

export interface AiStreamHandlers {
  onText: (text: string) => void
  onError: (message: string) => void
  onComplete: () => void
}

interface ChunkReceivedResult {
  data: ArrayBuffer
}

type ChunkedRequestTask = UniApp.RequestTask & {
  onChunkReceived: (callback: (result: ChunkReceivedResult) => void) => void
}

function readBusinessError(raw: string): string {
  const text = raw.trim()
  if (!text.startsWith('{')) return ''
  try {
    const body = JSON.parse(text) as { code?: number; message?: string; description?: string }
    if (typeof body.code !== 'number' || body.code === 0) return ''
    const message = body.description || body.message || 'AI 请求失败'
    if (body.code === 40100) {
      clearLoginStorage()
      redirectToLogin()
    }
    return message
  } catch {
    return '服务返回格式异常'
  }
}

class Utf8StreamDecoder {
  private carry: number[] = []

  decode(input: ArrayBuffer, flush = false): string {
    const bytes = [...this.carry, ...Array.from(new Uint8Array(input))]
    this.carry = []
    let output = ''
    let index = 0

    while (index < bytes.length) {
      const first = bytes[index]
      let length = 1
      if (first >= 0xc2 && first <= 0xdf) length = 2
      else if (first >= 0xe0 && first <= 0xef) length = 3
      else if (first >= 0xf0 && first <= 0xf4) length = 4

      if (index + length > bytes.length) {
        if (!flush) {
          this.carry = bytes.slice(index)
          break
        }
        output += '�'
        break
      }

      const sequence = bytes.slice(index, index + length)
      let codePoint = first
      let valid = true
      if (length === 1) {
        codePoint = first
      } else {
        codePoint = first & ((1 << (7 - length)) - 1)
        for (let offset = 1; offset < length; offset += 1) {
          const value = sequence[offset]
          if ((value & 0xc0) !== 0x80) {
            valid = false
            break
          }
          codePoint = (codePoint << 6) | (value & 0x3f)
        }
      }

      if (!valid || (length === 1 && first >= 0x80)) {
        output += '�'
        index += 1
        continue
      }

      try {
        output += String.fromCodePoint(codePoint)
      } catch {
        output += '�'
      }
      index += length
    }

    return output
  }
}

class SseParser {
  private pending = ''
  private eventData: string[] = []

  constructor(private readonly onText: (text: string) => void) {}

  push(chunk: string): void {
    this.pending += chunk
    const lines = this.pending.split('\n')
    this.pending = lines.pop() || ''
    lines.forEach((line) => this.consumeLine(line))
  }

  end(): void {
    if (this.pending) this.consumeLine(this.pending)
    this.pending = ''
    this.dispatch()
  }

  private consumeLine(rawLine: string): void {
    const line = rawLine.endsWith('\r') ? rawLine.slice(0, -1) : rawLine
    if (!line) {
      this.dispatch()
      return
    }
    if (line.startsWith('data:')) {
      this.eventData.push(line.slice(5).replace(/^ /, ''))
    }
  }

  private dispatch(): void {
    if (!this.eventData.length) return
    const text = this.eventData.join('\n')
    this.eventData = []
    if (text && text !== '[DONE]') this.onText(text)
  }
}

export function streamAiChat(message: string, handlers: AiStreamHandlers): () => void {
  const url = `${API_BASE_URL}/ai/chat?message=${encodeURIComponent(message)}`
  const token = getToken()
  let finished = false
  const parser = new SseParser((text) => {
    if (!finished) handlers.onText(text)
  })

  const complete = () => {
    if (finished) return
    finished = true
    handlers.onComplete()
  }
  const fail = (error: unknown) => {
    if (finished) return
    finished = true
    handlers.onError(error instanceof Error ? error.message : String(error || 'AI 请求失败'))
  }

  // #ifdef H5
  const controller = new AbortController()
  void (async () => {
    try {
      const response = await fetch(url, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        credentials: 'include',
        signal: controller.signal,
      })
      const contentType = response.headers.get('content-type') || ''
      if (contentType.includes('application/json')) {
        const raw = await response.text()
        const businessError = readBusinessError(raw)
        if (businessError) throw new Error(businessError)
        if (!response.ok) throw new Error(`AI 请求失败（${response.status}）`)
        throw new Error('服务返回格式异常')
      }
      if (!response.ok) throw new Error(`AI 请求失败（${response.status}）`)
      if (!response.body) throw new Error('当前环境不支持流式响应')

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        parser.push(decoder.decode(value, { stream: true }))
      }
      parser.push(decoder.decode())
      parser.end()
      complete()
    } catch (error) {
      if (!controller.signal.aborted) fail(error)
    }
  })()
  return () => {
    finished = true
    controller.abort()
  }
  // #endif

  // #ifndef H5
  const decoder = new Utf8StreamDecoder()
  let receivedChunk = false
  const requestTask = uni.request({
    url,
    method: 'GET',
    header: token ? { Authorization: `Bearer ${token}` } : {},
    responseType: 'arraybuffer',
    enableChunked: true,
    success: (response) => {
      if (finished) return
      try {
        if (response.statusCode < 200 || response.statusCode >= 300) {
          throw new Error(`AI 请求失败（${response.statusCode}）`)
        }
        if (!receivedChunk) {
          let raw = ''
          if (typeof response.data === 'string') raw = response.data
          else if (response.data instanceof ArrayBuffer) raw = decoder.decode(response.data, true)
          const businessError = readBusinessError(raw)
          if (businessError) throw new Error(businessError)
          parser.push(raw)
        }
        parser.end()
        complete()
      } catch (error) {
        fail(error)
      }
    },
    fail: (error) => fail(error.errMsg || 'AI 请求失败'),
  }) as ChunkedRequestTask

  requestTask.onChunkReceived((response) => {
    if (finished) return
    try {
      if (!(response.data instanceof ArrayBuffer)) return
      const text = decoder.decode(response.data)
      if (!receivedChunk) {
        const businessError = readBusinessError(text)
        if (businessError) {
          fail(new Error(businessError))
          requestTask.abort()
          return
        }
      }
      receivedChunk = true
      parser.push(text)
    } catch (error) {
      fail(error)
    }
  })

  return () => {
    finished = true
    requestTask.abort()
  }
  // #endif
}
