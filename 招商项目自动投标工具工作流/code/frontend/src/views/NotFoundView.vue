<template>
  <div class="not-found">
    <div class="code">404</div>
    <h1>页面不存在或已被移动</h1>
    <p>请检查地址，或返回工作台继续处理投标项目。</p>
    <div class="actions">
      <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
      <el-button @click="$router.push('/projects')">投标项目</el-button>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: var(--bg);
  text-align: center;
  padding: 24px;
}

.code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  color: var(--brand);
  letter-spacing: 4px;
}

h1 {
  font-size: 22px;
}

p {
  color: var(--muted);
  font-size: 13px;
}

.actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}
</style>
