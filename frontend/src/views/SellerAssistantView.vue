<template>
  <main class="seller-assistant-page">
    <header class="assistant-header surface">
      <div class="assistant-title">
        <span class="assistant-mark"><Bot :size="22" /></span>
        <div>
          <h1>卖家 AI 助手</h1>
          <p>可以查询经营数据、订单、退款、评价和商品库存。</p>
        </div>
      </div>
      <div class="online-status"><span />在线</div>
    </header>

    <section ref="messageList" class="message-list surface">
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
          <div v-if="message.status" class="stream-status">
            {{ message.status }}
          </div>
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

    <section class="quick-actions">
      <el-button
        v-for="item in suggestions"
        :key="item"
        plain
        @click="sendSuggestion(item)"
      >
        {{ item }}
      </el-button>
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
  </main>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bot, Send, UserRound } from 'lucide-vue-next'
import { useAssistantStore } from '../stores/assistant'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const assistant = useAssistantStore()
const draft = ref('')
const messageList = ref(null)
const conversationKey = computed(() => `${auth.role}:${auth.userId}`)
const messages = computed(
  () => assistant.get(conversationKey.value)?.messages || []
)
const sending = computed(
  () => assistant.get(conversationKey.value)?.sending || false
)
assistant.ensure(
  conversationKey.value,
  '你好，我可以协助你处理发货、退款、评价、收入和商品库存。'
)
const suggestions = [
  '有哪些待发货订单？',
  '查看本月收入统计',
  '有哪些待处理的退款申请？',
  '查看我的商品和库存',
  '回复最新评价'
]

function scrollToBottom() {
  nextTick(() => {
    if (messageList.value) {
      messageList.value.scrollTop = messageList.value.scrollHeight
    }
  })
}

async function sendMessage(content) {
  if (!content || sending.value) return
  scrollToBottom()
  await assistant.send(conversationKey.value, content)
  scrollToBottom()
}

function send() {
  const content = draft.value.trim()
  if (!content) return
  draft.value = ''
  sendMessage(content)
}

function sendSuggestion(content) {
  if (sending.value) return
  sendMessage(content)
}
</script>

<style scoped>
.seller-assistant-page {
  display: grid;
  min-height: calc(100vh - 118px);
  grid-template-rows: auto minmax(360px, 1fr) auto auto;
  gap: 12px;
  padding: 0;
}

.assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
}

.assistant-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.assistant-title h1,
.assistant-title p {
  margin: 0;
}

.assistant-title h1 {
  font-size: 24px;
}

.assistant-title p {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.assistant-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: var(--primary);
}

.online-status {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--accent);
  font-size: 13px;
}

.online-status span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
}

.message-list {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  padding: 18px;
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
  max-width: min(760px, 86%);
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
}

.message-row.user .message-bubble {
  color: #fff;
  border-color: var(--primary);
  background: var(--primary);
}

.message-content {
  line-height: 1.65;
  white-space: pre-wrap;
}

.stream-status {
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 12px;
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

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.composer {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 10px;
  padding: 12px;
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

@media (max-width: 620px) {
  .seller-assistant-page {
    padding: 12px;
  }

  .assistant-header {
    align-items: flex-start;
  }

  .assistant-title h1 {
    font-size: 20px;
  }

  .assistant-title p,
  .online-status {
    display: none;
  }

  .composer {
    grid-template-columns: 1fr;
  }
}
</style>
