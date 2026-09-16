<template>
  <BuyerLayout>
    <el-button text :icon="ArrowLeft" class="back-button" @click="router.back()">
      返回
    </el-button>

    <div v-if="loading" class="detail-loading">
      <el-skeleton animated :rows="7" />
    </div>

    <EmptyState v-else-if="!product" title="商品不存在" description="商品可能已下架" />

    <div v-else class="detail-grid">
      <section class="image-panel surface">
        <img v-if="product.mainImage" :src="product.mainImage" :alt="product.name" />
        <PackageOpen v-else :size="76" />
      </section>

      <section class="detail-panel">
        <div class="seller-line">{{ product.sellerName || '平台商家' }}</div>
        <h1>{{ product.name }}</h1>
        <p class="description">{{ product.description || '暂无商品描述' }}</p>
        <div class="price-row">
          <span class="detail-price price">{{ formatPrice(product.price) }}</span>
          <el-tag :type="product.stock > 0 ? 'success' : 'danger'" effect="plain">
            {{ product.stock > 0 ? `库存 ${product.stock}` : '暂时缺货' }}
          </el-tag>
        </div>
        <div class="purchase-row">
          <el-input-number v-model="quantity" :min="1" :max="Math.max(product.stock, 1)" />
          <el-button
            type="primary"
            size="large"
            :icon="ShoppingBag"
            :disabled="product.stock <= 0"
            @click="addToCart"
          >
            加入购物车
          </el-button>
        </div>
      </section>
    </div>

    <section v-if="product" class="reviews section-block">
      <div class="review-heading">
        <h2 class="section-title">商品评价</h2>
        <div class="rating-summary">
          <span class="rating-number">{{ summary.averageRating || 0 }}</span>
          <el-rate :model-value="Number(summary.averageRating || 0)" disabled />
          <span class="muted">{{ summary.reviewCount || 0 }} 条评价</span>
        </div>
      </div>

      <template v-if="reviews.length">
        <div class="review-list">
          <article v-for="review in reviews" :key="review.id" class="review-item">
            <div class="review-top">
              <strong>{{ review.buyerName }}</strong>
              <el-rate :model-value="review.rating" disabled size="small" />
            </div>
            <p>{{ review.content || '用户未填写评价内容' }}</p>
            <div v-if="review.replyContent" class="seller-reply">
              卖家回复：{{ review.replyContent }}
            </div>
            <time>{{ formatDate(review.createTime) }}</time>
          </article>
        </div>
        <div v-if="reviews.length < reviewTotal" class="review-more">
          <el-button :loading="reviewLoading" @click="loadMoreReviews">
            加载更多评价
          </el-button>
        </div>
      </template>
      <EmptyState v-else title="暂无评价" description="购买并确认收货后可以评价" />
    </section>
  </BuyerLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, PackageOpen, ShoppingBag } from 'lucide-vue-next'
import BuyerLayout from '../components/BuyerLayout.vue'
import EmptyState from '../components/EmptyState.vue'
import { productApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { formatDate, formatPrice } from '../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const product = ref(null)
const loading = ref(true)
const quantity = ref(1)
const reviews = ref([])
const reviewPageNumber = ref(1)
const reviewTotal = ref(0)
const reviewLoading = ref(false)
const summary = reactive({ averageRating: 0, reviewCount: 0 })

async function load() {
  loading.value = true
  try {
    const id = route.params.id
    product.value = await productApi.detail(id)
    const pageData = await productApi.reviews(id, { page: 1, size: 10 })
    reviews.value = pageData.records || []
    reviewTotal.value = pageData.total || reviews.value.length
    reviewPageNumber.value = 1
    Object.assign(summary, await productApi.reviewSummary(id))
  } catch (error) {
    ElMessage.error(error.message || '商品加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMoreReviews() {
  reviewLoading.value = true
  try {
    const nextPage = reviewPageNumber.value + 1
    const data = await productApi.reviews(route.params.id, {
      page: nextPage,
      size: 10
    })
    reviews.value.push(...(data.records || []))
    reviewTotal.value = data.total || reviewTotal.value
    reviewPageNumber.value = nextPage
  } catch (error) {
    ElMessage.error(error.message || '评价加载失败')
  } finally {
    reviewLoading.value = false
  }
}

async function addToCart() {
  if (!auth.isLoggedIn || auth.role !== 'BUYER') {
    router.push({
      name: 'login',
      query: { redirect: route.fullPath }
    })
    return
  }
  try {
    await cart.add(product.value.id, quantity.value)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error.message || '加入购物车失败')
  }
}

onMounted(load)
</script>

<style scoped>
.back-button {
  margin-bottom: 12px;
}

.detail-loading {
  padding: 30px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 28px;
}

.image-panel {
  display: grid;
  aspect-ratio: 1 / 1;
  place-items: center;
  overflow: hidden;
  color: #d68a66;
  background: var(--surface-strong);
}

.image-panel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-panel {
  padding-top: 12px;
}

.seller-line {
  margin-bottom: 10px;
  color: var(--accent);
  font-weight: 800;
}

h1 {
  margin: 0 0 14px;
  font-size: clamp(25px, 3vw, 40px);
  line-height: 1.18;
}

.description {
  margin: 0 0 24px;
  color: var(--muted);
  line-height: 1.75;
}

.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.detail-price {
  font-size: 32px;
}

.purchase-row {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}

.section-block {
  margin-top: 36px;
}

.review-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.rating-summary {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-number {
  color: var(--primary-dark);
  font-size: 26px;
  font-weight: 900;
}

.review-list {
  display: grid;
  gap: 12px;
}

.review-more {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.review-item {
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.review-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.review-item p {
  margin: 10px 0;
  line-height: 1.65;
}

.review-item time {
  color: var(--muted);
  font-size: 12px;
}

.seller-reply {
  margin: 10px 0;
  padding: 10px;
  border-radius: 6px;
  color: #256b62;
  background: var(--accent-soft);
}

@media (max-width: 760px) {
  .detail-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .review-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
