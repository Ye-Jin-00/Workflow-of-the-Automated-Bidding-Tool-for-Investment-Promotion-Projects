<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <div class="brand-logo">
          <img :src="logoMark" alt="SINCIT" />
        </div>
        <div v-if="!collapsed" class="brand-text">
          <strong>SINCIT</strong>
          <span>行至集 · 招商智投</span>
        </div>
      </div>

      <div v-if="!collapsed" class="nav-caption">工作空间</div>
      <nav class="nav">
        <el-tooltip
          v-for="item in navItems"
          :key="item.path"
          :content="item.label"
          placement="right"
          :disabled="!collapsed"
        >
          <router-link :to="item.path" class="nav-item">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
            <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
            <span v-if="item.path === '/notifications' && notifyStore.unread > 0" class="nav-badge">
              {{ notifyStore.unread > 99 ? '99+' : notifyStore.unread }}
            </span>
          </router-link>
        </el-tooltip>
      </nav>

      <div class="sidebar-foot">
        <div v-if="!collapsed" class="foot-card">
          <div class="foot-title">自动投标工作流</div>
          <div class="foot-text">预审 → 抽取 → 测算 → 生成 → 复核 → 归档</div>
        </div>
        <div v-if="!collapsed" class="foot-company">
          <span>SINCIT · 行至集</span>
          <span class="version">v1.3</span>
        </div>
      </div>
    </aside>

    <div class="main-wrap">
      <header class="topbar">
        <div class="topbar-left">
          <button class="collapse-btn" type="button" @click="collapsed = !collapsed">
            <el-icon :size="18">
              <Expand v-if="collapsed" />
              <Fold v-else />
            </el-icon>
          </button>
          <div class="crumb">
            <span class="crumb-root">招商智投</span>
            <el-icon :size="12"><ArrowRight /></el-icon>
            <span class="crumb-current">{{ route.meta.title || '工作台' }}</span>
          </div>
          <el-input
            ref="searchInput"
            v-model="searchKeyword"
            class="global-search"
            placeholder="搜索项目 / 编号    Ctrl K"
            clearable
            @keyup.enter="submitSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>

        <div class="topbar-right">
          <el-tooltip content="消息中心" placement="bottom">
            <button class="icon-btn" type="button" @click="openNotifications">
              <el-badge :value="notifyStore.unread" :hidden="notifyStore.unread === 0" :max="99">
                <el-icon :size="19"><Bell /></el-icon>
              </el-badge>
            </button>
          </el-tooltip>
          <span class="date-text">{{ today }}</span>
          <el-divider direction="vertical" />
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-trigger">
              <UserAvatar :name="auth.displayName" :src="auth.avatarUrl" :size="34" shape="circle" />
              <div class="user-meta">
                <strong>{{ auth.displayName }}</strong>
                <small>{{ auth.roleLabel }}<template v-if="auth.department"> · {{ auth.department }}</template></small>
              </div>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div class="dropdown-user">
                    <strong>{{ auth.user?.username }}</strong>
                    <span>{{ auth.roleLabel }} / {{ auth.department }}</span>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="notifications">
                  <el-icon><Bell /></el-icon> 消息中心
                  <span v-if="notifyStore.unread" class="dropdown-badge">{{ notifyStore.unread }}</span>
                </el-dropdown-item>
                <el-dropdown-item command="help">
                  <el-icon><QuestionFilled /></el-icon> 帮助中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <el-drawer v-model="notifyDrawer" size="430px" :with-header="false" destroy-on-close>
      <div class="notify-head">
        <div>
          <div class="notify-title">消息中心</div>
          <div class="notify-sub">待处理 {{ notifyStore.unread }} 条 · 共 {{ notifyStore.total }} 条</div>
        </div>
        <div class="flex-center">
          <el-button text size="small" :disabled="notifyStore.unread === 0" @click="markAllRead">全部已读</el-button>
          <el-button text size="small" @click="goNotificationCenter">查看全部</el-button>
        </div>
      </div>

      <div class="notify-tabs">
        <button
          v-for="tab in notifyTabs"
          :key="tab.value"
          type="button"
          class="notify-tab"
          :class="{ active: notifyFilter === tab.value }"
          @click="switchNotifyTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-loading="notifyStore.loading" class="notify-list">
        <div
          v-for="item in notifyStore.items"
          :key="item.id"
          class="notify-item"
          :class="{ unread: !item.read }"
          @click="openNotification(item)"
        >
          <div class="notify-icon" :class="'type-' + item.type">
            <el-icon :size="17"><component :is="notificationIcon(item.type)" /></el-icon>
          </div>
          <div class="notify-body">
            <div class="notify-item-head">
              <span class="notify-item-title">
                <i v-if="!item.read" class="notify-unread-dot"></i>{{ item.title }}
              </span>
              <span class="notify-time">{{ item.createdAt }}</span>
            </div>
            <div class="notify-content">{{ item.content }}</div>
            <div class="notify-meta">
              <span v-if="item.projectCode" class="mono">{{ item.projectCode }}</span>
              <span class="notify-type-label">{{ item.typeLabel || typeText(item.type) }}</span>
            </div>
          </div>
          <button class="notify-delete" type="button" title="删除" @click.stop="removeNotification(item)">
            <el-icon :size="13"><Close /></el-icon>
          </button>
        </div>
        <div v-if="!notifyStore.loading && !notifyStore.items.length" class="empty-block">
          <el-icon :size="28" color="#c9d1dc"><Bell /></el-icon>
          <p>暂无消息</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import logoMark from '../assets/sincit-mark.png'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notifications'
