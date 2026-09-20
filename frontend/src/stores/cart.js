import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { cartApi } from '../api'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)
  const loaded = ref(false)
  let loadTask = null
  let mutationVersion = 0
  const updateTasks = new Map()

  const count = computed(() =>
    items.value.reduce((total, item) => total + (item.num || 0), 0)
  )

  async function load(options = {}) {
    const force = Boolean(options.force)
    if (loadTask) {
      return loadTask
    }
    if (loaded.value && !force) {
      return items.value
    }
    loading.value = true
    const versionAtStart = mutationVersion
    loadTask = Promise.allSettled([...updateTasks.values()])
      .then(() => cartApi.list())
      .then((data) => {
        const nextItems = data || []
        if (versionAtStart !== mutationVersion) {
          for (const next of nextItems) {
            const current = items.value.find((item) => item.id === next.id)
            if (current) {
              next.num = current.num
            }
          }
        }
        items.value = nextItems
        loaded.value = true
        return items.value
      })
      .finally(() => {
        loading.value = false
        loadTask = null
      })
    return loadTask
  }

  async function ensureLoaded() {
    return load()
  }

  async function add(productId, num = 1) {
    await cartApi.add(productId, num)
    await load({ force: true })
  }

  async function updateNum(id, num) {
    const item = items.value.find((current) => current.id === id)
    if (!item) return
    mutationVersion += 1
    item.num = num

    const previousTask = updateTasks.get(id) || Promise.resolve()
    const task = previousTask
      .catch(() => {})
      .then(() => cartApi.updateNum(id, num))
    updateTasks.set(id, task)
    try {
      await task
    } catch (error) {
      if (updateTasks.get(id) === task) {
        await load({ force: true })
      }
      throw error
    } finally {
      if (updateTasks.get(id) === task) {
        updateTasks.delete(id)
      }
    }
  }

  async function remove(id) {
    await cartApi.remove(id)
    await load({ force: true })
  }

  async function clear() {
    await cartApi.clear()
    items.value = []
    loaded.value = true
  }

  return {
    items,
    loading,
    loaded,
    count,
    load,
    ensureLoaded,
    add,
    updateNum,
    remove,
    clear
  }
})
