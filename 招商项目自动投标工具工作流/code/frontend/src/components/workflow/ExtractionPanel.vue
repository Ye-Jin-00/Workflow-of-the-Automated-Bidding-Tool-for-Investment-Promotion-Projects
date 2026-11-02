<template>
  <div class="extract-panel">
    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">招标文件正文 <small>粘贴文本后按规则抽取</small></div>
        <el-tag v-if="project.extraction" size="small" effect="plain" type="success">
          已抽取 {{ project.extraction.items.length }} 项
        </el-tag>
      </div>

      <el-input
        v-model="tenderText"
        type="textarea"
        :rows="9"
        :readonly="!editable"
        maxlength="50000"
        show-word-limit
        placeholder="粘贴招标公告、资格预审文件或招标文件正文，系统将自动识别预算、保证金、截止时间、资格条件、评分办法等要素。"
      />

      <div class="extract-actions">
        <span class="small muted">
          说明：演示版使用规则引擎抽取，生产环境可接入 PDF/OCR 解析；抽取结果需人工复核确认。
        </span>
        <div class="flex-center">
          <el-button v-if="editable" text @click="fillSample">填入示例</el-button>
          <el-button v-if="editable" type="primary" :loading="submitting" @click="runExtract">
            {{ project.extraction ? '重新抽取' : '开始抽取' }}
          </el-button>
        </div>
      </div>
    </section>

    <template v-if="project.extraction">
      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">结构化字段</div>
          <span class="small muted">抽取时间 {{ project.extraction.extractedAt }}</span>
        </div>
        <div class="parsed-grid">
          <div class="parsed-item">
            <small>预算 / 最高限价</small>
            <strong>{{ project.extraction.budgetAmount ? `${money(project.extraction.budgetAmount)} 万元` : '未识别' }}</strong>
          </div>
          <div class="parsed-item">
            <small>投标保证金</small>
            <strong>{{ project.extraction.depositAmount ? `${money(project.extraction.depositAmount)} 万元` : '未识别' }}</strong>
          </div>
          <div class="parsed-item">
            <small>投标截止时间</small>
            <strong>{{ project.extraction.deadline || '未识别' }}</strong>
          </div>
          <div class="parsed-item">
            <small>原文长度</small>
            <strong>{{ project.extraction.charCount }} 字</strong>
          </div>
        </div>
        <el-alert type="info" :closable="false" show-icon :title="project.extraction.summary" class="mt-12" />
        <el-alert
          v-if="project.extraction.warnings?.length"
          type="warning"
          :closable="false"
          show-icon
          title="抽取预警"
          class="mt-10"
        >
          <div v-for="warning in project.extraction.warnings" :key="warning" class="warn-line">{{ warning }}</div>
        </el-alert>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">要素清单</div>
          <span class="small muted">标记“硬性”的条款将影响资格与废标判断</span>
        </div>
        <el-table :data="project.extraction.items" size="small" style="width: 100%" max-height="620">
          <el-table-column prop="category" label="分类" width="110">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :type="categoryType(row.category)">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="要素内容" min-width="380">
            <template #default="{ row }">
              <div class="req-content">{{ row.content }}</div>
              <div v-if="row.source" class="req-source">原文：{{ row.source }}</div>
            </template>
          </el-table-column>
          <el-table-column label="性质" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.mandatory" type="danger" size="small" effect="light">硬性</el-tag>
              <el-tag v-else type="info" size="small" effect="plain">一般</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="置信度" width="120">
            <template #default="{ row }">
              <div class="confidence">
                <el-progress
                  :percentage="Math.round(row.confidence * 100)"
                  :stroke-width="5"
                  :show-text="false"
                  :color="row.confidence >= 0.8 ? '#2e8b57' : '#c47a14'"
                />
                <span>{{ Math.round(row.confidence * 100) }}%</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <section v-else class="panel">
      <div class="empty-block">
        <el-icon :size="30" color="#c9d1dc"><Document /></el-icon>
        <p>尚未抽取招标要素</p>
        <span class="small muted">粘贴正文后点击“开始抽取”，系统将输出结构化要求清单。</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi } from '../../api'
