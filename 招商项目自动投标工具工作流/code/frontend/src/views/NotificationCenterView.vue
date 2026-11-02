<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">消息中心</h1>
        <p class="page-subtitle">复核任务、复核结果、截止提醒、评论与项目状态变更都汇聚在这里。</p>
      </div>
      <div class="page-actions">
        <el-button :disabled="notifyStore.unread === 0" @click="markAll">全部已读</el-button>
        <el-button type="primary" plain @click="goProfile">通知偏好</el-button>
      </div>
    </div>

    <div class="panel filter-panel">
      <div class="filters">
        <el-radio-group v-model="filter.unreadOnly" @change="search">
          <el-radio-button :label="false">全部</el-radio-button>
          <el-radio-button :label="true">未读</el-radio-button>
        </el-radio-group>
        <el-select v-model="filter.type" placeholder="全部类型" clearable class="type-select" @change="search">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <span class="small muted">共 {{ total }} 条 · 未读 {{ notifyStore.unread }} 条</span>
      </div>
    </div>

    <div class="panel list-panel" v-loading="loading">
      <div v-if="items.length" class="message-list">
        <div
          v-for="item in items"
          :key="item.id"
          class="message-item"
          :class="{ unread: !item.read }"
          @click="open(item)"
        >
          <div class="message-icon" :class="'type-' + item.type">
            <el-icon :size="18"><component :is="iconOf(item.type)" /></el-icon>
          </div>
          <div class="message-body">
            <div class="message-head">
              <span class="message-title">{{ item.title }}</span>
              <el-tag size="small" effect="plain" :type="tagType(item.type)">{{ typeText(item.type) }}</el-tag>
              <span class="message-time">{{ item.createdAt }}</span>
            </div>
            <div class="message-content">{{ item.content }}</div>
            <div class="message-meta">
              <span v-if="item.projectCode" class="mono">{{ item.projectCode }}</span>
              <span v-if="!item.read" class="unread-mark">未读</span>
            </div>
          </div>
          <div class="message-actions">
            <el-tooltip v-if="!item.read" content="标记已读" placement="top">
              <el-button circle text type="primary" size="small" aria-label="标记已读" @click.stop="markRead(item)">
                <el-icon><Check /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="删除消息" placement="top">
              <el-button circle text type="danger" size="small" aria-label="删除消息" @click.stop="remove(item)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </div>

      <div v-else class="empty-block">
        <el-icon :size="32" color="#c9d1dc"><Bell /></el-icon>
        <p>{{ filter.unreadOnly ? '没有未读消息' : '暂无消息' }}</p>
        <span class="small muted">有新的复核任务或项目动态时会出现在这里。</span>
      </div>

      <div class="pager" v-if="total > size">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { notificationApi } from '../api'
import { useNotificationStore } from '../stores/notifications'

const router = useRouter()
const notifyStore = useNotificationStore()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 20

const filter = reactive({
  unreadOnly: false,
  type: ''
})

const typeOptions = [
  { label: '待复核任务', value: 'REVIEW_TASK' },
  { label: '复核结果', value: 'REVIEW_RESULT' },
  { label: '截止提醒', value: 'DEADLINE' },
  { label: '项目评论', value: 'COMMENT' },
  { label: '项目文件', value: 'ATTACHMENT' },
  { label: '状态变更', value: 'STATUS' },
  { label: '系统通知', value: 'SYSTEM' }
]

function typeText(type) {
  return typeOptions.find((item) => item.value === type)?.label || type
}

function iconOf(type) {
  const map = {
    REVIEW_TASK: 'Stamp',
    REVIEW_RESULT: 'CircleCheck',
    DEADLINE: 'Clock',
    COMMENT: 'ChatDotRound',
    ATTACHMENT: 'Paperclip',
    STATUS: 'Refresh',
    SYSTEM: 'InfoFilled'
  }
  return map[type] || 'Bell'
}

function tagType(type) {
  if (type === 'DEADLINE') return 'danger'
  if (type === 'REVIEW_RESULT') return 'success'
  if (type === 'REVIEW_TASK') return 'warning'
  if (type === 'COMMENT') return 'primary'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await notificationApi.list({
      page: page.value,
      size,
      unreadOnly: filter.unreadOnly || undefined,
      type: filter.type || undefined
    })
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

async function markRead(item) {
  await notificationApi.read(item.id)
  item.read = true
  notifyStore.refreshUnread()
}

async function markAll() {
  const data = await notificationApi.readAll()
  ElMessage.success(data.count > 0 ? `已标记 ${data.count} 条为已读` : '没有未读消息')
  notifyStore.refreshUnread()
  load()
}

async function remove(item) {
  try {
    await ElMessageBox.confirm('删除后不可恢复，是否继续？', '删除消息', { type: 'warning' })
  } catch (e) {
    return
  }
  await notificationApi.remove(item.id)
  ElMessage.success('消息已删除')
  notifyStore.refreshUnread()
  load()
}

async function open(item) {
  if (!item.read) await markRead(item)
  if (item.link) router.push(item.link)
}

function goProfile() {
  router.push('/profile')
}

onMounted(() => {
  load()
  notifyStore.refreshUnread()
})
</script>

<style scoped>
.filter-panel {
  padding: 14px 18px;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.type-select {
  width: 160px;
}

.list-panel {
  padding: 8px 20px 18px;
}

.message-list {
  display: flex;
  flex-direction: column;
}

.message-item {
  display: flex;
  gap: 14px;
  padding: 16px 4px;
  border-bottom: 1px solid var(--line-soft);
  cursor: pointer;
  transition: background 0.15s ease;
}

.message-item:last-child {
  border-bottom: none;
}

.message-item:hover {
  background: #fbfcfd;
}

.message-item.unread {
  background: #fffafa;
}

.message-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  flex: 0 0 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f4f7;
  color: var(--ink-2);
}

.message-icon.type-REVIEW_TASK {
  background: #fbf3e6;
  color: #a56a12;
}

.message-icon.type-REVIEW_RESULT {
  background: #eaf6ef;
  color: #2e8b57;
}

.message-icon.type-DEADLINE {
  background: #fdeeee;
  color: #c0392b;
}

.message-icon.type-COMMENT {
  background: #eef4fb;
  color: #2e6f9e;
}

.message-icon.type-ATTACHMENT {
  background: #f1ecfa;
  color: #6d4db3;
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.message-title {
  font-size: 13px;
  font-weight: 650;
}

.message-time {
  margin-left: auto;
  font-size: 10px;
  color: var(--muted);
}

.message-content {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--ink-2);
}

.message-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 10px;
  color: var(--muted);
}

.unread-mark {
  color: var(--brand);
  background: var(--brand-soft);
  border-radius: 4px;
  padding: 1px 6px;
}

.message-actions {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 2px;
  flex: 0 0 auto;
  align-self: flex-start;
  opacity: 0.55;
  transition: opacity 0.15s ease;
}

.message-item:hover .message-actions,
.message-item.unread .message-actions {
  opacity: 1;
}

.message-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.message-actions :deep(.el-button) {
  width: 28px;
  height: 28px;
  padding: 0;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

@media (max-width: 800px) {
  .message-time {
    width: 100%;
    margin-left: 0;
  }

  .message-actions {
    display: none;
  }
}
</style>
