<template>
  <div class="page profile-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理个人资料、头像、登录安全与消息偏好。修改会记录在审计日志中。</p>
      </div>
      <div class="page-actions">
        <el-button @click="$router.push('/projects')">我的项目</el-button>
        <el-button type="primary" plain @click="$router.push('/help')">使用帮助</el-button>
      </div>
    </div>

    <section class="panel profile-hero">
      <div class="hero-avatar">
        <UserAvatar :name="profile.displayName" :src="profile.avatarUrl" :size="88" shape="circle" />
        <button class="avatar-edit" type="button" title="更换头像" @click="fileInput?.click()">
          <el-icon :size="14"><Camera /></el-icon>
        </button>
      </div>
      <div class="hero-copy">
        <div class="hero-name">
          {{ profile.displayName }}
          <el-tag size="small" effect="light">{{ profile.roleLabel }}</el-tag>
        </div>
        <div class="hero-title">{{ profile.title || '暂未填写职位' }} · {{ profile.department }}</div>
        <div class="hero-meta">
          <span><el-icon><User /></el-icon>{{ profile.username }}</span>
          <span><el-icon><Clock /></el-icon>最近登录 {{ profile.lastLoginAt || '—' }}</span>
          <span><el-icon><Calendar /></el-icon>加入于 {{ (profile.createdAt || '').slice(0, 10) }}</span>
        </div>
      </div>
      <div class="hero-actions">
        <el-button size="small" :loading="uploading" @click="fileInput?.click()">更换头像</el-button>
        <el-button v-if="profile.avatarUrl" size="small" text type="danger" @click="removeAvatar">恢复默认</el-button>
        <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/webp,image/gif" class="hidden-file" @change="onAvatarChange" />
      </div>
    </section>

    <div class="stat-grid">
      <div class="stat-card">
        <span>我负责的项目</span>
        <strong>{{ summary.myProjects || 0 }}</strong>
        <small>推进中 {{ summary.activeProjects || 0 }}</small>
      </div>
      <div class="stat-card">
        <span>已完成</span>
        <strong>{{ summary.completedProjects || 0 }}</strong>
        <small>已提交 / 已归档</small>
      </div>
      <div class="stat-card">
        <span>我关注的项目</span>
        <strong>{{ summary.followingProjects || 0 }}</strong>
        <small>接收状态与截止提醒</small>
      </div>
      <div class="stat-card">
        <span>未读消息</span>
        <strong>{{ summary.unreadNotifications || 0 }}</strong>
        <small><router-link to="/notifications" class="link">查看消息中心</router-link></small>
      </div>
    </div>

    <section class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本资料" name="basic">
          <div class="tab-layout">
            <div class="info-panel">
              <div class="section-head">
                <div>
                  <div class="section-title">个人资料</div>
                  <p class="small muted">昵称与头像会展示在评论、复核记录和审计日志中。</p>
                </div>
                <el-button type="primary" plain @click="openProfileDialog">
                  <el-icon><EditPen /></el-icon> 修改资料
                </el-button>
              </div>
              <div class="info-grid">
                <div class="info-item">
                  <small>昵称 / 姓名</small>
                  <strong>{{ profile.displayName || '未填写' }}</strong>
                </div>
                <div class="info-item">
                  <small>职位</small>
                  <strong>{{ profile.title || '未填写' }}</strong>
                </div>
                <div class="info-item">
                  <small>手机号</small>
                  <strong>{{ profile.phone || '未填写' }}</strong>
                </div>
                <div class="info-item">
                  <small>邮箱</small>
                  <strong>{{ profile.email || '未填写' }}</strong>
                </div>
                <div class="info-item info-item-wide">
                  <small>个人简介</small>
                  <p>{{ profile.bio || '暂未填写个人简介，可补充专业方向与负责领域。' }}</p>
                </div>
              </div>
            </div>

            <aside class="account-card">
              <div class="account-title">账号信息</div>
              <div class="account-row"><span>登录账号</span><strong class="mono">{{ profile.username }}</strong></div>
              <div class="account-row"><span>角色</span><strong>{{ profile.roleLabel }}</strong></div>
              <div class="account-row"><span>部门</span><strong>{{ profile.department }}</strong></div>
              <div class="account-row"><span>账号状态</span><el-tag size="small" type="success" effect="light">正常</el-tag></div>
              <div class="account-row"><span>创建时间</span><strong>{{ profile.createdAt || '—' }}</strong></div>
              <div class="account-note">账号、角色与部门由管理员维护；个人可修改昵称、职位、联系方式与简介。</div>
            </aside>
          </div>
        </el-tab-pane>

        <el-tab-pane label="登录安全" name="security">
          <div class="security-layout">
            <div class="info-panel">
              <div class="section-head">
                <div>
                  <div class="section-title">登录密码</div>
                  <p class="small muted">修改密码后，其他设备上的旧登录令牌会立即失效。</p>
                </div>
                <el-button type="primary" plain @click="openPasswordDialog">
                  <el-icon><Lock /></el-icon> 修改密码
                </el-button>
              </div>
              <div class="security-list">
                <div class="security-line">
                  <el-icon color="#2e8b57"><CircleCheck /></el-icon>
                  <div><strong>密码状态</strong><small>已设置，建议每 90 天更换一次</small></div>
                </div>
                <div class="security-line">
                  <el-icon color="#2e8b57"><CircleCheck /></el-icon>
                  <div><strong>令牌保护</strong><small>密码修改或重置后，旧设备自动下线</small></div>
                </div>
                <div class="security-line">
                  <el-icon color="#c47a14"><Warning /></el-icon>
                  <div><strong>登录保护</strong><small>连续失败 5 次将锁定 5 分钟</small></div>
                </div>
                <div class="security-line">
                  <el-icon color="#5b6b7c"><Clock /></el-icon>
                  <div><strong>最近登录</strong><small>{{ profile.lastLoginAt || '—' }}</small></div>
                </div>
              </div>
            </div>

            <aside class="account-card">
              <div class="account-title">安全建议</div>
              <div class="security-item">
                <el-icon color="#2e8b57"><CircleCheck /></el-icon>
                <div><strong>使用独立密码</strong><small>不要与其他系统共用同一密码</small></div>
              </div>
              <div class="security-item">
                <el-icon color="#c47a14"><Warning /></el-icon>
                <div><strong>定期更换</strong><small>建议每 90 天更新一次登录密码</small></div>
              </div>
              <div class="security-item">
                <el-icon color="#2e6f9e"><InfoFilled /></el-icon>
                <div><strong>异常及时反馈</strong><small>发现陌生登录请联系系统管理员</small></div>
              </div>
              <div class="account-note">当前账号：{{ profile.username }} · {{ profile.roleLabel }}</div>
            </aside>
          </div>
        </el-tab-pane>

        <el-tab-pane label="消息偏好" name="preferences">
          <div class="preference-head">
            <div>
              <div class="section-title">通知偏好</div>
              <p class="small muted">关闭后将不再接收对应类型的站内消息；截止提醒由系统定时任务生成。</p>
            </div>
            <el-button type="primary" :loading="savingPrefs" @click="savePreferences">保存偏好</el-button>
          </div>
          <div class="preference-list">
            <div v-for="item in preferenceItems" :key="item.key" class="preference-item">
              <div class="preference-icon"><el-icon :size="17"><component :is="item.icon" /></el-icon></div>
              <div class="preference-copy">
                <strong>{{ item.title }}</strong>
                <small>{{ item.desc }}</small>
              </div>
              <el-switch v-model="preferences[item.key]" />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的动态" name="activity">
          <div class="activity-head">
            <span class="small muted">最近 20 条操作记录</span>
            <el-button v-if="!auth.isViewer" text type="primary" size="small" @click="$router.push('/audits')">查看全部审计</el-button>
          </div>
          <el-timeline v-if="activity.length" class="activity-timeline">
            <el-timeline-item
              v-for="log in activity"
              :key="log.id"
              :timestamp="log.createdAt"
              :color="activityColor(log.action)"
              placement="top"
            >
              <div class="activity-title">
                {{ log.actionLabel }}
                <span v-if="log.projectCode" class="mono activity-code" @click="openProject(log.projectId)">{{ log.projectCode }}</span>
              </div>
              <div class="activity-detail">{{ log.detail }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="empty-block">暂无操作记录</div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="profileDialogVisible" title="修改个人资料" width="580px" append-to-body
      :close-on-click-modal="false">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top">
        <div class="grid-2">
          <el-form-item label="昵称 / 姓名" prop="displayName">
            <el-input v-model="profileForm.displayName" maxlength="20" show-word-limit />
          </el-form-item>
          <el-form-item label="职位">
            <el-input v-model="profileForm.title" maxlength="30" placeholder="如：高级投标经理" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" maxlength="11" placeholder="用于联系与通知" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" maxlength="80" placeholder="name@company.com" />
          </el-form-item>
        </div>
        <el-form-item label="个人简介">
          <el-input v-model="profileForm.bio" type="textarea" :rows="4" maxlength="200" show-word-limit
            placeholder="介绍你的专业方向与负责领域" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改登录密码" width="500px" append-to-body
      :close-on-click-modal="false">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <div v-if="passwordForm.newPassword" class="strength-row">
          <div class="strength-bar">
            <i :style="{ width: passwordStrength.score + '%', background: passwordStrength.color }"></i>
          </div>
          <span :style="{ color: passwordStrength.color }">{{ passwordStrength.label }}</span>
        </div>
        <div class="password-hint">8-32 位，至少包含字母与数字；修改成功后旧登录令牌立即失效，当前页面会自动续签。</div>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="changingPassword" @click="changePassword">修改密码</el-button>
      </template>
    </el-dialog>

    <AvatarCropper v-model="cropperVisible" :file="avatarFile" @confirm="uploadCroppedAvatar" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { profileApi } from '../api'
import { useAuthStore } from '../stores/auth'
import UserAvatar from '../components/UserAvatar.vue'
import AvatarCropper from '../components/AvatarCropper.vue'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(true)
const savingProfile = ref(false)
const uploading = ref(false)
const changingPassword = ref(false)
const savingPrefs = ref(false)
const activeTab = ref('basic')
const profileFormRef = ref()
const passwordFormRef = ref()
const profileDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const cropperVisible = ref(false)
const avatarFile = ref(null)
const fileInput = ref()
const activity = ref([])
const summary = ref({})
const profile = reactive({ ...(auth.user || {}) })

const profileForm = reactive({
  displayName: auth.user?.displayName || '',
  title: auth.user?.title || '',
  phone: auth.user?.phone || '',
  email: auth.user?.email || '',
  bio: auth.user?.bio || ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const preferences = reactive({
  reviewTask: true,
  reviewResult: true,
  comment: true,
  deadline: true,
  status: true
})

const profileRules = {
  displayName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度需为 2-20 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度需为 8-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const passwordStrength = computed(() => {
  const value = passwordForm.newPassword || ''
  let score = 0
  if (value.length >= 8) score += 30
  if (value.length >= 12) score += 10
  if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score += 20
  if (/\d/.test(value)) score += 20
  if (/[^A-Za-z0-9]/.test(value)) score += 20
  score = Math.min(score, 100)
  if (score < 45) return { score, label: '弱', color: '#c0392b' }
  if (score < 75) return { score, label: '中等', color: '#c47a14' }
  return { score, label: '强', color: '#2e8b57' }
})

const preferenceItems = [
  { key: 'reviewTask', title: '待复核任务', desc: '有项目提交复核时通知我', icon: 'Stamp' },
  { key: 'reviewResult', title: '复核结果', desc: '我负责的项目通过或被退回时通知我', icon: 'CircleCheck' },
  { key: 'comment', title: '项目评论', desc: '我负责或关注的项目有新评论时通知我', icon: 'ChatDotRound' },
  { key: 'deadline', title: '截止提醒', desc: '距投标截止 3 天内提醒我', icon: 'Clock' },
  { key: 'status', title: '状态变更', desc: '我关注的项目状态变化时通知我', icon: 'Refresh' }
]

function applyProfile(data) {
  Object.assign(profile, data)
  Object.assign(profileForm, {
    displayName: data.displayName || '',
    title: data.title || '',
    phone: data.phone || '',
    email: data.email || '',
    bio: data.bio || ''
  })
  Object.assign(preferences, {
    reviewTask: data.notificationPrefs?.reviewTask !== false,
    reviewResult: data.notificationPrefs?.reviewResult !== false,
    comment: data.notificationPrefs?.comment !== false,
    deadline: data.notificationPrefs?.deadline !== false,
    status: data.notificationPrefs?.status !== false
  })
}

async function load() {
  loading.value = true
  try {
    const [data, stats, logs] = await Promise.all([
      profileApi.view(),
      profileApi.summary(),
      profileApi.activity()
    ])
    applyProfile(data)
    auth.setUser(data)
    summary.value = stats
    activity.value = logs
  } finally {
    loading.value = false
  }
}

function openProfileDialog() {
  applyProfile(profile)
  profileDialogVisible.value = true
}

function openPasswordDialog() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

async function saveProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingProfile.value = true
  try {
    const data = await profileApi.update({ ...profileForm })
    applyProfile(data)
    auth.setUser(data)
    profileDialogVisible.value = false
    ElMessage.success('个人资料已保存')
  } finally {
    savingProfile.value = false
  }
}

function resetProfileForm() {
  applyProfile(profile)
}

function onAvatarChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 3 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 3MB')
    return
  }
  avatarFile.value = file
  cropperVisible.value = true
}

async function uploadCroppedAvatar(blob) {
  if (!blob) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', new File([blob], 'avatar.png', { type: 'image/png' }))
    const data = await profileApi.uploadAvatar(formData)
    applyProfile(data)
    auth.setUser(data)
    ElMessage.success('头像已更换')
  } finally {
    uploading.value = false
  }
}

