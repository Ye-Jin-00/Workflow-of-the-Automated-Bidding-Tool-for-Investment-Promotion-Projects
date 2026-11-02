<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">资格预审规则</h1>
        <p class="page-subtitle">规则可配置，业务人员无需改代码。必填规则失败会自动驳回，非必填规则仅预警。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="auth.isAdmin" @click="resetRules">恢复默认</el-button>
        <el-tag effect="plain" type="info">权重合计 {{ totalWeight }}</el-tag>
      </div>
    </div>

    <el-alert
      v-if="!auth.isAdmin"
      type="info"
      :closable="false"
      show-icon
      title="当前角色只读。规则调整仅系统管理员可执行，且会写入审计日志。"
      class="mb-16"
    />

    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">规则清单 <small>{{ rules.length }} 条</small></div>
        <span class="small muted">阈值为金额时单位为万元；资质规则支持“|”分隔多个关键词</span>
      </div>
      <el-table v-loading="loading" :data="rules" style="width: 100%">
        <el-table-column prop="code" label="编号" width="72">
          <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="规则" width="140">
          <template #default="{ row }">
            <div class="rule-cell">
              <span>{{ row.name }}</span>
              <small>{{ row.category }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="判定条件" min-width="220">
          <template #default="{ row }">
            <span class="condition">{{ row.fieldLabel }} {{ row.operatorSymbol }} {{ row.threshold }}</span>
            <div class="small muted desc">{{ row.description }}</div>
          </template>
        </el-table-column>
        <el-table-column label="性质" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.mandatory ? 'danger' : 'info'" size="small" effect="plain">
              {{ row.mandatory ? '必填' : '预警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权重" width="72" align="center" prop="weight" />
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              size="small"
              :disabled="!auth.isAdmin"
              @change="(value) => toggleEnabled(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="失败建议" min-width="240" show-overflow-tooltip prop="suggestion" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button v-if="auth.isAdmin" text type="primary" size="small" @click="edit(row)">编辑</el-button>
            <span v-else class="small muted">只读</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" title="编辑资格规则" width="520px">
      <el-form v-if="current" label-position="top">
        <div class="dialog-head">
          <span class="mono">{{ current.code }}</span>
          <strong>{{ current.name }}</strong>
        </div>
        <div class="grid-2">
          <el-form-item label="阈值">
            <el-input v-model="form.threshold" placeholder="如 300 / AA / 招商运营|房地产经纪" />
          </el-form-item>
          <el-form-item label="权重">
            <el-input-number v-model="form.weight" :min="0" :max="100" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="grid-2">
          <el-form-item label="必填（失败即驳回）">
            <el-switch v-model="form.mandatory" />
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.enabled" />
          </el-form-item>
        </div>
        <el-form-item label="失败建议">
          <el-input v-model="form.suggestion" maxlength="200" />
        </el-form-item>
        <el-form-item label="规则说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="300" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ruleApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const rules = ref([])
const dialog = ref(false)
const current = ref(null)

const form = reactive({
  threshold: '',
  weight: 0,
  mandatory: false,
  enabled: true,
  suggestion: '',
  description: ''
})

const totalWeight = computed(() => rules.value.filter((r) => r.enabled).reduce((sum, r) => sum + r.weight, 0))

async function load() {
  loading.value = true
  try {
    rules.value = await ruleApi.list()
  } finally {
    loading.value = false
  }
}

function edit(row) {
  current.value = row
  Object.assign(form, {
    threshold: row.threshold,
    weight: row.weight,
    mandatory: row.mandatory,
    enabled: row.enabled,
    suggestion: row.suggestion,
    description: row.description
  })
  dialog.value = true
}

async function save() {
  if (!form.threshold?.trim()) {
    ElMessage.warning('阈值不能为空')
    return
  }
  saving.value = true
  try {
    await ruleApi.update(current.value.code, { ...form })
    ElMessage.success('规则已更新，将在下一次资格预审时生效')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row, value) {
  try {
    await ruleApi.update(row.code, { enabled: value })
    ElMessage.success(value ? '规则已启用' : '规则已停用')
    load()
  } catch (e) {
    row.enabled = !value
  }
}

async function resetRules() {
  try {
    await ElMessageBox.confirm('将覆盖当前所有规则配置，恢复为系统默认值，是否继续？', '恢复默认规则', {
      type: 'warning',
      confirmButtonText: '恢复默认',
      cancelButtonText: '取消'
    })
    rules.value = await ruleApi.reset()
    ElMessage.success('已恢复默认规则')
  } catch (e) {
    // 用户取消
  }
}

load()
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.rule-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.rule-cell small {
  font-size: 10px;
  color: var(--muted);
}

.condition {
  font-size: 12px;
  font-weight: 600;
  color: var(--ink);
}

.desc {
  margin-top: 4px;
  line-height: 1.5;
}

.dialog-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 14px;
}

.dialog-head .mono {
  font-size: 11px;
  color: var(--brand);
  background: var(--brand-soft);
  padding: 2px 7px;
  border-radius: 4px;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
</style>
