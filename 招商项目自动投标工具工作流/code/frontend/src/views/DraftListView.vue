<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">草稿箱</h1>
        <p class="page-subtitle">草稿只保存在这里，不会出现在投标项目中；完成资格预审后自动进入正式项目。</p>
      </div>
      <div class="page-actions">
        <el-button @click="batchVisible = true">
          <el-icon><Upload /></el-icon> 批量导入
        </el-button>
        <el-button type="primary" @click="$router.push('/projects/new')">
          <el-icon><Plus /></el-icon> 新建草稿
        </el-button>
      </div>
    </div>

    <div class="panel filter-panel">
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索草稿名称 / 编号 / 客户" clearable class="keyword" @keyup.enter="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="load">刷新</el-button>
        <span class="small muted">共 {{ total }} 份草稿</span>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <div v-if="selectedRows.length" class="batch-bar">
        <span>已选 {{ selectedRows.length }} 份草稿</span>
        <el-button type="danger" plain size="small" @click="batchDelete">批量删除</el-button>
        <el-button text size="small" @click="clearSelection">取消选择</el-button>
      </div>
      <el-table v-if="items.length" ref="tableRef" :data="items" style="width: 100%"
        @row-click="viewDraft" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="44" />
        <el-table-column prop="code" label="草稿编号" width="130">
          <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="项目名称" min-width="230">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="name">{{ row.name }}</span>
              <span class="small muted">{{ row.clientName }}<template v-if="row.carrierName"> · {{ row.carrierName }}</template></span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预算" width="100" align="right">
          <template #default="{ row }">{{ money(row.budgetAmount) }} 万</template>
        </el-table-column>
        <el-table-column label="截止" width="140">
          <template #default="{ row }">{{ row.deadline || '未设置' }}</template>
        </el-table-column>
        <el-table-column label="负责人" width="90">
          <template #default="{ row }">{{ row.ownerName }}</template>
        </el-table-column>
        <el-table-column label="最近更新" width="150" prop="updatedAt" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" size="small" @click.stop="editDraft(row)">编辑</el-button>
              <el-button link type="primary" size="small" @click.stop="precheck(row)">预审</el-button>
              <el-button v-if="canDelete(row)" link type="danger" size="small" @click.stop="removeDraft(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-else-if="!loading" class="empty-block">
        <el-icon :size="34" color="#c9d1dc"><EditPen /></el-icon>
        <p>草稿箱是空的</p>
        <span class="small muted">新建项目时选择“保存草稿”即可暂存，之后再完善。</span>
        <el-button type="primary" plain @click="$router.push('/projects/new')">新建草稿</el-button>
      </div>

      <div class="pager" v-if="total > size">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </div>

    <BatchImportDialog v-model="batchVisible" @imported="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { money } from '../utils/format'
import BatchImportDialog from '../components/BatchImportDialog.vue'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const keyword = ref('')
const page = ref(1)
const size = 10
const tableRef = ref()
const selectedRows = ref([])
const batchVisible = ref(false)

function canDelete(row) {
  return auth.isAdmin || row.ownerId === auth.user?.id
}

async function load() {
  loading.value = true
  try {
    const data = await projectApi.list({
      status: 'DRAFT',
      keyword: keyword.value || undefined,
      page: page.value,
      size
    })
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function editDraft(row) {
  router.push({ path: `/projects/${row.id}/edit`, query: { from: 'drafts' } })
}

function precheck(row) {
  router.push(`/projects/${row.id}`)
}

function viewDraft(row, column) {
  if (column?.type === 'selection') return
  router.push(`/projects/${row.id}`)
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

async function batchDelete() {
  const ids = selectedRows.value.map((row) => row.id)
  try {
    await ElMessageBox.confirm(
      `将选中的 ${ids.length} 份草稿移入回收站，管理员可在 30 天内恢复。`,
      '批量删除草稿',
      { type: 'warning', confirmButtonText: '批量删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const result = await projectApi.batchDelete(ids, '草稿箱批量删除')
  if (result.failed) {
    ElMessage.warning(`成功 ${result.success} 份，失败 ${result.failed} 份，请查看提示`)
  } else {
    ElMessage.success(`已删除 ${result.success} 份草稿`)
  }
  clearSelection()
  load()
}

async function removeDraft(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除草稿「${row.name}」吗？删除后会进入回收站，管理员可在 30 天内恢复。`,
      '删除草稿',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await projectApi.deleteProject(row.id, '草稿箱删除')
  ElMessage.success('草稿已移入回收站')
  load()
}

onMounted(load)
</script>

<style scoped>
.filter-panel {
  padding: 14px 18px;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.keyword {
  width: 320px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.name-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.name {
  font-weight: 600;
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

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

:deep(.el-table__row) {
  cursor: pointer;
}

@media (max-width: 800px) {
  .keyword {
    width: 100%;
  }
}
</style>
