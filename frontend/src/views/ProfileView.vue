<template>
  <BuyerLayout>
    <section class="profile-head">
      <button class="avatar-button" type="button" @click="openProfile">
        <img v-if="auth.avatarUrl" :src="auth.avatarUrl" alt="头像" />
        <span v-else>{{ avatarText }}</span>
        <i class="camera-mark"><Camera :size="15" /></i>
      </button>
      <div class="profile-head-copy">
        <h1>{{ profile.nickname || auth.displayName }}</h1>
        <p>@{{ profile.username || auth.username }}</p>
      </div>
      <div class="profile-head-actions">
        <el-badge :value="unreadCount" :hidden="!unreadCount">
          <el-button
            circle
            :icon="Bell"
            aria-label="通知"
            @click="openNotifications"
          />
        </el-badge>
        <el-button type="primary" plain :icon="Pencil" @click="openProfile">
          个人信息
        </el-button>
      </div>
    </section>

    <section class="status-grid">
      <button
        v-for="item in statusCards"
        :key="item.value"
        type="button"
        class="status-card"
        :class="{ active: activeStatus === item.value }"
        @click="selectStatus(item.value)"
      >
        <component :is="item.icon" :size="20" />
        <span>{{ item.label }}</span>
        <strong>{{ counts[item.value] || 0 }}</strong>
      </button>
    </section>

    <section class="orders-section">
      <div class="orders-heading">
        <div>
          <h2 class="section-title">历史订单</h2>
          <p class="muted">状态卡统计待付款、待发货、待收货和已完成；已取消订单在历史订单中展示。</p>
        </div>
        <div class="orders-heading-actions">
          <el-button text :icon="RotateCcw" @click="openReturns">
            退款记录
          </el-button>
          <el-button text :icon="RefreshCw" @click="refreshOrders">
            刷新
          </el-button>
        </div>
      </div>

      <div v-if="loadingOrders" class="order-list">
        <el-skeleton v-for="item in 3" :key="item" animated :rows="3" />
      </div>

      <EmptyState
        v-else-if="!orders.length"
        title="暂无订单"
        description="下单后可以在这里查看状态"
      />

      <div v-else class="order-list">
        <article v-for="order in orders" :key="order.id" class="order-card surface">
          <header class="order-top">
            <div>
              <div class="order-no">订单号 {{ order.orderNo }}</div>
              <div class="order-time">{{ formatDate(order.createTime) }}</div>
            </div>
            <div class="order-status-tags">
              <el-tag :type="orderStatus(order.status).type" effect="plain">
                {{ orderStatus(order.status).text }}
              </el-tag>
              <el-tag
                v-if="order.status === 2"
                :type="hasArrived(order) ? 'success' : 'warning'"
                effect="plain"
              >
                {{ hasArrived(order) ? '已到货' : '未到货' }}
              </el-tag>
            </div>
          </header>

          <div v-if="orderDetails[order.id]" class="order-context">
            <div>
              <span>商家</span>
              <strong>{{ shopNamesText(order) }}</strong>
            </div>
            <div>
              <span>下单账号</span>
              <strong>{{ buyerText(order) }}</strong>
            </div>
            <div>
              <span>收货人</span>
              <strong>
                {{ orderDetails[order.id].receiverName }}
                {{ orderDetails[order.id].receiverPhone }}
              </strong>
            </div>
            <div>
              <span>收货地址</span>
              <strong>{{ orderDetails[order.id].receiverAddress }}</strong>
            </div>
          </div>

          <div class="order-times">
            <div>
              <span>下单时间</span>
              <strong>{{ formatDate(order.createTime) }}</strong>
            </div>
            <div>
              <span>付款时间</span>
              <strong>{{ formatDate(order.payTime) }}</strong>
            </div>
            <div>
              <span>发货时间</span>
              <strong>{{ formatDate(order.shipTime) }}</strong>
            </div>
            <div>
              <span>确认/完成时间</span>
              <strong>{{ formatDate(order.completeTime) }}</strong>
            </div>
          </div>

          <div class="order-items">
            <div
              v-for="item in orderDetails[order.id]?.items || []"
              :key="item.id"
              class="order-item"
            >
              <div class="order-item-image">
                <img
                  v-if="item.productImage"
                  :src="item.productImage"
                  :alt="item.productName"
                />
                <PackageOpen v-else :size="23" />
              </div>
              <div class="order-item-name">{{ item.productName }}</div>
              <div class="order-item-quantity">×{{ item.quantity }}</div>
              <div class="price">{{ formatPrice(item.subtotal) }}</div>
              <div
                v-if="order.status === 2 || order.status === 3"
                class="order-item-actions"
              >
                <el-button
                  v-if="!isRefundedItem(item) && !isItemReviewed(order, item)"
                  text
                  type="primary"
                  size="small"
                  @click="openReview(order, item)"
                >
                  评价
                </el-button>
                <el-tag
                  v-else-if="!isRefundedItem(item)"
                  type="success"
                  size="small"
                >
                  已评价
                </el-tag>

                <el-tag
                  v-if="returnByOrderItem[item.id]"
                  :type="returnStatus(returnByOrderItem[item.id].status).type"
                  size="small"
                >
                  {{ returnStatus(returnByOrderItem[item.id].status).text }}
                </el-tag>
                <el-button
                  v-else-if="canApplyReturn(order)"
                  text
                  type="warning"
                  size="small"
                  @click="openReturn(order, item)"
                >
                  申请退款
                </el-button>
              </div>
              <div
                v-if="reviewForItem(order, item)"
                class="order-item-review"
              >
                <div class="review-heading">
                  <span>我的评价</span>
                  <el-rate
                    :model-value="reviewForItem(order, item).rating"
                    disabled
                    show-score
                  />
                </div>
                <p>
                  {{ reviewForItem(order, item).content || '用户未填写评价内容' }}
                </p>
                <p
                  v-if="reviewForItem(order, item).replyContent"
                  class="seller-reply"
                >
                  商家回复：{{ reviewForItem(order, item).replyContent }}
                </p>
              </div>
            </div>
          </div>

          <footer class="order-footer">
            <div class="order-total">
              实付 <span class="price">{{ formatPrice(order.totalAmount) }}</span>
            </div>
            <div class="order-actions">
              <el-button
                v-if="order.status === 0"
                type="danger"
                plain
                :icon="XCircle"
                @click="cancelOrder(order)"
              >
                取消
              </el-button>
              <el-button
                v-if="order.status === 0"
                type="primary"
                :icon="CreditCard"
                @click="payOrder(order)"
              >
                付款
              </el-button>
              <el-button
                v-if="order.status === 2"
                type="success"
                :icon="CircleCheck"
                @click="confirmOrder(order)"
              >
                确认收货
              </el-button>
            </div>
          </footer>
        </article>
      </div>

      <div v-if="orderPage.total > pageSize" class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="orderPage.total"
          layout="prev, pager, next"
          background
          @current-change="loadOrders"
        />
      </div>
    </section>

    <el-drawer
      v-model="profileVisible"
      title="个人信息"
      size="min(460px, 92vw)"
    >
      <div class="profile-editor-head">
        <el-upload
          :show-file-list="false"
          accept="image/*"
          :http-request="uploadAvatar"
        >
          <button class="avatar-button avatar-button-small" type="button">
            <img v-if="auth.avatarUrl" :src="auth.avatarUrl" alt="头像" />
            <span v-else>{{ avatarText }}</span>
            <i class="camera-mark"><Camera :size="15" /></i>
          </button>
        </el-upload>
        <div>
          <strong>个人头像</strong>
          <p class="muted">点击头像选择本地图片上传</p>
        </div>
      </div>

      <el-tabs v-model="profileTab">
        <el-tab-pane label="资料" name="profile">
          <el-form label-position="top">
            <el-form-item label="用户名">
              <el-input :model-value="profile.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model.trim="editForm.nickname" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model.trim="editForm.phone" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model.trim="editForm.email" />
            </el-form-item>
            <el-form-item label="收货地址">
              <el-input
                v-model.trim="editForm.address"
                type="textarea"
                :rows="3"
              />
            </el-form-item>
            <el-button
              class="full-button"
              type="primary"
              :loading="savingProfile"
              @click="saveProfile"
            >
              保存资料
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form label-position="top">
            <el-form-item label="当前密码">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-button
              class="full-button"
              type="primary"
              :loading="savingPassword"
              @click="changePassword"
            >
              更新密码
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <el-button
        class="logout-button"
        type="danger"
        plain
        :icon="LogOut"
        @click="logout"
      >
        退出登录
      </el-button>
    </el-drawer>

    <el-dialog
      v-model="reviewVisible"
      title="评价商品"
      width="min(520px, 92vw)"
    >
      <div class="dialog-product-name">
        {{ reviewTarget?.productName || '商品' }}
      </div>
      <el-form label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model.trim="reviewForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="分享你的使用感受"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="reviewSubmitting"
          @click="submitReview"
        >
          提交评价
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="returnVisible"
      title="申请退款"
      width="min(520px, 92vw)"
    >
      <div class="dialog-product-name">
        {{ returnTarget?.productName || '商品' }}
      </div>
      <el-form label-position="top">
        <el-form-item label="退款原因">
          <el-input
            v-model.trim="returnForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请说明退款原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="returnSubmitting"
          @click="submitReturn"
        >
          提交申请
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="returnsVisible"
      title="退款记录"
      size="min(520px, 92vw)"
    >
      <div v-if="loadingReturns" class="drawer-list">
        <el-skeleton v-for="item in 3" :key="item" animated :rows="3" />
      </div>
      <EmptyState
        v-else-if="!returnRecords.length"
        title="暂无退款记录"
        description="发货后或确认收货后七天内可以申请退款"
      />
      <div v-else class="drawer-list">
        <article
          v-for="item in returnRecords"
          :key="item.id"
          class="return-record surface"
        >
          <header class="record-header">
            <div>
              <strong>{{ item.productName }}</strong>
              <p class="muted">订单号 {{ item.orderNo }}</p>
            </div>
            <el-tag :type="returnStatus(item.status).type" size="small">
              {{ returnStatus(item.status).text }}
            </el-tag>
          </header>
          <div class="record-row">数量：{{ item.quantity }}</div>
          <div class="record-row">退款金额：<span class="price">{{ formatPrice(item.refundAmount) }}</span></div>
          <div class="record-row">退款原因：{{ item.reason }}</div>
          <div v-if="item.handleNote" class="record-row">
            卖家说明：{{ item.handleNote }}
          </div>
          <div class="record-time">申请时间：{{ formatDate(item.applyTime) }}</div>
          <el-button
            v-if="item.status === 0"
            class="full-button"
            type="danger"
            plain
            @click="cancelReturn(item)"
          >
            撤销申请
          </el-button>
        </article>
      </div>
    </el-drawer>

    <el-drawer
      v-model="notificationsVisible"
      title="订单通知"
      size="min(480px, 92vw)"
    >
      <div v-if="loadingNotifications" class="drawer-list">
        <el-skeleton v-for="item in 3" :key="item" animated :rows="2" />
      </div>
      <EmptyState
        v-else-if="!notifications.length"
        title="暂无通知"
        description="订单状态变化后会在这里提醒"
      />
      <div v-else class="drawer-list">
        <button
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.readFlag }"
          type="button"
          @click="markNotificationRead(item)"
        >
          <span class="notification-dot" />
          <span class="notification-content">
            <strong>{{ localizeMessage(item.title) }}</strong>
            <span>{{ localizeMessage(item.content) }}</span>
            <time>{{ formatDate(item.createTime) }}</time>
          </span>
        </button>
      </div>
    </el-drawer>
  </BuyerLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Camera,
  CircleCheck,
  CreditCard,
  LogOut,
  MessageSquareText,
  PackageOpen,
  Pencil,
  RefreshCw,
  RotateCcw,
  Truck,
  WalletCards,
  XCircle
} from 'lucide-vue-next'
import BuyerLayout from '../components/BuyerLayout.vue'
import EmptyState from '../components/EmptyState.vue'
import {
  notificationApi,
  orderApi,
  profileApi,
  returnApi,
  reviewApi,
  uploadApi
} from '../api'
import { useAuthStore } from '../stores/auth'
import {
  BUYER_ORDER_TABS,
  formatDate,
  formatPrice,
  orderStatus,
  returnStatus
} from '../utils/format'
import { localizeMessage } from '../utils/message'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const profile = reactive({
  id: null,
  username: '',
  nickname: '',
  phone: '',
  email: '',
  address: ''
})
const editForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  address: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileVisible = ref(false)
const profileTab = ref('profile')
const savingProfile = ref(false)
const savingPassword = ref(false)
const loadingOrders = ref(false)
const orders = ref([])
const orderPage = reactive({ total: 0 })
const orderDetails = reactive({})
const counts = reactive({ 0: 0, 1: 0, 2: 0, 3: 0, reviewed: 0 })
const statusQuery = route.query.status
const activeStatus = ref(
  statusQuery === undefined
    ? null
    : statusQuery === 'reviewed'
      ? 'reviewed'
      : Number(statusQuery)
)
const page = ref(1)
const pageSize = 6
const now = ref(Date.now())
const AUTO_ARRIVAL_HOURS = 24
let deliveryTimer = null
const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewTarget = ref(null)
const reviewedItemIds = ref(new Set())
const reviewForm = reactive({
  rating: 5,
  content: ''
})
const returnVisible = ref(false)
const returnSubmitting = ref(false)
const returnTarget = ref(null)
const returnForm = reactive({
  reason: ''
})
const returnsVisible = ref(false)
const loadingReturns = ref(false)
const returnRecords = ref([])
const returnByOrderItem = reactive({})
const notificationsVisible = ref(false)
const loadingNotifications = ref(false)
const notifications = ref([])

