<template>
  <div class="draft-panel">
    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">AI 标书初稿 <small>生成的是“可复核的初稿”，不是最终投标文件</small></div>
        <div class="flex-center">
          <el-tag v-if="hasDraft && draft.degraded" type="warning" effect="light" size="small">降级生成</el-tag>
          <el-tag v-else-if="hasDraft" type="success" effect="light" size="small">
            {{ draft.provider === 'local-template' ? '本地生成' : '生成成功' }}
          </el-tag>
          <el-tag v-if="hasDraft" effect="plain" size="small" type="info">{{ draft.provider }} / {{ draft.model }}</el-tag>
        </div>
      </div>

      <el-alert
        v-if="draft?.warning"
        :type="draft.degraded ? 'warning' : 'info'"
        :closable="false"
        show-icon
        :title="draft.warning"
        class="mb-14"
      />

      <div v-if="canGenerate" class="generate-bar">
        <div class="generate-fields">
          <el-form-item label="编制侧重">
            <el-select
              v-model="emphasis"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入本次编制重点"
              style="width: 100%"
            >
              <el-option
                v-for="item in emphasisOptions"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="语气风格">
            <el-select
              v-model="tone"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入语气，如：更有说服力"
              style="width: 100%"
            >
              <el-option label="专业、稳健" value="专业、稳健" />
              <el-option label="简洁、有力" value="简洁、有力" />
              <el-option label="创新、进取" value="创新、进取" />
            </el-select>
            <div class="tone-hint">可直接输入自定义语气，AI 将在生成时按该风格组织表达。</div>
          </el-form-item>
        </div>
        <el-button type="primary" :loading="generating" @click="generate">
          <el-icon><MagicStick /></el-icon>
          {{ hasDraft ? '重新生成初稿' : '生成标书初稿' }}
        </el-button>
      </div>

      <div v-if="!hasDraft" class="empty-block">
        <el-icon :size="32" color="#c9d1dc"><MagicStick /></el-icon>
        <p>尚未生成初稿</p>
        <span class="small muted">系统将结合项目信息、招标要素、匹配结果与报价，生成技术标、商务与资格响应初稿。</span>
      </div>
    </section>

    <template v-if="hasDraft">
      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">生成信息</div>
          <span class="small muted">提示词版本 {{ draft.promptVersion }} · 耗时 {{ draft.elapsedMs }} ms</span>
        </div>
        <div class="meta-grid">
          <div><small>生成时间</small><strong>{{ draft.generatedAt }}</strong></div>
          <div><small>供应商</small><strong>{{ draft.provider }}</strong></div>
          <div><small>模型</small><strong>{{ draft.model }}</strong></div>
          <div><small>正文字数</small><strong>{{ draft.technicalProposal?.length || 0 }} 字</strong></div>
        </div>
      </section>

      <section class="panel">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="技术标" name="technical">
            <div class="editor-head">
              <span class="small muted">技术标正文（可直接编辑，保存后进入复核）</span>
              <el-button v-if="canEdit" type="primary" :loading="saving" @click="save">保存编辑</el-button>
            </div>
            <el-input
              v-if="canEdit"
              v-model="draft.technicalProposal"
              type="textarea"
              :rows="24"
              maxlength="30000"
              show-word-limit
            />
            <pre v-else class="draft-readonly">{{ draft.technicalProposal }}</pre>
          </el-tab-pane>

          <el-tab-pane label="商务响应" name="commercial">
            <el-input
              v-if="canEdit"
              v-model="draft.commercialResponse"
              type="textarea"
              :rows="16"
              maxlength="10000"
              show-word-limit
            />
            <pre v-else class="draft-readonly">{{ draft.commercialResponse }}</pre>
          </el-tab-pane>

          <el-tab-pane label="资格响应" name="qualification">
            <el-input
              v-if="canEdit"
              v-model="draft.qualificationResponse"
              type="textarea"
              :rows="14"
              maxlength="10000"
              show-word-limit
            />
            <pre v-else class="draft-readonly">{{ draft.qualificationResponse }}</pre>
          </el-tab-pane>

          <el-tab-pane label="风险提示" name="risks">
            <el-input
              v-if="canEdit"
              v-model="riskText"
              type="textarea"
              :rows="10"
              placeholder="每行一条风险提示"
            />
            <ul v-else class="risk-ul">
              <li v-for="item in draft.riskNotes" :key="item">{{ item }}</li>
            </ul>
            <p class="small muted risk-tip">风险提示会随初稿一起进入人工复核，无法闭合的事项必须显式说明。</p>
          </el-tab-pane>

          <el-tab-pane label="章节大纲" name="outline">
            <div class="outline-grid">
              <div v-for="(section, index) in draft.outline" :key="index" class="outline-card">
                <div class="outline-title"><span>{{ String(index + 1).padStart(2, '0') }}</span>{{ section.title }}</div>
                <ul>
                  <li v-for="point in section.points" :key="point">{{ point }}</li>
                </ul>
              </div>
              <div v-if="!draft.outline?.length" class="empty-block">暂无大纲</div>
            </div>
          </el-tab-pane>
        </el-tabs>

        <div v-if="canEdit" class="save-row">
          <span class="small muted">编辑不会改变业务状态；保存后可在顶部提交人工复核。</span>
          <el-button type="primary" :loading="saving" @click="save">保存编辑</el-button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi } from '../../api'

