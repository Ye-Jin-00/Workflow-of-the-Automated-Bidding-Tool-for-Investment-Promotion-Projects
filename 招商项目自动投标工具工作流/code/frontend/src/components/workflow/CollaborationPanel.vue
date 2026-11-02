<template>
  <div class="collab-panel">
    <div class="collab-grid">
      <section class="panel file-panel">
        <div class="panel-head">
          <div class="panel-title">项目文件 <small>{{ project.attachmentCount || 0 }} 个</small></div>
          <span class="small muted">单个文件不超过 20MB</span>
        </div>

        <div v-if="canUpload" class="upload-box">
          <div class="upload-row">
            <el-select v-model="category" class="category-select">
              <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
            </el-select>
            <button type="button" class="file-picker" @click="fileInput?.click()">
              <el-icon><FolderOpened /></el-icon>
              {{ selectedFile ? selectedFile.name : '选择文件' }}
            </button>
            <el-button v-if="selectedFile" text type="danger" size="small" @click="clearFile">移除</el-button>
            <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="upload">上传</el-button>
          </div>
          <div class="upload-hint">
            支持 PDF、Word、Excel、PPT、TXT、ZIP 与图片。上传后会通知项目负责人和关注人。
          </div>
          <input ref="fileInput" type="file" class="hidden-file" :accept="accept" @change="onFileChange" />
        </div>
        <div v-else class="readonly-tip">当前状态不可上传文件。</div>

        <el-table v-if="project.attachments?.length" :data="project.attachments" size="small" style="width: 100%" class="file-table">
          <el-table-column prop="category" label="分类" width="96">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :type="categoryType(row.category)">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="文件名" min-width="210">
            <template #default="{ row }">
              <button type="button" class="file-name" @click="download(row)">{{ row.filename }}</button>
              <div class="small muted">{{ formatSize(row.size) }} · {{ row.uploadedAt }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="uploadedBy" label="上传人" width="90" />
          <el-table-column label="操作" width="100" align="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="download(row)">下载</el-button>
              <el-button v-if="canDelete" text type="danger" size="small" @click="removeAttachment(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-block">
          <el-icon :size="26" color="#c9d1dc"><FolderOpened /></el-icon>
          <p>暂无项目文件</p>
        </div>
      </section>

      <section class="panel comment-panel">
        <div class="panel-head">
          <div class="panel-title">项目评论 <small>{{ project.commentCount || 0 }} 条</small></div>
          <span class="small muted">评论会同步到消息中心</span>
        </div>

        <div class="comment-list">
          <div v-for="comment in project.comments" :key="comment.id" class="comment-item">
            <UserAvatar :name="comment.authorName" :src="comment.avatarUrl" :size="30" shape="circle" />
            <div class="comment-body">
              <div class="comment-head">
                <strong>{{ comment.authorName }}</strong>
                <span class="comment-time">{{ comment.createdAt }}</span>
                <button
                  v-if="canDeleteComment(comment)"
                  type="button"
                  class="comment-delete"
                  title="删除评论"
                  @click="removeComment(comment)"
                >
                  删除
                </button>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
            </div>
          </div>
          <div v-if="!project.comments?.length" class="empty-block comment-empty">
            <el-icon :size="26" color="#c9d1dc"><ChatDotRound /></el-icon>
            <p>还没有评论，发起讨论吧</p>
          </div>
        </div>

        <div v-if="canComment" class="comment-editor">
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="补充资料说明、复核意见或协作提醒……"
            @keyup.ctrl.enter="submitComment"
          />
          <div class="comment-actions">
            <span class="small muted">Ctrl + Enter 发送</span>
            <el-button type="primary" :loading="commenting" :disabled="!commentText.trim()" @click="submitComment">
              发表评论
            </el-button>
          </div>
        </div>
        <div v-else class="readonly-tip">
          {{ auth.isViewer ? '只读观察员不能发表评论。' : '项目已归档，评论只读。' }}
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { collaborationApi, metaApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import { downloadWithAuth } from '../../utils/download'
import UserAvatar from '../UserAvatar.vue'

const props = defineProps({
  project: { type: Object, required: true }
})
const emit = defineEmits(['updated'])

const auth = useAuthStore()
const categories = ref(['招标文件', '资格材料', '标书文件', '回执凭证', '其他'])
const category = ref('招标文件')
const selectedFile = ref(null)
const fileInput = ref()
const uploading = ref(false)
const commentText = ref('')
const commenting = ref(false)
const accept = '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.png,.jpg,.jpeg'

const canUpload = computed(() => props.project.permissions?.operate && props.project.status !== 'ARCHIVED')
const canDelete = computed(() => props.project.permissions?.operate && props.project.status !== 'ARCHIVED')
const canComment = computed(
  () => !auth.isViewer && props.project.status !== 'ARCHIVED' && props.project.permissions?.viewAudit
)

onMounted(async () => {
  try {
    const options = await metaApi.options()
    if (options.attachmentCategories?.length) {
      categories.value = options.attachmentCategories
      category.value = options.attachmentCategories[0]
    }
  } catch (e) {
    // 使用默认分类
  }
})

function categoryType(value) {
  if (value === '招标文件') return 'danger'
  if (value === '资格材料') return 'warning'
  if (value === '标书文件') return 'primary'
  if (value === '回执凭证') return 'success'
  return 'info'
}

function formatSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function onFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 20MB')
    return
  }
  selectedFile.value = file
}

