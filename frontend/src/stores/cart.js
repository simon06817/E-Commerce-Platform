import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { cartApi } from '../api'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)

  const count = computed(() =>
    items.value.reduce((total, item) => total + (item.num || 0), 0)
  )

  async function load() {
    loading.value = true
    try {
      items.value = await cartApi.list()
      return items.value
    } finally {
      loading.value = false
    }
  }

  async function add(productId, num = 1) {
    await cartApi.add(productId, num)
    await load()
  }

  async function updateNum(id, num) {
    await cartApi.updateNum(id, num)
    await load()
  }

  async function remove(id) {
    await cartApi.remove(id)
    await load()
  }

  async function clear() {
    await cartApi.clear()
    items.value = []
  }

  return { items, loading, count, load, add, updateNum, remove, clear }
})