const props = defineProps({
  project: { type: Object, required: true }
})
const emit = defineEmits(['updated'])

const activeTab = ref('technical')
const generating = ref(false)
const saving = ref(false)
const emphasis = ref('')
const tone = ref('专业、稳健')
const riskText = ref('')

const hasDraft = computed(() => Boolean(props.project.aiDraft))

const canGenerate = computed(
  () => props.project.allowedActions?.includes('AI_DRAFT') && props.project.permissions?.operate
)
const canEdit = computed(
  () => props.project.allowedActions?.includes('UPDATE_DRAFT') && props.project.permissions?.operate
)

const emphasisOptions = [
  '产业定位与招商策略',
  '渠道资源与去化保障',
  '国企协同与合规',
  '品牌运营与活动策划',
  '企业服务与长期运营'
]

const draft = reactive({
  outline: [],
  technicalProposal: '',
  commercialResponse: '',
  qualificationResponse: '',
  riskNotes: [],
  provider: '',
  model: '',
  promptVersion: '',
  generatedAt: '',
  elapsedMs: 0,
  degraded: false,
  warning: ''
})

function syncDraft() {
  const source = props.project.aiDraft
  if (!source) {
    Object.assign(draft, {
      outline: [],
      technicalProposal: '',
      commercialResponse: '',
      qualificationResponse: '',
      riskNotes: [],
      provider: '',
      model: '',
      promptVersion: '',
      generatedAt: '',
      elapsedMs: 0,
      degraded: false,
      warning: ''
    })
    riskText.value = ''
    return
  }
  Object.assign(draft, JSON.parse(JSON.stringify(source)))
  draft.riskNotes = draft.riskNotes || []
  riskText.value = (draft.riskNotes || []).join('\n')
}

watch(() => [props.project.id, props.project.version], syncDraft, { immediate: true })

async function generate() {
  generating.value = true
  try {
    const updated = await workflowApi.aiDraft(props.project.id, {
      emphasis: emphasis.value,
      tone: tone.value,
      version: props.project.version
    })
    ElMessage.success('标书初稿已生成')
    emit('updated', updated)
  } finally {
    generating.value = false
  }
}

async function save() {
  if (!draft.technicalProposal?.trim()) {
    ElMessage.warning('技术标正文不能为空')
    return
  }
  saving.value = true
  try {
    const updated = await workflowApi.updateDraft(props.project.id, {
      outline: draft.outline,
      technicalProposal: draft.technicalProposal,
      commercialResponse: draft.commercialResponse,
      qualificationResponse: draft.qualificationResponse,
      riskNotes: riskText.value.split('\n').map((item) => item.trim()).filter(Boolean),
      version: props.project.version
    })
    ElMessage.success('初稿修改已保存')
    emit('updated', updated)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.draft-panel .panel + .panel {
  margin-top: 18px;
}

.mb-14 {
  margin-bottom: 14px;
}

.generate-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  background: #f8f9fb;
  border-radius: 10px;
  padding: 16px 18px;
}

.tone-hint {
  font-size: 9px;
  color: var(--muted);
  line-height: 1.5;
  margin-top: 4px;
}

.generate-fields {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 14px;
  flex: 1;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.meta-grid > div {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 10px 12px;
}

.meta-grid small {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-bottom: 5px;
}

.meta-grid strong {
  font-size: 12px;
  color: var(--ink);
}

.editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.draft-readonly {
  margin: 0;
  padding: 16px;
  background: #fbfcfd;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  font-family: inherit;
  font-size: 12px;
  line-height: 2;
  color: var(--ink-2);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 640px;
  overflow: auto;
}

.risk-ul {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  line-height: 1.9;
  color: var(--ink-2);
}

.risk-tip {
  margin-top: 12px;
}

.outline-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.outline-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 14px;
}

.outline-title {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
}

.outline-title span {
  color: var(--brand);
  font-family: Consolas, monospace;
  font-size: 10px;
}

.outline-card ul {
  margin: 0;
  padding-left: 16px;
  font-size: 11px;
  line-height: 1.8;
  color: var(--muted);
}

.save-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-top: 1px dashed var(--line);
  padding-top: 14px;
  margin-top: 6px;
}

@media (max-width: 900px) {
  .generate-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .generate-fields,
  .meta-grid,
  .outline-grid {
    grid-template-columns: 1fr;
  }

  .meta-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
