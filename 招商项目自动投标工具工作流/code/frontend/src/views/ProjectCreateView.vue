<template>
  <div class="page">
    <div class="page-head">
      <div>
        <div class="back-line">
          <el-button text @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
          <span class="small muted">/ {{ isEdit ? '编辑项目' : '新建项目' }}</span>
        </div>
        <h1 class="page-title">{{ isEdit ? '编辑投标项目' : '新建投标项目' }}</h1>
        <p class="page-subtitle">
          {{ isEdit ? '仅在草稿或资格驳回阶段可修改基础信息，进入后续环节后信息自动冻结。' : '一次录入项目信息与资格画像，保存后即可发起资格预审。' }}
        </p>
      </div>
      <div class="page-actions">
        <el-button v-if="!isEdit" @click="batchVisible = true">
          <el-icon><Upload /></el-icon> 批量导入
        </el-button>
        <el-button @click="goBack">取消</el-button>
        <el-button :loading="saving" @click="save(false)">{{ isEdit ? '保存修改' : '保存草稿' }}</el-button>
        <el-button v-if="!isEdit" type="primary" :loading="saving" @click="save(true)">保存并发起预审</el-button>
      </div>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" v-loading="loading">
      <div class="form-layout">
        <div class="form-main">
          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">基本信息 <small>用于项目台账与数据权限</small></div>
            </div>
            <div class="grid-2">
              <el-form-item label="项目名称" prop="name" class="span-2">
                <el-input v-model="form.name" maxlength="120" show-word-limit placeholder="如：泛悦城市广场 T2 写字楼招商代理服务" />
              </el-form-item>
              <el-form-item label="招标人 / 委托方" prop="clientName">
                <el-input v-model="form.clientName" maxlength="100" placeholder="项目业主或采购单位" />
              </el-form-item>
              <el-form-item label="项目载体">
                <el-input v-model="form.carrierName" maxlength="100" placeholder="如：泛悦城市广场 T2" />
              </el-form-item>
              <el-form-item label="所属产业">
                <el-select v-model="form.industry" filterable allow-create default-first-option placeholder="选择或输入产业方向" style="width: 100%">
                  <el-option v-for="item in options.industries" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="线索来源">
                <el-select v-model="form.source" placeholder="选择来源" style="width: 100%">
                  <el-option v-for="item in options.sources" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="归属部门">
                <el-select v-model="form.department" :disabled="!isAdmin" placeholder="选择部门" style="width: 100%">
                  <el-option v-for="item in options.departments" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="canAssignOwner" label="项目负责人">
                <el-select v-model="form.ownerId" clearable placeholder="默认由本人负责" style="width: 100%">
                  <el-option v-for="user in users" :key="user.id" :label="`${user.displayName}（${user.department}）`" :value="user.id" />
                </el-select>
              </el-form-item>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">招标信息 <small>金额单位：万元</small></div>
            </div>
            <div class="grid-2">
              <el-form-item label="投标截止时间" prop="deadline">
                <el-date-picker
                  v-model="form.deadline"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  format="YYYY-MM-DD HH:mm"
                  placeholder="选择截止时间"
                  style="width: 100%"
                />
              </el-form-item>
              <div class="grid-2 inner">
                <el-form-item label="招标预算 / 最高限价" prop="budgetAmount">
                  <el-input-number v-model="form.budgetAmount" :min="0" :precision="2" :step="10" controls-position="right" style="width: 100%" />
                </el-form-item>
                <el-form-item label="投标保证金">
                  <el-input-number v-model="form.depositAmount" :min="0" :precision="2" :step="1" controls-position="right" style="width: 100%" />
                </el-form-item>
              </div>
              <el-form-item label="项目背景与招商诉求" class="span-2">
                <el-input
                  v-model="form.projectBrief"
                  type="textarea"
                  :rows="3"
                  maxlength="1000"
                  show-word-limit
                  placeholder="项目区位、载体体量、产业定位、去化目标、招标人关注点等"
                />
              </el-form-item>
              <el-form-item label="招标文件正文（可选）" class="span-2">
                <el-input
                  v-model="form.tenderText"
                  type="textarea"
                  :rows="7"
                  maxlength="20000"
                  placeholder="粘贴招标公告/招标文件中的资格要求、服务要求、商务条款与评标办法，供要素抽取使用"
                />
                <div class="field-hint">
                  支持直接粘贴文本；PDF/OCR 解析可在生产环境接入同一抽取接口。
                  <el-button text type="primary" size="small" @click="fillSampleTender">填入示例文本</el-button>
                </div>
              </el-form-item>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">资格预审画像 <small>用于自动预审与企业能力匹配</small></div>
              <el-tag effect="plain" type="info" size="small">演示口径</el-tag>
            </div>
            <div class="grid-3">
              <el-form-item label="注册资本（万元）">
                <el-input-number v-model="form.profile.registeredCapital" :min="0" :precision="2" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="成立年份">
                <el-input-number v-model="form.profile.establishedYear" :min="1980" :max="2030" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="信用等级">
                <el-select v-model="form.profile.creditLevel" style="width: 100%">
                  <el-option v-for="level in ['AAA', 'AA', 'A', 'BBB', 'C']" :key="level" :label="level" :value="level" />
                </el-select>
              </el-form-item>
              <el-form-item label="近三年同类项目（个）">
                <el-input-number v-model="form.profile.similarProjectCount" :min="0" :max="999" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="同类项目合同额（万元）">
                <el-input-number v-model="form.profile.similarProjectAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="可投入团队（人）">
                <el-input-number v-model="form.profile.teamSize" :min="0" :max="999" controls-position="right" style="width: 100%" />
              </el-form-item>
            </div>
            <el-form-item label="资质 / 备案 / 认证">
              <el-select
                v-model="form.profile.qualifications"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="选择或输入资质名称"
                style="width: 100%"
              >
                <el-option v-for="item in qualificationOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <div class="switch-grid">
              <div class="switch-item">
                <div>
                  <strong>可缴纳投标保证金</strong>
                  <small>资金或保函已落实</small>
                </div>
                <el-switch v-model="form.profile.bidBondAvailable" />
              </div>
              <div class="switch-item">
                <div>
                  <strong>具备政府 / 国资服务经验</strong>
                  <small>影响本地资源评分</small>
                </div>
                <el-switch v-model="form.profile.hasGovernmentClient" />
              </div>
              <div class="switch-item">
                <div>
                  <strong>存在失信 / 黑名单记录</strong>
                  <small>命中即一票否决</small>
                </div>
                <el-switch v-model="form.profile.blacklisted" style="--el-switch-on-color: #c0392b" />
              </div>
              <div class="switch-item">
                <div>
                  <strong>存在关联关系 / 利益冲突</strong>
                  <small>需回避或取得书面同意</small>
                </div>
                <el-switch v-model="form.profile.conflictsOfInterest" style="--el-switch-on-color: #c0392b" />
              </div>
            </div>
            <div class="grid-2" style="margin-top: 16px">
              <el-form-item label="财务状况">
                <el-select v-model="form.profile.financialStatus" style="width: 100%">
                  <el-option v-for="item in ['盈利', '持平', '亏损']" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="form.profile.notes" maxlength="300" placeholder="数据口径、特殊情况说明" />
              </el-form-item>
            </div>
          </section>
        </div>

        <aside class="form-side">
          <div class="panel side-panel">
            <div class="panel-title">推进路径</div>
            <div class="flow-list">
              <div class="flow-item">
                <span class="flow-index active">1</span>
                <div><strong>录入项目</strong><small>填写基础信息与资格画像</small></div>
              </div>
              <div class="flow-item">
                <span class="flow-index">2</span>
                <div><strong>资格预审</strong><small>规则自动判定，不满足即驳回</small></div>
              </div>
              <div class="flow-item">
                <span class="flow-index">3</span>
                <div><strong>抽取与测算</strong><small>要素抽取、匹配评分、报价</small></div>
              </div>
              <div class="flow-item">
                <span class="flow-index">4</span>
                <div><strong>初稿与复核</strong><small>AI 生成，人工复核把关</small></div>
              </div>
              <div class="flow-item">
                <span class="flow-index">5</span>
                <div><strong>提交归档</strong><small>回执留痕，材料归档</small></div>
              </div>
            </div>
          </div>

          <div class="panel side-panel" v-if="!isEdit">
            <div class="panel-title">预审提示</div>
            <p class="side-text">
              保存并发起预审后，系统将立即按当前 10 条规则判定。
              必填规则不满足会<strong>自动驳回</strong>，可在项目详情中修正画像后重新预审。
            </p>
            <el-alert type="info" :closable="false" show-icon title="资格数据请以营业执照、资质证书、审计报告为准。" />
          </div>

          <div class="panel side-panel">
            <div class="panel-title">数据安全</div>
            <p class="side-text">
              项目仅对负责人、本部门与复核/管理员可见；所有推进动作记录操作人、IP 与追踪号。
            </p>
          </div>
        </aside>
      </div>
    </el-form>

    <div class="bottom-actions">
      <div class="bottom-hint">
        <el-icon><InfoFilled /></el-icon>
        保存草稿只会进入草稿箱；发起预审后才会进入正式投标项目流程。
      </div>
      <div class="bottom-buttons">
        <el-button :loading="saving" @click="save(false)">{{ isEdit ? '保存修改' : '保存草稿' }}</el-button>
        <el-button v-if="!isEdit" type="primary" :loading="saving" @click="save(true)">保存并发起预审</el-button>
      </div>
    </div>

    <BatchImportDialog v-model="batchVisible" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { metaApi, projectApi, workflowApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { downloadWithAuth } from '../utils/download'
import { money } from '../utils/format'
import BatchImportDialog from '../components/BatchImportDialog.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const isEdit = computed(() => Boolean(route.params.id))
const isAdmin = computed(() => auth.isAdmin)
const canAssignOwner = computed(() => ['ADMIN', 'BID_MANAGER'].includes(auth.user?.role))
const users = ref([])
const batchVisible = ref(false)
const importing = ref(false)
const downloadingTemplate = ref(false)
const importResult = ref(null)
const importFileInput = ref()
const overwriteImport = ref(false)
const currentStatus = ref('DRAFT')

const importCategories = computed(() => {
  const map = {}
  for (const item of importResult.value?.requirements || []) {
    map[item.category] = (map[item.category] || 0) + 1
  }
  return Object.entries(map).map(([name, count]) => ({ name, count }))
})

const options = reactive({
  departments: ['招商一部', '招商二部', '产业研究中心', '品牌运营部', '风控合规部', '总经办'],
  industries: ['商务服务/总部经济', '数字经济', '文旅商业', '科技服务', '现代金融', '文化创意', '先进制造', '生物医药'],
  sources: ['政府推荐', '公共资源交易平台', '客户委托', '渠道推荐', '自主开发', '老客户复购'],
  methods: ['电子招投标平台', '纸质标书现场递交', '邮箱/邮寄递交', '其他']
})

const qualificationOptions = [
  '房地产经纪机构备案',
  '招商运营服务能力',
  '产业园区运营',
  'ISO9001质量管理体系认证',
  'ISO14001环境管理体系认证',
  'ISO45001职业健康安全管理体系认证'
]

const form = reactive({
  name: '',
  clientName: '',
  carrierName: '',
  industry: '',
  source: '',
  department: auth.department || '招商一部',
  ownerId: '',
  deadline: '',
  budgetAmount: null,
  depositAmount: null,
  projectBrief: '',
  tenderText: '',
  profile: {
    companyName: '南京市行至集商业服务有限公司',
    registeredCapital: 1000,
    establishedYear: 2019,
    qualifications: [],
    creditLevel: 'AAA',
    similarProjectCount: 6,
    similarProjectAmount: 2600,
    teamSize: 28,
    hasGovernmentClient: true,
    bidBondAvailable: true,
    blacklisted: false,
    conflictsOfInterest: false,
    financialStatus: '盈利',
    notes: ''
  },
  version: null
})

const rules = {
  name: [{ required: true, message: '请填写项目名称', trigger: 'blur' }],
  clientName: [{ required: true, message: '请填写招标人/委托方', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择投标截止时间', trigger: 'change' }],
  budgetAmount: [
    { required: true, message: '请填写招标预算', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || Number(value) <= 0) {
          callback(new Error('招标预算必须大于 0'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

function defaultDeadline() {
  const date = new Date(Date.now() + 14 * 86400000)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} 17:00:00`
}

onMounted(async () => {
  loading.value = true
  try {
    const meta = await metaApi.options()
    Object.assign(options, {
      departments: meta.departments || options.departments,
      industries: meta.industries || options.industries,
      sources: meta.sources || options.sources,
      methods: meta.methods || options.methods
    })
    if (canAssignOwner.value) {
      try {
        users.value = await metaApi.users()
      } catch (e) {
        users.value = []
      }
    }
    const template = await metaApi.profileTemplate()
    form.profile = { ...template }

    if (isEdit.value) {
      const detail = await projectApi.detail(route.params.id)
      if (!detail.permissions?.operate) {
        ElMessage.error('无权编辑该项目')
        router.replace(`/projects/${detail.id}`)
        return
      }
      if (!['DRAFT', 'QUALIFICATION_REJECTED'].includes(detail.status)) {
        ElMessage.warning('项目已进入后续阶段，基础信息已冻结')
        router.replace(`/projects/${detail.id}`)
        return
      }
      currentStatus.value = detail.status
      Object.assign(form, {
        name: detail.name,
        clientName: detail.clientName,
        carrierName: detail.carrierName,
        industry: detail.industry,
        source: detail.source,
        department: detail.department,
        ownerId: detail.ownerId,
        deadline: detail.deadline,
        budgetAmount: detail.budgetAmount,
        depositAmount: detail.depositAmount,
        projectBrief: detail.projectBrief,
        tenderText: detail.tenderText,
        profile: detail.profile || template,
        version: detail.version
      })
    } else {
      form.deadline = defaultDeadline()
    }
  } finally {
    loading.value = false
  }
})

function buildPayload() {
  return {
    name: form.name,
    clientName: form.clientName,
    carrierName: form.carrierName,
    industry: form.industry,
    source: form.source,
    department: form.department,
    ownerId: canAssignOwner.value ? form.ownerId || undefined : undefined,
    deadline: form.deadline,
    budgetAmount: form.budgetAmount,
    depositAmount: form.depositAmount,
    projectBrief: form.projectBrief,
    tenderText: form.tenderText,
    profile: form.profile
  }
}

async function save(andCheck) {
  // 草稿允许信息不完整；只有发起预审或编辑非草稿项目时才做完整校验
  if (andCheck || (isEdit.value && currentStatus.value !== 'DRAFT')) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
  }
  saving.value = true
  let project = null
  try {
    if (isEdit.value) {
      project = await projectApi.update(route.params.id, { ...buildPayload(), version: form.version })
      ElMessage.success('项目信息已更新')
      router.replace(project.status === 'DRAFT' ? '/drafts' : `/projects/${project.id}`)
      return
    }
    project = await projectApi.create(buildPayload())
    if (andCheck) {
      try {
        const checked = await workflowApi.qualificationCheck(project.id, {
          profile: form.profile,
          version: project.version
        })
        if (checked.status === 'QUALIFICATION_REJECTED') {
          ElMessage.warning('项目已创建，但资格预审未通过，系统已自动驳回')
        } else {
          ElMessage.success('项目已创建，资格预审通过')
        }
      } catch (e) {
        ElMessage.warning('项目已创建，但预审未完成，请到详情页重试')
      }
    } else {
      ElMessage.success('草稿已保存到草稿箱')
      router.replace('/drafts')
      return
    }
    router.replace(`/projects/${project.id}`)
  } catch (e) {
    // 错误提示由拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function downloadTemplate() {
  downloadingTemplate.value = true
  try {
    await downloadWithAuth('/projects/import/template', '项目批量导入模板.xlsx')
    ElMessage.success('模板已开始下载')
  } finally {
    downloadingTemplate.value = false
  }
}

async function onImportFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!['txt', 'md', 'markdown', 'csv', 'docx', 'xlsx'].includes(ext)) {
    ElMessage.warning('支持 TXT、MD、CSV、DOCX、XLSX；PDF 请复制正文后粘贴识别')
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 20MB')
    return
  }
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const result = await projectApi.importFile(formData)
    handleImportResult(result)
  } finally {
    importing.value = false
  }
}

async function parseTenderText() {
  const text = form.tenderText?.trim()
  if (!text || text.length < 20) {
    ElMessage.warning('请先粘贴至少 20 个字的招标文件正文')
    return
  }
  importing.value = true
  try {
    const result = await projectApi.importText(text)
    handleImportResult(result)
  } finally {
    importing.value = false
  }
}

function handleImportResult(result) {
  importResult.value = result
  applyImport()
  ElMessage.success('已识别并回填，请核对项目信息与资格条件')
}

function applyImport() {
  const result = importResult.value
  if (!result) return
  const overwrite = overwriteImport.value
  if (overwrite || !form.name.trim()) form.name = result.projectName || form.name
  if (overwrite || !form.clientName.trim()) form.clientName = result.clientName || form.clientName
  if (overwrite || !form.industry) form.industry = result.industry || form.industry
  if (overwrite || !form.source) form.source = result.source || form.source
  if (overwrite || form.budgetAmount === null || form.budgetAmount === undefined) {
    form.budgetAmount = result.budgetAmount ?? form.budgetAmount
  }
  if (overwrite || form.depositAmount === null || form.depositAmount === undefined) {
    form.depositAmount = result.depositAmount ?? form.depositAmount
  }
  if (overwrite || !form.deadline) form.deadline = result.deadline || form.deadline
  if (result.tenderText) form.tenderText = result.tenderText
}

function fillSampleTender() {
  const deadline = form.deadline || defaultDeadline()
  const date = new Date(deadline.replace(/-/g, '/'))
  const pad = (n) => String(n).padStart(2, '0')
  const cn = `${date.getFullYear()}年${pad(date.getMonth() + 1)}月${pad(date.getDate())}日 ${pad(date.getHours())}:${pad(date.getMinutes())}`
  const budget = form.budgetAmount || 300
  form.tenderText = `项目名称：${form.name || '招商代理服务项目'}
招标人：${form.clientName || '招标人'}
预算金额：人民币${budget}万元
投标保证金：人民币${form.depositAmount || 5}万元
投标截止时间：${cn}
一、资格要求
1. 投标人须具备房地产经纪机构备案或招商运营服务能力，注册资本不低于300万元。
2. 近三年须具有不少于2个同类项目业绩，并提供合同关键页。
3. 信用等级AA级及以上，无失信被执行记录。
4. 拟投入项目团队不少于10人，须配备项目负责人、招商经理与运营经理。
二、服务要求
5. 负责产业定位、招商策略制定、渠道组织、企业招引与落地服务。
6. 合作期内完成可招商面积60%以上去化，并引入不少于3家产业链关键企业。
7. 每月提交经营分析报告，每季度组织不少于1场招商推介活动。
三、商务要求
8. 投标保证金须在截止时间前足额缴纳，未缴纳按无效标处理。
9. 付款方式：合同签订后30%，中期验收后40%，服务期满30%。
四、评标办法
10. 采用综合评分法：价格分30分、技术分50分、商务分20分。
11. 服务期12个月，核心团队须驻场服务。`
  ElMessage.success('示例招标文本已填入')
}

function goBack() {
  if (route.query.from === 'drafts') {
    router.replace('/drafts')
    return
  }
  if (isEdit.value) {
    router.replace(`/projects/${route.params.id}`)
  } else {
    router.back()
  }
}
</script>

<style scoped>
.back-line {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}

.import-panel {
  margin-bottom: 18px;
  border-color: #efd9da;
  background: linear-gradient(120deg, #fff, #fffafa);
}

.import-drop {
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px dashed #e6c3c5;
  border-radius: 10px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.7);
}

.import-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--brand-soft);
  color: var(--brand);
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 44px;
}

.import-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.import-copy strong {
  font-size: 12px;
}

.import-copy span {
  font-size: 10px;
  color: var(--muted);
  line-height: 1.6;
}

.import-result {
  margin-top: 12px;
  padding: 14px;
  border-radius: 10px;
  background: #f8f9fb;
  border: 1px solid var(--line);
}

.import-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.import-result-head > div {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.import-badge {
  font-size: 10px;
  color: #2e8b57;
  background: #eaf6ef;
  border-radius: 4px;
  padding: 2px 7px;
}

.import-fields {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.import-fields > div {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 8px 10px;
  min-width: 0;
}

.import-fields small {
  display: block;
  font-size: 9px;
  color: var(--muted);
  margin-bottom: 4px;
}

.import-fields strong {
  display: block;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.import-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.import-tag {
  font-size: 10px;
  color: var(--ink-2);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 1px 8px;
}

.import-warnings {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.import-warnings > div {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  font-size: 10px;
  line-height: 1.6;
  color: var(--muted);
}

.hidden-file {
  display: none;
}

.bottom-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  padding: 14px 18px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 12px;
  box-shadow: 0 6px 18px rgba(31, 42, 55, 0.04);
}

.bottom-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: var(--muted);
  line-height: 1.6;
}

.bottom-buttons {
  display: flex;
  gap: 10px;
  flex: 0 0 auto;
}

@media (max-width: 700px) {
  .bottom-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .bottom-buttons {
    justify-content: flex-end;
  }
}

.form-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

.form-main {
  min-width: 0;
}

.form-main .panel + .panel {
  margin-top: 18px;
}

.form-side {
  position: sticky;
  top: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-panel + .side-panel {
  margin-top: 0;
}

.span-2 {
  grid-column: span 2;
}

.inner {
  gap: 12px;
}

.field-hint {
  width: 100%;
  margin-top: 6px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--muted);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.switch-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 4px;
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 10px 12px;
  background: #fbfcfd;
}

.switch-item strong {
  display: block;
  font-size: 12px;
  margin-bottom: 3px;
}

.switch-item small {
  font-size: 10px;
  color: var(--muted);
}

.flow-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.flow-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.flow-index {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #f1f3f7;
  color: var(--muted);
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 22px;
  font-weight: 700;
}

.flow-index.active {
  background: var(--brand);
  color: #fff;
}

.flow-item strong {
  display: block;
  font-size: 12px;
  margin-bottom: 2px;
}

.flow-item small {
  font-size: 10px;
  color: var(--muted);
  line-height: 1.5;
}

.side-text {
  margin-top: 12px;
  font-size: 11px;
  line-height: 1.8;
  color: var(--muted);
}

.side-text strong {
  color: var(--danger);
}

@media (max-width: 1100px) {
  .form-layout {
    grid-template-columns: 1fr;
  }

  .form-side {
    position: static;
  }

  .grid-3 {
    grid-template-columns: repeat(2, 1fr);
  }

  .import-fields {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 700px) {
  .import-drop {
    align-items: flex-start;
  }

  .import-fields {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .grid-2,
  .grid-3,
  .switch-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }
}
</style>
