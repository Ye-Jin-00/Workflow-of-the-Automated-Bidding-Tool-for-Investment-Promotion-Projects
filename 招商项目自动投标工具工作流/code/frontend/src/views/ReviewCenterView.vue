<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">复核中心</h1>
        <p class="page-subtitle">待人工复核的投标项目。复核人须回避本人负责的项目，通过后进入提交环节。</p>
      </div>
      <div class="page-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索项目名称 / 编号"
          clearable
          class="search-input"
          @keyup.enter="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="search">查询</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">待复核 <small>{{ total }} 项</small></div>
        <span class="small muted">按最近更新时间排序</span>
      </div>

      <el-table v-loading="loading" :data="items" style="width: 100%" @row-click="openReview">
        <el-table-column prop="code" label="项目编号" width="150">
          <template #default="{ row }"><span class="mono code">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="项目名称" min-width="300">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="name">{{ row.name }}</span>
              <span class="small muted">{{ row.clientName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="130">
          <template #default="{ row }">
            <div class="owner-cell">
              <span>{{ row.ownerName }}</span>
              <span class="small muted">{{ row.department }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预算" width="120" align="right">
          <template #default="{ row }">{{ money(row.budgetAmount) }} 万</template>
        </el-table-column>
        <el-table-column label="截止" width="150">
          <template #default="{ row }">
            <div class="deadline-cell">
              <span :class="{ danger: row.daysToDeadline >= 0 && row.daysToDeadline <= 3 }">
                {{ daysLabel(row.daysToDeadline) }}
              </span>
              <span class="small muted">{{ row.deadline }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="复核轮次" width="90" align="center">
          <template #default="{ row }">{{ row.reviewRound }} 轮</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click.stop="openReview(row)">开始复核</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-block">
            <el-icon :size="30" color="#c9d1dc"><CircleCheck /></el-icon>
            <p>当前没有待复核项目</p>
          </div>
        </template>
      </el-table>

      <div class="pager">
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

    <el-drawer v-model="drawer" title="人工复核" size="640px" destroy-on-close>
      <div v-if="detail" class="drawer-body" v-loading="detailLoading">
        <div class="drawer-title">{{ detail.name }}</div>
        <div class="drawer-meta">
          <span class="mono">{{ detail.code }}</span>
          <StatusTag :label="detail.statusLabel" :type="detail.statusType" />
          <span class="muted small">{{ detail.ownerName }} · {{ detail.department }}</span>
        </div>

        <div class="kpi-grid">
          <div><small>预算</small><strong>{{ money(detail.budgetAmount) }} 万</strong></div>
          <div><small>建议报价</small><strong>{{ money(detail.bidAmount) }} 万</strong></div>
          <div><small>预审得分</small><strong>{{ detail.qualificationResult?.score ?? '—' }}</strong></div>
          <div><small>匹配得分</small><strong>{{ detail.matchReport?.score ?? '—' }}</strong></div>
        </div>

        <div class="section">
          <div class="section-title">技术标摘要</div>
          <p class="preview">{{ shortText(detail.aiDraft?.technicalProposal, 600) || '暂无初稿内容' }}</p>
        </div>

        <div class="section">
          <div class="section-title">风险提示</div>
          <ul class="risk-list">
            <li v-for="risk in detail.risks" :key="risk">{{ risk }}</li>
            <li v-if="!detail.risks?.length" class="muted">未发现明显风险</li>
          </ul>
        </div>

        <div class="section">
          <div class="section-title">复核结论</div>
          <el-radio-group v-model="decision" class="decision-group">
            <el-radio-button label="APPROVE">复核通过</el-radio-button>
            <el-radio-button label="RETURN">退回修改</el-radio-button>
          </el-radio-group>
          <el-input
            v-model="opinion"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :placeholder="decision === 'RETURN' ? '请写明修改意见（不少于 5 个字）' : '复核意见（可选）'"
          />
        </div>

        <div class="drawer-actions">
          <el-button text @click="$router.push(`/projects/${detail.id}`)">查看完整项目</el-button>
          <el-button type="primary" :loading="submitting" @click="submitReview">
            {{ decision === 'RETURN' ? '确认退回' : '确认通过' }}
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { projectApi, workflowApi } from '../api'
import { money, shortText, daysLabel } from '../utils/format'
import StatusTag from '../components/StatusTag.vue'

const loading = ref(false)
const detailLoading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const drawer = ref(false)
const detail = ref(null)
const submitting = ref(false)
const decision = ref('APPROVE')
const opinion = ref('')

async function load() {
  loading.value = true
  try {
    const data = await projectApi.list({
      status: 'REVIEW_PENDING',
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

function search() {
  page.value = 1
  load()
}

async function openReview(row) {
  drawer.value = true
  detailLoading.value = true
  detail.value = null
  decision.value = 'APPROVE'
  opinion.value = ''
  try {
    detail.value = await projectApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function submitReview() {
  if (!detail.value) return
  if (decision.value === 'RETURN' && opinion.value.trim().length < 5) {
    ElMessage.warning('退回时必须填写不少于 5 个字的修改意见')
    return
  }
  submitting.value = true
  try {
    await workflowApi.review(detail.value.id, {
      decision: decision.value,
      opinion: opinion.value.trim(),
      version: detail.value.version
    })
    ElMessage.success(decision.value === 'APPROVE' ? '复核通过' : '已退回负责人修改')
    drawer.value = false
    load()
  } finally {
    submitting.value = false
  }
}

load()
</script>

<style scoped>
.search-input {
  width: 260px;
}

.name-cell,
.owner-cell,
.deadline-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.name {
  font-weight: 600;
}

.code {
  font-size: 11px;
  color: var(--muted);
}

.deadline-cell .danger {
  color: var(--danger);
  font-weight: 600;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

:deep(.el-table__row) {
  cursor: pointer;
}

.drawer-title {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.5;
  margin-bottom: 8px;
}

.drawer-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 18px;
}

.kpi-grid > div {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 10px;
}

.kpi-grid small {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-bottom: 4px;
}

.kpi-grid strong {
  font-size: 12px;
}

.section {
  margin-bottom: 18px;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}

.preview {
  font-size: 12px;
  line-height: 1.9;
  color: var(--ink-2);
  background: #fbfcfd;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  padding: 12px;
  white-space: pre-wrap;
}

.risk-list {
  margin: 0;
  padding-left: 16px;
  font-size: 11px;
  line-height: 1.9;
  color: var(--ink-2);
}

.decision-group {
  margin-bottom: 10px;
}

.drawer-actions {
  display: flex;
  justify-content: space-between;
  border-top: 1px solid var(--line);
  padding: 14px 0 4px;
  position: sticky;
  bottom: 0;
  background: #fff;
  box-shadow: 0 -8px 16px rgba(255, 255, 255, 0.9);
}
</style>
