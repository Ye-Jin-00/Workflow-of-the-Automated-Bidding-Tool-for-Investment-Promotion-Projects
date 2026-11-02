<template>
  <div class="review-panel">
    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">人工复核</div>
        <el-tag v-if="project.status === 'REVIEW_PENDING'" type="warning" effect="light" size="small">待复核</el-tag>
        <el-tag v-else-if="project.status === 'REVIEW_REJECTED'" type="danger" effect="light" size="small">已退回</el-tag>
        <el-tag v-else-if="project.status === 'APPROVED'" type="success" effect="light" size="small">复核通过</el-tag>
        <el-tag v-else type="info" effect="plain" size="small">未进入复核</el-tag>
      </div>

      <el-alert
        v-if="project.lastReviewOpinion"
        :type="project.status === 'REVIEW_REJECTED' ? 'error' : 'success'"
        :closable="false"
        show-icon
        :title="project.status === 'REVIEW_REJECTED' ? '最近一轮复核退回意见' : '最近一轮复核意见'"
        class="opinion-alert"
      >
        <div class="opinion-text">{{ project.lastReviewOpinion }}</div>
      </el-alert>

      <div class="status-guide">
        <template v-if="project.status === 'AI_DRAFTED'">
          <div class="guide-title">初稿已生成，等待提交复核</div>
          <p>请先在“标书初稿”中完成编辑与自检，再提交人工复核。复核人不能是项目负责人。</p>
          <el-button v-if="canSubmitReview" type="primary" :loading="submitting" @click="submitReview">
            提交人工复核
          </el-button>
        </template>
        <template v-else-if="project.status === 'REVIEW_PENDING'">
          <div class="guide-title">已进入人工复核队列</div>
          <p v-if="canReview">请核对资格响应、技术方案、报价与风险提示后给出复核结论。退回时必须填写具体修改意见。</p>
          <p v-else>项目正在等待复核人处理。复核通过后方可提交投标。</p>
        </template>
        <template v-else-if="project.status === 'REVIEW_REJECTED'">
          <div class="guide-title">复核退回，需修改后重新提交</div>
          <p>请根据退回意见修改初稿（或重新生成），确认后再次提交复核。历史意见全部保留。</p>
          <el-button v-if="canSubmitReview" type="primary" :loading="submitting" @click="submitReview">
            修改完成，重新提交复核
          </el-button>
        </template>
        <template v-else-if="project.status === 'APPROVED'">
          <div class="guide-title">复核已通过</div>
          <p>项目已具备提交条件，请到“提交归档”页填写投标平台回执号并完成提交。</p>
        </template>
        <template v-else>
          <div class="guide-title">尚未进入人工复核</div>
          <p>当前状态为“{{ project.statusLabel }}”。完成匹配测算与初稿生成后，即可提交人工复核。</p>
        </template>
      </div>

      <div v-if="canReview" class="review-form">
        <div class="divider"></div>
        <div class="form-title">我的复核结论 <span class="small muted">第 {{ project.reviewRound + 1 }} 轮</span></div>
        <el-radio-group v-model="decision" class="decision-group">
          <el-radio-button label="APPROVE">
            <el-icon><CircleCheck /></el-icon> 复核通过
          </el-radio-button>
          <el-radio-button label="RETURN">
            <el-icon><RefreshLeft /></el-icon> 退回修改
          </el-radio-button>
        </el-radio-group>
        <el-input
          v-model="opinion"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          :placeholder="decision === 'RETURN' ? '请写明需补充或修改的具体内容（不少于 5 个字）' : '复核意见（可选）'"
          class="opinion-input"
        />
        <div class="review-submit">
          <span class="small muted">复核动作将记录复核人、时间与 IP，不可撤销。</span>
          <el-button type="primary" :loading="submitting" @click="submitReviewDecision">
            {{ decision === 'RETURN' ? '确认退回' : '确认通过' }}
          </el-button>
        </div>
      </div>
    </section>

    <section class="panel" v-if="project.reviews?.length">
      <div class="panel-head">
        <div class="panel-title">复核记录 <small>{{ project.reviews.length }} 轮</small></div>
      </div>
      <div class="review-history">
        <div v-for="review in sortedReviews" :key="review.id" class="review-card">
          <div class="review-card-head">
            <div class="review-round">第 {{ review.round }} 轮</div>
            <el-tag :type="review.decision === 'APPROVE' ? 'success' : 'danger'" size="small" effect="light">
              {{ review.decision === 'APPROVE' ? '通过' : '退回' }}
            </el-tag>
            <span class="small muted">{{ review.createdAt }}</span>
            <span class="reviewer">{{ review.reviewerName }}</span>
          </div>
          <p class="review-opinion">{{ review.opinion }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi } from '../../api'

const props = defineProps({
  project: { type: Object, required: true }
})
const emit = defineEmits(['updated'])

const submitting = ref(false)
const decision = ref('APPROVE')
const opinion = ref('')

const canReview = computed(
  () => props.project.allowedActions?.includes('REVIEW') && props.project.permissions?.review
)
const canSubmitReview = computed(
  () => props.project.allowedActions?.includes('SUBMIT_REVIEW') && props.project.permissions?.operate
)

const sortedReviews = computed(() =>
  [...(props.project.reviews || [])].sort((a, b) => b.round - a.round)
)

async function submitReview() {
  submitting.value = true
  try {
    const updated = await workflowApi.submitReview(props.project.id, props.project.version)
    ElMessage.success('已提交人工复核')
    emit('updated', updated)
  } finally {
    submitting.value = false
  }
}

async function submitReviewDecision() {
  if (decision.value === 'RETURN' && opinion.value.trim().length < 5) {
    ElMessage.warning('退回时必须填写不少于 5 个字的修改意见')
    return
  }
  submitting.value = true
  try {
    const updated = await workflowApi.review(props.project.id, {
      decision: decision.value,
      opinion: opinion.value.trim(),
      version: props.project.version
    })
    ElMessage.success(decision.value === 'APPROVE' ? '复核通过' : '已退回负责人修改')
    opinion.value = ''
    emit('updated', updated)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.review-panel .panel + .panel {
  margin-top: 18px;
}

.opinion-alert {
  margin-bottom: 16px;
}

.opinion-text {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.8;
}

.status-guide {
  background: #f8f9fb;
  border-radius: 10px;
  padding: 16px 18px;
}

.guide-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 6px;
}

.status-guide p {
  font-size: 12px;
  line-height: 1.8;
  color: var(--muted);
  margin-bottom: 12px;
}

.review-form {
  margin-top: 6px;
}

.form-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
}

.decision-group {
  margin-bottom: 14px;
}

.opinion-input {
  margin-bottom: 12px;
}

.review-submit {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.review-history {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 14px;
  background: #fbfcfd;
}

.review-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.review-round {
  font-size: 12px;
  font-weight: 700;
  color: var(--brand);
}

.reviewer {
  margin-left: auto;
  font-size: 11px;
  color: var(--ink-2);
}

.review-opinion {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--ink-2);
  white-space: pre-wrap;
}
</style>
