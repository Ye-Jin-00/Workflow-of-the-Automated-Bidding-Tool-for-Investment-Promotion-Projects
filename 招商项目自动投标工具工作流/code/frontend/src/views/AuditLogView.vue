<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">审计日志</h1>
        <p class="page-subtitle">记录项目状态变化、AI 生成、人工复核与规则调整。日志只读，按数据权限过滤。</p>
      </div>
      <div class="page-actions">
        <el-button :loading="exporting" @click="exportCsv">
          <el-icon><Download /></el-icon> 导出 CSV
        </el-button>
      </div>
    </div>

    <div class="panel filter-panel">
      <div class="filters">
        <el-input v-model="filters.keyword" placeholder="搜索操作人 / 项目编号 / 详情" clearable class="keyword" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.action" placeholder="全部动作" clearable class="action-select">
          <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span v-if="projectId" class="project-filter">
          当前仅看项目 <span class="mono">{{ projectId }}</span>
          <el-button text type="primary" size="small" @click="clearProject">清除</el-button>
        </span>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">操作记录 <small>{{ total }} 条</small></div>
        <span class="small muted">时间倒序 · 含追踪号</span>
      </div>
      <el-table v-loading="loading" :data="items" style="width: 100%">
        <el-table-column prop="createdAt" label="时间" width="160" />
        <el-table-column label="项目" width="150">
          <template #default="{ row }">
            <span class="mono code">{{ row.projectCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">
            <div class="actor">
              <span>{{ row.actorName }}</span>
              <small>{{ roleName(row.actorRole) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="动作" width="140">
          <template #default="{ row }">
            <el-tag :type="actionType(row.action)" size="small" effect="light">{{ row.actionLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="360">
          <template #default="{ row }">
            <div class="detail-text" @click="showDetail(row)">{{ row.detail }}</div>
          </template>
        </el-table-column>
        <el-table-column label="IP" width="120" prop="ip" />
        <el-table-column label="追踪号" width="140">
          <template #default="{ row }"><span class="mono trace">{{ row.traceId || '—' }}</span></template>
        </el-table-column>
        <template #empty>
          <div class="empty-block">暂无日志记录</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialog" title="日志详情" width="560px">
      <div v-if="current" class="log-detail">
        <div><small>时间</small><strong>{{ current.createdAt }}</strong></div>
        <div><small>项目</small><strong class="mono">{{ current.projectCode || '—' }}</strong></div>
        <div><small>操作人</small><strong>{{ current.actorName }}（{{ current.actorRole }}）</strong></div>
        <div><small>动作</small><strong>{{ current.actionLabel }}</strong></div>
        <div class="full"><small>详情</small><p>{{ current.detail }}</p></div>
        <div><small>IP</small><strong>{{ current.ip || '—' }}</strong></div>
        <div><small>追踪号</small><strong class="mono">{{ current.traceId || '—' }}</strong></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { auditApi } from '../api'
import { downloadWithAuth } from '../utils/download'
import { roleName } from '../utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const dialog = ref(false)
const current = ref(null)
const exporting = ref(false)
const projectId = ref(route.query.projectId || '')

const filters = ref({
  keyword: '',
  action: ''
})

const actionOptions = [
  { label: '创建项目', value: 'PROJECT_CREATE' },
  { label: '修改项目', value: 'PROJECT_UPDATE' },
  { label: '资格预审', value: 'QUALIFICATION_CHECK' },
  { label: '自动驳回', value: 'AUTO_REJECT' },
  { label: '要素抽取', value: 'EXTRACT' },
  { label: '匹配与报价', value: 'MATCH_PRICE' },
  { label: 'AI 生成初稿', value: 'AI_DRAFT' },
  { label: '编辑初稿', value: 'DRAFT_UPDATE' },
  { label: '提交复核', value: 'SUBMIT_REVIEW' },
  { label: '复核通过', value: 'REVIEW_APPROVE' },
  { label: '复核退回', value: 'REVIEW_RETURN' },
  { label: '提交投标', value: 'SUBMIT' },
  { label: '归档', value: 'ARCHIVE' },
  { label: '规则调整', value: 'RULE_UPDATE' }
]

function actionType(action) {
  if (action?.includes('REJECT') || action?.includes('RETURN')) return 'danger'
  if (action?.includes('APPROVE') || action === 'ARCHIVE' || action === 'SUBMIT') return 'success'
  if (action === 'AI_DRAFT') return 'warning'
  if (action === 'RULE_UPDATE') return 'info'
  return ''
}

async function load() {
  loading.value = true
  try {
    const data = await auditApi.list({
      projectId: projectId.value || undefined,
      action: filters.value.action || undefined,
      keyword: filters.value.keyword || undefined,
      page: page.value,
      size: size.value
    })
    items.value = data.items
    total.value = data.total
  } catch (e) {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filters.value = { keyword: '', action: '' }
  page.value = 1
  load()
}

function clearProject() {
  projectId.value = ''
  router.replace('/audits')
  load()
}

function onSizeChange() {
  page.value = 1
  load()
}

function showDetail(row) {
  current.value = row
  dialog.value = true
}

async function exportCsv() {
  exporting.value = true
  try {
    const params = new URLSearchParams()
    if (projectId.value) params.set('projectId', projectId.value)
    if (filters.value.action) params.set('action', filters.value.action)
    if (filters.value.keyword) params.set('keyword', filters.value.keyword)
    const query = params.toString()
    await downloadWithAuth(`/audits/export${query ? '?' + query : ''}`, 'audit.csv')
    ElMessage.success('CSV 已开始下载')
  } finally {
    exporting.value = false
  }
}

load()
</script>

<style scoped>
.filter-panel {
  padding: 16px 18px;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.keyword {
  width: 300px;
}

.action-select {
  width: 170px;
}

.project-filter {
  margin-left: auto;
  font-size: 11px;
  color: var(--muted);
}

.code {
  font-size: 11px;
  color: var(--muted);
}

.actor {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.actor small {
  font-size: 10px;
  color: var(--muted);
}

.detail-text {
  font-size: 11px;
  line-height: 1.6;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.detail-text:hover {
  color: var(--brand);
}

.trace {
  font-size: 10px;
  color: var(--muted);
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.log-detail {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.log-detail small {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-bottom: 4px;
}

.log-detail strong {
  font-size: 12px;
}

.log-detail .full {
  grid-column: span 2;
}

.log-detail p {
  font-size: 12px;
  line-height: 1.8;
  background: #f8f9fb;
  border-radius: 8px;
  padding: 10px;
  margin: 0;
}
</style>
