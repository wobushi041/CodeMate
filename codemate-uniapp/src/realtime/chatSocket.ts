import { WS_BASE_URL } from '@/config/env'
import { getToken } from '@/utils/storage'

export type ChatSocketStatus = 'connecting' | 'connected' | 'reconnecting' | 'closed'

interface ChatSocketOptions {
  joinMessage: Record<string, any>
  onMessage: (message: any) => void
  onStatus?: (status: ChatSocketStatus) => void
}

export class ChatSocket {
  private socket: UniApp.SocketTask | null = null
  private manuallyClosed = false
  private reconnectAttempts = 0
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private readonly maxReconnectAttempts = 6

  constructor(private readonly options: ChatSocketOptions) {}

  connect(): void {
    this.clearReconnectTimer()
    if (this.socket || this.manuallyClosed) return

    this.options.onStatus?.(this.reconnectAttempts ? 'reconnecting' : 'connecting')
    const token = getToken()
    const socket = uni.connectSocket({
      url: WS_BASE_URL,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      complete: () => undefined,
    })
    this.socket = socket

    socket.onOpen(() => {
      if (this.socket !== socket) return
      this.reconnectAttempts = 0
      this.options.onStatus?.('connected')
      socket.send({ data: JSON.stringify(this.options.joinMessage) })
    })

    socket.onMessage((event) => {
      if (this.socket !== socket) return
      try {
        const data = typeof event.data === 'string' ? event.data : ''
        this.options.onMessage(JSON.parse(data))
      } catch {
        // 忽略非 JSON 消息，避免单条异常中断聊天页
      }
    })

    socket.onError(() => {
      // onClose 中统一处理重连，避免重复提示
    })

    socket.onClose(() => {
      if (this.socket !== socket) return
      this.socket = null
      if (!this.manuallyClosed) this.scheduleReconnect()
    })
  }

  send(message: Record<string, any>): Promise<boolean> {
    return new Promise((resolve) => {
      if (!this.socket) {
        resolve(false)
        return
      }
      this.socket.send({
        data: JSON.stringify(message),
        success: () => resolve(true),
        fail: () => {
          const activeSocket = this.socket
          this.socket = null
          activeSocket?.close({})
          this.scheduleReconnect()
          resolve(false)
        },
      })
    })
  }

  reconnect(): void {
    this.manuallyClosed = false
    if (this.socket) return
    this.reconnectAttempts = 0
    this.connect()
  }

  close(): void {
    this.manuallyClosed = true
    this.clearReconnectTimer()
    const socket = this.socket
    this.socket = null
    if (socket) socket.close({})
    this.options.onStatus?.('closed')
  }

  private scheduleReconnect(): void {
    if (this.manuallyClosed || this.reconnectAttempts >= this.maxReconnectAttempts) {
      this.options.onStatus?.('closed')
      return
    }
    this.reconnectAttempts += 1
    this.options.onStatus?.('reconnecting')
    const delay = Math.min(15000, 1000 * 2 ** (this.reconnectAttempts - 1))
    this.reconnectTimer = setTimeout(() => {
      this.socket = null
      this.connect()
    }, delay)
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.reconnectTimer = null
  }
}
