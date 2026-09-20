<template>
  <div class="seller-page-block">
    <header class="page-heading">
      <div>
        <h1>订单发货</h1>
        <p>仅展示包含本店商品的订单，已付款订单可以发货。</p>
      </div>
      <el-button :icon="RefreshCw" @click="loadOrders">刷新</el-button>
    </header>

    <el-tabs v-model="statusTab" class="status-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="已发货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="已取消" name="4" />
    </el-tabs>

    <div class="table-panel">
      <el-table v-loading="loading" :data="orders" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="250" show-overflow-tooltip />
        <el-table-column label="订单金额" width="130">
          <template #default="{ row }">
            <strong class="price">{{ formatPrice(row.totalAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="收货人" min-width="150">
          <template #default="{ row }">
            {{ row.receiverName }} {{ row.receiverPhone }}
          </template>
        </el-table-column>
        <el-table-column label="买家账号" min-width="140">
          <template #default="{ row }">
            {{ orderDetails[row.id]?.buyerUsername || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="商品信息" min-width="210">
          <template #default="{ row }">
            {{ itemSummary(row) }}
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatus(row.status).type" effect="plain">
              {{ orderStatus(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="row.status === 1"
              type="primary"
              size="small"
              :icon="Truck"
              @click="shipOrder(row)"
            >
              发货
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="loadOrders"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="订单详情" size="min(620px, 92vw)">
      <div v-if="detail" class="order-detail">
        <div class="detail-head">
          <div>
            <strong>{{ detail.orderNo }}</strong>
            <span>{{ formatDate(detail.createTime) }}</span>
          </div>
          <el-tag :type="orderStatus(detail.status).type">
            {{ orderStatus(detail.status).text }}
          </el-tag>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="下单账号">
            {{ detail.buyerUsername || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="买家昵称">
            {{ detail.buyerNickname || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="买家手机号">
            {{ detail.buyerPhone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="买家邮箱">
            {{ detail.buyerEmail || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收货人">
            {{ detail.receiverName }} {{ detail.receiverPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="收货地址">
            {{ detail.receiverAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="付款时间">
            {{ formatDate(detail.payTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="发货时间">
            {{ formatDate(detail.shipTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-items">
          <article v-for="item in detail.items || []" :key="item.id" class="detail-item">
            <div class="detail-item-image">
              <img v-if="item.productImage" :src="item.productImage" :alt="item.productName" />
              <PackageOpen v-else :size="22" />
            </div>
            <div>
              <strong>{{ item.productName }}</strong>
              <span>¥{{ formatPrice(item.price) }} × {{ item.quantity }}</span>
            </div>
            <strong class="price">{{ formatPrice(item.subtotal) }}</strong>
          </article>
        </div>

        <el-button
          v-if="detail.status === 1"
          class="full-button"
          type="primary"
          :icon="Truck"
          @click="shipOrder(detail)"
        >
          发货
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PackageOpen, RefreshCw, Truck } from 'lucide-vue-next'
import { sellerOrderApi } from '../api'
import { formatDate, formatPrice, orderStatus } from '../utils/format'

const statusTab = ref('all')
const orders = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const detailVisible = ref(false)
const detail = ref(null)
const orderDetails = reactive({})

async function loadOrders() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize }
    if (statusTab.value !== 'all') {
      params.status = Number(statusTab.value)
    }
    const data = await sellerOrderApi.page(params)
    orders.value = data.records || []
    total.value = data.total || 0

    const details = await Promise.all(
      orders.value.map(async (order) => {
        try {
          return [order.id, await sellerOrderApi.detail(order.id)]
        } catch {
          return [order.id, { items: [] }]
        }
      })
    )
    Object.keys(orderDetails).forEach((key) => delete orderDetails[key])
    for (const [id, item] of details) {
      orderDetails[id] = item
    }
  } catch (error) {
    ElMessage.error(error.message || '订单加载失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  page.value = 1
  loadOrders()
}

async function openDetail(order) {
  try {
    detail.value = await sellerOrderApi.detail(order.id)
    orderDetails[order.id] = detail.value
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '订单详情加载失败')
  }
}

function itemSummary(order) {
  const items = orderDetails[order.id]?.items || []
  if (!items.length) return '-'
  const first = items[0]
  return `${first.productName} × ${first.quantity}${items.length > 1 ? ` 等 ${items.length} 件` : ''}`
}

async function shipOrder(order) {
  try {
    await ElMessageBox.confirm(
      `确认订单 ${order.orderNo} 已经发货吗？发货 24 小时后自动确认到货。`,
      '确认发货',
      { type: 'warning' }
    )
    await sellerOrderApi.ship(order.id)
    ElMessage.success('订单已发货')
    detailVisible.value = false
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '发货失败')
    }
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.seller-page-block {
  display: grid;
  gap: 18px;
}

.page-heading,
.detail-head,
.detail-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-heading h1,
.page-heading p,
.detail-head strong,
.detail-head span {
  margin: 0;
}

.page-heading h1 {
  font-size: 28px;
}

.page-heading p,
.detail-head span {
  margin-top: 5px;
  color: var(--muted);
}

.status-tabs {
  padding: 0 16px;
  border-radius: 8px;
  background: var(--surface);
}

.table-panel {
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.order-detail {
  display: grid;
  gap: 18px;
}

.detail-head div {
  display: grid;
}

.detail-items {
  display: grid;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.detail-item {
  padding: 12px;
  border-bottom: 1px solid var(--line);
}

.detail-item:last-child {
  border-bottom: 0;
}

.detail-item-image {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border-radius: 6px;
  background: var(--surface-strong);
}

.detail-item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-item > div:nth-child(2) {
  display: grid;
  flex: 1;
}

.detail-item span {
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 700px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