function clearFile() {
  selectedFile.value = null
}

async function upload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('category', category.value)
    const updated = await collaborationApi.uploadAttachment(props.project.id, formData)
    ElMessage.success('文件已上传')
    selectedFile.value = null
    emit('updated', updated)
  } finally {
    uploading.value = false
  }
}

async function download(attachment) {
  await downloadWithAuth(attachment.downloadUrl, attachment.filename)
}

async function removeAttachment(attachment) {
  try {
    await ElMessageBox.confirm(`确定删除文件「${attachment.filename}」吗？`, '删除文件', { type: 'warning' })
  } catch (e) {
    return
  }
  const updated = await collaborationApi.deleteAttachment(props.project.id, attachment.id)
  ElMessage.success('文件已删除')
  emit('updated', updated)
}

async function submitComment() {
  const content = commentText.value.trim()
  if (!content) return
  commenting.value = true
  try {
    const updated = await collaborationApi.addComment(props.project.id, content)
    commentText.value = ''
    ElMessage.success('评论已发表')
    emit('updated', updated)
  } finally {
    commenting.value = false
  }
}

function canDeleteComment(comment) {
  if (props.project.status === 'ARCHIVED') return false
  return auth.isAdmin || comment.authorId === auth.user?.id
}

async function removeComment(comment) {
  try {
    await ElMessageBox.confirm('删除后不可恢复，是否继续？', '删除评论', { type: 'warning' })
  } catch (e) {
    return
  }
  const updated = await collaborationApi.deleteComment(props.project.id, comment.id)
  ElMessage.success('评论已删除')
  emit('updated', updated)
}
</script>

<style scoped>
.collab-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.file-panel,
.comment-panel {
  min-width: 0;
}

.upload-box {
  background: #f8f9fb;
  border: 1px dashed var(--line);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 14px;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.category-select {
  width: 110px;
}

.file-picker {
  flex: 1;
  min-width: 160px;
  height: 32px;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 6px;
  font-size: 11px;
  color: var(--ink-2);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.file-picker:hover {
  border-color: #e2b6b8;
}

.hidden-file {
  display: none;
}

.upload-hint {
  margin-top: 8px;
  font-size: 10px;
  color: var(--muted);
}

.readonly-tip {
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8f9fb;
  color: var(--muted);
  font-size: 11px;
  margin-bottom: 10px;
}

.file-table {
  margin-top: 2px;
}

.file-name {
  border: none;
  background: transparent;
  color: var(--brand);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  text-align: left;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-name:hover {
  text-decoration: underline;
}

.comment-list {
  max-height: 430px;
  overflow: auto;
  padding-right: 4px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--line-soft);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-head strong {
  font-size: 11px;
}

.comment-time {
  font-size: 10px;
  color: var(--muted);
}

.comment-delete {
  margin-left: auto;
  border: none;
  background: transparent;
  color: #b7c0cc;
  font-size: 10px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s;
}

.comment-item:hover .comment-delete {
  opacity: 1;
}

.comment-delete:hover {
  color: var(--danger);
}

.comment-content {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--ink-2);
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-empty {
  padding: 24px 10px;
}

.comment-editor {
  border-top: 1px solid var(--line);
  padding-top: 12px;
  margin-top: 8px;
}

.comment-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

@media (max-width: 1100px) {
  .collab-grid {
    grid-template-columns: 1fr;
  }
}
</style>
