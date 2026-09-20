<template>
  <BuyerLayout>
    <div class="cart-heading">
      <div>
        <h1>购物车</h1>
        <p class="muted">确认商品和数量后统一结算。</p>
      </div>
      <el-button
        v-if="cart.items.length"
        text
        type="danger"
        :icon="Trash2"
        @click="clearCart"
      >
        清空
      </el-button>
    </div>

    <div v-if="cart.loading && !cart.items.length" class="cart-list">
      <el-skeleton v-for="item in 3" :key="item" animated :rows="3" />
    </div>

    <EmptyState
      v-else-if="!cart.items.length"
      title="购物车还是空的"
      description="去首页挑选一些商品吧"
    />

    <template v-else>
      <div class="cart-list">
        <article
          v-for="line in lines"
          :key="line.id"
          class="cart-item surface"
          @click="openProduct(line)"
        >
          <div class="item-image">
            <img
              v-if="line.product?.mainImage"
              :src="line.product.mainImage"
              :alt="line.product.name"
            />
            <PackageOpen v-else :size="38" />
          </div>
          <div class="item-content">
            <div class="item-top">
              <div>
                <h2>{{ line.product?.name || `商品 #${line.productId}` }}</h2>
                <div class="seller-name">{{ line.product?.sellerName || '平台商家' }}</div>
              </div>
              <el-button
                text
                type="danger"
                :icon="Trash2"
                @click.stop="removeItem(line)"
              />
            </div>
            <div class="item-bottom">
              <span class="price">{{ formatPrice(line.product?.price) }}</span>
              <div class="quantity-control" @click.stop>
                <el-input-number
                  v-model="line.num"
                  :min="1"
                  :max="Math.max(line.product?.stock || 1, 1)"
                  size="small"
                  @change="(value) => changeNum(line, value)"
                />
              </div>
            </div>
          </div>
        </article>
      </div>

      <section class="checkout-bar surface">
        <div>
          <div class="muted">合计</div>
          <div class="total price">{{ formatPrice(totalAmount) }}</div>
        </div>
        <el-button
          type="primary"
          size="large"
          :icon="CreditCard"
          @click="openCheckout"
        >
          结算全部商品
        </el-button>
      </section>
    </template>

    <el-drawer v-model="checkoutVisible" title="确认收货信息" size="min(440px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="收货人">
          <el-input v-model.trim="checkout.receiverName" size="large" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model.trim="checkout.receiverPhone" size="large" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input
            v-model.trim="checkout.receiverAddress"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-button
          class="full-button"
          type="primary"
          size="large"
          :loading="submitting"
          :icon="Check"
          @click="submitOrder"
        >
          提交订单
        </el-button>
      </el-form>
    </el-drawer>
  </BuyerLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, CreditCard, PackageOpen, Trash2 } from 'lucide-vue-next'
import BuyerLayout from '../components/BuyerLayout.vue'
import EmptyState from '../components/EmptyState.vue'
import { orderApi, productApi, profileApi } from '../api'
import { useCartStore } from '../stores/cart'
import { formatPrice } from '../utils/format'

const router = useRouter()
const cart = useCartStore()

const productMap = ref({})
const buyerProfile = ref(null)
const checkoutVisible = ref(false)
const submitting = ref(false)
const idempotencyKey = ref('')
const checkout = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

const lines = computed(() =>
  cart.items.map((item) => ({
    ...item,
    product: productMap.value[item.productId]
  }))
)

const totalAmount = computed(() =>
  lines.value.reduce(
    (sum, line) => sum + Number(line.product?.price || 0) * Number(line.num || 0),
    0
  )
)

async function loadCart() {
  try {
    const items = await cart.load({ force: true })
    const entries = await Promise.all(
      (items || []).map(async (item) => {
        try {
          return [item.productId, await productApi.detail(item.productId)]
        } catch {
          return null
        }
      })
    )
    productMap.value = Object.fromEntries(entries.filter(Boolean))
  } catch (error) {
    ElMessage.error(error.message || '购物车加载失败')
  }
}

async function loadProfile() {
  try {
    const profile = await profileApi.get()
    buyerProfile.value = profile
    checkout.receiverName = profile.nickname || profile.username || ''
    checkout.receiverPhone = profile.phone || ''
    checkout.receiverAddress = profile.address || ''
  } catch {
    // Checkout fields remain editable when profile loading fails.
  }
}

function isProfileIncomplete(profile) {
  return (
    !profile?.nickname ||
    !profile?.phone ||
    !profile?.email ||
    !profile?.address
  )
}

