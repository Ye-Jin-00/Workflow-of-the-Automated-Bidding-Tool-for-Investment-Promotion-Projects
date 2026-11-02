<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">成员管理</h1>
        <p class="page-subtitle">维护团队成员、角色与账号状态。停用或重置密码会立即使旧令牌失效。</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> 新增成员
        </el-button>
      </div>
    </div>

    <div class="panel filter-panel">
      <div class="filters">
        <el-input v-model="filter.keyword" placeholder="搜索姓名 / 账号 / 部门" clearable class="keyword" @keyup.enter="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filter.role" placeholder="全部角色" clearable class="role-select">
          <el-option v-for="role in roles" :key="role.code" :label="role.label" :value="role.code" />
        </el-select>
        <el-select v-model="filter.status" placeholder="全部状态" clearable class="status-select">
          <el-option label="启用" value="enabled" />
          <el-option label="停用" value="disabled" />
        </el-select>
        <el-button @click="load">刷新</el-button>
        <span class="small muted">共 {{ filteredUsers.length }} 位成员</span>
      </div>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="filteredUsers" style="width: 100%">
        <el-table-column label="成员" min-width="220">
          <template #default="{ row }">
            <div class="member-cell">
              <UserAvatar :name="row.displayName" :src="row.avatarUrl" :size="36" />
              <div class="member-copy">
                <div class="member-name">
                  {{ row.displayName }}
                  <el-tag v-if="row.id === auth.user?.id" size="small" effect="plain">我</el-tag>
                </div>
                <small class="mono">{{ row.username }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="roleType(row.role)" effect="light">{{ row.roleLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="部门 / 职位" min-width="170">
          <template #default="{ row }">
            <div class="dept-cell">
              <span>{{ row.department }}</span>
              <small class="muted">{{ row.title || '—' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" min-width="190">
          <template #default="{ row }">
            <div class="contact-cell">
              <span>{{ row.phone || '—' }}</span>
              <small class="muted">{{ row.email || '—' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="156" prop="lastLoginAt">
          <template #default="{ row }">{{ row.lastLoginAt || '从未登录' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :disabled="row.id === auth.user?.id"
              style="--el-switch-on-color: #2e8b57"
              @change="(value) => toggle(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text type="warning" size="small" @click="resetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="editing ? '编辑成员' : '新增成员'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-2">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="form.username" :disabled="editing" placeholder="小写字母开头，3-20 位" />
          </el-form-item>
          <el-form-item v-if="!editing" label="初始密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="至少 8 位，含字母与数字" />
          </el-form-item>
          <el-form-item label="姓名 / 昵称" prop="displayName">
            <el-input v-model="form.displayName" maxlength="20" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="form.role" :disabled="editing && editingId === auth.user?.id" style="width: 100%">
              <el-option v-for="role in roles" :key="role.code" :label="role.label" :value="role.code">
                <div class="role-option">
                  <strong>{{ role.label }}</strong>
                  <small>{{ role.description }}</small>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="部门" prop="department">
            <el-select v-model="form.department" filterable allow-create style="width: 100%">
              <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
            </el-select>
          </el-form-item>
          <el-form-item label="职位">
            <el-input v-model="form.title" maxlength="30" placeholder="如：高级投标经理" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" maxlength="11" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" maxlength="80" />
          </el-form-item>
        </div>
        <el-form-item v-if="editing" label="个人简介">
          <el-input v-model="form.bio" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editing ? '保存修改' : '创建成员' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialog" title="临时密码已生成" width="440px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" show-icon title="请通过安全渠道将临时密码告知成员，该密码只显示一次。" />
      <div class="temp-password">
        <span class="mono">{{ tempPassword }}</span>
        <el-button text type="primary" @click="copyPassword">复制</el-button>
      </div>
      <p class="small muted">成员使用临时密码登录后，建议立即在个人中心修改密码。</p>
      <template #footer>
        <el-button type="primary" @click="passwordDialog = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, metaApi } from '../api'
import { useAuthStore } from '../stores/auth'
import UserAvatar from '../components/UserAvatar.vue'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const users = ref([])
const departments = ref([])
const roles = ref([])
const dialog = ref(false)
const editing = ref(false)
const formRef = ref()
const passwordDialog = ref(false)
const tempPassword = ref('')
const editingId = ref('')

const filter = reactive({
  keyword: '',
  role: '',
  status: ''
})

const form = reactive({
  username: '',
  password: '',
  displayName: '',
  role: 'BID_MANAGER',
  department: '招商一部',
  title: '',
  phone: '',
  email: '',
  bio: ''
})

const rules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{2,19}$/, message: '小写字母开头，3-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度需为 8-32 位', trigger: 'blur' }
  ],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  department: [{ required: true, message: '请选择部门', trigger: 'change' }]
}

const filteredUsers = computed(() => users.value.filter((user) => {
  const keyword = filter.keyword.trim()
  const matchKeyword = !keyword || [user.displayName, user.username, user.department, user.title]
    .filter(Boolean).some((value) => value.includes(keyword))
  const matchRole = !filter.role || user.role === filter.role
  const matchStatus = !filter.status
    || (filter.status === 'enabled' && user.enabled)
    || (filter.status === 'disabled' && !user.enabled)
  return matchKeyword && matchRole && matchStatus
}))

function roleType(role) {
  const map = { ADMIN: 'danger', BID_MANAGER: 'primary', REVIEWER: 'warning', VIEWER: 'info' }
  return map[role] || 'info'
}

async function load() {
  loading.value = true
  try {
    users.value = await adminApi.users()
  } finally {
    loading.value = false
  }
}

async function loadMeta() {
  try {
    const options = await metaApi.options()
    departments.value = options.departments || []
    roles.value = options.roles || []
  } catch (e) {
    roles.value = [
      { code: 'BID_MANAGER', label: '投标经理', description: '' },
      { code: 'REVIEWER', label: '复核/风控', description: '' },
      { code: 'VIEWER', label: '只读观察员', description: '' }
    ]
  }
}

function resetForm() {
  Object.assign(form, {
    username: '',
    password: '',
    displayName: '',
    role: 'BID_MANAGER',
    department: departments.value[0] || '招商一部',
    title: '',
    phone: '',
    email: '',
    bio: ''
  })
}

function openCreate() {
  editing.value = false
  editingId.value = ''
  resetForm()
  dialog.value = true
}

function openEdit(row) {
  editing.value = true
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    password: '',
    displayName: row.displayName,
    role: row.role,
    department: row.department,
    title: row.title || '',
    phone: row.phone || '',
    email: row.email || '',
    bio: row.bio || ''
  })
  dialog.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await adminApi.update(editingId.value, {
        displayName: form.displayName,
        role: form.role,
        department: form.department,
        title: form.title,
        phone: form.phone,
        email: form.email,
        bio: form.bio
      })
      ElMessage.success('成员信息已更新')
    } else {
      await adminApi.create({ ...form })
      ElMessage.success('成员已创建')
    }
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row, value) {
  try {
    await adminApi.toggle(row.id)
    ElMessage.success(value ? '账号已启用' : '账号已停用，旧令牌立即失效')
  } catch (e) {
    row.enabled = !value
  }
}

async function resetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `将为 ${row.displayName} 生成新的临时密码，原密码与登录令牌立即失效。`,
      '重置密码',
      { type: 'warning', confirmButtonText: '生成临时密码' }
    )
  } catch (e) {
    return
  }
  const data = await adminApi.resetPassword(row.id)
  tempPassword.value = data.password
  passwordDialog.value = true
}

async function copyPassword() {
  try {
    await navigator.clipboard.writeText(tempPassword.value)
    ElMessage.success('临时密码已复制')
  } catch (e) {
    ElMessage.warning('浏览器不支持自动复制，请手动选择复制')
  }
}

onMounted(() => {
  load()
  loadMeta()
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
  gap: 10px;
  flex-wrap: wrap;
}

.keyword {
  width: 280px;
}

.role-select,
.status-select {
  width: 150px;
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.member-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
}

.member-copy small {
  font-size: 10px;
  color: var(--muted);
}

.dept-cell,
.contact-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 11px;
}

.contact-cell small,
.dept-cell small {
  font-size: 10px;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.role-option {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.role-option small {
  font-size: 10px;
  color: var(--muted);
}

.temp-password {
  margin: 18px 0;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f8f9fb;
  border: 1px dashed var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 20px;
  letter-spacing: 1px;
}

@media (max-width: 800px) {
  .keyword,
  .role-select,
  .status-select {
    width: 100%;
  }

  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
