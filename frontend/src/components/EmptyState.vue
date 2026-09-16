<template>
  <div class="empty-state">
    <PackageOpen :size="34" />
    <div class="empty-title">{{ title }}</div>
    <div class="empty-copy">{{ description }}</div>
  </div>
</template>

<script setup>
import { PackageOpen } from 'lucide-vue-next'

defineProps({
  title: { type: String, default: '暂无内容' },
  description: { type: String, default: '换个条件再试试' }
})
</script>

<style scoped>
.empty-state {
  display: grid;
  place-items: center;
  gap: 8px;
  padding: 44px 20px;
  color: var(--muted);
}

.empty-title {
  color: var(--text);
  font-weight: 700;
}

.empty-copy {
  font-size: 13px;
}
</style>
