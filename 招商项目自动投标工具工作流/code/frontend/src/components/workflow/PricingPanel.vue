<template>
  <div class="pricing-panel">
    <section class="panel" v-if="project.matchReport">
      <div class="panel-head">
        <div class="panel-title">资质匹配评估</div>
        <span class="small muted">生成于 {{ project.matchReport.generatedAt }}</span>
      </div>
      <div class="match-head">
        <div class="score-ring" :style="{ '--score': project.matchReport.score }">
          <span>{{ project.matchReport.score }}</span>
          <small>匹配得分</small>
        </div>
        <div class="match-copy">
          <el-tag type="success" effect="light">{{ project.matchReport.recommendation }}</el-tag>
          <p>{{ project.matchReport.summary }}</p>
        </div>
      </div>
      <div class="dimension-grid">
        <div v-for="dim in project.matchReport.dimensions" :key="dim.name" class="dimension">
          <div class="dimension-top">
            <span>{{ dim.name }}</span>
            <strong>{{ dim.score }}<small>/{{ dim.fullScore }}</small></strong>
          </div>
          <el-progress
            :percentage="Math.round((dim.score / dim.fullScore) * 100)"
            :stroke-width="6"
            :show-text="false"
            color="#b4232a"
          />
          <div class="dimension-comment">{{ dim.comment }}</div>
        </div>
      </div>
      <div class="grid-2 match-lists">
        <div class="match-list">
          <div class="list-title"><el-icon color="#2e8b57"><CircleCheck /></el-icon> 优势与证据</div>
          <ul>
            <li v-for="item in project.matchReport.matched" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div class="match-list">
          <div class="list-title"><el-icon color="#c47a14"><Warning /></el-icon> 差距与补强</div>
          <ul>
            <li v-for="item in project.matchReport.gaps" :key="item">{{ item }}</li>
            <li v-if="!project.matchReport.gaps.length" class="muted">未发现明显差距</li>
          </ul>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">报价测算 <small>金额单位：万元</small></div>
        <el-tag v-if="project.pricingPlan" size="small" effect="plain" type="info">
          {{ project.pricingPlan.strategy }}
        </el-tag>
      </div>

      <div v-if="editable" class="pricing-form">
        <div class="grid-4">
          <el-form-item label="投入人天">
            <el-input-number v-model="form.personDays" :min="1" :max="9999" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="人天单价（元）">
            <el-input-number v-model="form.dailyRate" :min="1" :step="100" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="预计竞争家数">
            <el-input-number v-model="form.competitorCount" :min="1" :max="30" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="目标下浮率">
            <el-input-number v-model="form.discountRate" :min="0" :max="0.3" :step="0.01" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <el-collapse class="advanced">
          <el-collapse-item title="高级成本假设（分包、差旅、税率）" name="advanced">
            <div class="grid-4">
              <el-form-item label="分包/渠道费率">
                <el-input-number v-model="form.subcontractRate" :min="0" :max="0.3" :step="0.01" :precision="3" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="差旅尽调费率">
                <el-input-number v-model="form.travelRate" :min="0" :max="0.2" :step="0.01" :precision="3" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="税率">
                <el-input-number v-model="form.taxRate" :min="0" :max="0.2" :step="0.01" :precision="3" controls-position="right" style="width: 100%" />
              </el-form-item>
            </div>
          </el-collapse-item>
        </el-collapse>
        <div class="pricing-action">
          <span class="small muted">成本底线 = 总成本 × 1.12；建议价不高于预算的 96%。</span>
          <el-button type="primary" :loading="submitting" @click="recalculate">
            {{ project.pricingPlan ? '重新测算' : '开始测算' }}
          </el-button>
        </div>
      </div>

      <div v-if="project.pricingPlan" class="pricing-result">
        <div class="price-hero">
          <div>
            <small>建议报价</small>
            <strong>{{ money(project.pricingPlan.recommendedPrice) }}<i>万元</i></strong>
            <div class="price-range">
              成本底线 {{ money(project.pricingPlan.floorPrice) }} · 报价上限 {{ money(project.pricingPlan.ceilingPrice) }}
            </div>
          </div>
          <div class="kpi-grid">
            <div class="kpi">
              <small>毛利率</small>
              <strong :class="{ danger: project.pricingPlan.grossMarginRate < 8 }">{{ percent(project.pricingPlan.grossMarginRate) }}</strong>
            </div>
            <div class="kpi">
              <small>价格分</small>
              <strong>{{ project.pricingPlan.priceScore }}<span>/30</span></strong>
            </div>
            <div class="kpi">
              <small>中标概率</small>
              <strong>{{ percent(project.pricingPlan.winProbability) }}</strong>
            </div>
            <div class="kpi">
              <small>总成本</small>
              <strong>{{ money(project.pricingPlan.totalCost) }}</strong>
            </div>
          </div>
        </div>

        <el-table :data="project.pricingPlan.costItems" size="small" style="width: 100%" class="cost-table">
          <el-table-column prop="name" label="成本项" width="160" />
          <el-table-column prop="remark" label="测算口径" min-width="260" />
          <el-table-column label="金额（万元）" width="140" align="right">
            <template #default="{ row }">{{ money(row.amount) }}</template>
          </el-table-column>
        </el-table>

        <el-alert
          v-if="project.pricingPlan.riskNotes?.length"
          type="warning"
          :closable="false"
          show-icon
          title="报价风险提示"
          class="mt-14"
        >
          <div v-for="risk in project.pricingPlan.riskNotes" :key="risk" class="risk-line">{{ risk }}</div>
        </el-alert>
      </div>

      <div v-else class="empty-block">
        <el-icon :size="30" color="#c9d1dc"><TrendCharts /></el-icon>
        <p>尚未完成报价测算</p>
        <span class="small muted">完成要素抽取后，可在此进行资质匹配与报价测算。</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi } from '../../api'
