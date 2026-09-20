<template>
  <div class="admin-dashboard">
    <section class="dashboard-heading">
      <div>
        <h1>数据概览</h1>
        <p>查看平台账号、订单和基础数据规模。</p>
      </div>
      <el-button :icon="RefreshCw" :loading="loading" @click="loadDashboard">
        刷新
      </el-button>
    </section>

    <section class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="metric-item surface">
        <span class="metric-icon" :class="item.tone">
          <component :is="item.icon" :size="22" />
        </span>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </article>
    </section>

    <section class="dashboard-panel surface">
      <div class="panel-heading">
        <div>
          <h2>最近订单</h2>
          <p>快速查看平台最新订单状态。</p>
        </div>
        <el-button text type="primary" @click="router.push('/admin/orders')">
          查看全部
        </el-button>
      </div>

      <el-table :data="recentOrders" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="buyerId" label="买家 ID" width="100" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥{{ formatPrice(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="orderStatus(row.status).type">
              {{ orderStatus(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ClipboardList,
  Package,
  RefreshCw,
  Store,
  Tags,
  Users
} from 'lucide-vue-next'
import {
  adminCategoryApi,
  adminOrderApi,
  adminUserApi,
  productApi
} from '../api'
import { formatDate, formatPrice, orderStatus } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const buyerTotal = ref(0)
const sellerTotal = ref(0)
const orderTotal = ref(0)
const productTotal = ref(0)
const categoryTotal = ref(0)
const recentOrders = ref([])

const metrics = computed(() => [
  { label: '买家账号', value: buyerTotal.value, icon: Users, tone: 'blue' },
  { label: '卖家账号', value: sellerTotal.value, icon: Store, tone: 'orange' },
  { label: '订单总数', value: orderTotal.value, icon: ClipboardList, tone: 'green' },
  { label: '商品总数', value: productTotal.value, icon: Package, tone: 'violet' },
  { label: '分类总数', value: categoryTotal.value, icon: Tags, tone: 'red' }
])

async function loadDashboard() {
  loading.value = true
  try {
    const [buyers, sellers, orders, products, categories] = await Promise.all([
      adminUserApi.buyers({ page: 1, size: 1 }),
      adminUserApi.sellers({ page: 1, size: 1 }),
      adminOrderApi.page({ page: 1, size: 5 }),
      productApi.page({ page: 1, size: 1 }),
      adminCategoryApi.page({ page: 1, size: 1 })
    ])
    buyerTotal.value = buyers.total || 0
    sellerTotal.value = sellers.total || 0
    orderTotal.value = orders.total || 0
    productTotal.value = products.total || 0
    categoryTotal.value = categories.total || 0
    recentOrders.value = orders.records || []
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.admin-dashboard {
  display: grid;
  gap: 18px;
}

.dashboard-heading,
.panel-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.dashboard-heading h1,
.dashboard-heading p,
.panel-heading h2,
.panel-heading p {
  margin: 0;
}

.dashboard-heading h1 {
  font-size: 28px;
}

.dashboard-heading p,
.panel-heading p {
  margin-top: 5px;
  color: var(--muted);
}

.panel-heading h2 {
  font-size: 20px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 12px;
}

.metric-item {
  display: flex;
  min-height: 108px;
  align-items: center;
  gap: 14px;
  padding: 18px;
}

.metric-item > div {
  display: grid;
  gap: 5px;
}

.metric-item span {
  color: var(--muted);
  font-size: 13px;
}

.metric-item strong {
  font-size: 27px;
}

.metric-icon {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #fff;
}

.metric-icon.blue {
  background: #3b82a0;
}

.metric-icon.orange {
  background: #c8613d;
}

.metric-icon.green {
  background: #4d8f6f;
}

.metric-icon.violet {
  background: #7562a8;
}

.metric-icon.red {
  background: #b14f55;
}

.dashboard-panel {
  padding: 18px;
}

@media (max-width: 1150px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
  }
}

@media (max-width: 680px) {
  .metric-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