async function removeAvatar() {
  try {
    await ElMessageBox.confirm('将恢复为默认文字头像，是否继续？', '恢复默认头像', { type: 'warning' })
  } catch (e) {
    return
  }
  const data = await profileApi.removeAvatar()
  applyProfile(data)
  auth.setUser(data)
  ElMessage.success('已恢复默认头像')
}

async function changePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  changingPassword.value = true
  try {
    const data = await profileApi.changePassword({ ...passwordForm })
    auth.updateToken(data.token, data.expireAt)
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordDialogVisible.value = false
    ElMessage.success('密码已修改，当前会话已自动续签')
  } finally {
    changingPassword.value = false
  }
}

async function savePreferences() {
  savingPrefs.value = true
  try {
    const data = await profileApi.updatePreferences({ ...preferences })
    Object.assign(preferences, data)
    profile.notificationPrefs = data
    auth.setUser({ ...auth.user, notificationPrefs: data })
    ElMessage.success('消息偏好已保存')
  } finally {
    savingPrefs.value = false
  }
}

function openProject(projectId) {
  if (projectId) router.push(`/projects/${projectId}`)
}

function activityColor(action) {
  if (action?.includes('PASSWORD') || action?.includes('PROFILE') || action?.includes('AVATAR')) return '#b4232a'
  if (action?.includes('LOGIN') || action?.includes('LOGOUT')) return '#2e6f9e'
  return '#2e8b57'
}

