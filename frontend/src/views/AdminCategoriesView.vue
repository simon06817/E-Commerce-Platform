<template>
  <div class="admin-categories">
    <section class="page-heading">
      <div>
        <h1>分类管理</h1>
        <p>维护商品分类名称、层级、排序和上下线状态。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">
        新增分类
      </el-button>
    </section>

    <section class="filter-bar surface">
      <el-input
        v-model.trim="filters.name"
        clearable
        placeholder="分类名称"
        @keyup.enter="search"
      />
      <el-select v-model="filters.status" clearable placeholder="全部状态">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="RotateCcw" @click="resetFilters">重置</el-button>
    </section>

    <section class="table-panel surface">
      <el-table :data="categories" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="180" />
        <el-table-column label="上级分类" min-width="150">
          <template #default="{ row }">
            {{ parentName(row.parentId) }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button text type="danger" @click="removeCategory(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑分类' : '新增分类'"
      width="min(520px, 92vw)"
    >
      <el-form label-position="top">
        <el-form-item label="分类名称">
          <el-input v-model.trim="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" class="full-width">
            <el-option label="顶级分类" :value="0" />
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCategory">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RotateCcw, Search } from 'lucide-vue-next'
import { adminCategoryApi } from '../api'
import { formatDate } from '../utils/format'

const categories = ref([])
const allCategories = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const filters = reactive({ name: '', status: null })
const dialogVisible = ref(false)
const form = reactive({
  id: null,
  name: '',
  parentId: 0,
  sortOrder: 0,
  status: 1
})

const categoryById = computed(() =>
  Object.fromEntries(allCategories.value.map((item) => [item.id, item]))
)
const parentOptions = computed(() =>
  allCategories.value.filter((item) => item.id !== form.id)
)

function parentName(parentId) {
  if (!parentId) return '顶级分类'
  return categoryById.value[parentId]?.name || `分类 #${parentId}`
}

async function loadAllCategories() {
  const data = await adminCategoryApi.page({ page: 1, size: 200 })
  allCategories.value = data.records || []
}

async function loadCategories() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (filters.name) params.name = filters.name
    if (filters.status !== null && filters.status !== '') {
      params.status = filters.status
    }
    const data = await adminCategoryApi.page(params)
    categories.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '分类加载失败')
  } finally {
    loading.value = false
  }
}

async function refresh() {
  await Promise.all([loadCategories(), loadAllCategories()])
}

function search() {
  page.value = 1
  loadCategories()
}

function resetFilters() {
  Object.assign(filters, { name: '', status: null })
  search()
}

function changePage(value) {
  page.value = value
  loadCategories()
}

function resetForm() {
  Object.assign(form, {
    id: null,
    name: '',
    parentId: 0,
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function saveCategory() {
  if (!form.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name,
      parentId: form.parentId,
      sortOrder: form.sortOrder,
      status: form.status
    }
    if (form.id) {
      await adminCategoryApi.update(form.id, { ...payload, id: form.id })
    } else {
      await adminCategoryApi.create(payload)
    }
    ElMessage.success(form.id ? '分类已更新' : '分类已创建')
    dialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error(error.message || '分类保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确认${nextStatus === 1 ? '启用' : '停用'}分类“${row.name}”吗？`,
      '分类状态',
      { type: 'warning' }
    )
    await adminCategoryApi.updateStatus(row.id, nextStatus)
    ElMessage.success('分类状态已更新')
    refresh()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '状态更新失败')
    }
  }
}

async function removeCategory(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类“${row.name}”吗？存在子分类或商品时无法删除。`,
      '删除分类',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    await adminCategoryApi.remove(row.id)
    ElMessage.success('分类已删除')
    refresh()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(refresh)
</script>

<style scoped>
.admin-categories {
  display: grid;
  gap: 16px;
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

.filter-bar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 160px auto auto;
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

.full-width {
  width: 100%;
}

@media (max-width: 700px) {
  .filter-bar {
    grid-template-columns: 1fr;
  }

  .page-heading {
    align-items: flex-start;
  }
}
</style>
