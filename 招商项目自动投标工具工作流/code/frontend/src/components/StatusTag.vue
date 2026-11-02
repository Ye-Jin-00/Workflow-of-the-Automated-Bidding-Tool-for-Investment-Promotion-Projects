<template>
  <el-tag :type="type || 'info'" effect="light" size="small" class="status-tag">
    {{ label || '未知状态' }}
  </el-tag>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  type: { type: String, default: 'info' }
})
</script>

<style scoped>
.status-tag {
  font-weight: 600;
  letter-spacing: 0.3px;
}
</style>
