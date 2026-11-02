<template>
  <el-dialog
    :model-value="modelValue"
    title="批量导入项目"
    width="720px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
    @close="clearRedirect"
    @closed="reset"
  >
    <div class="import-intro">
      <div>
        <strong>用 Excel 一次导入多个项目</strong>
        <span>导入后统一保存为草稿，核对信息后再发起资格预审。</span>
      </div>
      <el-button text type="primary" :loading="downloading" @click="downloadTemplate">
        <el-icon><Download /></el-icon> 下载导入模板
      </el-button>
    </div>

    <div class="steps">
      <div class="step">
        <span class="step-no">1</span>
        <div>
          <strong>下载模板并填写</strong>
          <small>按模板列填写，不要修改表头；单次最多 200 行。</small>
        </div>
      </div>
      <div class="step">
        <span class="step-no">2</span>
        <div>
          <strong>上传填写后的文件</strong>
          <small>支持 .xlsx / .csv；示例行会自动跳过。</small>
        </div>
      </div>
    </div>

    <div class="drop-zone" :class="{ filled: file }" @click="fileInput?.click()">
      <el-icon :size="28"><UploadFilled /></el-icon>
      <template v-if="file">
        <strong>{{ file.name }}</strong>
        <span>{{ (file.size / 1024).toFixed(1) }} KB · 点击重新选择</span>
      </template>
      <template v-else>
        <strong>点击选择 Excel / CSV 文件</strong>
        <span>文件不超过 20MB；建议直接使用上方模板</span>
      </template>
      <input ref="fileInput" type="file" accept=".xlsx,.csv" class="hidden-file" @change="onFileChange" />
    </div>

    <div v-if="result" class="result-panel">
      <div class="result-head">
        <div class="result-numbers">
          <div><strong class="ok">{{ result.success }}</strong><span>成功</span></div>
          <div><strong :class="{ bad: result.failed > 0 }">{{ result.failed }}</strong><span>失败</span></div>
          <div><strong>{{ result.total }}</strong><span>总行数</span></div>
        </div>
        <el-button v-if="result.success" text type="primary" @click="goDrafts">查看草稿箱</el-button>
      </div>
      <div v-if="result.errors?.length" class="error-list">
        <div class="error-title">失败明细</div>
        <div v-for="item in result.errors" :key="item.row" class="error-row">
          <span class="row-no">第 {{ item.row }} 行</span>
          <span>{{ item.message }}</span>
        </div>
      </div>
      <div v-else class="success-note">
        <el-icon><CircleCheck /></el-icon>
        全部导入成功，已进入草稿箱。
      </div>
    </div>

    <template #footer>
      <el-button @click="close">{{ completed ? '关闭' : '取消' }}</el-button>
      <el-button v-if="completed" type="primary" @click="goDrafts">查看</el-button>
      <el-button v-else type="primary" :loading="importing" :disabled="!file" @click="startImport">
        开始导入
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { projectApi } from '../api'
import { downloadWithAuth } from '../utils/download'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'imported'])
const router = useRouter()
const fileInput = ref()
const file = ref(null)
const importing = ref(false)
const downloading = ref(false)
const completed = ref(false)
const result = ref(null)
let redirectTimer = null

function clearRedirect() {
  if (redirectTimer) {
    clearTimeout(redirectTimer)
    redirectTimer = null
  }
}

function close() {
  clearRedirect()
  emit('update:modelValue', false)
}

function reset() {
  clearRedirect()
  file.value = null
  result.value = null
  completed.value = false
  if (fileInput.value) fileInput.value.value = ''
}

onBeforeUnmount(clearRedirect)

async function downloadTemplate() {
  downloading.value = true
  try {
    await downloadWithAuth('/projects/import/template', '项目批量导入模板.xlsx')
    ElMessage.success('模板已开始下载')
  } finally {
    downloading.value = false
  }
}

function onFileChange(event) {
  const selected = event.target.files?.[0]
  event.target.value = ''
  if (!selected) return
  const ext = (selected.name.split('.').pop() || '').toLowerCase()
  if (!['xlsx', 'csv'].includes(ext)) {
    ElMessage.warning('请选择 .xlsx 或 .csv 文件')
    return
  }
  if (selected.size > 20 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 20MB')
    return
  }
  file.value = selected
  result.value = null
  completed.value = false
}

async function startImport() {
  if (!file.value) return
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    result.value = await projectApi.importBatch(formData)
    if (result.value.success > 0) {
      completed.value = true
      ElMessage.success(`成功导入 ${result.value.success} 个项目，正在进入草稿箱`)
      emit('imported', result.value)
      // 导入完成后保留结果页短暂展示，随后自动进入草稿箱
      clearRedirect()
      redirectTimer = setTimeout(() => {
        goDrafts()
      }, 1400)
    } else {
      ElMessage.warning('没有项目导入成功，请查看失败明细')
    }
  } finally {
    importing.value = false
  }
}

function goDrafts() {
  clearRedirect()
  close()
  router.push('/drafts')
}
</script>

<style scoped>
.import-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--brand-soft);
  margin-bottom: 16px;
}

.import-intro > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.import-intro strong {
  font-size: 13px;
}

.import-intro span {
  font-size: 10px;
  color: var(--muted);
}

.steps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.step {
  display: flex;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 9px;
}

.step-no {
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  border-radius: 50%;
  background: var(--brand);
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.step strong {
  font-size: 12px;
}

.step small {
  font-size: 10px;
  color: var(--muted);
  line-height: 1.5;
}

.drop-zone {
  border: 1px dashed #d6b5b7;
  border-radius: 10px;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--muted);
  cursor: pointer;
  background: #fcfcfd;
  transition: border-color 0.2s, background 0.2s;
}

.drop-zone:hover,
.drop-zone.filled {
  border-color: var(--brand);
  background: #fffafa;
}

.drop-zone strong {
  color: var(--ink);
  font-size: 12px;
}

.drop-zone span {
  font-size: 10px;
}

.hidden-file {
  display: none;
}

.result-panel {
  margin-top: 16px;
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow: hidden;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8f9fb;
}

.result-numbers {
  display: flex;
  gap: 26px;
}

.result-numbers > div {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.result-numbers strong {
  font-size: 20px;
}

.result-numbers strong.ok {
  color: #2e8b57;
}

.result-numbers strong.bad {
  color: var(--brand);
}

.result-numbers span {
  font-size: 10px;
  color: var(--muted);
}

.error-list {
  max-height: 170px;
  overflow-y: auto;
  padding: 10px 16px 14px;
}

.error-title {
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 6px;
}

.error-row {
  display: flex;
  gap: 12px;
  padding: 5px 0;
  border-top: 1px dashed var(--line);
  font-size: 11px;
  color: var(--ink-2);
}

.row-no {
  flex: 0 0 58px;
  color: var(--brand);
}

.success-note {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  font-size: 11px;
  color: #2e8b57;
}

@media (max-width: 700px) {
  .steps {
    grid-template-columns: 1fr;
  }

  .import-intro {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
