<template>
  <div class="qual-panel">
    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">资格预审画像 <small>规则判定依据</small></div>
        <el-tag v-if="result" :type="conclusionType(result.conclusion)" effect="light">
          {{ conclusionText(result.conclusion) }} · {{ result.score }} 分
        </el-tag>
      </div>

      <div v-if="editable" class="profile-form">
        <div class="grid-3">
          <el-form-item label="注册资本（万元）">
            <el-input-number v-model="profile.registeredCapital" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="成立年份">
            <el-input-number v-model="profile.establishedYear" :min="1980" :max="2030" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="信用等级">
            <el-select v-model="profile.creditLevel" style="width: 100%">
              <el-option v-for="level in ['AAA', 'AA', 'A', 'BBB', 'C']" :key="level" :label="level" :value="level" />
            </el-select>
          </el-form-item>
          <el-form-item label="近三年同类项目（个）">
            <el-input-number v-model="profile.similarProjectCount" :min="0" :max="999" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="同类项目合同额（万元）">
            <el-input-number v-model="profile.similarProjectAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="可投入团队（人）">
            <el-input-number v-model="profile.teamSize" :min="0" :max="999" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="资质 / 备案 / 认证">
          <el-select v-model="profile.qualifications" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="item in qualificationOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <div class="switch-grid">
          <div class="switch-item">
            <div><strong>可缴纳投标保证金</strong><small>资金或保函已落实</small></div>
            <el-switch v-model="profile.bidBondAvailable" />
          </div>
          <div class="switch-item">
            <div><strong>具备政府 / 国资服务经验</strong><small>影响本地资源评分</small></div>
            <el-switch v-model="profile.hasGovernmentClient" />
          </div>
          <div class="switch-item">
            <div><strong>存在失信 / 黑名单记录</strong><small>命中即一票否决</small></div>
            <el-switch v-model="profile.blacklisted" style="--el-switch-on-color: #c0392b" />
          </div>
          <div class="switch-item">
            <div><strong>存在关联关系 / 利益冲突</strong><small>需回避或取得书面同意</small></div>
            <el-switch v-model="profile.conflictsOfInterest" style="--el-switch-on-color: #c0392b" />
          </div>
        </div>
        <div class="grid-2" style="margin-top: 14px">
          <el-form-item label="财务状况">
            <el-select v-model="profile.financialStatus" style="width: 100%">
              <el-option v-for="item in ['盈利', '持平', '亏损']" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="profile.notes" maxlength="300" placeholder="数据口径与特殊情况说明" />
          </el-form-item>
        </div>
        <div class="action-row">
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="必填规则不满足将立即驳回项目；非必填规则失败仅预警，允许有条件通过。"
            class="action-alert"
          />
          <el-button type="primary" :loading="submitting" @click="runCheck">
            {{ result ? '重新执行预审' : '执行资格预审' }}
          </el-button>
        </div>
      </div>

      <div v-else>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="注册资本">{{ money(profile.registeredCapital) }} 万元</el-descriptions-item>
          <el-descriptions-item label="成立年份">{{ profile.establishedYear }}</el-descriptions-item>
          <el-descriptions-item label="信用等级">{{ profile.creditLevel }}</el-descriptions-item>
          <el-descriptions-item label="同类业绩">{{ profile.similarProjectCount }} 个</el-descriptions-item>
          <el-descriptions-item label="团队规模">{{ profile.teamSize }} 人</el-descriptions-item>
          <el-descriptions-item label="财务状况">{{ profile.financialStatus }}</el-descriptions-item>
          <el-descriptions-item label="资质备案" :span="3">
            <el-tag v-for="item in profile.qualifications || []" :key="item" size="small" effect="plain" class="qual-tag">{{ item }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="合规状态" :span="3">
            <el-tag :type="profile.blacklisted ? 'danger' : 'success'" size="small" effect="light">
              {{ profile.blacklisted ? '存在失信记录' : '无失信记录' }}
            </el-tag>
            <el-tag :type="profile.conflictsOfInterest ? 'danger' : 'success'" size="small" effect="light" class="qual-tag">
              {{ profile.conflictsOfInterest ? '存在关联关系' : '无利益冲突' }}
            </el-tag>
            <el-tag :type="profile.bidBondAvailable ? 'success' : 'warning'" size="small" effect="light" class="qual-tag">
              {{ profile.bidBondAvailable ? '保证金可覆盖' : '保证金待落实' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div class="readonly-tip">
          当前角色或状态不可修改资格画像。项目负责人可在草稿/驳回阶段重新预审。
        </div>
      </div>
    </section>

    <section class="panel" v-if="result">
      <div class="panel-head">
        <div class="panel-title">判定结果</div>
        <span class="small muted">检查时间 {{ result.checkedAt }}</span>
      </div>

      <el-alert
        v-if="result.hardFailures.length"
        type="error"
        :closable="false"
        show-icon
        :title="`${result.hardFailures.length} 项必填条件不满足，项目已自动驳回`"
        class="result-alert"
      >
        <div v-for="item in result.hardFailures" :key="item" class="alert-line">{{ item }}</div>
      </el-alert>
      <el-alert
        v-else-if="result.warnings.length"
        type="warning"
        :closable="false"
        show-icon
        :title="`有条件通过：${result.warnings.length} 项非必填条件预警`"
        class="result-alert"
      >
        <div v-for="item in result.warnings" :key="item" class="alert-line">{{ item }}</div>
      </el-alert>
      <el-alert
        v-else
        type="success"
        :closable="false"
        show-icon
        title="全部规则满足，资格预审通过"
        class="result-alert"
      />

      <el-table :data="result.checks" size="small" style="width: 100%">
        <el-table-column prop="ruleName" label="校验项" min-width="150">
          <template #default="{ row }">
            <div class="rule-name">
              <span>{{ row.ruleName }}</span>
              <el-tag v-if="row.mandatory" size="small" effect="plain" type="danger">必填</el-tag>
              <el-tag v-else size="small" effect="plain" type="info">预警</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="actualValue" label="实际值" min-width="150" show-overflow-tooltip />
        <el-table-column prop="expectedValue" label="要求" min-width="140" />
        <el-table-column label="结果" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : row.mandatory ? 'danger' : 'warning'" size="small" effect="light">
              {{ row.passed ? '通过' : row.mandatory ? '不通过' : '预警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="说明 / 建议" min-width="260" show-overflow-tooltip />
      </el-table>
    </section>

    <section class="panel" v-else>
      <div class="empty-block">
        <el-icon :size="30" color="#c9d1dc"><DocumentChecked /></el-icon>
        <p>尚未执行资格预审</p>
        <span class="small muted">执行后将生成逐条判定结果，并自动推进或驳回项目。</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi } from '../../api'
import { money } from '../../utils/format'

const props = defineProps({
  project: { type: Object, required: true }
})
const emit = defineEmits(['updated'])

const submitting = ref(false)
const result = computed(() => props.project.qualificationResult)
const editable = computed(
  () => props.project.allowedActions?.includes('QUALIFICATION_CHECK') && props.project.permissions?.operate
)

const qualificationOptions = [
  '房地产经纪机构备案',
  '招商运营服务能力',
  '产业园区运营',
  'ISO9001质量管理体系认证',
  'ISO14001环境管理体系认证',
  'ISO45001职业健康安全管理体系认证'
]

const profile = reactive({
  companyName: '',
  registeredCapital: 0,
  establishedYear: 2019,
  qualifications: [],
  creditLevel: 'AAA',
  similarProjectCount: 0,
  similarProjectAmount: 0,
  teamSize: 0,
  hasGovernmentClient: true,
  bidBondAvailable: true,
  blacklisted: false,
  conflictsOfInterest: false,
  financialStatus: '盈利',
  notes: ''
})

function syncProfile() {
  const source = props.project.profile || {}
  Object.assign(profile, {
    companyName: source.companyName || '南京市行至集商业服务有限公司',
    registeredCapital: source.registeredCapital ?? 0,
    establishedYear: source.establishedYear ?? 2019,
    qualifications: [...(source.qualifications || [])],
    creditLevel: source.creditLevel || 'AAA',
    similarProjectCount: source.similarProjectCount ?? 0,
    similarProjectAmount: source.similarProjectAmount ?? 0,
    teamSize: source.teamSize ?? 0,
    hasGovernmentClient: Boolean(source.hasGovernmentClient),
    bidBondAvailable: Boolean(source.bidBondAvailable),
    blacklisted: Boolean(source.blacklisted),
    conflictsOfInterest: Boolean(source.conflictsOfInterest),
    financialStatus: source.financialStatus || '盈利',
    notes: source.notes || ''
  })
}

watch(() => props.project.version, syncProfile, { immediate: true })

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

async function runCheck() {
  if (!profile.registeredCapital || Number(profile.registeredCapital) <= 0) {
    ElMessage.warning('请填写注册资本')
    return
  }
  submitting.value = true
  try {
    const updated = await workflowApi.qualificationCheck(props.project.id, {
      profile: { ...profile },
      version: props.project.version
    })
    if (updated.status === 'QUALIFICATION_REJECTED') {
      ElMessage.warning('资格预审未通过，项目已自动驳回')
    } else {
      ElMessage.success('资格预审通过')
    }
    emit('updated', updated)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.qual-panel .panel + .panel {
  margin-top: 18px;
}

.qual-tag {
  margin-right: 6px;
}

.switch-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 10px 12px;
  background: #fbfcfd;
}

.switch-item strong {
  display: block;
  font-size: 12px;
  margin-bottom: 3px;
}

.switch-item small {
  font-size: 10px;
  color: var(--muted);
}

.action-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
}

.action-alert {
  flex: 1;
}

.readonly-tip {
  margin-top: 14px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 8px;
  font-size: 11px;
  color: var(--muted);
}

.result-alert {
  margin-bottom: 16px;
}

.alert-line {
  font-size: 11px;
  line-height: 1.7;
  margin-top: 4px;
}

.rule-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 800px) {
  .grid-3,
  .switch-grid {
    grid-template-columns: 1fr;
  }
}
</style>
