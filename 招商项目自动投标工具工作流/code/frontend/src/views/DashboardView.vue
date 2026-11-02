<template>
  <div class="page dashboard">
    <div class="page-head">
      <div>
        <div class="welcome-line">
          <h1 class="page-title">{{ greeting() }}，{{ auth.displayName }}</h1>
          <span class="role-chip">{{ auth.roleLabel }}</span>
        </div>
        <p class="page-subtitle">
          {{ todayText() }} · 以下数据按您的角色与部门权限实时计算
        </p>
      </div>
      <div class="page-actions">
        <el-button v-if="canCreate" type="primary" @click="$router.push('/projects/new')">
          <el-icon><Plus /></el-icon> 新建投标项目
        </el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div class="stat-grid">
        <div
          class="stat-card clickable"
          v-for="card in statCards"
          :key="card.label"
          :style="{ '--accent': card.color }"
          role="button"
          tabindex="0"
          @click="openCard(card)"
          @keyup.enter="openCard(card)"
        >
          <div class="stat-top">
            <span>{{ card.label }}</span>
            <div class="stat-icon"><el-icon :size="18"><component :is="card.icon" /></el-icon></div>
          </div>
          <div class="stat-value">
            <template v-if="card.money">
              {{ money(card.value) }}<small>万元</small>
            </template>
            <template v-else>
              {{ card.value }}<small>个</small>
            </template>
          </div>
          <div class="stat-hint">
            {{ card.hint }}
            <el-icon class="stat-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <div class="middle-grid">
        <div class="panel todo-panel">
          <div class="panel-head">
            <div class="panel-title">待我处理 <small>{{ overview?.todos?.length || 0 }} 项</small></div>
            <el-button text type="primary" @click="$router.push('/projects')">全部项目</el-button>
          </div>
          <div v-if="!overview?.todos?.length" class="empty-block">
            <el-icon :size="28" color="#c9d1dc"><CircleCheck /></el-icon>
            <p>当前没有待处理事项</p>
          </div>
          <div v-else class="todo-list">
            <div
              v-for="todo in overview.todos"
              :key="todo.projectId + todo.action"
              class="todo-item"
              @click="openProject(todo.projectId)"
            >
              <div class="todo-main">
                <div class="todo-title">
                  <span class="mono code">{{ todo.code }}</span>
                  <span class="todo-name">{{ todo.name }}</span>
                </div>
                <div class="todo-meta">
                  <span class="tag-soft">{{ todo.actionLabel }}</span>
                  <span class="muted">{{ todo.statusLabel }}</span>
                </div>
              </div>
              <div class="todo-deadline" :class="{ urgent: todo.daysToDeadline >= 0 && todo.daysToDeadline <= 3 }">
                {{ daysLabel(todo.daysToDeadline) }}
              </div>
            </div>
          </div>
        </div>

        <div class="panel risk-panel">
          <div class="panel-head">
            <div class="panel-title">风险提醒</div>
            <el-icon color="#c47a14"><WarningFilled /></el-icon>
          </div>
          <div v-if="!overview?.risks?.length" class="empty-block">
            <p>暂无高风险事项</p>
          </div>
          <div v-else class="risk-list">
            <div v-for="risk in overview.risks" :key="risk.projectId + risk.text" class="risk-item" @click="openProject(risk.projectId)">
              <span class="risk-dot" :style="{ background: riskColor(risk.level) }"></span>
              <div class="risk-body">
                <div class="risk-name">{{ risk.name }}</div>
                <div class="risk-text">{{ risk.text }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="lower-grid">
        <div class="panel">
          <div class="panel-head">
            <div class="panel-title">阶段分布</div>
            <span class="small muted">按工作流七个环节统计</span>
          </div>
          <div class="funnel">
            <div v-for="stage in overview?.stages || []" :key="stage.stage" class="funnel-row">
              <div class="funnel-label">
                <span class="stage-index">{{ stage.stage }}</span>
                {{ stage.label }}
              </div>
              <div class="funnel-bar">
                <div class="funnel-fill" :style="{ width: barWidth(stage.count) }"></div>
              </div>
              <div class="funnel-count">{{ stage.count }}</div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-head">
            <div class="panel-title">自动化分布</div>
          </div>
          <div class="auto-list">
            <div v-for="node in automation" :key="node.name" class="auto-item">
              <span class="auto-dot" :class="node.type"></span>
              <span class="auto-name">{{ node.name }}</span>
              <span class="auto-tag" :class="node.type">{{ node.tag }}</span>
            </div>
          </div>
          <p class="auto-note">规则与 AI 负责“快”，人工负责“准”。所有自动动作均可回溯。</p>
        </div>
      </div>

      <div class="panel recent-panel">
        <div class="panel-head">
          <div class="panel-title">最近更新</div>
          <el-button text type="primary" @click="$router.push('/projects')">查看全部</el-button>
        </div>
        <el-table :data="overview?.recentProjects || []" style="width: 100%" @row-click="(row) => openProject(row.id)">
          <el-table-column prop="code" label="项目编号" width="150">
            <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
          </el-table-column>
          <el-table-column label="项目名称" min-width="280">
            <template #default="{ row }">
              <div class="project-cell">
                <span class="project-name">{{ row.name }}</span>
                <span class="small muted">{{ row.clientName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }"><StatusTag :label="row.statusLabel" :type="row.statusType" /></template>
          </el-table-column>
          <el-table-column label="预算" width="120" align="right">
            <template #default="{ row }">{{ money(row.budgetAmount) }}</template>
          </el-table-column>
          <el-table-column label="截止" width="130">
            <template #default="{ row }">
              <span :class="{ 'text-danger': row.daysToDeadline >= 0 && row.daysToDeadline <= 3 }">
                {{ daysLabel(row.daysToDeadline) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="负责人" width="100" prop="ownerName" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { daysLabel, greeting, money, riskColor, todayText } from '../utils/format'
import StatusTag from '../components/StatusTag.vue'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(true)
const overview = ref(null)

const canCreate = computed(() => ['ADMIN', 'BID_MANAGER'].includes(auth.user?.role))

const statCards = computed(() => {
  const stats = overview.value?.stats || {}
  return [
    {
      label: '项目总数',
      value: stats.total || 0,
      hint: `草稿 ${stats.drafts || 0} 个 · 已完成 ${stats.finished || 0} 个`,
      icon: 'Files',
      color: '#1f2a37',
      route: '/projects'
    },
    {
      label: '推进中',
      value: stats.active || 0,
      hint: `待人工复核 ${stats.pendingReview || 0} 个`,
      icon: 'DataLine',
      color: '#b4232a',
      route: '/projects?view=active'
    },
    {
      label: '待我处理',
      value: stats.todo || 0,
      hint: '按角色与数据权限自动汇总',
      icon: 'Bell',
      color: '#c47a14',
      route: '/projects?view=todo'
    },
    {
      label: '预算总额',
      value: stats.totalBudget || 0,
      hint: `已测算报价 ${money(stats.totalBid || 0)} 万元`,
      icon: 'Coin',
      color: '#2e8b57',
      money: true,
      route: '/projects'
    }
  ]
})

const automation = [
  { name: '资格预审', type: 'auto', tag: '规则自动' },
  { name: '招标要素抽取', type: 'auto', tag: '规则自动' },
  { name: '资质匹配与报价', type: 'auto', tag: '模型测算' },
  { name: '标书初稿', type: 'ai', tag: 'AI 生成' },
  { name: '人工复核', type: 'human', tag: '人工' },
  { name: '提交与归档', type: 'human', tag: '人工 + 留痕' }
]

const maxStage = computed(() => Math.max(...(overview.value?.stages || []).map((s) => s.count), 1))

function barWidth(count) {
  const width = Math.max((count / maxStage.value) * 100, count > 0 ? 8 : 0)
  return `${width}%`
}

function openCard(card) {
  if (card?.route) router.push(card.route)
}

function openProject(id) {
  router.push(`/projects/${id}`)
}

async function load() {
  loading.value = true
  try {
    overview.value = await dashboardApi.overview()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.welcome-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-chip {
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 20px;
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 600;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.stat-card {
  position: relative;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 18px 20px;
  box-shadow: var(--shadow);
  overflow: hidden;
}

.stat-card::after {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--accent);
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--muted);
  font-size: 13px;
}

.stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 9%, white);
}

.stat-value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 750;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
}

.stat-value small {
  font-size: 12px;
  font-weight: 400;
  color: var(--muted);
  margin-left: 6px;
}

.stat-hint {
  margin-top: 8px;
  font-size: 11px;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 5px;
}

.stat-arrow {
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.stat-card.clickable {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(31, 42, 55, 0.08);
}

.stat-card.clickable:hover .stat-arrow {
  opacity: 1;
  transform: translateX(0);
}

.stat-card.clickable:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}

.middle-grid {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: 18px;
  margin-bottom: 18px;
}

.todo-list {
  display: flex;
  flex-direction: column;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 4px;
  border-bottom: 1px solid var(--line-soft);
  cursor: pointer;
  transition: background 0.15s ease;
}

.todo-item:last-child {
  border-bottom: none;
}

.todo-item:hover {
  background: #fbfcfd;
}

.todo-main {
  min-width: 0;
}

.todo-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.todo-name {
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code {
  font-size: 10px;
  color: var(--muted);
  flex: 0 0 auto;
}

.todo-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
  font-size: 11px;
}

.todo-deadline {
  flex: 0 0 auto;
  font-size: 11px;
  color: var(--muted);
}

.todo-deadline.urgent {
  color: var(--danger);
  font-weight: 600;
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.risk-item {
  display: flex;
  gap: 8px;
  padding: 11px 4px;
  border-bottom: 1px solid var(--line-soft);
  cursor: pointer;
}

.risk-item:last-child {
  border-bottom: none;
}

.risk-dot {
  margin-top: 5px;
  flex: 0 0 8px;
}

.risk-name {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 3px;
}

.risk-text {
  font-size: 11px;
  line-height: 1.6;
  color: var(--muted);
}

.lower-grid {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: 18px;
  margin-bottom: 18px;
}

.funnel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.funnel-row {
  display: grid;
  grid-template-columns: 108px 1fr 32px;
  align-items: center;
  gap: 12px;
}

.funnel-label {
  font-size: 12px;
  color: var(--ink-2);
  display: flex;
  align-items: center;
  gap: 8px;
}

.stage-index {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f3f5f8;
  color: var(--muted);
  font-size: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.funnel-bar {
  height: 8px;
  background: #f1f3f7;
  border-radius: 6px;
  overflow: hidden;
}

.funnel-fill {
  height: 100%;
  border-radius: 6px;
  background: linear-gradient(90deg, #c43840, #b4232a);
  transition: width 0.5s ease;
}

.funnel-count {
  font-size: 12px;
  color: var(--ink-2);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.auto-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.auto-item {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
}

.auto-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #aab4c0;
}

.auto-dot.auto {
  background: #2e8b57;
}

.auto-dot.ai {
  background: #7c5cbf;
}

.auto-dot.human {
  background: #c47a14;
}

.auto-name {
  flex: 1;
  color: var(--ink-2);
}

.auto-tag {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 4px;
  background: #f2f4f7;
  color: var(--muted);
}

.auto-tag.auto {
  background: #eaf6ef;
  color: #2e8b57;
}

.auto-tag.ai {
  background: #f1ecfa;
  color: #6d4db3;
}

.auto-tag.human {
  background: #fbf3e6;
  color: #a56a12;
}

.auto-note {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
  font-size: 11px;
  line-height: 1.7;
  color: var(--muted);
}

.recent-panel :deep(.el-table__row) {
  cursor: pointer;
}

.project-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.project-name {
  font-weight: 600;
  color: var(--ink);
}

.text-danger {
  color: var(--danger);
  font-weight: 600;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .middle-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }
}
</style>
