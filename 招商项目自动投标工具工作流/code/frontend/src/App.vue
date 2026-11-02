<template>
  <router-view />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

function handleAuthExpired() {
  ElMessage.warning('登录已失效，请重新登录')
  router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

onMounted(() => window.addEventListener('auth-expired', handleAuthExpired))
onUnmounted(() => window.removeEventListener('auth-expired', handleAuthExpired))
</script>