import { money } from '../../utils/format'

const props = defineProps({
  project: { type: Object, required: true }
})
const emit = defineEmits(['updated'])

const submitting = ref(false)
const tenderText = ref('')
const editable = computed(
  () => props.project.allowedActions?.includes('EXTRACT') && props.project.permissions?.operate
)

watch(
  () => [props.project.id, props.project.version],
  () => {
    tenderText.value = props.project.tenderText || ''
  },
  { immediate: true }
)

function categoryType(category) {
  if (category === '资格条件') return 'danger'
  if (category === '报价要求' || category === '商务条款') return 'warning'
  if (category === '时间节点') return 'success'
  return 'info'
}

function cnDeadlineText(value) {
  const date = value ? new Date(String(value).replace(/-/g, '/')) : new Date(Date.now() + 14 * 86400000)
  if (Number.isNaN(date.getTime())) return '2026年12月31日 17:00'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}年${pad(date.getMonth() + 1)}月${pad(date.getDate())}日 ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function fillSample() {
  const cnDeadline = cnDeadlineText(props.project.deadline)
  tenderText.value = `项目名称：${props.project.name || '招商代理服务项目'}
招标人：${props.project.clientName || '招标人'}
预算金额：人民币${props.project.budgetAmount || 300}万元
投标保证金：人民币${props.project.depositAmount || 5}万元
投标截止时间：${cnDeadline}
一、资格要求
1. 投标人须具备房地产经纪机构备案或招商运营服务能力，注册资本不低于300万元。
2. 近三年须具有不少于2个同类项目业绩，并提供合同关键页。
3. 信用等级AA级及以上，无失信被执行记录。
4. 拟投入项目团队不少于10人，须配备项目负责人、招商经理与运营经理。
二、服务要求
5. 负责产业定位、招商策略制定、渠道组织、企业招引与落地服务。
6. 合作期内完成可招商面积60%以上去化，并引入不少于3家产业链关键企业。
三、商务要求
7. 投标保证金须在截止时间前足额缴纳，未缴纳按无效标处理。
8. 付款方式：合同签订后30%，中期验收后40%，服务期满30%。
四、评标办法
9. 采用综合评分法：价格分30分、技术分50分、商务分20分。
10. 服务期12个月，核心团队须驻场服务。`
  ElMessage.success('示例文本已填入，可直接抽取')
}

async function runExtract() {
  if (!tenderText.value || tenderText.value.trim().length < 20) {
    ElMessage.warning('请粘贴至少 20 个字的招标文件正文')
    return
  }
  submitting.value = true
  try {
    const updated = await workflowApi.extract(props.project.id, {
      tenderText: tenderText.value,
      version: props.project.version
    })
    ElMessage.success(`抽取完成，共识别 ${updated.extraction.items.length} 项要素`)
    emit('updated', updated)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.extract-panel .panel + .panel {
  margin-top: 18px;
}

.extract-actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.parsed-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.parsed-item {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 10px 12px;
}

.parsed-item small {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-bottom: 5px;
}

.parsed-item strong {
  font-size: 13px;
  color: var(--ink);
}

.mt-12 {
  margin-top: 12px;
}

.mt-10 {
  margin-top: 10px;
}

.warn-line {
  font-size: 11px;
  line-height: 1.7;
  margin-top: 4px;
}

.req-content {
  line-height: 1.7;
  color: var(--ink-2);
}

.req-source {
  margin-top: 4px;
  font-size: 10px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.confidence {
  display: flex;
  align-items: center;
  gap: 6px;
}

.confidence .el-progress {
  flex: 1;
}

.confidence span {
  font-size: 10px;
  color: var(--muted);
}

@media (max-width: 900px) {
  .parsed-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .extract-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