const statusCards = computed(() =>
  BUYER_ORDER_TABS.filter((item) => item.value !== null).map((item) => ({
    ...item,
    icon:
      item.value === 0
        ? WalletCards
        : item.value === 1
          ? PackageOpen
          : item.value === 2
            ? Truck
            : item.value === 3
              ? CircleCheck
              : MessageSquareText
  }))
)

const avatarText = computed(() =>
  (profile.nickname || profile.username || '买').slice(0, 1).toUpperCase()
)

const unreadCount = computed(
  () => notifications.value.filter((item) => !item.readFlag).length
)

const reviewedItemsStorageKey = computed(
  () => `ecommerce_reviewed_items_${auth.userId || 'guest'}`
)

function loadReviewedItems() {
  try {
    const ids = JSON.parse(
      localStorage.getItem(reviewedItemsStorageKey.value) || '[]'
    )
    reviewedItemIds.value = new Set(ids)
  } catch {
    reviewedItemIds.value = new Set()
  }
}

function rememberReviewedItem(orderItemId) {
  const reviewed = new Set(reviewedItemIds.value)
  reviewed.add(orderItemId)
  reviewedItemIds.value = reviewed
  localStorage.setItem(
    reviewedItemsStorageKey.value,
    JSON.stringify([...reviewed])
  )
}