import { money, percent } from '../../utils/format'

const props = defineProps({
  project: { type: Object, required: true }
})
const emit = defineEmits(['updated'])

const submitting = ref(false)
const editable = computed(
  () => props.project.allowedActions?.includes('MATCH_PRICE') && props.project.permissions?.operate
)

const form = reactive({
  personDays: 60,
  dailyRate: 1200,
  competitorCount: 4,
  discountRate: 0.08,
  subcontractRate: 0.05,
  travelRate: 0.02,
  taxRate: 0.06
})

watch(
  () => [props.project.id, props.project.version],
  () => {
    const plan = props.project.pricingPlan
    if (plan) {
      Object.assign(form, {
        personDays: plan.personDays,
        dailyRate: Number(plan.dailyRate),
        competitorCount: plan.competitorCount,
        discountRate: plan.discountRate,
        subcontractRate: plan.subcontractRate,
        travelRate: plan.travelRate,
        taxRate: plan.taxRate
      })
    }
  },
  { immediate: true }
)

async function recalculate() {
  if (!props.project.budgetAmount) {
    ElMessage.warning('请先在项目信息中填写招标预算')
    return
  }
  submitting.value = true
  try {
    const updated = await workflowApi.matchPrice(props.project.id, {
      ...form,
      version: props.project.version
    })
    ElMessage.success('匹配与报价测算完成')
    emit('updated', updated)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.pricing-panel .panel + .panel {
  margin-top: 18px;
}

.match-head {
  display: flex;
  align-items: center;
  gap: 18px;
}

.match-copy p {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-2);
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  margin-top: 20px;
}

.dimension {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 12px;
}

.dimension-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12px;
  margin-bottom: 8px;
}

.dimension-top strong {
  font-size: 16px;
  color: var(--brand);
}

.dimension-top small {
  font-size: 10px;
  color: var(--muted);
  font-weight: 400;
}

.dimension-comment {
  margin-top: 8px;
  font-size: 10px;
  line-height: 1.6;
  color: var(--muted);
}

.match-lists {
  margin-top: 18px;
  gap: 20px;
}

.list-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
}

.match-list ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.match-list li {
  font-size: 11px;
  line-height: 1.7;
  color: var(--ink-2);
  padding-left: 12px;
  position: relative;
}

.match-list li::before {
  content: "";
  position: absolute;
  left: 0;
  top: 8px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #b9c2cd;
}

.grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.advanced {
  margin: 4px 0 12px;
  border: none;
}

.pricing-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 14px;
  margin-bottom: 18px;
  padding: 12px 14px;
  background: #fafbfc;
  border: 1px solid var(--line-soft);
  border-radius: 9px;
}

.pricing-action .el-button {
  flex: 0 0 auto;
}

@media (max-width: 720px) {
  .pricing-action {
    align-items: flex-start;
    flex-direction: column;
  }
}

.price-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  background: linear-gradient(120deg, #fdf3f3, #fff 60%);
  border: 1px solid #f3dddd;
  border-radius: 10px;
  padding: 18px 20px;
  margin-bottom: 16px;
}

.price-hero > div > small {
  display: block;
  font-size: 11px;
  color: var(--muted);
  margin-bottom: 4px;
}

.price-hero > div > strong {
  font-size: 32px;
  color: var(--brand);
  line-height: 1.1;
}

.price-hero strong i {
  font-style: normal;
  font-size: 12px;
  color: var(--muted);
  margin-left: 6px;
}

.price-range {
  margin-top: 6px;
  font-size: 10px;
  color: var(--muted);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(74px, 1fr));
  gap: 12px;
}

.kpi {
  text-align: right;
}

.kpi small {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-bottom: 4px;
}

.kpi strong {
  font-size: 16px;
  color: var(--ink);
}

.kpi strong.danger {
  color: var(--danger);
}

.kpi strong span {
  font-size: 10px;
  color: var(--muted);
  font-weight: 400;
}

.cost-table {
  margin-top: 4px;
}

.mt-14 {
  margin-top: 14px;
}

.risk-line {
  font-size: 11px;
  line-height: 1.7;
  margin-top: 4px;
}

@media (max-width: 1100px) {
  .dimension-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .grid-4 {
    grid-template-columns: repeat(2, 1fr);
  }

  .price-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .kpi-grid {
    width: 100%;
    text-align: left;
  }

  .kpi {
    text-align: left;
  }
}
</style>
