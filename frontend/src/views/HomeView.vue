<template>
  <BuyerLayout>
    <section class="hero-strip">
      <div>
        <div class="eyebrow">今天想找些什么</div>
        <h1>搜商品，也可以直接搜店铺</h1>
      </div>
      <div class="search-box">
        <el-input
          v-model.trim="keyword"
          size="large"
          clearable
          :prefix-icon="Search"
          placeholder="输入商品名或店铺名"
          @keyup.enter="applySearch"
          @clear="applySearch"
        />
        <el-button type="primary" size="large" :icon="Search" @click="applySearch">
          搜索
        </el-button>
      </div>
    </section>

    <section class="category-section">
      <div class="section-heading">
        <h2 class="section-title">分类</h2>
        <span class="muted">共 {{ products.total || 0 }} 件商品</span>
      </div>
      <div class="category-list">
        <button
          type="button"
          class="category-chip"
          :class="{ active: categoryId === null }"
          @click="selectCategory(null)"
        >
          全部
        </button>
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          class="category-chip"
          :class="{ active: categoryId === category.id }"
          @click="selectCategory(category.id)"
        >
          {{ category.name }}
        </button>
      </div>
    </section>

    <section class="product-section">
      <div v-if="loading" class="product-grid">
        <el-skeleton v-for="item in 8" :key="item" animated>
          <template #template>
            <el-skeleton-item variant="image" class="skeleton-image" />
            <div class="skeleton-copy">
              <el-skeleton-item variant="h3" />
              <el-skeleton-item variant="text" />
            </div>
          </template>
        </el-skeleton>
      </div>

      <div v-else-if="products.records?.length" class="product-grid">
        <ProductCard
          v-for="product in products.records"
          :key="product.id"
          :product="product"
          @add="addToCart"
        />
      </div>

      <EmptyState
        v-else
        title="没有匹配结果"
        description="请尝试其他商品名或店铺名"
      />

      <div v-if="products.total > pageSize" class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="products.total"
          layout="prev, pager, next"
          background
          @current-change="loadProducts"
        />
      </div>
    </section>
  </BuyerLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from 'lucide-vue-next'
import BuyerLayout from '../components/BuyerLayout.vue'
import ProductCard from '../components/ProductCard.vue'
import EmptyState from '../components/EmptyState.vue'
import { categoryApi, productApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const categories = ref([])
const products = reactive({ records: [], total: 0 })
const keyword = ref('')
const categoryId = ref(null)
const page = ref(1)
const pageSize = 12
const loading = ref(false)

async function loadCategories() {
  try {
    const tree = await categoryApi.list()
    categories.value = tree.flatMap((item) => [item, ...(item.children || [])])
  } catch {
    categories.value = []
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const data = await productApi.page({
      page: page.value,
      size: pageSize,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      status: 1
    })
    products.records = data.records || []
    products.total = data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '商品加载失败')
  } finally {
    loading.value = false
  }
}

function applySearch() {
  page.value = 1
  loadProducts()
}

function selectCategory(id) {
  categoryId.value = id
  page.value = 1
  loadProducts()
}

async function addToCart(product) {
  if (!auth.isLoggedIn || auth.role !== 'BUYER') {
    router.push({
      name: 'login',
      query: { redirect: router.currentRoute.value.fullPath }
    })
    return
  }
  try {
    await cart.add(product.id, 1)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error.message || '加入购物车失败')
  }
}

onMounted(() => {
  loadCategories()
  loadProducts()
})
</script>

<style scoped>
.hero-strip {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(320px, 1.2fr);
  align-items: end;
  gap: 24px;
  margin-bottom: 28px;
  padding: 26px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: var(--shadow);
}

.eyebrow {
  margin-bottom: 8px;
  color: var(--accent);
  font-size: 13px;
  font-weight: 800;
}

h1 {
  margin: 0;
  font-size: clamp(24px, 3vw, 38px);
  line-height: 1.2;
}

.search-box {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.category-section,
.product-section {
  margin-top: 24px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.category-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.category-chip {
  flex: 0 0 auto;
  min-height: 36px;
  padding: 0 16px;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: var(--muted);
  background: var(--surface);
  cursor: pointer;
}

.category-chip.active {
  border-color: var(--primary);
  color: #fff;
  background: var(--primary);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.skeleton-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  height: auto;
}

.skeleton-copy {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

@media (max-width: 960px) {
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-strip {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .search-box {
    grid-template-columns: 1fr;
  }
}
</style>
