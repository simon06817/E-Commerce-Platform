<template>
  <div class="users-panel">
    <section class="page-heading">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
    </section>

    <section class="filter-bar surface">
      <el-input
        v-model.trim="keyword"
        clearable
        placeholder="账号、名称或手机号"
        @keyup.enter="search"
      />
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="RotateCcw" @click="reset">重置</el-button>
    </section>

    <section class="table-panel surface">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="账号" min-width="140" />
        <el-table-column
          v-if="isSeller"
          prop="shopName"
          label="店铺名称"
          min-width="150"
        />
        <el-table-column
          v-else
          prop="nickname"
          label="昵称"
          min-width="130"
        />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="190" />
        <el-table-column
          v-if="!isSeller"
          prop="address"
          label="地址"
          min-width="160"
        />
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">详情</el-button>
            <el-button text type="danger" @click="removeUser(row)">删除</el-button>
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

    <el-drawer
      v-model="detailVisible"
      :title="isSeller ? '卖家详情' : '买家详情'"
      size="min(460px, 92vw)"
    >
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="账号">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item v-if="isSeller" label="店铺">
          {{ detail.shopName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-else label="昵称">
          {{ detail.nickname || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="!isSeller" label="收货地址">
          {{ detail.address || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ formatDate(detail.createTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RotateCcw, Search } from 'lucide-vue-next'
import { adminUserApi } from '../api'
import { formatDate } from '../utils/format'

const props = defineProps({
  userType: {
    type: String,
    required: true,
    validator: (value) => ['buyer', 'seller'].includes(value)
  }
})

const isSeller = computed(() => props.userType === 'seller')
const title = computed(() => (isSeller.value ? '卖家管理' : '买家管理'))
const description = computed(() =>
  isSeller.value
    ? '查询平台卖家账号和店铺资料，并处理无效账号。'
    : '查询平台买家账号和收货资料，并处理无效账号。'
)

const users = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const detailVisible = ref(false)
const detail = ref(null)

function api() {
  return isSeller.value ? adminUserApi.sellers : adminUserApi.buyers
}

async function loadUsers() {
  loading.value = true
  try {
    const data = await api()({
      page: page.value,
      size,
      keyword: keyword.value || undefined
    })
    users.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '用户加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadUsers()
}

function reset() {
  keyword.value = ''
  search()
}

function changePage(value) {
  page.value = value
  loadUsers()
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

async function removeUser(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除${isSeller.value ? '卖家' : '买家'}账号 ${row.username} 吗？`,
      '删除账号',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    if (isSeller.value) {
      await adminUserApi.removeSeller(row.id)
    } else {
      await adminUserApi.removeBuyer(row.id)
    }
    ElMessage.success('账号已删除')
    if (users.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    loadUsers()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

watch(
  () => props.userType,
  () => {
    keyword.value = ''
    page.value = 1
    loadUsers()
  },
  { immediate: true }
)
</script>

<style scoped>
.users-panel {
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
  grid-template-columns: minmax(220px, 1fr) auto auto;
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

@media (max-width: 620px) {
  .filter-bar {
    grid-template-columns: 1fr;
  }
}
</style>
