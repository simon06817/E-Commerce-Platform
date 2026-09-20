<template>
  <div class="seller-page-block">
    <header class="page-heading">
      <div>
        <h1>商品管理</h1>
        <p>维护商品信息、库存和上下架状态。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
    </header>

    <section class="filter-bar">
      <el-input
        v-model.trim="filters.keyword"
        clearable
        placeholder="搜索商品名称或描述"
        :prefix-icon="Search"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="filters.categoryId" clearable placeholder="全部分类" @change="search">
        <el-option
          v-for="item in flatCategories"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="全部状态" @change="search">
        <el-option label="已上架" :value="1" />
        <el-option label="已下架" :value="0" />
      </el-select>
      <el-button :icon="RefreshCw" @click="loadProducts">刷新</el-button>
    </section>

    <div class="table-panel">
      <el-table v-loading="loading" :data="products" stripe>
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="product-cell">
              <div class="product-thumb">
                <img v-if="row.mainImage" :src="row.mainImage" :alt="row.name" />
                <PackageOpen v-else :size="22" />
              </div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.description || '暂无描述' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryId" label="分类编号" width="100" />
        <el-table-column label="价格" width="120">
          <template #default="{ row }">
            <strong class="price">{{ formatPrice(row.price) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="updateStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="removeProduct(row)">删除</el-button>
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
          @current-change="loadProducts"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑商品' : '新增商品'"
      width="min(640px, 94vw)"
    >
      <el-form label-position="top">
        <el-form-item label="商品名称" required>
          <el-input v-model.trim="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input
            v-model.trim="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="价格" required>
            <el-input-number v-model="form.price" :min="0.01" :precision="2" />
          </el-form-item>
          <el-form-item label="库存" required>
            <el-input-number v-model="form.stock" :min="0" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="分类" required>
            <el-select v-model="form.categoryId" placeholder="请选择分类">
              <el-option
                v-for="item in flatCategories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio-button :value="1">上架</el-radio-button>
              <el-radio-button :value="0">下架</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
        <el-form-item label="商品主图">
          <el-upload
            :show-file-list="false"
            accept="image/*"
            :http-request="uploadMainImage"
          >
            <div class="image-uploader">
              <img v-if="form.mainImage" :src="form.mainImage" alt="商品主图" />
              <Upload v-else :size="24" />
              <span>{{ form.mainImage ? '点击更换图片' : '上传商品主图' }}</span>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProduct">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PackageOpen, Plus, RefreshCw, Search, Upload } from 'lucide-vue-next'
import { categoryApi, sellerProductApi, uploadApi } from '../api'
import { formatDate, formatPrice } from '../utils/format'

const products = ref([])
const flatCategories = ref([])
const loading = ref(false)
const submitting = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref(null)
const filters = reactive({
  keyword: '',
  categoryId: null,
  status: null
})
const form = reactive({
  name: '',
  description: '',
  price: 1,
  stock: 0,
  categoryId: null,
  mainImage: '',
  status: 1
})

function flattenCategories(nodes, output = []) {
  for (const node of nodes || []) {
    output.push(node)
    flattenCategories(node.children, output)
  }
  return output
}

async function loadCategories() {
  flatCategories.value = flattenCategories(await categoryApi.list())
}

async function loadProducts() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.categoryId) params.categoryId = filters.categoryId
    if (filters.status !== null && filters.status !== '') params.status = filters.status
    const data = await sellerProductApi.page(params)
    products.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '商品加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadProducts()
}

function resetForm() {
  Object.assign(form, {
    name: '',
    description: '',
    price: 1,
    stock: 0,
    categoryId: null,
    mainImage: '',
    status: 1
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(product) {
  editingId.value = product.id
  Object.assign(form, {
    name: product.name,
    description: product.description || '',
    price: Number(product.price),
    stock: product.stock,
    categoryId: product.categoryId,
    mainImage: product.mainImage || '',
    status: product.status
  })
  dialogVisible.value = true
}

async function uploadMainImage(options) {
  try {
    form.mainImage = await uploadApi.image(options.file)
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  }
}

async function submitProduct() {
  if (!form.name || !form.categoryId || Number(form.price) <= 0) {
    ElMessage.warning('请填写商品名称、价格和分类')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form, price: Number(form.price), stock: Number(form.stock) }
    if (editingId.value) {
      await sellerProductApi.update(editingId.value, payload)
      ElMessage.success('商品已更新')
    } else {
      await sellerProductApi.create(payload)
      ElMessage.success('商品已创建')
    }
    dialogVisible.value = false
    await loadProducts()
  } catch (error) {
    ElMessage.error(error.message || '商品保存失败')
  } finally {
    submitting.value = false
  }
}

async function updateStatus(product) {
  try {
    await sellerProductApi.updateStatus(product.id, product.status)
    ElMessage.success(product.status === 1 ? '商品已上架' : '商品已下架')
  } catch (error) {
    product.status = product.status === 1 ? 0 : 1
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function removeProduct(product) {
  try {
    await ElMessageBox.confirm(`确认删除商品“${product.name}”吗？`, '删除商品', {
      type: 'warning'
    })
    await sellerProductApi.remove(product.id)
    ElMessage.success('商品已删除')
    await loadProducts()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(async () => {
  await loadCategories()
  await loadProducts()
})
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

.filter-bar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px 150px auto;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.table-panel {
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.product-cell > div:last-child {
  display: grid;
  min-width: 0;
}

.product-cell span {
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-thumb {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border-radius: 6px;
  background: var(--surface-strong);
}

.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-grid .el-select {
  width: 100%;
}

.image-uploader {
  display: grid;
  width: 170px;
  height: 120px;
  place-items: center;
  overflow: hidden;
  border: 1px dashed var(--line);
  border-radius: 8px;
  color: var(--muted);
  background: var(--surface-strong);
}

.image-uploader img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-uploader span {
  margin-top: -20px;
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

@media (max-width: 860px) {
  .filter-bar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-bar,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