onMounted(load)
</script>

<style scoped>
.profile-hero {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 24px 26px;
}

.hero-avatar {
  position: relative;
  flex: 0 0 auto;
}

.avatar-edit {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: var(--brand);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.hero-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 750;
}

.hero-title {
  margin-top: 5px;
  font-size: 12px;
  color: var(--muted);
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
  font-size: 11px;
  color: var(--muted);
}

.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.hidden-file {
  display: none;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin: 16px 0;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 16px 18px;
  box-shadow: var(--shadow);
}

.stat-card span {
  font-size: 11px;
  color: var(--muted);
}

.stat-card strong {
  display: block;
  margin: 6px 0 4px;
  font-size: 26px;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.stat-card small {
  font-size: 10px;
  color: var(--muted);
}

.link {
  color: var(--brand);
}

.tab-layout,
.security-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) 320px;
  gap: 26px;
  padding: 6px 2px;
  align-items: start;
}

.form-col {
  min-width: 0;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}

.info-panel {
  min-width: 0;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line-soft);
  margin-bottom: 16px;
}

.section-head p {
  margin: 4px 0 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-item {
  padding: 12px 14px;
  border-radius: 9px;
  background: #f8f9fb;
  border: 1px solid var(--line-soft);
}

.info-item small {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-bottom: 5px;
}

.info-item strong {
  font-size: 12px;
  font-weight: 600;
  color: var(--ink);
  word-break: break-all;
}

.info-item p {
  margin: 0;
  font-size: 11px;
  line-height: 1.75;
  color: var(--ink-2);
}

.info-item-wide {
  grid-column: span 2;
}

.security-list {
  display: flex;
  flex-direction: column;
}

.security-line {
  display: flex;
  gap: 10px;
  padding: 13px 2px;
  border-bottom: 1px dashed var(--line);
}

.security-line strong {
  display: block;
  font-size: 11px;
  margin-bottom: 3px;
}

.security-line small {
  font-size: 10px;
  color: var(--muted);
}

.account-card {
  background: #f8f9fb;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 16px;
}

.account-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 12px;
}