async function loadProfile() {
  try {
    Object.assign(profile, await profileApi.get())
    Object.assign(editForm, {
      nickname: profile.nickname || '',
      phone: profile.phone || '',
      email: profile.email || '',
      address: profile.address || ''
    })
  } catch (error) {
    ElMessage.error(error.message || '个人信息加载失败')
  }
}

function openProfile() {
  Object.assign(editForm, {
    nickname: profile.nickname || '',
    phone: profile.phone || '',
    email: profile.email || '',
    address: profile.address || ''
  })
  profileVisible.value = true
}

async function uploadAvatar(options) {
  if (options.file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }
  try {
    const url = await uploadApi.image(options.file)
    auth.setAvatar(url)
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(error.message || '头像上传失败')
  }
}

async function saveProfile() {
  if (!editForm.nickname) {
    ElMessage.warning('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    Object.assign(profile, await profileApi.update(editForm))
    auth.updateDisplayName(profile.nickname)
    ElMessage.success('资料已更新')
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少需要 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPassword.value = true
  try {
    await profileApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    Object.assign(passwordForm, {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    ElMessage.success('密码已更新')
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    savingPassword.value = false
  }
}

async function loadCounts() {
  try {
    const statuses = [0, 1, 2, 3]
    const [pages, reviewedPage] = await Promise.all([
      Promise.all(
        statuses.map((status) =>
          orderApi.page({ page: 1, size: 1, status })
        )
      ),
      orderApi.page({ page: 1, size: 1, reviewed: true })
    ])
    statuses.forEach((status, index) => {
      counts[status] = pages[index].total || 0
    })
    counts.reviewed = reviewedPage.total || 0
  } catch {
    // Counts are non-critical.
  }
}

function isRefundedItem(item) {
  return returnByOrderItem[item.id]?.status === 1
}

function isItemReviewed(order, item) {
  if (reviewedItemIds.value.has(item.id)) {
    return true
  }
  const recordedIds = orderDetails[order.id]?.reviewedItemIds || []
  return recordedIds.includes(item.id)
}

function reviewForItem(order, item) {
  const reviews = orderDetails[order.id]?.reviews || []
  return reviews.find((review) => review.orderItemId === item.id) || null
}

async function loadOrders() {
  loadingOrders.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize
    }
    if (activeStatus.value === 'reviewed') {
      params.reviewed = true
    } else if (activeStatus.value !== null) {
      params.status = activeStatus.value
    }
    const data = await orderApi.pageDetails(params)
    orders.value = data.records || []
    orderPage.total = data.total || 0

    Object.keys(orderDetails).forEach((key) => delete orderDetails[key])
    for (const order of orders.value) {
      orderDetails[order.id] = order
    }
  } catch (error) {
    ElMessage.error(error.message || '订单加载失败')
  } finally {
    loadingOrders.value = false
  }
}

function withinReturnWindow(order) {
  if (!order?.completeTime) return false
  const completedAt = new Date(order.completeTime).getTime()
  if (!Number.isFinite(completedAt)) return false
  const elapsed = Date.now() - completedAt
  return elapsed >= 0 && elapsed <= 7 * 24 * 60 * 60 * 1000
}

function canApplyReturn(order) {
  if (order?.status === 2) return true
  return order?.status === 3 && withinReturnWindow(order)
}

function shopNamesText(order) {
  const names = orderDetails[order.id]?.shopNames || []
  return names.length ? names.join('、') : '平台商家'
}

function hasArrived(order) {
  if (!order?.shipTime) return false
  const shipTime = new Date(String(order.shipTime).replace(' ', 'T')).getTime()
  if (!Number.isFinite(shipTime)) return false
  return now.value - shipTime >= AUTO_ARRIVAL_HOURS * 60 * 60 * 1000
}

function buyerText(order) {
  const detail = orderDetails[order.id]
  if (!detail) return '-'
  return detail.buyerNickname
    ? `${detail.buyerUsername}（${detail.buyerNickname}）`
    : detail.buyerUsername || '-'
}

async function loadReturns() {
  loadingReturns.value = true
  try {
    const data = await returnApi.page({ page: 1, size: 100 })
    returnRecords.value = data.records || []
    Object.keys(returnByOrderItem).forEach(
      (key) => delete returnByOrderItem[key]
    )
    for (const item of returnRecords.value) {
      returnByOrderItem[item.orderItemId] = item
    }
  } catch (error) {
    ElMessage.error(error.message || '退货记录加载失败')
  } finally {
    loadingReturns.value = false
  }
}

function openReturns() {
  returnsVisible.value = true
  loadReturns()
}

function openReview(order, item) {
  if (isRefundedItem(item)) {
    ElMessage.warning('已退款商品不能评价')
    return
  }
  reviewTarget.value = {
    ...item,
    orderId: order.id
  }
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewTarget.value || !reviewForm.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  if (!reviewForm.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  reviewSubmitting.value = true
  try {
    await reviewApi.create({
      orderItemId: reviewTarget.value.id,
      rating: reviewForm.rating,
      content: reviewForm.content
    })
    rememberReviewedItem(reviewTarget.value.id)
    reviewVisible.value = false
    ElMessage.success('评价已提交')
    await refreshOrders()
  } catch (error) {
    ElMessage.error(error.message || '评价提交失败')
  } finally {
    reviewSubmitting.value = false
  }
}

function openReturn(order, item) {
  if (!canApplyReturn(order)) {
    ElMessage.warning('当前订单状态或退款期限不允许申请')
    return
  }
  returnTarget.value = {
    ...item,
    orderId: order.id
  }
  returnForm.reason = ''
  returnVisible.value = true
}

async function submitReturn() {
  if (!returnTarget.value || !returnForm.reason) {
    ElMessage.warning('请填写退货原因')
    return
  }
  returnSubmitting.value = true
  try {
    await returnApi.apply({
      orderItemId: returnTarget.value.id,
      reason: returnForm.reason
    })
    returnVisible.value = false
    ElMessage.success('退货申请已提交')
    await loadReturns()
  } catch (error) {
    ElMessage.error(error.message || '退货申请失败')
  } finally {
    returnSubmitting.value = false
  }
}

async function cancelReturn(item) {
  try {
    await ElMessageBox.confirm('确认撤销这条退货申请吗？', '提示', {
      type: 'warning'
    })
    await returnApi.cancel(item.id)
    ElMessage.success('退货申请已撤销')
    await loadReturns()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '撤销失败')
    }
  }
}

async function loadNotifications() {
  loadingNotifications.value = true
  try {
    const data = await notificationApi.page({ page: 1, size: 50 })
    notifications.value = data.records || []
  } catch {
    notifications.value = []
  } finally {
    loadingNotifications.value = false
  }
}

function openNotifications() {
  notificationsVisible.value = true
  loadNotifications()
}

async function markNotificationRead(item) {
  if (item.readFlag) return
  try {
    await notificationApi.markRead(item.id)
    item.readFlag = true
  } catch (error) {
    ElMessage.error(error.message || '通知更新失败')
  }
}

function selectStatus(status) {
  activeStatus.value = activeStatus.value === status ? null : status
  page.value = 1
  router.replace({
    path: '/profile',
    query: activeStatus.value === null ? {} : { status: activeStatus.value }
  })
  loadOrders()
}

async function refreshOrders() {
  await Promise.all([loadOrders(), loadCounts()])
}

async function payOrder(order) {
  try {
    await orderApi.pay(order.id)
    ElMessage.success('付款成功')
    await refreshOrders()
  } catch (error) {
    ElMessage.error(error.message || '付款失败')
  }
}

async function cancelOrder(order) {
  try {
    await ElMessageBox.confirm('确认取消该订单吗？', '提示', {
      type: 'warning'
    })
    await orderApi.cancel(order.id)
    ElMessage.success('订单已取消')
    await refreshOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '取消失败')
    }
  }
}

