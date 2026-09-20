export const ORDER_STATUS = {
  0: { text: '待付款', type: 'warning' },
  1: { text: '待发货', type: 'primary' },
  2: { text: '待收货', type: 'success' },
  3: { text: '已完成', type: 'info' },
  4: { text: '已取消', type: 'danger' }
}

export const BUYER_ORDER_TABS = [
  { label: '历史订单', value: null },
  { label: '待付款', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已评价', value: 'reviewed' }
]

export const RETURN_STATUS = {
  0: { text: '待卖家处理', type: 'warning' },
  1: { text: '已同意并退款', type: 'success' },
  2: { text: '已拒绝', type: 'danger' },
  3: { text: '已撤销', type: 'info' }
}

export function formatPrice(value) {
  const number = Number(value || 0)
  return number.toFixed(2)
}

export function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

export function orderStatus(status) {
  return ORDER_STATUS[status] || { text: '未知状态', type: 'info' }
}

export function returnStatus(status) {
  return RETURN_STATUS[status] || { text: '未知状态', type: 'info' }
}