import UserAvatar from '../components/UserAvatar.vue'
import { todayText } from '../utils/format'

const auth = useAuthStore()
const notifyStore = useNotificationStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const today = todayText()
const searchKeyword = ref('')
const searchInput = ref()
const notifyDrawer = ref(false)
const notifyFilter = ref('all')
let notifyTimer = null

const notifyTabs = [
  { label: '全部', value: 'all' },
  { label: '未读', value: 'unread' }
]

const allNav = [
  { path: '/dashboard', label: '工作台', icon: 'Odometer', roles: ['ADMIN', 'BID_MANAGER', 'REVIEWER', 'VIEWER'] },
  { path: '/projects', label: '投标项目', icon: 'Files', roles: ['ADMIN', 'BID_MANAGER', 'REVIEWER', 'VIEWER'] },
  { path: '/drafts', label: '草稿箱', icon: 'EditPen', roles: ['ADMIN', 'BID_MANAGER'] },
  { path: '/reviews', label: '复核中心', icon: 'Stamp', roles: ['ADMIN', 'REVIEWER'] },
  { path: '/notifications', label: '消息中心', icon: 'Bell', roles: ['ADMIN', 'BID_MANAGER', 'REVIEWER', 'VIEWER'] },
  { path: '/rules', label: '资格规则', icon: 'SetUp', roles: ['ADMIN'] },
  { path: '/admin/users', label: '成员管理', icon: 'UserFilled', roles: ['ADMIN'] },
  { path: '/admin/recycle-bin', label: '回收站', icon: 'Delete', roles: ['ADMIN'] },
  { path: '/audits', label: '审计日志', icon: 'Tickets', roles: ['ADMIN', 'REVIEWER', 'BID_MANAGER'] },
  { path: '/help', label: '帮助中心', icon: 'QuestionFilled', roles: ['ADMIN', 'BID_MANAGER', 'REVIEWER', 'VIEWER'] }
]

const navItems = computed(() => allNav.filter((item) => item.roles.includes(auth.user?.role)))

function notificationIcon(type) {
  switch (type) {
    case 'REVIEW_TASK': return 'Stamp'
    case 'REVIEW_RESULT': return 'CircleCheck'
    case 'DEADLINE': return 'Clock'
    case 'COMMENT': return 'ChatDotRound'
    case 'ATTACHMENT': return 'Paperclip'
    case 'STATUS': return 'Refresh'
    default: return 'InfoFilled'
  }
}

function typeText(type) {
  const map = {
    REVIEW_TASK: '待复核',
    REVIEW_RESULT: '复核结果',
    DEADLINE: '截止提醒',
    COMMENT: '项目评论',
    ATTACHMENT: '项目文件',
    STATUS: '状态变更',
    SYSTEM: '系统通知'
  }
  return map[type] || type
}

function submitSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  router.push({ path: '/projects', query: { keyword } })
}

function handleHotkey(event) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    searchInput.value?.focus()
  }
}

async function openNotifications() {
  notifyDrawer.value = true
  notifyFilter.value = 'all'
  await notifyStore.load(false)
  await notifyStore.refreshUnread()
}

async function switchNotifyTab(value) {
  notifyFilter.value = value
  await notifyStore.load(value === 'unread')
}

async function openNotification(item) {
  if (!item.read) {
    await notifyStore.markRead(item.id)
  }
  if (item.link) {
    notifyDrawer.value = false
    router.push(item.link)
  }
}

async function markAllRead() {
  const count = await notifyStore.markAll()
  ElMessage.success(count > 0 ? `已将 ${count} 条消息标记为已读` : '没有未读消息')
}

async function removeNotification(item) {
  try {
    await notifyStore.remove(item.id)
    ElMessage.success('消息已删除')
  } catch (e) {
    // 拦截器已提示
  }
}

function goNotificationCenter() {
  notifyDrawer.value = false
  router.push('/notifications')
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'notifications') {
    openNotifications()
  } else if (command === 'help') {
    router.push('/help')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (e) {
      return
    }
    try {
      await authApi.logout()
    } catch (e) {
      // 即使接口失败也清理本地登录态
    }
    auth.logout()
    ElMessage.success('已安全退出')
    router.replace('/login')
  }
}

onMounted(() => {
  notifyStore.refreshUnread()
  notifyTimer = setInterval(() => notifyStore.refreshUnread(), 60000)
  window.addEventListener('keydown', handleHotkey)
})

onUnmounted(() => {
  if (notifyTimer) clearInterval(notifyTimer)
  window.removeEventListener('keydown', handleHotkey)
})

watch(() => route.fullPath, () => {
  notifyStore.refreshUnread()
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  background: var(--bg);
  overflow: hidden;
}

.sidebar {
  width: 232px;
  flex: 0 0 232px;
  background: #18212c;
  color: #cfd6df;
  display: flex;
  flex-direction: column;
  transition: width 0.22s ease, flex-basis 0.22s ease;
  position: relative;
  z-index: 5;
}

.sidebar::after {
  content: "";
  position: absolute;
  top: 0;
  right: 0;
  width: 1px;
  height: 100%;
  background: rgba(255, 255, 255, 0.06);
}

.sidebar.collapsed {
  width: 76px;
  flex-basis: 76px;
}

.sidebar.collapsed .brand {
  padding: 0;
  justify-content: center;
}

.brand {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

.brand-logo {
  width: 46px;
  height: 46px;
  border-radius: 11px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 46px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.brand-logo img {
  width: 38px;
  height: 38px;
  object-fit: contain;
  display: block;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
}

.brand-text strong {
  color: #fff;
  font-size: 16px;
  letter-spacing: 1.6px;
}

.brand-text span {
  color: #8f9bab;
  font-size: 11px;
  letter-spacing: 0.6px;
}

.nav-caption {
  padding: 20px 22px 8px;
  color: #6c7889;
  font-size: 11px;
  letter-spacing: 2px;
}

.nav {
  padding: 4px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  overflow: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #aab5c2;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.18s ease;
  position: relative;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
}

.nav-item.router-link-active {
  background: linear-gradient(90deg, rgba(180, 35, 42, 0.22), rgba(180, 35, 42, 0.06));
  color: #fff;
  font-weight: 600;
}

.nav-item.router-link-active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 3px;
  background: #d84149;
}

.nav-label {
  flex: 1;
}

.nav-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #d84149;
  color: #fff;
  font-size: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.sidebar.collapsed .nav {
  padding: 10px 12px;
}

.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 12px 0;
}

.sidebar-foot {
  padding: 14px;
}

.foot-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  border-radius: 10px;
  padding: 12px;
}

.foot-title {
  font-size: 12px;
  color: #dfe5ec;
  font-weight: 600;
  margin-bottom: 6px;
}

