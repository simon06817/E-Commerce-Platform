<template>
  <article class="product-card" @click="openDetail">
    <div class="product-image">
      <img v-if="product.mainImage" :src="product.mainImage" :alt="product.name" />
      <PackageOpen v-else :size="42" />
    </div>
    <div class="product-body">
      <div class="product-name">{{ product.name }}</div>
      <div class="seller-name">{{ product.sellerName || '平台商家' }}</div>
      <div class="product-meta">
        <span class="price">{{ formatPrice(product.price) }}</span>
        <span class="stock">库存 {{ product.stock ?? 0 }}</span>
      </div>
      <el-button
        class="add-button"
        type="primary"
        :icon="ShoppingBag"
        :disabled="product.stock <= 0"
        @click.stop="$emit('add', product)"
      >
        加入购物车
      </el-button>
    </div>
  </article>
</template>

<script setup>
import { PackageOpen, ShoppingBag } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { formatPrice } from '../utils/format'

const props = defineProps({
  product: { type: Object, required: true }
})

defineEmits(['add'])

const router = useRouter()

function openDetail() {
  router.push(`/product/${props.product.id}`)
}
</script>

<style scoped>
.product-card {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: 0 10px 30px rgba(127, 74, 42, 0.07);
  transition:
    transform 160ms ease,
    box-shadow 160ms ease;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 36px rgba(127, 74, 42, 0.12);
}

.product-image {
  display: grid;
  aspect-ratio: 1 / 1;
  place-items: center;
  overflow: hidden;
  color: #d68a66;
  background: var(--surface-strong);
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-body {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.product-name {
  overflow: hidden;
  color: var(--text);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seller-name {
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.stock {
  color: var(--muted);
  font-size: 12px;
}

.add-button {
  width: 100%;
}
</style>
