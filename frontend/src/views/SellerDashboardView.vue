<template>
  <div class="dashboard-page">
    <section class="page-heading">
      <div>
        <h1>经营概览</h1>
        <p>查看店铺关键数据和需要处理的事项。</p>
      </div>
      <el-select v-model="range" class="range-select" @change="loadStats">
        <el-option label="今日" value="today" />
        <el-option label="近 7 天" value="7d" />
        <el-option label="近 30 天" value="30d" />
        <el-option label="全部" value="all" />
      </el-select>
    </section>

    <section class="metric-grid">
      <article v-for="item in metricCards" :key="item.label" class="metric-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.helper }}</small>
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="dashboard-section">
        <header class="section-heading">
          <div>
            <h2>待发货订单</h2>
            <p>已付款订单需要及时发货。</p>
          </div>
          <router-link to="/seller/orders">
            <el-button text type="primary">查看全部</el-button>
          </router-link>
        </header>
        <EmptyState
          v-if="!pendingOrders.length"
          title="暂无待发货订单"
          description="已付款订单会显示在这里"
        />
        <div v-else class="compact-list">
          <div v-for="order in pendingOrders" :key="order.id" class="compact-row">
            <div>
              <strong>{{ order.orderNo }}</strong>
              <span>{{ order.receiverName }} · {{ formatDate(order.createTime) }}</span>
            </div>
            <strong class="price">{{ formatPrice(order.totalAmount) }}</strong>
          </div>
        </div>
      </article>

      <article class="dashboard-section">
        <header class="section-heading">
          <div>
            <h2>待处理退款</h2>
            <p>退款申请需要卖家审核后完成。</p>
          </div>
          <router-link to="/seller/returns">
            <el-button text type="primary">查看全部</el-button>
          </router-link>
        </header>
        <EmptyState
          v-if="!pendingReturns.length"
          title="暂无待处理退款"
          description="新的退款申请会显示在这里"
        />
        <div v-else class="compact-list">
          <div v-for="item in pendingReturns" :key="item.id" class="compact-row">
            <div>
              <strong>{{ item.productName }}</strong>
              <span>{{ item.orderNo }} · {{ formatDate(item.applyTime) }}</span>
            </div>
            <strong class="price">{{ formatPrice(item.refundAmount) }}</strong>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { sellerOrderApi, sellerProductApi, sellerReturnApi, sellerStatsApi } from '../api'
import EmptyState from '../components/EmptyState.vue'
import { formatDate, formatPrice } from '../utils/format'

const range = ref('30d')
const stats = ref({
  totalRevenue: 0,
  orderCount: 0,
  itemCount: 0,
  averageOrderAmount: 0
})
const orders = ref([])
const returns = ref([])
const productTotal = ref(0)

const pendingOrders = computed(() =>
  orders.value.filter((item) => item.status === 1).slice(0, 5)
)
const pendingReturns = computed(() =>
  returns.value.filter((item) => item.status === 0).slice(0, 5)
)
const metricCards = computed(() => [
  {
    label: '营业额',
    value: `¥${formatPrice(stats.value.totalRevenue)}`,
    helper: `${rangeLabel.value}有效订单收入`
  },
  {
    label: '有效订单',
    value: stats.value.orderCount || 0,
    helper: `${rangeLabel.value}已付款订单`
  },
  {
    label: '销售件数',
    value: stats.value.itemCount || 0,
    helper: '已付款商品数量'
  },
  {
    label: '平均订单金额',
    value: `¥${formatPrice(stats.value.averageOrderAmount)}`,
    helper: '每笔有效订单平均金额'
  },
  {
    label: '商品总数',
    value: productTotal.value,
    helper: '店铺全部商品'
  }
])
const rangeLabel = computed(
  () =>
    ({ today: '今日', '7d': '近 7 天', '30d': '近 30 天', all: '全部' })[
      range.value
    ]
)

async function loadStats() {
  stats.value = await sellerStatsApi.stats(range.value)
}

async function loadData() {
  const [orderPage, returnPage, productPage] = await Promise.all([
    sellerOrderApi.page({ page: 1, size: 100 }),
    sellerReturnApi.page({ page: 1, size: 100 }),
    sellerProductApi.page({ page: 1, size: 1 })
  ])
  orders.value = orderPage.records || []
  returns.value = returnPage.records || []
  productTotal.value = productPage.total || 0
  await loadStats()
}

onMounted(loadData)
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 22px;
}

.page-heading,
.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-heading h1,
.page-heading p,
.section-heading h2,
.section-heading p {
  margin: 0;
}

.page-heading h1 {
  font-size: 28px;
}

.page-heading p,
.section-heading p {
  margin-top: 5px;
  color: var(--muted);
}

.range-select {
  width: 150px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  display: grid;
  gap: 7px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.metric-item span,
.metric-item small {
  color: var(--muted);
}

.metric-item strong {
  font-size: 25px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.dashboard-section {
  display: grid;
  align-content: start;
  gap: 14px;
  min-width: 0;
}

.compact-list {
  display: grid;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.compact-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
}

.compact-row:last-child {
  border-bottom: 0;
}

.compact-row div {
  display: grid;
  min-width: 0;
}

.compact-row span {
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-row strong:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1120px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .metric-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
