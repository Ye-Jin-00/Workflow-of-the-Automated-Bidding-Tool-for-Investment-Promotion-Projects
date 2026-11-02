<template>
  <div class="submit-panel">
    <section v-if="project.status === 'APPROVED'" class="panel">
      <div class="panel-head">
        <div class="panel-title">提交投标</div>
        <el-tag type="success" effect="light" size="small">复核通过</el-tag>
      </div>
      <el-alert
        type="success"
        :closable="false"
        show-icon
        title="项目已完成复核，可提交投标。提交前请确认截止时间、报价与盖章文件。"
        class="mb-16"
      />
      <el-form label-position="top" class="submit-form">
        <div class="grid-2">
          <el-form-item label="提交方式">
            <el-select v-model="form.method" style="width: 100%">
              <el-option v-for="item in methods" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="平台回执号 / 递交编号">
            <el-input v-model="form.referenceNo" maxlength="80" placeholder="如 NJGGZY-2026-1024-018" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="form.note" type="textarea" :rows="3" maxlength="300" placeholder="盖章、上传、回执等说明" />
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">
          确认提交投标
        </el-button>
      </el-form>
    </section>

    <section v-else-if="project.status === 'SUBMITTED'" class="panel">
      <div class="panel-head">
        <div class="panel-title">提交与归档</div>
        <el-tag type="success" effect="light" size="small">已提交</el-tag>
      </div>
      <div class="info-grid">
        <div><small>提交时间</small><strong>{{ project.submission?.submittedAt }}</strong></div>
        <div><small>提交方式</small><strong>{{ project.submission?.method }}</strong></div>
        <div><small>回执号</small><strong class="mono">{{ project.submission?.referenceNo }}</strong></div>
        <div><small>操作人</small><strong>{{ project.submission?.operatorName }}</strong></div>
      </div>
      <p v-if="project.submission?.note" class="note-text">{{ project.submission.note }}</p>

      <div class="divider"></div>
      <div class="form-title">项目归档</div>
      <p class="small muted mb-12">归档后项目只读，投标文件、评审记录与回执将按保存年限留存。</p>
      <el-form label-position="top">
        <div class="grid-3">
          <el-form-item label="档号">
            <el-input v-model="archiveForm.archiveNo" :placeholder="`默认 ARC-${project.code}`" />
          </el-form-item>
          <el-form-item label="保存年限">
            <el-input-number v-model="archiveForm.retentionYears" :min="1" :max="30" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="归档说明">
            <el-input v-model="archiveForm.note" maxlength="200" placeholder="归档材料清单等" />
          </el-form-item>
        </div>
        <el-button type="primary" :loading="archiving" @click="archive">确认归档</el-button>
      </el-form>
    </section>

    <section v-else-if="project.status === 'ARCHIVED'" class="panel archive-done">
      <div class="archive-badge"><el-icon :size="30"><CircleCheckFilled /></el-icon></div>
      <h2>项目已归档</h2>
      <p class="muted">本项目已完成投标与归档，全部过程材料只读留存。</p>
      <div class="info-grid archive-grid">
        <div><small>档号</small><strong class="mono">{{ project.archive?.archiveNo }}</strong></div>
        <div><small>归档时间</small><strong>{{ project.archive?.archivedAt }}</strong></div>
        <div><small>保存年限</small><strong>{{ project.archive?.retentionYears }} 年</strong></div>
        <div><small>归档人</small><strong>{{ project.archive?.operatorName }}</strong></div>
        <div><small>投标回执</small><strong class="mono">{{ project.submission?.referenceNo }}</strong></div>
        <div><small>最终报价</small><strong>{{ money(project.bidAmount) }} 万元</strong></div>
      </div>
      <p v-if="project.archive?.note" class="note-text">{{ project.archive.note }}</p>
      <el-button @click="$router.push('/projects')">返回项目列表</el-button>
    </section>

    <section v-else class="panel">
      <div class="empty-block">
        <el-icon :size="32" color="#c9d1dc"><Finished /></el-icon>
        <p>尚未进入提交阶段</p>
        <span class="small muted">
          当前状态为“{{ project.statusLabel }}”。完成人工复核后，方可提交投标并归档。
        </span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { metaApi, workflowApi } from '../../api'
import { money } from '../../utils/format'

const props = defineProps({
  project: { type: Object, required: true }
})
const emit = defineEmits(['updated'])

const submitting = ref(false)
const archiving = ref(false)
const methods = ref(['电子招投标平台', '纸质标书现场递交', '邮箱/邮寄递交', '其他'])

const form = reactive({
  method: '电子招投标平台',
  referenceNo: '',
  note: ''
})

const archiveForm = reactive({
  archiveNo: '',
  retentionYears: 5,
  note: ''
})

onMounted(async () => {
  try {
    const options = await metaApi.options()
    if (options.methods?.length) methods.value = options.methods
  } catch (e) {
    // 使用默认值
  }
})

async function submit() {
  if (!form.referenceNo || form.referenceNo.trim().length < 3) {
    ElMessage.warning('请填写平台回执号 / 递交编号')
    return
  }
  submitting.value = true
  try {
    const updated = await workflowApi.submit(props.project.id, {
      ...form,
      version: props.project.version
    })
    ElMessage.success('投标已提交，回执已记录')
    emit('updated', updated)
  } finally {
    submitting.value = false
  }
}

async function archive() {
  archiving.value = true
  try {
    const updated = await workflowApi.archive(props.project.id, {
      ...archiveForm,
      version: props.project.version
    })
    ElMessage.success('项目已归档')
    emit('updated', updated)
  } finally {
    archiving.value = false
  }
}
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.mb-12 {
  margin-bottom: 12px;
}

.submit-form {
  max-width: 760px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.info-grid > div {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 10px 12px;
}

.info-grid small {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-bottom: 5px;
}

.info-grid strong {
  font-size: 12px;
  color: var(--ink);
}

.note-text {
  margin-top: 12px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--muted);
  border-left: 3px solid var(--line);
  padding-left: 10px;
}

.form-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 6px;
}

.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.archive-done {
  text-align: center;
  padding: 40px;
}

.archive-badge {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  margin: 0 auto 14px;
  background: #eaf6ef;
  color: #2e8b57;
  display: flex;
  align-items: center;
  justify-content: center;
}

.archive-done h2 {
  font-size: 22px;
  margin-bottom: 6px;
}

.archive-grid {
  margin: 24px auto;
  text-align: left;
  max-width: 820px;
}

.archive-done .el-button {
  margin-top: 8px;
}

.archive-done .note-text {
  border-left: none;
  padding-left: 0;
  text-align: center;
}

@media (max-width: 900px) {
  .info-grid,
  .grid-3 {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
