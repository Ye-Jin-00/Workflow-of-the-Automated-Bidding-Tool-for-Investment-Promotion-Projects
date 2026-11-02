<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">投标项目</h1>
        <p class="page-subtitle">从招商线索到投标归档的全部项目。数据范围由角色与部门决定。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="canCreate" @click="batchVisible = true">
          <el-icon><Upload /></el-icon> 批量导入
        </el-button>
        <el-button v-if="canCreate" type="primary" @click="$router.push('/projects/new')">
          <el-icon><Plus /></el-icon> 新建投标项目
        </el-button>
      </div>
    </div>

    <div v-if="filters.view" class="view-banner">
      <span><el-icon><Filter /></el-icon> 当前视图：{{ viewLabel }}</span>
      <el-button text size="small" type="primary" @click="clearView">查看全部项目</el-button>
    </div>

    <div class="panel filter-panel">
      <div class="filters">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索项目名称 / 编号 / 客户 / 载体"
          clearable
          class="filter-keyword"
          @keyup.enter="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.status" placeholder="全部状态" clearable class="filter-select">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.department" placeholder="全部部门" clearable class="filter-select">
          <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
        </el-select>
        <el-checkbox v-model="filters.following" border @change="search">我关注的</el-checkbox>
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button text title="清空搜索与筛选条件，不是刷新数据" @click="reset">清空筛选</el-button>
      </div>
    </div>

    <div class="panel table-panel">
      <div class="table-head">
        <span class="small muted">
          <template v-if="filters.view === 'todo'">共 {{ total }} 个待处理项目</template>
          <template v-else>共 {{ total }} 个正式项目 · 草稿请到草稿箱查看</template>
        </span>
        <span class="small muted">按最近更新时间排序</span>
      </div>
      <div v-if="selectedRows.length" class="batch-bar">
        <span>已选 {{ selectedRows.length }} 个项目</span>
        <el-button v-if="auth.isAdmin" type="danger" plain size="small" @click="batchDelete">批量删除</el-button>
        <el-button text size="small" @click="clearSelection">取消选择</el-button>
      </div>
      <el-table ref="tableRef" v-loading="loading" :data="items" style="width: 100%" @row-click="openProject"
        @selection-change="onSelectionChange">
        <el-table-column v-if="auth.isAdmin" type="selection" width="44" />
        <el-table-column prop="code" label="项目编号" width="142">
          <template #default="{ row }"><span class="mono code">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="项目名称" min-width="250">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="name">
                {{ row.name }}
                <el-icon v-if="row.following" class="follow-star" color="#c47a14"><StarFilled /></el-icon>
              </span>
              <span class="small muted">
                {{ row.clientName }}<template v-if="row.carrierName"> · {{ row.carrierName }}</template>
                <template v-if="row.attachmentCount || row.commentCount">
                  · {{ row.attachmentCount }} 文件 · {{ row.commentCount }} 评论
                </template>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="122">
          <template #default="{ row }"><StatusTag :label="row.statusLabel" :type="row.statusType" /></template>
        </el-table-column>
        <el-table-column label="负责人" width="116">
          <template #default="{ row }">
            <div class="owner-cell">
              <span>{{ row.ownerName }}</span>
              <span class="small muted">{{ row.department }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预算 / 报价" width="126" align="right">
          <template #default="{ row }">
            <div class="money-cell">
              <span>{{ money(row.budgetAmount) }}</span>
              <small v-if="row.bidAmount">报 {{ money(row.bidAmount) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="截止" width="146">
          <template #default="{ row }">
            <div class="deadline-cell">
              <span class="risk-inline">
                <span class="risk-dot" :style="{ background: riskColor(row.riskLevel) }"></span>
                <span :class="{ danger: row.daysToDeadline >= 0 && row.daysToDeadline <= 3 }">
                  {{ row.status === 'ARCHIVED' ? '已归档' : row.status === 'SUBMITTED' ? '已提交' : daysLabel(row.daysToDeadline) }}
                </span>
              </span>
              <span class="small muted date-sub">{{ row.deadline || '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click.stop="openProject(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-block">
            <el-icon :size="28" color="#c9d1dc"><FolderOpened /></el-icon>
            <p>没有符合条件的项目</p>
            <el-button v-if="canCreate" text type="primary" @click="$router.push('/projects/new')">立即新建</el-button>
          </div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <BatchImportDialog v-model="batchVisible" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { metaApi, projectApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { daysLabel, money, riskColor } from '../utils/format'
import StatusTag from '../components/StatusTag.vue'
import BatchImportDialog from '../components/BatchImportDialog.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const departments = ref([])
const tableRef = ref()
const selectedRows = ref([])
const batchVisible = ref(false)

const filters = reactive({
  keyword: route.query.keyword || '',
  status: '',
  department: '',
  following: false,
  view: route.query.view || ''
})

const viewLabel = computed(() => {
  if (filters.view === 'active') return '推进中的项目'
  if (filters.view === 'todo') return '待我处理的项目'
  return '全部项目'
})

const canCreate = computed(() => ['ADMIN', 'BID_MANAGER'].includes(auth.user?.role))

const statusOptions = [
  { label: '资格预审驳回', value: 'QUALIFICATION_REJECTED' },
  { label: '资格预审通过', value: 'QUALIFICATION_PASSED' },
  { label: '要素已抽取', value: 'REQUIREMENTS_EXTRACTED' },
  { label: '匹配测算完成', value: 'MATCHED' },
  { label: '初稿已生成', value: 'AI_DRAFTED' },
  { label: '待人工复核', value: 'REVIEW_PENDING' },
  { label: '复核退回', value: 'REVIEW_REJECTED' },
  { label: '复核通过', value: 'APPROVED' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已归档', value: 'ARCHIVED' },
  { label: '全部驳回', value: 'REJECTED' },
  { label: '全部完成', value: 'FINISHED' }
]

async function load() {
  loading.value = true
  try {
    const data = await projectApi.list({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      department: filters.department || undefined,
      following: filters.following || undefined,
      view: filters.view || undefined,
      page: page.value,
      size: size.value
    })
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.status = ''
  filters.department = ''
  filters.following = false
  filters.view = ''
  page.value = 1
  if (Object.keys(route.query).length) {
    router.replace({ path: '/projects' })
  } else {
    load()
  }
}

function clearView() {
  filters.view = ''
  page.value = 1
  router.replace({ path: '/projects' })
}

function onSizeChange() {
  page.value = 1
  load()
}

function openProject(row, column) {
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
      `将选中的 ${ids.length} 个项目移入回收站，保留 30 天，期间管理员可恢复。`,
      '批量删除项目',
      { type: 'warning', confirmButtonText: '批量删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const result = await projectApi.batchDelete(ids, '项目列表批量删除')
  if (result.failed) {
    ElMessage.warning(`成功 ${result.success} 个，失败 ${result.failed} 个，请查看后端提示`)
  } else {
    ElMessage.success(`已删除 ${result.success} 个项目，可在回收站恢复`)
  }
  clearSelection()
  load()
}

watch(() => route.query.keyword, (value) => {
  filters.keyword = value || ''
  page.value = 1
  load()
})

watch(() => route.query.view, (value) => {
  filters.view = value || ''
  page.value = 1
  load()
})

onMounted(async () => {
  load()
  try {
    const options = await metaApi.options()
    departments.value = options.departments || []
  } catch (e) {
    departments.value = []
  }
})
</script>

<style scoped>
.filter-panel {
  padding: 16px 18px;
  margin-bottom: 16px;
}

.view-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 14px;
  margin-bottom: 12px;
  border-radius: 9px;
  background: var(--brand-soft);
  color: var(--ink-2);
  font-size: 11px;
}

.view-banner span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-keyword {
  width: 320px;
}

.filter-select {
  width: 160px;
}

.table-panel {
  padding: 12px 18px 18px;
}

.table-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 2px 12px;
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

.code {
  font-size: 11px;
  color: var(--muted);
}

.name-cell,
.owner-cell,
.money-cell,
.deadline-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.name {
  font-weight: 600;
  color: var(--ink);
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.follow-star {
  font-size: 12px;
}

.money-cell {
  align-items: flex-end;
}

.money-cell small {
  font-size: 10px;
  color: var(--muted);
}

.deadline-cell .danger {
  color: var(--danger);
  font-weight: 600;
}

.risk-inline {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
}

.risk-inline .risk-dot {
  flex: 0 0 7px;
  width: 7px;
  height: 7px;
}

.date-sub {
  font-size: 10px;
  white-space: nowrap;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

:deep(.el-table__row) {
  cursor: pointer;
}

@media (max-width: 900px) {
  .filter-keyword,
  .filter-select {
    width: 100%;
  }
}
</style>