async function confirmOrder(order) {
  try {
    await ElMessageBox.confirm(
      '确认已经收到商品吗？确认后订单将完成，并可以发布评价。',
      '确认收货',
      { type: 'warning' }
    )
    await orderApi.confirm(order.id)
    ElMessage.success('已确认收货')
    await refreshOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '确认收货失败')
    }
  }
}

async function logout() {
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', {
      type: 'warning'
    })
    await auth.logout()
    router.replace('/login')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '退出失败')
    }
  }
}

onMounted(() => {
  loadReviewedItems()
  loadProfile()
  refreshOrders()
  loadReturns()
  loadNotifications()
  deliveryTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 30000)
})

onBeforeUnmount(() => {
  if (deliveryTimer) {
    window.clearInterval(deliveryTimer)
  }
})
</script>

<style scoped>
.profile-head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: var(--shadow);
}

.avatar-button {
  position: relative;
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  overflow: hidden;
  border: 0;
  border-radius: 50%;
  color: #fff;
  background: var(--primary);
  font-size: 28px;
  font-weight: 900;
  cursor: pointer;
}

.profile-head-actions,
.orders-heading-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-editor-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-strong);
}

.profile-editor-head p {
  margin: 5px 0 0;
  font-size: 13px;
}

.avatar-button-small {
  width: 58px;
  height: 58px;
  font-size: 22px;
}

