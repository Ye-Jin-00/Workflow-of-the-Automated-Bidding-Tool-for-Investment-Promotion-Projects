<template>
  <div class="user-avatar" :style="style" :title="name">
    <img v-if="src && !failed" :src="src" :alt="name" @error="failed = true" />
    <span v-else>{{ initial }}</span>
    <i v-if="showStatus" class="status-dot"></i>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  name: { type: String, default: '用户' },
  src: { type: String, default: '' },
  size: { type: Number, default: 34 },
  shape: { type: String, default: 'square' },
  showStatus: { type: Boolean, default: false }
})

const failed = ref(false)
watch(() => props.src, () => { failed.value = false })

const initial = computed(() => (props.name || '用').trim().slice(0, 1))
const palettes = [
  ['#B4232A', '#7F141A'],
  ['#2E6F9E', '#1D4E73'],
  ['#2E8B57', '#1F6B42'],
  ['#8A5CB8', '#65408C'],
  ['#C47A14', '#9A5E0D'],
  ['#3A6EA5', '#2A527D']
]
const palette = computed(() => {
  let sum = 0
  for (const ch of (props.name || '用户')) sum += ch.charCodeAt(0)
  return palettes[sum % palettes.length]
})

const style = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  borderRadius: props.shape === 'circle' ? '50%' : `${Math.round(props.size * 0.28)}px`,
  background: `linear-gradient(145deg, ${palette.value[0]}, ${palette.value[1]})`,
  fontSize: `${Math.max(12, Math.round(props.size * 0.4))}px`
}))
</script>

<style scoped>
.user-avatar {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.5px;
  flex: 0 0 auto;
  user-select: none;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.status-dot {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #2e8b57;
  border: 2px solid #fff;
}
</style>