async function ensureProfileComplete() {
  let profile = buyerProfile.value
  if (!profile) {
    try {
      profile = await profileApi.get()
      buyerProfile.value = profile
    } catch (error) {
      ElMessage.error(error.message || '个人信息加载失败')
      return false
    }
  }
  if (!isProfileIncomplete(profile)) {
    checkout.receiverName ||= profile.nickname || profile.username || ''
    checkout.receiverPhone ||= profile.phone || ''
    checkout.receiverAddress ||= profile.address || ''
    return true
  }

  try {
    await ElMessageBox.confirm(
      '请先补全昵称、手机号、邮箱和收货地址后再下单。',
      '个人信息不完整',
      {
        confirmButtonText: '去补全信息',
        cancelButtonText: '暂不结算',
        type: 'warning'
      }
    )
    router.push('/profile')
  } catch {
    // User chose to stay on the cart page.
  }
  return false
}

async function changeNum(line, value) {
  if (!value) return
  try {
    await cart.updateNum(line.id, value)
    idempotencyKey.value = ''
  } catch (error) {
    ElMessage.error(error.message || '数量修改失败')
  }
}

function openProduct(line) {
  router.push({
    name: 'product-detail',
    params: { id: line.productId }
  })
}

async function removeItem(line) {
  try {
    await cart.remove(line.id)
    delete productMap.value[line.productId]
    idempotencyKey.value = ''
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

async function clearCart() {
  try {
    await ElMessageBox.confirm('确认清空购物车吗？', '提示', {
      type: 'warning'
    })
    await cart.clear()
    productMap.value = {}
    idempotencyKey.value = ''
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '清空失败')
    }
  }
}

async function openCheckout() {
  if (!(await ensureProfileComplete())) {
    return
  }
  if (!idempotencyKey.value) {
    idempotencyKey.value = newIdempotencyKey()
  }
  if (!checkout.receiverName || !checkout.receiverPhone || !checkout.receiverAddress) {
    loadProfile()
  }
  checkoutVisible.value = true
}

function newIdempotencyKey() {
  if (crypto.randomUUID) return crypto.randomUUID()
  return `order-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function submitOrder() {
  if (!(await ensureProfileComplete())) {
    return
  }
  if (
    !checkout.receiverName ||
    !checkout.receiverPhone ||
    !checkout.receiverAddress
  ) {
    ElMessage.warning('请填写完整的收货信息')
    return
  }
  submitting.value = true
  try {
    const createdOrders = await orderApi.create({
      idempotencyKey: idempotencyKey.value,
      ...checkout
    })
    idempotencyKey.value = ''
    await cart.load()
    checkoutVisible.value = false
    ElMessage.success(
      Array.isArray(createdOrders) && createdOrders.length > 1
        ? `订单创建成功，已按卖家拆分为 ${createdOrders.length} 张订单`
        : '订单创建成功'
    )
    router.push({ path: '/profile', query: { status: '0' } })
  } catch (error) {
    ElMessage.error(error.message || '订单创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCart()
  loadProfile()
})
</script>

<style scoped>
.cart-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.cart-heading h1 {
  margin: 0 0 6px;
  font-size: 28px;
}

.cart-heading p {
  margin: 0;
}

.cart-list {
  display: grid;
  gap: 12px;
}

.cart-item {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 16px;
  padding: 14px;
  cursor: pointer;
  transition: border-color 0.16s, box-shadow 0.16s, transform 0.16s;
}

.cart-item:hover {
  border-color: rgba(232, 111, 58, 0.42);
  box-shadow: 0 10px 26px rgba(127, 74, 42, 0.09);
  transform: translateY(-1px);
}

.item-image {
  display: grid;
  width: 112px;
  height: 112px;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  color: #d68a66;
  background: var(--surface-strong);
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-content {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
}

.item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.item-top h2 {
  margin: 0 0 6px;
  font-size: 17px;
  line-height: 1.35;
}

.seller-name {
  color: var(--muted);
  font-size: 12px;
}

.item-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.quantity-control {
  display: flex;
}

.checkout-bar {
  position: sticky;
  bottom: 86px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-top: 18px;
  padding: 16px;
}

.total {
  font-size: 26px;
}

.full-button {
  width: 100%;
}

@media (max-width: 560px) {
  .cart-item {
    grid-template-columns: 84px 1fr;
  }

  .item-image {
    width: 84px;
    height: 84px;
  }

  .checkout-bar {
    bottom: 82px;
  }
}
</style>
