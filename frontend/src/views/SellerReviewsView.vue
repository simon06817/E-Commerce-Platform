<template>
  <div class="seller-page-block">
    <header class="page-heading">
      <div>
        <h1>评价管理</h1>
        <p>查看买家评价并及时回复。</p>
      </div>
      <el-select v-model="ratingFilter" clearable placeholder="全部评分" @change="filterReviews">
        <el-option v-for="rating in [5, 4, 3, 2, 1]" :key="rating" :label="`${rating} 星`" :value="rating" />
      </el-select>
    </header>

    <div v-if="loading" class="review-list">
      <el-skeleton v-for="item in 4" :key="item" animated :rows="3" />
    </div>
    <EmptyState
      v-else-if="!filteredReviews.length"
      title="暂无评价"
      description="买家确认收货并评价后会显示在这里"
    />
    <div v-else class="review-list">
      <article v-for="item in filteredReviews" :key="item.id" class="review-card surface">
        <header class="review-head">
          <div>
            <strong>{{ item.productName || `商品 #${item.productId}` }}</strong>
            <span>{{ item.buyerName }} · {{ formatDate(item.createTime) }}</span>
          </div>
          <el-rate :model-value="item.rating" disabled show-score />
        </header>
        <p class="review-content">{{ item.content }}</p>
        <div v-if="item.replyContent" class="seller-reply">
          <strong>我的回复</strong>
          <p>{{ item.replyContent }}</p>
          <span>{{ formatDate(item.replyTime) }}</span>
        </div>
        <el-button
          v-else
          type="primary"
          plain
          :icon="MessageSquareText"
          @click="openReply(item)"
        >
          回复评价
        </el-button>
      </article>
    </div>

    <el-dialog v-model="replyVisible" title="回复评价" width="min(520px, 92vw)">
      <div class="reply-product">{{ replyTarget?.productName }}</div>
      <el-input
        v-model.trim="replyContent"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请输入回复内容"
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">
          提交回复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MessageSquareText } from 'lucide-vue-next'
import { sellerReviewApi } from '../api'
import EmptyState from '../components/EmptyState.vue'
import { formatDate } from '../utils/format'

const reviews = ref([])
const loading = ref(false)
const ratingFilter = ref(null)
const replyVisible = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')
const replying = ref(false)

const filteredReviews = computed(() =>
  ratingFilter.value
    ? reviews.value.filter((item) => item.rating === ratingFilter.value)
    : reviews.value
)

async function loadReviews() {
  loading.value = true
  try {
    const data = await sellerReviewApi.page({ page: 1, size: 200 })
    reviews.value = data.records || []
  } catch (error) {
    ElMessage.error(error.message || '评价加载失败')
  } finally {
    loading.value = false
  }
}

function filterReviews() {
  // Local filtering keeps the demo UI simple; the API already scopes seller products.
}

function openReply(item) {
  replyTarget.value = item
  replyContent.value = ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyContent.value) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await sellerReviewApi.reply(replyTarget.value.id, replyContent.value)
    ElMessage.success('回复已提交')
    replyVisible.value = false
    await loadReviews()
  } catch (error) {
    ElMessage.error(error.message || '回复失败')
  } finally {
    replying.value = false
  }
}

onMounted(loadReviews)
</script>

<style scoped>
.seller-page-block {
  display: grid;
  gap: 18px;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-heading h1,
.page-heading p,
.review-head strong,
.review-head span,
.seller-reply p {
  margin: 0;
}

.page-heading h1 {
  font-size: 28px;
}

.page-heading p {
  margin-top: 5px;
  color: var(--muted);
}

.review-list {
  display: grid;
  gap: 12px;
}

.review-card {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.review-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.review-head div {
  display: grid;
  gap: 4px;
}

.review-head span {
  color: var(--muted);
  font-size: 12px;
}

.review-content {
  margin: 0;
  line-height: 1.65;
}

.seller-reply {
  display: grid;
  gap: 5px;
  padding: 12px;
  border-left: 3px solid var(--primary);
  border-radius: 4px;
  background: var(--primary-soft);
}

.seller-reply span {
  color: var(--muted);
  font-size: 12px;
}

.reply-product {
  margin-bottom: 12px;
  color: var(--primary-dark);
  font-weight: 700;
}

@media (max-width: 700px) {
  .page-heading,
  .review-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
