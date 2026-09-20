import { reactive, watch } from 'vue'
import { defineStore } from 'pinia'
import { agentApi } from '../api'
import { localizeMessage } from '../utils/message'

const STORAGE_KEY = 'ecommerce_assistant_sessions'

function loadSessions() {
  try {
    const stored = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || '{}')
    for (const session of Object.values(stored)) {
      session.sending = false
      for (const message of session.messages || []) {
        message.status = ''
      }
    }
    return stored
  } catch {
    return {}
  }
}

export const useAssistantStore = defineStore('assistant', () => {
  const sessions = reactive(loadSessions())

  watch(
    sessions,
    (value) => {
      try {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value))
      } catch {
        // Conversation still works in memory if browser storage is unavailable.
      }
    },
    { deep: true }
  )

  function ensure(key, welcome) {
    if (!sessions[key]) {
      sessions[key] = {
        messages: [
          {
            id: `${key}-welcome`,
            role: 'assistant',
            content: welcome,
            status: ''
          }
        ],
        sending: false
      }
    }
    return sessions[key]
  }

  function get(key) {
    return sessions[key] || null
  }

  function clear(key) {
    delete sessions[key]
  }

  function clearAll() {
    for (const key of Object.keys(sessions)) {
      delete sessions[key]
    }
    try {
      sessionStorage.removeItem(STORAGE_KEY)
    } catch {
      // The in-memory sessions are already cleared.
    }
  }

  async function send(key, content) {
    const session = ensure(key, '你好，我是暖集 AI 助手。')
    if (!content || session.sending) return

    const normalized = content.trim().toLowerCase()
    const isDecision = ['确认', '确定', '是', '好的', '好', 'yes', 'y', 'ok',
      '取消', '不用了', '不要了', 'no', 'n'].includes(normalized)
    if (!isDecision) {
      for (const message of session.messages) {
        message.pendingAction = false
      }
    }

    session.messages.push({
      id: `user-${Date.now()}`,
      role: 'user',
      content
    })
    const assistantMessage = {
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: '',
      status: '正在处理…'
    }
    session.messages.push(assistantMessage)
    session.sending = true

    try {
      const finalData = await agentApi.streamChat(content, {
        onStatus: (text) => {
          assistantMessage.status = text
        },
        onDelta: (text) => {
          assistantMessage.status = ''
          assistantMessage.content += text
        }
      })
      if (finalData.answer) {
        assistantMessage.content = localizeMessage(finalData.answer)
      }
      assistantMessage.productIds = finalData.product_ids || []
      assistantMessage.pendingAction = Boolean(finalData.pending_action)
      assistantMessage.status = ''
    } catch (error) {
      assistantMessage.status = ''
      assistantMessage.content = localizeMessage(
        error.message || 'AI 服务暂不可用'
      )
    } finally {
      session.sending = false
    }
  }

  return { sessions, ensure, get, clear, clearAll, send }
})