.avatar-button img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-mark {
  position: absolute;
  right: 0;
  bottom: 2px;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border: 2px solid var(--surface);
  border-radius: 50%;
  color: #fff;
  background: var(--accent);
}

.profile-head-copy h1 {
  margin: 0 0 5px;
  font-size: 26px;
}

.profile-head-copy p {
  margin: 0;
  color: var(--muted);
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin: 20px 0 28px;
}

.status-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 9px;
  min-height: 60px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--text);
  background: var(--surface);
  cursor: pointer;
}

.status-card strong {
  color: var(--primary-dark);
  font-size: 20px;
}

.status-card.active {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.orders-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.orders-heading p {
  margin: 5px 0 0;
  font-size: 13px;
}

.order-list {
  display: grid;
  gap: 14px;
}

.order-card {
  overflow: hidden;
}

.order-top,
.order-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
}

.order-top {
  border-bottom: 1px solid var(--line);
}

.order-no {
  margin-bottom: 4px;
  font-weight: 700;
}

.order-time {
  color: var(--muted);
  font-size: 12px;
}

.order-status-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.order-items {
  padding: 4px 16px;
}

.order-context {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  background: #fffbf7;
}

.order-context div {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.order-context span {
  flex: 0 0 auto;
  color: var(--muted);
}

.order-context strong {
  min-width: 0;
  text-align: right;
  word-break: break-all;
}

.order-times {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
}

.order-times div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.order-times span {
  color: var(--muted);
  font-size: 12px;
}

.order-times strong {
  font-size: 13px;
  font-weight: 600;
  word-break: break-all;
}

.order-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f7e7dc;
}

