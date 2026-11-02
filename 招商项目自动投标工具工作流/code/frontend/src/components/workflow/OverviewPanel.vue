<template>
  <div class="overview">
    <div class="overview-main">
      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">项目概况</div>
          <el-button v-if="canEdit" text type="primary" size="small" @click="$router.push(`/projects/${project.id}/edit`)">
            编辑信息
          </el-button>
        </div>
        <el-descriptions :column="2" border size="small" label-width="96px">
          <el-descriptions-item label="招标人">{{ project.clientName }}</el-descriptions-item>
          <el-descriptions-item label="项目载体">{{ project.carrierName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="所属产业">{{ project.industry || '—' }}</el-descriptions-item>
          <el-descriptions-item label="线索来源">{{ project.source || '—' }}</el-descriptions-item>
          <el-descriptions-item label="预算 / 报价">
            {{ money(project.budgetAmount) }} 万元
            <span v-if="project.bidAmount" class="muted"> / 报价 {{ money(project.bidAmount) }} 万元</span>
          </el-descriptions-item>
          <el-descriptions-item label="投标保证金">{{ project.depositAmount ? `${money(project.depositAmount)} 万元` : '—' }}</el-descriptions-item>
          <el-descriptions-item label="投标截止">
            {{ project.deadline || '—' }}
            <span class="deadline-tip">{{ daysLabel(project.daysToDeadline) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="负责人">{{ project.ownerName }} / {{ project.department }}</el-descriptions-item>
          <el-descriptions-item label="创建信息">{{ project.createdBy }} · {{ project.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="最近更新">{{ project.updatedAt }}</el-descriptions-item>
          <el-descriptions-item label="项目背景" :span="2">
            <span class="brief">{{ project.projectBrief || '尚未填写项目背景' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">资格预审 <small>规则自动判定</small></div>
          <el-button text type="primary" size="small" @click="$emit('go', 'qualification')">
            {{ project.qualificationResult ? '查看明细' : '去预审' }}
          </el-button>
        </div>
        <div v-if="project.qualificationResult" class="qual-summary">
          <div class="score-ring" :style="{ '--score': project.qualificationResult.score }">
            <span>{{ project.qualificationResult.score }}</span>
            <small>预审得分</small>
          </div>
          <div class="qual-body">
            <div class="qual-head">
              <el-tag :type="conclusionType(project.qualificationResult.conclusion)" effect="light">
                {{ conclusionText(project.qualificationResult.conclusion) }}
              </el-tag>
              <span class="small muted">{{ project.qualificationResult.checkedAt }}</span>
            </div>
            <p class="qual-text">{{ project.qualificationResult.summary }}</p>
            <div v-if="project.qualificationResult.hardFailures.length" class="fail-list">
              <div v-for="item in project.qualificationResult.hardFailures.slice(0, 3)" :key="item" class="fail-item">
                <el-icon color="#c0392b" :size="13"><CircleClose /></el-icon>{{ item }}
              </div>
            </div>
            <div v-else-if="project.qualificationResult.warnings.length" class="warn-list">
              <div v-for="item in project.qualificationResult.warnings.slice(0, 3)" :key="item" class="warn-item">
                <el-icon color="#c47a14" :size="13"><Warning /></el-icon>{{ item }}
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-inline">
          尚未执行资格预审。资格不满足时系统会自动驳回，避免无效投入。
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">要素抽取 <small>{{ project.extraction?.items?.length || 0 }} 项</small></div>
          <el-button text type="primary" size="small" @click="$emit('go', 'extraction')">
            {{ project.extraction ? '查看要素' : '去抽取' }}
          </el-button>
        </div>
        <div v-if="project.extraction">
          <div class="category-row">
            <span v-for="cat in categories" :key="cat.name" class="category-chip">
              {{ cat.name }} <strong>{{ cat.count }}</strong>
            </span>
          </div>
          <p class="small muted extraction-summary">{{ project.extraction.summary }}</p>
          <ul class="req-list">
            <li v-for="item in mandatoryPreview" :key="item.id">
              <span class="req-cat">{{ item.category }}</span>
              <span class="req-content">{{ item.content }}</span>
              <el-tag v-if="item.mandatory" type="danger" size="small" effect="plain">硬性</el-tag>
            </li>
          </ul>
        </div>
        <div v-else class="empty-inline">尚未抽取招标文件要素，可粘贴正文后一键抽取。</div>
      </section>

      <div class="grid-2">
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">资质匹配</div>
            <el-button text type="primary" size="small" @click="$emit('go', 'pricing')">查看</el-button>
          </div>
          <div v-if="project.matchReport" class="mini-match">
            <div class="mini-score">{{ project.matchReport.score }}<small>分</small></div>
            <div>
              <el-tag type="success" effect="light">{{ project.matchReport.recommendation }}</el-tag>
              <p class="small muted mini-summary">{{ project.matchReport.summary }}</p>
            </div>
          </div>
          <div v-else class="empty-inline">尚未完成匹配评估</div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">报价测算</div>
            <el-button text type="primary" size="small" @click="$emit('go', 'pricing')">查看</el-button>
          </div>
          <div v-if="project.pricingPlan" class="mini-price">
            <div>
              <small>建议报价</small>
              <strong>{{ money(project.pricingPlan.recommendedPrice) }}<i>万元</i></strong>
            </div>
            <div class="mini-kpi">
              <span>毛利率 {{ percent(project.pricingPlan.grossMarginRate) }}</span>
              <span>中标概率 {{ percent(project.pricingPlan.winProbability) }}</span>
            </div>
          </div>
          <div v-else class="empty-inline">尚未完成报价测算</div>
        </section>
      </div>

      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">标书初稿</div>
          <el-button text type="primary" size="small" @click="$emit('go', 'draft')">
            {{ project.aiDraft ? '查看 / 编辑' : '去生成' }}
          </el-button>
        </div>
        <div v-if="project.aiDraft" class="draft-preview">
          <div class="draft-meta">
            <el-tag size="small" effect="plain" type="info">{{ project.aiDraft.provider }}</el-tag>
            <span class="small muted">生成于 {{ project.aiDraft.generatedAt }} · {{ project.aiDraft.elapsedMs }} ms</span>
            <el-tag v-if="project.aiDraft.degraded" size="small" type="warning" effect="light">降级生成</el-tag>
          </div>
          <p class="draft-text">{{ shortText(project.aiDraft.technicalProposal, 240) }}</p>
        </div>
        <div v-else class="empty-inline">尚未生成初稿</div>
      </section>
    </div>

    <aside class="overview-side">
      <section class="panel timeline-panel">
        <div class="panel-head">
          <div class="panel-title">流程时间线</div>
          <el-icon color="#9aa5b2"><Clock /></el-icon>
        </div>
        <div v-if="timeline.length" class="timeline-scroll">
          <el-timeline>
            <el-timeline-item
              v-for="log in timeline"
              :key="log.id"
              :timestamp="log.createdAt"
              :color="timelineColor(log.action)"
              placement="top"
            >
              <div class="timeline-title">{{ log.actionLabel }}</div>
              <div class="timeline-detail">{{ log.detail }}</div>
              <div class="timeline-actor">{{ log.actorName }}<template v-if="log.ip"> · {{ log.ip }}</template></div>
            </el-timeline-item>
          </el-timeline>
        </div>
        <div v-else class="empty-inline">暂无流程记录</div>
      </section>

      <section v-if="project.risks?.length" class="panel risk-card">
        <div class="panel-head">
          <div class="panel-title">风险与提醒</div>
          <span class="risk-count">{{ project.risks.length }}</span>
        </div>
        <ul class="risk-list">
          <li v-for="risk in project.risks" :key="risk">
            <el-icon color="#c47a14" :size="13"><WarningFilled /></el-icon>
            <span>{{ risk }}</span>
          </li>
        </ul>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { projectApi } from '../../api'
import { daysLabel, money, percent, shortText } from '../../utils/format'

const props = defineProps({
  project: { type: Object, required: true }
})
defineEmits(['go'])

const timeline = ref([])
const canEdit = computed(() => props.project.allowedActions?.includes('EDIT'))

const categories = computed(() => {
  const map = {}
  for (const item of props.project.extraction?.items || []) {
    map[item.category] = (map[item.category] || 0) + 1
  }
  return Object.entries(map).map(([name, count]) => ({ name, count }))
})

const mandatoryPreview = computed(() =>
  (props.project.extraction?.items || []).filter((item) => item.mandatory).slice(0, 4)
)

function conclusionType(value) {
  if (value === 'PASS') return 'success'
  if (value === 'CONDITIONAL_PASS') return 'warning'
  return 'danger'
}

function conclusionText(value) {
  if (value === 'PASS') return '预审通过'
  if (value === 'CONDITIONAL_PASS') return '有条件通过'
  return '预审驳回'
}

function timelineColor(action) {
  if (action?.startsWith('AUTO_REJECT') || action?.startsWith('REVIEW_RETURN')) return '#c0392b'
  if (action?.startsWith('REVIEW_APPROVE') || action?.startsWith('ARCHIVE') || action?.startsWith('SUBMIT')) return '#2e8b57'
  if (action?.startsWith('AI_DRAFT')) return '#7c5cbf'
  return '#b4232a'
}

async function loadTimeline() {
  try {
    timeline.value = await projectApi.timeline(props.project.id)
  } catch (e) {
    timeline.value = []
  }
}

onMounted(loadTimeline)
watch(() => props.project.updatedAt, loadTimeline)
</script>

<style scoped>
.overview {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(300px, 1fr);
  gap: 18px;
  align-items: start;
}

.overview-main .panel + .panel {
  margin-top: 18px;
}

.overview-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.overview-side .panel + .panel {
  margin-top: 0;
}

.deadline-tip {
  margin-left: 8px;
  font-size: 11px;
  color: var(--brand);
}

.brief {
  line-height: 1.8;
  color: var(--ink-2);
}

.qual-summary {
  display: flex;
  gap: 20px;
  align-items: center;
}

.qual-body {
  flex: 1;
  min-width: 0;
}

.qual-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.qual-text {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-2);
}

.fail-list,
.warn-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fail-item,
.warn-item {
  display: flex;
  gap: 6px;
  align-items: flex-start;
  font-size: 11px;
  line-height: 1.6;
  color: var(--danger);
}

.warn-item {
  color: var(--warning);
}

.empty-inline {
  padding: 18px;
  border: 1px dashed var(--line);
  border-radius: 8px;
  color: var(--muted);
  font-size: 12px;
  text-align: center;
  background: #fbfcfd;
}

.category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-chip {
  font-size: 11px;
  color: var(--ink-2);
  background: #f4f6f9;
  border-radius: 4px;
  padding: 4px 9px;
}

.category-chip strong {
  color: var(--brand);
  margin-left: 4px;
}

.extraction-summary {
  margin: 10px 0;
}

.req-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.req-list li {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 8px 0;
  border-top: 1px solid var(--line-soft);
  font-size: 12px;
}

.req-cat {
  flex: 0 0 auto;
  color: var(--muted);
  font-size: 10px;
  border: 1px solid var(--line);
  border-radius: 3px;
  padding: 1px 5px;
}

.req-content {
  flex: 1;
  line-height: 1.6;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.mini-match {
  display: flex;
  gap: 14px;
  align-items: center;
}

.mini-score {
  font-size: 30px;
  font-weight: 750;
  color: var(--brand);
  line-height: 1;
}

.mini-score small {
  font-size: 11px;
  color: var(--muted);
  margin-left: 2px;
}

.mini-summary {
  margin-top: 6px;
  line-height: 1.6;
}

.mini-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.mini-price small {
  display: block;
  font-size: 11px;
  color: var(--muted);
  margin-bottom: 4px;
}

.mini-price strong {
  font-size: 22px;
  color: var(--brand);
}

.mini-price strong i {
  font-style: normal;
  font-size: 11px;
  color: var(--muted);
  margin-left: 4px;
}

.mini-kpi {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 10px;
  color: var(--muted);
  text-align: right;
}

.draft-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.draft-text {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.9;
  color: var(--ink-2);
  white-space: pre-wrap;
}

.timeline-scroll {
  max-height: 620px;
  overflow: auto;
  padding-right: 4px;
}

.timeline-title {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 3px;
}

.timeline-detail {
  font-size: 11px;
  line-height: 1.6;
  color: var(--ink-2);
  word-break: break-all;
}

.timeline-actor {
  margin-top: 4px;
  font-size: 10px;
  color: var(--muted);
}

.risk-card {
  border-top: 3px solid #d68910;
}

.risk-count {
  background: #fbf3e6;
  color: #a56a12;
  font-size: 11px;
  border-radius: 10px;
  padding: 1px 8px;
}

.risk-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.risk-list li {
  display: flex;
  gap: 7px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--ink-2);
}

@media (max-width: 1200px) {
  .overview {
    grid-template-columns: 1fr;
  }
}
</style>
