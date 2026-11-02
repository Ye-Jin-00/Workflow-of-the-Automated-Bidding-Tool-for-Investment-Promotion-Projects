<template>
  <el-dialog
    :model-value="modelValue"
    title="调整头像"
    width="420px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
    @closed="cleanup"
  >
    <div class="crop-hint">拖动图片调整位置，使用滑块或滚轮缩放；圆形区域即为最终头像。</div>
    <div class="crop-shell">
      <canvas
        ref="canvas"
        class="crop-canvas"
        width="600"
        height="600"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointercancel="onPointerUp"
        @wheel.prevent="onWheel"
      ></canvas>
      <div class="crop-ring"></div>
    </div>
    <div class="zoom-row">
      <el-icon><ZoomOut /></el-icon>
      <el-slider v-model="zoom" :min="1" :max="3" :step="0.01" :show-tooltip="false" @input="draw" />
      <el-icon><ZoomIn /></el-icon>
      <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
    </div>
    <div class="crop-tip">
      <el-icon><InfoFilled /></el-icon>
      输出为 512 × 512 的 PNG 圆形头像，支持 JPG / PNG / WEBP / GIF。
    </div>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" @click="confirm">使用这张头像</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  file: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const canvas = ref()
const zoom = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
let image = null
let objectUrl = ''
let dragging = false
let startX = 0
let startY = 0
let startOffsetX = 0
let startOffsetY = 0

watch(
  () => [props.modelValue, props.file],
  async ([visible, file]) => {
    if (!visible || !file) return
    await nextTick()
    loadImage(file)
  },
  { immediate: true }
)

function loadImage(file) {
  cleanup()
  objectUrl = URL.createObjectURL(file)
  const img = new Image()
  img.onload = () => {
    image = img
    zoom.value = 1
    offsetX.value = 0
    offsetY.value = 0
    draw()
  }
  img.onerror = () => ElMessage.error('图片读取失败，请重新选择')
  img.src = objectUrl
}

function draw() {
  if (!image || !canvas.value) return
  const size = canvas.value.width
  const ctx = canvas.value.getContext('2d')
  render(ctx, size, 1)
}

function render(ctx, size, offsetScale) {
  const scale = Math.max(size / image.width, size / image.height) * zoom.value
  const drawWidth = image.width * scale
  const drawHeight = image.height * scale
  const maxX = Math.max(0, (drawWidth - size) / 2)
  const maxY = Math.max(0, (drawHeight - size) / 2)
  const px = Math.max(-maxX, Math.min(maxX, offsetX.value * offsetScale))
  const py = Math.max(-maxY, Math.min(maxY, offsetY.value * offsetScale))
  const x = size / 2 - drawWidth / 2 + px
  const y = size / 2 - drawHeight / 2 + py

  ctx.clearRect(0, 0, size, size)
  ctx.save()
  ctx.beginPath()
  ctx.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2)
  ctx.clip()
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, size, size)
  ctx.drawImage(image, x, y, drawWidth, drawHeight)
  ctx.restore()
}

function clampOffsets() {
  if (!image || !canvas.value) return
  const size = canvas.value.width
  const scale = Math.max(size / image.width, size / image.height) * zoom.value
  const maxX = Math.max(0, (image.width * scale - size) / 2)
  const maxY = Math.max(0, (image.height * scale - size) / 2)
  offsetX.value = Math.max(-maxX, Math.min(maxX, offsetX.value))
  offsetY.value = Math.max(-maxY, Math.min(maxY, offsetY.value))
}

function onPointerDown(event) {
  if (!image) return
  dragging = true
  startX = event.clientX
  startY = event.clientY
  startOffsetX = offsetX.value
  startOffsetY = offsetY.value
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

function onPointerMove(event) {
  if (!dragging || !canvas.value) return
  const rect = canvas.value.getBoundingClientRect()
  const ratio = canvas.value.width / rect.width
  offsetX.value = startOffsetX + (event.clientX - startX) * ratio
  offsetY.value = startOffsetY + (event.clientY - startY) * ratio
  clampOffsets()
  draw()
}

function onPointerUp() {
  dragging = false
}

function onWheel(event) {
  zoom.value = Math.max(1, Math.min(3, zoom.value + (event.deltaY < 0 ? 0.06 : -0.06)))
  clampOffsets()
  draw()
}

function confirm() {
  if (!image) return
  const output = document.createElement('canvas')
  output.width = 512
  output.height = 512
  const ctx = output.getContext('2d')
  render(ctx, 512, 512 / canvas.value.width)
  output.toBlob((blob) => {
    if (!blob) {
      ElMessage.error('头像生成失败，请重试')
      return
    }
    emit('confirm', blob)
    close()
  }, 'image/png')
}

function close() {
  emit('update:modelValue', false)
}

function cleanup() {
  dragging = false
  image = null
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl)
    objectUrl = ''
  }
}
</script>

<style scoped>
.crop-hint {
  font-size: 10px;
  color: var(--muted);
  line-height: 1.6;
  margin-bottom: 12px;
}

.crop-shell {
  position: relative;
  width: 300px;
  height: 300px;
  margin: 0 auto;
  border-radius: 50%;
  overflow: hidden;
  background: #eef1f4;
  cursor: grab;
  touch-action: none;
}

.crop-shell:active {
  cursor: grabbing;
}

.crop-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

.crop-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.95);
  pointer-events: none;
}

.zoom-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 16px 6px 8px;
  color: var(--muted);
}

.zoom-row :deep(.el-slider) {
  flex: 1;
}

.zoom-value {
  width: 42px;
  text-align: right;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.crop-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  color: var(--muted);
  background: #f7f8fa;
  border-radius: 7px;
  padding: 8px 10px;
}
</style>
