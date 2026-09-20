<template>
  <div class="admin-orders">
    <section class="page-heading">
      <div>
        <h1>订单管理</h1>
        <p>查询平台全部订单，并在异常情况下强制关单。</p>
      </div>
    </section>

    <section class="filter-bar surface">
      <el-input
        v-model.trim="filters.orderNo"
        clearable
        placeholder="订单号"
        @keyup.enter="search"
      />
      <el-input
        v-model.trim="filters.buyerId"
        clearable
        placeholder="买家 ID"
        @keyup.enter="search"
      />
      <el-select v-model="filters.status" clearable placeholder="全部状态">
        <el-option
          v-for="item in ADMIN_ORDER_STATUS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="RotateCcw" @click="resetFilters">重置</el-button>
    </section>

    <section class="table-panel surface">
      <el-table :data="orders" v-loading="loading">
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
        <el-table-column prop="receiverName" label="收货人" width="110" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button
              v-if="canForceCancel(row.status)"
              text
              type="danger"
              @click="forceCancel(row)"
            >
              强制关单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="prev, pager, next, total"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="changePage"
      />
    </section>

    <el-drawer v-model="detailVisible" title="订单详情" size="min(680px, 94vw)">
      <div v-if="detail" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="orderStatus(detail.status).type">
              {{ orderStatus(detail.status).text }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">
            ¥{{ formatPrice(detail.totalAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(detail.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="买家账号">
            {{ detail.buyerUsername || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="买家昵称">
            {{ detail.buyerNickname || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="买家手机">
            {{ detail.buyerPhone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="买家邮箱">
            {{ detail.buyerEmail || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收货人">
            {{ detail.receiverName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收货电话">
            {{ detail.receiverPhone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">
            {{ detail.receiverAddress || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="店铺" :span="2">
            {{ detail.shopNames?.join('、') || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-table :data="detail.items || []" border>
          <el-table-column prop="productName" label="商品" min-width="180" />
          <el-table-column label="单价" width="110">
            <template #default="{ row }">¥{{ formatPrice(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">¥{{ formatPrice(row.subtotal) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RotateCcw, Search } from 'lucide-vue-next'
import { adminOrderApi } from '../api'
import { formatDate, formatPrice, orderStatus } from '../utils/format'

const ADMIN_ORDER_STATUS = [
  { label: '待付款', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 }
]

const orders = ref([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const filters = reactive({
  orderNo: '',
  buyerId: '',
  status: null
})
const detailVisible = ref(false)
const detail = ref(null)

function canForceCancel(status) {
  return [0, 1, 2].includes(status)
}

async function loadOrders() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (filters.orderNo) params.orderNo = filters.orderNo
    if (filters.buyerId) params.buyerId = Number(filters.buyerId)
    if (filters.status !== null && filters.status !== '') {
      params.status = filters.status
    }
    const data = await adminOrderApi.page(params)
    orders.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '订单加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadOrders()
}

function resetFilters() {
  Object.assign(filters, { orderNo: '', buyerId: '', status: null })
  search()
}

function changePage(value) {
  page.value = value
  loadOrders()
}

async function openDetail(id) {
  try {
    detail.value = await adminOrderApi.detail(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '订单详情加载失败')
  }
}

async function forceCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确认强制关闭订单 ${row.orderNo} 吗？库存和退款状态将按后端规则处理。`,
      '强制关单',
      {
        type: 'warning',
        confirmButtonText: '确认关单',
        cancelButtonText: '取消'
      }
    )
    await adminOrderApi.forceCancel(row.id)
    ElMessage.success('订单已强制关闭')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '强制关单失败')
    }
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.admin-orders {
  display: grid;
  gap: 16px;
}

.page-heading h1,
.page-heading p {
  margin: 0;
}

.page-heading h1 {
  font-size: 28px;
}

.page-heading p {
  margin-top: 5px;
  color: var(--muted);
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(190px, 1fr) 150px 150px auto auto;
  gap: 10px;
  padding: 14px;
}

.table-panel {
  min-width: 0;
  padding: 14px;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

.order-detail {
  display: grid;
  gap: 18px;
}

@media (max-width: 900px) {
  .filter-bar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .filter-bar {
    grid-template-columns: 1fr;
  }
}
</style>
