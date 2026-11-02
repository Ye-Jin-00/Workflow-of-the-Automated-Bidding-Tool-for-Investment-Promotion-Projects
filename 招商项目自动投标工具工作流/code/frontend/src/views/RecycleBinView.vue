<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">回收站</h1>
        <p class="page-subtitle">删除的项目进入回收站，保留 30 天后由系统自动彻底清理；管理员可随时恢复。</p>
      </div>
      <div class="page-actions">
        <el-input v-model="keyword" placeholder="搜索项目 / 编号 / 删除人" clearable class="keyword">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="mb-16"
      title="回收站内容最多保留 30 天。彻底删除会同时清理项目附件，且不可恢复。" />

    <div class="panel" v-loading="loading">
      <div v-if="selectedRows.length" class="batch-bar">
        <span>已选 {{ selectedRows.length }} 个项目</span>
        <el-button type="primary" plain size="small" @click="batchRestore">批量恢复</el-button>
        <el-button type="danger" plain size="small" @click="batchPurge">批量彻底删除</el-button>
        <el-button text size="small" @click="clearSelection">取消选择</el-button>
      </div>
      <el-table v-if="filteredItems.length" ref="tableRef" :data="filteredItems" style="width: 100%"
        @selection-change="onSelectionChange">
        <el-table-column type="selection" width="44" />
        <el-table-column prop="code" label="编号" width="130">
          <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="项目" min-width="220">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="name">{{ row.name }}</span>
              <span class="small muted">{{ row.clientName }} · {{ row.ownerName }} / {{ row.department }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="原状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="删除信息" width="170">
          <template #default="{ row }">
            <div class="meta-cell">
              <span>{{ row.deletedBy }}</span>
              <small class="muted">{{ row.deletedAt }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="删除原因" min-width="150" show-overflow-tooltip prop="deleteReason" />
        <el-table-column label="剩余保留" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.remainingDays <= 3 ? 'danger' : 'warning'" size="small" effect="light">
              {{ row.remainingDays }} 天
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="restore(row)">恢复</el-button>
            <el-button text type="danger" size="small" @click="purge(row)">彻底删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-else-if="!loading" class="empty-block">
        <el-icon :size="34" color="#c9d1dc"><Delete /></el-icon>
        <p>回收站是空的</p>
        <span class="small muted">在项目详情或草稿箱删除的项目会出现在这里。</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { recycleApi } from '../api'

const router = useRouter()
const loading = ref(false)
const items = ref([])
const keyword = ref('')
const tableRef = ref()
const selectedRows = ref([])

const filteredItems = computed(() => {
  const value = keyword.value.trim()
  if (!value) return items.value
  return items.value.filter((item) =>
    [item.name, item.code, item.clientName, item.deletedBy]
      .filter(Boolean).some((text) => text.includes(value))
  )
})

async function load() {
  loading.value = true
  try {
    items.value = await recycleApi.list()
  } finally {
    loading.value = false
  }
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

async function batchRestore() {
  const ids = selectedRows.value.map((row) => row.id)
  try {
    await ElMessageBox.confirm(`确定恢复选中的 ${ids.length} 个项目吗？`, '批量恢复', {
      type: 'info', confirmButtonText: '批量恢复'
    })
  } catch (e) {
    return
  }
  const result = await recycleApi.batchRestore(ids)
  ElMessage.success(`已恢复 ${result.success} 个项目${result.failed ? `，${result.failed} 个失败` : ''}`)
  clearSelection()
  load()
}

async function batchPurge() {
  const ids = selectedRows.value.map((row) => row.id)
  try {
    await ElMessageBox.confirm(
      `彻底删除 ${ids.length} 个项目后不可恢复，附件也会被清理。`,
      '批量彻底删除',
      { type: 'error', confirmButtonText: '确认彻底删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const result = await recycleApi.batchPurge(ids)
  ElMessage.success(`已彻底删除 ${result.success} 个项目${result.failed ? `，${result.failed} 个失败` : ''}`)
  clearSelection()
  load()
}

async function restore(row) {
  try {
    await ElMessageBox.confirm(
      `将「${row.name}」恢复到删除前的“${row.statusLabel}”状态，是否继续？`,
      '恢复项目',
      { type: 'info', confirmButtonText: '恢复项目' }
    )
  } catch (e) {
    return
  }
  await recycleApi.restore(row.id)
  ElMessage.success('项目已恢复')
  load()
}

async function purge(row) {
  try {
    await ElMessageBox.confirm(
      `彻底删除后不可恢复，项目「${row.name}」的附件也会被清理。此操作不可撤销。`,
      '彻底删除',
      { type: 'error', confirmButtonText: '确认彻底删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await recycleApi.purge(row.id)
  ElMessage.success('项目已彻底删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.keyword {
  width: 260px;
}

.mb-16 {
  margin-bottom: 16px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: var(--brand-soft);
  font-size: 11px;
  color: var(--ink-2);
}

.name-cell,
.meta-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.name {
  font-weight: 600;
}

.meta-cell small {
  font-size: 10px;
}

@media (max-width: 800px) {
  .keyword {
    width: 100%;
  }
}
</style>