.foot-text {
  font-size: 10px;
  line-height: 1.6;
  color: #7d8998;
}

.foot-company {
  margin-top: 12px;
  padding: 0 4px;
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #5f6b7a;
}

.version {
  color: #718093;
}

.main-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 64px;
  flex: 0 0 64px;
  background: #fff;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px 0 14px;
  gap: 18px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.collapse-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: var(--ink-2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.collapse-btn:hover {
  background: var(--line-soft);
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--muted);
  white-space: nowrap;
}

.crumb-current {
  color: var(--ink);
  font-weight: 600;
}

.global-search {
  width: 260px;
  margin-left: 10px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: var(--ink-2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover {
  background: #f7f8fa;
}

.date-text {
  font-size: 12px;
  color: var(--muted);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 8px;
}

.user-trigger:hover {
  background: #f7f8fa;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.user-meta strong {
  font-size: 13px;
  color: var(--ink);
}

.user-meta small {
  font-size: 11px;
  color: var(--muted);
}

.dropdown-user {
  display: flex;
  flex-direction: column;
  line-height: 1.6;
}

.dropdown-user strong {
  color: var(--ink);
}

.dropdown-user span {
  font-size: 11px;
  color: var(--muted);
}

.dropdown-badge {
  margin-left: auto;
  background: var(--brand-soft);
  color: var(--brand);
  border-radius: 10px;
  padding: 0 6px;
  font-size: 10px;
}

.content {
  flex: 1;
  overflow: auto;
}

.notify-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 2px 14px;
  border-bottom: 1px solid var(--line);
}

.notify-title {
  font-size: 18px;
  font-weight: 700;
}

.notify-sub {
  margin-top: 4px;
  font-size: 11px;
  color: var(--muted);
}

.notify-tabs {
  display: flex;
  gap: 6px;
  padding: 12px 0 8px;
}

.notify-tab {
  border: none;
  background: #f4f6f9;
  color: var(--ink-2);
  border-radius: 16px;
  padding: 4px 12px;
  font-size: 11px;
  cursor: pointer;
}

.notify-tab.active {
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 600;
}

.notify-list {
  min-height: 200px;
}

.notify-item {
  display: flex;
  gap: 10px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--line-soft);
  cursor: pointer;
  position: relative;
}

.notify-item:hover {
  background: #fbfcfd;
}

.notify-item.unread {
  background: #fffaf9;
}

.notify-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  flex: 0 0 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f4f7;
  color: var(--ink-2);
}

.notify-icon.type-REVIEW_TASK {
  background: #fbf3e6;
  color: #a56a12;
}

.notify-icon.type-REVIEW_RESULT {
  background: #eaf6ef;
  color: #2e8b57;
}

.notify-icon.type-DEADLINE {
  background: #fdeeee;
  color: #c0392b;
}

.notify-icon.type-COMMENT {
  background: #eef4fb;
  color: #2e6f9e;
}

.notify-icon.type-ATTACHMENT {
  background: #f1ecfa;
  color: #6d4db3;
}

.notify-body {
  flex: 1;
  min-width: 0;
}

.notify-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.notify-item-title {
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.notify-unread-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand);
  flex: 0 0 6px;
}

.notify-time {
  font-size: 10px;
  color: var(--muted);
  flex: 0 0 auto;
}

.notify-content {
  margin-top: 4px;
  font-size: 11px;
  line-height: 1.65;
  color: var(--muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notify-meta {
  margin-top: 5px;
  display: flex;
  gap: 10px;
  font-size: 10px;
  color: var(--muted);
}

.notify-type-label {
  color: var(--brand);
}

.notify-delete {
  position: absolute;
  right: 0;
  bottom: 10px;
  border: none;
  background: transparent;
  color: #b7c0cc;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.notify-item:hover .notify-delete {
  opacity: 1;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.16s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 1100px) {
  .global-search {
    width: 180px;
  }

  .date-text {
    display: none;
  }
}

@media (max-width: 860px) {
  .global-search {
    display: none;
  }

  .user-meta {
    display: none;
  }
}
</style>
