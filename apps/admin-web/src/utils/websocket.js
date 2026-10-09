import { Notification } from 'element-ui'
import { getToken } from './auth'

const RECONNECT_DELAY = 5000
const HEARTBEAT_INTERVAL = 30000

const NOTICE_TYPES = {
  1: { title: '来单提醒', type: 'success' },
  2: { title: '待接单提醒', type: 'warning' },
  3: { title: '催发货提醒', type: 'warning' }
}

let socket = null
let reconnectTimer = null
let heartbeatTimer = null
let manualClosed = false

// 每次连接使用独立标识，后端以该标识保存会话
function createSessionId() {
  return `admin-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function getSocketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  // 开发环境由 vue.config.js 的 /ws 代理转发，生产环境由网关转发到后端
  return `${protocol}//${window.location.host}/ws/${createSessionId()}`
}

function showNotice(data) {
  const notice = NOTICE_TYPES[data.type] || { title: '消息提醒', type: 'info' }
  Notification({
    title: notice.title,
    message: data.content || '',
    type: notice.type,
    duration: 10000,
    position: 'bottom-right'
  })
}

function handleMessage(event) {
  let data
  try {
    data = JSON.parse(event.data)
  } catch (e) {
    return
  }
  if (!data || !data.type) {
    return
  }
  showNotice(data)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

// 定时发送心跳，避免连接被代理/网关当作空闲连接断开
function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send('ping')
    }
  }, HEARTBEAT_INTERVAL)
}

function scheduleReconnect() {
  if (manualClosed || reconnectTimer || !getToken()) {
    return
  }
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connectWebSocket()
  }, RECONNECT_DELAY)
}

/**
 * 建立与后端 /ws/{sid} 的连接，已连接时不会重复建立
 */
export function connectWebSocket() {
  if (!getToken()) {
    return
  }
  if (
    socket &&
    (socket.readyState === WebSocket.CONNECTING || socket.readyState === WebSocket.OPEN)
  ) {
    return
  }

  manualClosed = false
  try {
    socket = new WebSocket(getSocketUrl())
  } catch (e) {
    socket = null
    scheduleReconnect()
    return
  }

  socket.onopen = startHeartbeat
  socket.onmessage = handleMessage
  socket.onerror = () => {
    if (socket) {
      socket.close()
    }
  }
  socket.onclose = () => {
    stopHeartbeat()
    socket = null
    scheduleReconnect()
  }
}

/**
 * 主动断开连接（退出登录时调用），不再自动重连
 */
export function closeWebSocket() {
  manualClosed = true
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  stopHeartbeat()
  if (socket) {
    socket.onclose = null
    socket.close()
    socket = null
  }
}
