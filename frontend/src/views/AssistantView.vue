<template>
  <BuyerLayout :show-nav="true">
    <div class="assistant-page">
      <header class="assistant-header">
        <div>
          <h1>AI 助手</h1>
          <p class="muted">可以询问商品、订单、购物车和通知。</p>
        </div>
        <div class="assistant-status">
          <span class="status-dot" />
          在线
        </div>
      </header>

      <section ref="messageList" class="message-list">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div class="message-avatar">
            <Bot v-if="message.role === 'assistant'" :size="18" />
            <UserRound v-else :size="18" />
          </div>
          <div class="message-bubble">
            <div class="message-content">{{ message.content }}</div>
            <div v-if="message.productIds?.length" class="product-links">
              <el-button
                v-for="productId in message.productIds"
                :key="productId"
                text
                type="primary"
                @click="router.push(`/product/${productId}`)"
              >
                查看商品 #{{ productId }}
              </el-button>
            </div>
            <div v-if="message.pendingAction" class="confirm-tip">
              回复“确认”执行，回复“取消”放弃。
            </div>
          </div>
        </article>

        <article v-if="sending" class="message-row assistant">
          <div class="message-avatar"><Bot :size="18" /></div>
          <div class="message-bubble typing">
            <span />
            <span />
            <span />
          </div>
        </article>
      </section>

      <footer class="composer surface">
        <el-input
          v-model.trim="draft"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          resize="none"
          placeholder="输入你的问题"
          @keydown.enter.exact.prevent="send"
        />
        <el-button
          type="primary"
          :icon="Send"
          :loading="sending"
          :disabled="!draft"
          @click="send"
        >
          发送
        </el-button>
      </footer>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bot, Send, UserRound } from 'lucide-vue-next'
import BuyerLayout from '../components/BuyerLayout.vue'
import { agentApi } from '../api'

const router = useRouter()
const draft = ref('')
const sending = ref(false)
const messageList = ref(null)
const messages = ref([
  {
    id: 'welcome',
    role: 'assistant',
    content: '你好，我是暖集 AI 助手。'
  }
])

function scrollToBottom() {
  nextTick(() => {
    if (messageList.value) {
      messageList.value.scrollTop = messageList.value.scrollHeight
    }
  })
}

async function send() {
  const content = draft.value.trim()
  if (!content || sending.value) return
  messages.value.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content
  })
  draft.value = ''
  sending.value = true
  scrollToBottom()

  try {
    const data = await agentApi.chat(content)
    messages.value.push({
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: data.answer || '暂时无法回答。',
      productIds: data.product_ids || [],
      pendingAction: data.pending_action
    })
  } catch (error) {
    ElMessage.error(error.response?.data?.detail || error.message || 'AI 服务暂不可用')
  } finally {
    sending.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.assistant-page {
  display: grid;
  min-height: calc(100vh - 170px);
  grid-template-rows: auto 1fr auto;
}

.assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
}

.assistant-header h1 {
  margin: 0 0 5px;
  font-size: 28px;
}

.assistant-header p {
  margin: 0;
}

.assistant-status {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--accent);
  font-size: 13px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
}

.message-list {
  display: flex;
  max-height: calc(100vh - 310px);
  min-height: 360px;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  padding: 18px 4px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: var(--primary);
}

.message-row.user .message-avatar {
  background: var(--accent);
}

.message-bubble {
  max-width: min(720px, 82%);
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: 0 8px 22px rgba(127, 74, 42, 0.06);
}

.message-row.user .message-bubble {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
}

.message-content {
  line-height: 1.65;
  white-space: pre-wrap;
}

.product-links {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.confirm-tip {
  margin-top: 8px;
  color: var(--primary-dark);
  font-size: 12px;
}

.typing {
  display: flex;
  gap: 4px;
}

.typing span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--primary);
  animation: pulse 1.2s infinite ease-in-out;
}

.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing span:nth-child(3) {
  animation-delay: 0.3s;
}

.composer {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 10px;
  padding: 12px;
}

@keyframes pulse {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }

  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

@media (max-width: 540px) {
  .assistant-header h1 {
    font-size: 24px;
  }

  .message-bubble {
    max-width: 88%;
  }

  .composer {
    grid-template-columns: 1fr;
  }
}
</style>