.account-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 11px;
  color: var(--muted);
}

.account-row strong {
  font-size: 11px;
  color: var(--ink);
  font-weight: 600;
  text-align: right;
}

.account-note {
  margin-top: 12px;
  font-size: 10px;
  line-height: 1.7;
  color: var(--muted);
}

.strength-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: -4px 0 10px;
  font-size: 10px;
}

.strength-bar {
  flex: 1;
  height: 5px;
  border-radius: 4px;
  background: #eef1f5;
  overflow: hidden;
}

.strength-bar i {
  display: block;
  height: 100%;
  border-radius: 4px;
  transition: width 0.2s ease;
}

.password-hint {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8f9fb;
  font-size: 10px;
  line-height: 1.7;
  color: var(--muted);
}

.security-item {
  display: flex;
  gap: 9px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
}

.security-item strong {
  display: block;
  font-size: 11px;
  margin-bottom: 2px;
}

.security-item small {
  font-size: 10px;
  color: var(--muted);
  line-height: 1.5;
}

.preference-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 6px 0 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
}

.preference-list {
  display: flex;
  flex-direction: column;
}

.preference-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 4px;
  border-top: 1px solid var(--line-soft);
}

.preference-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: var(--brand-soft);
  color: var(--brand);
  display: flex;
  align-items: center;
  justify-content: center;
}

.preference-copy {
  flex: 1;
}

.preference-copy strong {
  display: block;
  font-size: 12px;
  margin-bottom: 3px;
}

.preference-copy small {
  font-size: 10px;
  color: var(--muted);
}

.activity-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0 14px;
}

.activity-timeline {
  padding: 4px 4px 0;
}

.activity-title {
  font-size: 12px;
  font-weight: 600;
}

.activity-code {
  margin-left: 8px;
  font-size: 10px;
  color: var(--brand);
  cursor: pointer;
}

.activity-detail {
  margin-top: 4px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--muted);
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 1100px) {
  .tab-layout,
  .security-layout {
    grid-template-columns: 1fr;
  }

  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 700px) {
  .profile-hero {
    flex-wrap: wrap;
  }

  .grid-2,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item-wide {
    grid-column: span 1;
  }

  .section-head {
    flex-direction: column;
  }
}
</style>
