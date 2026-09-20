<template>
  <div class="seller-page-block">
    <header class="page-heading">
      <div>
        <h1>退款审核</h1>
        <p>买家申请退款后，卖家同意才会完成退款和库存恢复。</p>
      </div>
      <el-button :icon="RefreshCw" @click="loadReturns">刷新</el-button>
    </header>

    <el-tabs v-model="statusTab" class="status-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待处理" name="0" />
      <el-tab-pane label="已同意" name="1" />
      <el-tab-pane label="已拒绝" name="2" />
      <el-tab-pane label="已撤销" name="3" />
    </el-tabs>

    <div class="table-panel">
      <el-table v-loading="loading" :data="filteredReturns" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="250" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
        <el-table-column label="退款金额" width="130">
          <template #default="{ row }">
            <strong class="price">{{ formatPrice(row.refundAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatDate(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="returnStatus(row.status).type" effect="plain">
              {{ returnStatus(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="success"
              size="small"
              @click="approveReturn(row)"
            >
              同意
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              plain
              size="small"
              @click="rejectReturn(row)"
            >
              拒绝
            </el-button>
            <span v-if="row.status !== 0" class="muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshCw } from 'lucide-vue-next'
import { sellerReturnApi } from '../api'
import { formatDate, formatPrice, returnStatus } from '../utils/format'

const statusTab = ref('all')
const returns = ref([])
const loading = ref(false)

const filteredReturns = computed(() =>
  statusTab.value === 'all'
    ? returns.value
    : returns.value.filter((item) => String(item.status) === statusTab.value)
)

async function loadReturns() {
  loading.value = true
  try {
    const data = await sellerReturnApi.page({ page: 1, size: 200 })
    returns.value = data.records || []
  } catch (error) {
    ElMessage.error(error.message || '退款申请加载失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  // Filtering is local because the backend list is already seller-scoped.
}

async function approveReturn(item) {
  try {
    const result = await ElMessageBox.prompt(
      '同意后将完成模拟退款并恢复库存。',
      '同意退款',
      {
        confirmButtonText: '同意并退款',
        cancelButtonText: '取消',
        inputPlaceholder: '处理说明，可留空',
        inputValidator: () => true
      }
    )
    await sellerReturnApi.approve(item.id, result.value || '卖家同意退款')
    ElMessage.success('退款已处理')
    await loadReturns()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '退款处理失败')
    }
  }
}

async function rejectReturn(item) {
  try {
    const result = await ElMessageBox.prompt(
      '请填写拒绝原因。',
      '拒绝退款',
      {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '拒绝原因，可留空',
        inputValidator: () => true
      }
    )
    await sellerReturnApi.reject(item.id, result.value || '卖家拒绝退款')
    ElMessage.success('已拒绝退款申请')
    await loadReturns()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '退款处理失败')
    }
  }
}

onMounted(loadReturns)
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

.muted {
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 700px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