.order-item:last-child {
  border-bottom: 0;
}

.order-item-image {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  overflow: hidden;
  border-radius: 6px;
  color: #d68a66;
  background: var(--surface-strong);
}

.order-item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-item-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-item-quantity {
  color: var(--muted);
}

.order-item-actions {
  display: flex;
  grid-column: 2 / -1;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.order-item-review {
  display: grid;
  grid-column: 2 / -1;
  gap: 6px;
  padding: 10px 12px;
  border-left: 3px solid var(--primary);
  border-radius: 4px;
  background: var(--primary-soft);
}

.order-item-review p {
  margin: 0;
  color: var(--text);
  font-size: 13px;
  line-height: 1.6;
}

.review-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--primary-dark);
  font-size: 13px;
  font-weight: 700;
}

.seller-reply {
  color: var(--muted) !important;
}

.order-footer {
  border-top: 1px solid var(--line);
  background: #fffbf7;
}

.order-total {
  color: var(--muted);
}

.order-total .price {
  margin-left: 5px;
  font-size: 20px;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.full-button,
.logout-button {
  width: 100%;
}

.logout-button {
  margin-top: 28px;
}

.dialog-product-name {
  margin-bottom: 18px;
  padding: 12px 14px;
  border-radius: 8px;
  color: var(--primary-dark);
  background: var(--primary-soft);
  font-weight: 700;
}

.drawer-list {
  display: grid;
  gap: 12px;
}

.return-record {
  padding: 16px;
  box-shadow: none;
}

.record-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.record-header p {
  margin: 5px 0 0;
  font-size: 12px;
}

.record-row {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.55;
}

.record-time {
  margin-top: 12px;
  color: var(--muted);
  font-size: 12px;
}

.notification-item {
  display: grid;
  grid-template-columns: 10px 1fr;
  align-items: start;
  gap: 10px;
  width: 100%;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--text);
  background: var(--surface);
  text-align: left;
  cursor: pointer;
}

.notification-item.unread {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.notification-dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 50%;
  background: #d8c4b8;
}

.notification-item.unread .notification-dot {
  background: var(--primary);
}

.notification-content {
  display: grid;
  gap: 6px;
}

.notification-content span {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.55;
}

.notification-content time {
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 760px) {
  .profile-head {
    grid-template-columns: auto 1fr;
  }

  .profile-head-actions {
    grid-column: 1 / -1;
  }

  .orders-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .order-context {
    grid-template-columns: 1fr;
  }

  .order-times {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .order-top,
  .order-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .order-item {
    grid-template-columns: 34px minmax(0, 1fr) auto;
  }

  .order-item-quantity {
    display: none;
  }

  .order-times {
    grid-template-columns: 1fr;
  }
}
</style>
