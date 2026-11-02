<template>
  <div class="page detail-page" v-loading="loading">
    <template v-if="project">
      <div class="back-line">
        <el-button text @click="$router.push('/projects')"><el-icon><ArrowLeft /></el-icon> 投标项目</el-button>
        <span class="small muted">/ {{ project.code }}</span>
      </div>

      <div class="header-card">
        <div class="header-main">
          <div class="title-row">
            <h1 class="project-title">{{ project.name }}</h1>
            <StatusTag :label="project.statusLabel" :type="project.statusType" />
            <span class="risk-chip" :class="project.risks?.length ? 'has-risk' : ''">
              <span class="risk-dot" :style="{ background: riskColor(project.daysToDeadline >= 0 && project.daysToDeadline <= 3 ? 'HIGH' : project.risks?.length ? 'MEDIUM' : 'LOW') }"></span>
              {{ deadlineChipText }}
            </span>
          </div>
          <div class="meta-row">
            <span><el-icon><OfficeBuilding /></el-icon>{{ project.clientName }}</span>
            <span><el-icon><Location /></el-icon>{{ project.carrierName || '未填写载体' }}</span>
            <span><el-icon><CollectionTag /></el-icon>{{ project.industry || '未分类' }}</span>
            <span><el-icon><User /></el-icon>{{ project.ownerName }} · {{ project.department }}</span>
            <span><el-icon><Clock /></el-icon>截止 {{ project.deadline || '未设置' }}</span>
          </div>
        </div>
        <div class="header-actions">
          <el-button :type="project.following ? 'warning' : 'default'" plain @click="toggleFollow">
            <el-icon><StarFilled v-if="project.following" /><Star v-else /></el-icon>
            {{ project.following ? '已关注' : '关注' }}
          </el-button>
          <el-dropdown trigger="click" @command="handleMore">
            <el-button circle title="更多操作">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="copyCode">复制项目编号</el-dropdown-item>
                <el-dropdown-item command="duplicate" :disabled="!project.permissions?.operate">复制为新项目</el-dropdown-item>
                <el-dropdown-item command="audit" divided>查看操作日志</el-dropdown-item>
                <el-dropdown-item v-if="canDeleteProject" command="delete">删除{{ project.status === 'DRAFT' ? '草稿' : '项目' }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button text circle title="刷新" @click="load">
            <el-icon><Refresh /></el-icon>
          </el-button>
          <el-button
            v-for="action in visibleActions"
            :key="action"
            :type="action === primaryAction ? 'primary' : 'default'"
            @click="handleAction(action)"
          >
            {{ action === 'EDIT' ? '编辑信息' : actionText(action) }}
          </el-button>
        </div>
      </div>

      <div class="summary-strip">
        <div class="summary-item">
          <small>招标预算</small>
          <strong>{{ money(project.budgetAmount) }}<i>万元</i></strong>
        </div>
        <div class="summary-item">
          <small>建议报价</small>
          <strong>{{ project.bidAmount ? money(project.bidAmount) : '—' }}<i v-if="project.bidAmount">万元</i></strong>
        </div>
        <div class="summary-item">
          <small>资格预审</small>
          <strong>{{ project.qualificationResult ? `${project.qualificationResult.score} 分` : '未预审' }}</strong>
        </div>
        <div class="summary-item">
          <small>匹配得分</small>
          <strong>{{ project.matchReport ? `${project.matchReport.score} 分` : '未评估' }}</strong>
        </div>
        <div class="summary-item">
          <small>复核轮次</small>
          <strong>{{ project.reviewRound }}<i>轮</i></strong>
        </div>
        <div class="summary-item">
          <small>数据版本</small>
          <strong>v{{ project.version }}</strong>
        </div>
      </div>

      <div class="panel step-panel">
        <el-steps :active="stepActive" finish-status="success" :process-status="processStatus" align-center>
          <el-step
            v-for="step in steps"
            :key="step.title"
            :title="step.title"
            :description="step.description"
          />
        </el-steps>
      </div>

      <div class="panel tab-panel">
        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane name="overview">
            <template #label>
              <span class="tab-label">概览</span>
            </template>
            <OverviewPanel :project="project" @go="activeTab = $event" />
          </el-tab-pane>
          <el-tab-pane name="qualification">
            <template #label>
              <span class="tab-label">资格预审
                <span v-if="project.qualificationResult" class="tab-count">{{ project.qualificationResult.score }}分</span>
              </span>
            </template>
            <QualificationPanel :project="project" @updated="handleUpdated" />
          </el-tab-pane>
          <el-tab-pane name="extraction">
            <template #label>
              <span class="tab-label">要素抽取
                <el-badge v-if="project.extraction" :value="project.extraction.items.length" type="info" class="tab-badge" />
              </span>
            </template>
            <ExtractionPanel :project="project" @updated="handleUpdated" />
          </el-tab-pane>
          <el-tab-pane label="匹配测算" name="pricing">
            <PricingPanel :project="project" @updated="handleUpdated" />
          </el-tab-pane>
          <el-tab-pane name="draft">
            <template #label>
              <span class="tab-label">标书初稿
                <el-badge v-if="project.aiDraft" value="AI" type="warning" class="tab-badge" />
              </span>
            </template>
            <DraftPanel :project="project" @updated="handleUpdated" />
          </el-tab-pane>
          <el-tab-pane name="review">
            <template #label>
              <span class="tab-label">人工复核
                <el-badge v-if="project.reviewRound" :value="project.reviewRound" type="danger" class="tab-badge" />
              </span>
            </template>
            <ReviewPanel :project="project" @updated="handleUpdated" />
          </el-tab-pane>
          <el-tab-pane name="collaboration">
            <template #label>
              <span class="tab-label">协作
                <el-badge v-if="collabCount" :value="collabCount" type="info" class="tab-badge" />
              </span>
            </template>
            <CollaborationPanel :project="project" @updated="handleUpdated" />
          </el-tab-pane>
          <el-tab-pane label="提交归档" name="submit">
            <SubmitPanel :project="project" @updated="handleUpdated" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>

    <el-result v-else-if="!loading" icon="warning" title="项目不存在或无权查看">
      <template #extra>
        <el-button type="primary" @click="$router.push('/projects')">返回项目列表</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { collaborationApi, projectApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { actionLabel, daysLabel, money, riskColor } from '../utils/format'
import StatusTag from '../components/StatusTag.vue'
import OverviewPanel from '../components/workflow/OverviewPanel.vue'
import QualificationPanel from '../components/workflow/QualificationPanel.vue'
import ExtractionPanel from '../components/workflow/ExtractionPanel.vue'
import PricingPanel from '../components/workflow/PricingPanel.vue'
import DraftPanel from '../components/workflow/DraftPanel.vue'
import ReviewPanel from '../components/workflow/ReviewPanel.vue'
import SubmitPanel from '../components/workflow/SubmitPanel.vue'
import CollaborationPanel from '../components/workflow/CollaborationPanel.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const project = ref(null)
const activeTab = ref('overview')

const steps = [
  { title: '项目录入', description: '基础信息' },
  { title: '资格预审', description: '规则判定' },
  { title: '要素抽取', description: '招标要求' },
  { title: '匹配测算', description: '能力与报价' },
  { title: '材料生成', description: 'AI 初稿' },
  { title: '人工复核', description: '职责分离' },
  { title: '提交归档', description: '回执留痕' }
]

const actionOrder = [
  'QUALIFICATION_CHECK',
  'EXTRACT',
  'MATCH_PRICE',
  'AI_DRAFT',
  'SUBMIT_REVIEW',
  'REVIEW',
  'SUBMIT',
  'ARCHIVE',
  'EDIT'
]

const visibleActions = computed(() => {
  const allowed = project.value?.allowedActions || []
  return actionOrder.filter((action) => allowed.includes(action)).slice(0, 4)
})

const primaryAction = computed(() => visibleActions.value.find((action) => action !== 'EDIT') || visibleActions.value[0])

const collabCount = computed(() => (project.value?.attachmentCount || 0) + (project.value?.commentCount || 0))

const canDeleteProject = computed(() => {
  const detail = project.value
  if (!detail) return false
  return auth.isAdmin || (detail.status === 'DRAFT' && detail.ownerId === auth.user?.id)
})

const processStatus = computed(() =>
  ['QUALIFICATION_REJECTED', 'REVIEW_REJECTED'].includes(project.value?.status) ? 'error' : 'process'
)

// el-steps 的 active 是 0 基索引：归档完成时取 7 以标记全部完成。
const stepActive = computed(() => {
  const detail = project.value
  if (!detail) return 0
  return detail.status === 'ARCHIVED' ? 7 : Math.max(0, detail.stage - 1)
})

const deadlineChipText = computed(() => {
  const detail = project.value
  if (!detail) return ''
  if (detail.status === 'ARCHIVED') return '已归档'
  if (detail.status === 'SUBMITTED') return '已提交'
  return daysLabel(detail.daysToDeadline)
})

function actionText(action) {
  return actionLabel(action)
}

function handleAction(action) {
  if (action === 'EDIT') {
    router.push(`/projects/${project.value.id}/edit`)
    return
  }
  const map = {
    QUALIFICATION_CHECK: 'qualification',
    EXTRACT: 'extraction',
    MATCH_PRICE: 'pricing',
    AI_DRAFT: 'draft',
    SUBMIT_REVIEW: 'review',
    REVIEW: 'review',
    SUBMIT: 'submit',
    ARCHIVE: 'submit'
  }
  activeTab.value = map[action] || 'overview'
}

function defaultTab(detail) {
  if (['REVIEW_PENDING', 'REVIEW_REJECTED'].includes(detail.status)) return 'review'
  if (detail.status === 'QUALIFICATION_REJECTED') return 'qualification'
  if (detail.status === 'DRAFT') return 'qualification'
  if (detail.status === 'ARCHIVED') return 'submit'
  return 'overview'
}

async function toggleFollow() {
  const data = await collaborationApi.toggleFollow(project.value.id)
  project.value.following = data.following
  ElMessage.success(data.following ? '已关注项目，状态与截止提醒会进入消息中心' : '已取消关注')
}

async function handleMore(command) {
  if (command === 'copyCode') {
    try {
      await navigator.clipboard.writeText(project.value.code)
      ElMessage.success('项目编号已复制')
    } catch (e) {
      ElMessage.warning('浏览器不支持自动复制')
    }
  } else if (command === 'audit') {
    router.push({ path: '/audits', query: { projectId: project.value.id } })
  } else if (command === 'delete') {
    try {
      const result = await ElMessageBox.prompt(
        '项目将进入回收站保留 30 天，管理员可恢复。请填写删除原因（可选）：',
        project.value.status === 'DRAFT' ? '删除草稿' : '删除项目',
        {
          confirmButtonText: '移入回收站',
          cancelButtonText: '取消',
          inputPlaceholder: '例如：重复项目 / 客户取消 / 信息有误',
          inputValidator: (value) => !value || value.length <= 100 || '删除原因不能超过 100 字'
        }
      )
      await projectApi.deleteProject(project.value.id, result.value || '')
      ElMessage.success('项目已移入回收站')
      router.replace(project.value.status === 'DRAFT' ? '/drafts' : '/projects')
    } catch (e) {
      // 用户取消或接口错误由拦截器提示
    }
  } else if (command === 'duplicate') {
    try {
      await ElMessageBox.confirm(
        '将复制项目基础信息与资格画像，生成一个新的草稿项目。',
        '复制为新项目',
        { confirmButtonText: '复制项目', cancelButtonText: '取消' }
      )
    } catch (e) {
      return
    }
    const copy = await collaborationApi.duplicate(project.value.id)
    ElMessage.success('已复制为新草稿')
    router.push(`/projects/${copy.id}`)
  }
}

function handleUpdated(updated) {
  const previousStatus = project.value?.status
  project.value = updated
  if (previousStatus !== updated.status) {
    if (updated.status === 'REVIEW_PENDING') activeTab.value = 'review'
    else if (updated.status === 'ARCHIVED') activeTab.value = 'submit'
    else if (updated.status === 'QUALIFICATION_REJECTED') activeTab.value = 'qualification'
  }
}

async function load() {
  loading.value = true
  try {
    const detail = await projectApi.detail(route.params.id)
    project.value = detail
    activeTab.value = defaultTab(detail)
  } catch (e) {
    project.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.detail-page {
  padding-top: 18px;
}

.back-line {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.header-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 20px 22px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  box-shadow: var(--shadow);
}

.header-main {
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.project-title {
  font-size: 22px;
  font-weight: 750;
  letter-spacing: 0.3px;
  line-height: 1.4;
}

.risk-chip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: var(--muted);
  background: #f4f6f9;
  border-radius: 20px;
  padding: 3px 10px;
}

.risk-chip.has-risk {
  color: #a56a12;
  background: #fbf3e6;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--muted);
}

.meta-row span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.header-actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
  flex: 0 0 auto;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  margin-top: 14px;
  padding: 14px 6px;
  box-shadow: var(--shadow);
}

.summary-item {
  padding: 2px 16px;
  border-right: 1px solid var(--line-soft);
}

.summary-item:last-child {
  border-right: none;
}

.summary-item small {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-bottom: 6px;
}

.summary-item strong {
  font-size: 17px;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.summary-item strong i {
  font-style: normal;
  font-size: 10px;
  color: var(--muted);
  margin-left: 3px;
}

.step-panel {
  margin-top: 14px;
  padding: 20px 16px 16px;
}

.step-panel :deep(.el-step__title) {
  font-size: 12px;
}

.step-panel :deep(.el-step__description) {
  font-size: 10px;
}

.tab-panel {
  margin-top: 14px;
  padding: 10px 22px 24px;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 22px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translateY(-8px) translateX(6px) scale(0.8);
}

.tab-badge {
  margin-left: 2px;
}

.tab-count {
  font-size: 10px;
  font-weight: 600;
  color: var(--brand);
  background: var(--brand-soft);
  border-radius: 8px;
  padding: 0 5px;
  line-height: 16px;
}

@media (max-width: 1200px) {
  .summary-strip {
    grid-template-columns: repeat(3, 1fr);
    row-gap: 14px;
  }

  .summary-item:nth-child(3n) {
    border-right: none;
  }

  .header-card {
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 700px) {
  .summary-strip {
    grid-template-columns: repeat(2, 1fr);
  }

  .summary-item {
    border-right: none;
  }

  .tab-panel {
    padding: 8px 12px 18px;
  }
}
</style>
