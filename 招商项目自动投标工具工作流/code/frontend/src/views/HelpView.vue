<template>
  <div class="page">
    <section class="help-hero">
      <div class="hero-left">
        <div class="eyebrow">SINCIT · 招商智投</div>
        <h1>把招商投标的经验，变成可复用的工作流</h1>
        <p>从线索到归档，系统用规则、模型与审计把每个环节串起来。这里是使用指南、角色说明与常见问题。</p>
        <div class="hero-actions">
          <el-button type="primary" @click="$router.push('/projects')">进入投标项目</el-button>
          <el-button plain @click="$router.push('/profile')">前往个人中心</el-button>
        </div>
      </div>
      <div class="hero-meta">
        <div class="meta-item">
          <span>系统版本</span>
          <strong>v{{ system.version || '1.1.0' }}</strong>
        </div>
        <div class="meta-item">
          <span>AI 模式</span>
          <strong>{{ system.aiEnabled ? '大模型已启用' : '本地模板兜底' }}</strong>
        </div>
        <div class="meta-item">
          <span>数据存储</span>
          <strong>{{ system.storage || 'JSON 文件' }}</strong>
        </div>
        <div class="meta-item">
          <span>登录保护</span>
          <strong>令牌版本 + 失败锁定</strong>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">工作流七环节</div>
        <span class="small muted">六个业务环节 + 提交归档</span>
      </div>
      <div class="workflow-grid">
        <div v-for="(step, index) in workflow" :key="step.title" class="workflow-card">
          <div class="step-index">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="step-copy">
            <div class="step-title">{{ step.title }}</div>
            <div class="step-desc">{{ step.desc }}</div>
            <el-tag size="small" :type="step.type" effect="plain">{{ step.tag }}</el-tag>
          </div>
        </div>
      </div>
    </section>

    <div class="grid-2">
      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">角色与权限</div>
        </div>
        <el-table :data="roleRows" size="small" style="width: 100%">
          <el-table-column prop="role" label="角色" width="110" />
          <el-table-column prop="scope" label="数据范围" min-width="150" />
          <el-table-column prop="action" label="核心操作" min-width="190" />
        </el-table>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">快捷操作</div>
        </div>
        <div class="shortcut-list">
          <div v-for="item in shortcuts" :key="item.keys" class="shortcut-item">
            <span class="shortcut-keys">{{ item.keys }}</span>
            <span class="shortcut-desc">{{ item.desc }}</span>
          </div>
        </div>
        <div class="shortcut-tip">
          提示：顶部搜索框会直接跳转项目列表；消息铃铛展示待复核、复核结果与截止提醒。
        </div>
      </section>
    </div>

    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">常见问题</div>
      </div>
      <el-collapse v-model="activeFaq">
        <el-collapse-item v-for="(faq, index) in faqs" :key="faq.q" :name="String(index)">
          <template #title>
            <span class="faq-title">{{ faq.q }}</span>
          </template>
          <p class="faq-answer">{{ faq.a }}</p>
        </el-collapse-item>
      </el-collapse>
    </section>

    <section class="panel about-panel">
      <div>
        <div class="panel-title">关于系统</div>
        <p class="about-text">
          招商智投是面向行至集产业招商业务的内部工作台。当前交付为可运行原型，
          生产环境建议接入 MySQL、统一身份认证、PDF/OCR、真实大模型、对象存储与电子签章。
        </p>
        <div class="about-tags">
          <el-tag effect="plain" v-for="feature in system.features || []" :key="feature" size="small">{{ feature }}</el-tag>
        </div>
      </div>
      <div class="about-contact">
        <strong>南京市行至集商业服务有限公司</strong>
        <span>南京市鼓楼区北祥路 67 号</span>
        <span>025-5806 7506 · www.svcgval.com</span>
        <el-button text type="primary" @click="copyContact">复制联系方式</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { metaApi } from '../api'

const system = ref({})
const activeFaq = ref('0')

const workflow = [
  { title: '项目录入', desc: '填写客户、载体、预算、截止与资格画像', tag: '人工', type: 'info' },
  { title: '资格预审', desc: '10 条可配置规则自动判定，硬性失败自动驳回', tag: '自动', type: 'success' },
  { title: '要素抽取', desc: '识别预算、保证金、截止与六类招标要素', tag: '规则', type: 'success' },
  { title: '匹配与报价', desc: '五维匹配评分、成本拆解、建议价与中标概率', tag: '模型', type: 'warning' },
  { title: '标书初稿', desc: '本地模板兜底，可选大模型生成，支持人工编辑', tag: 'AI', type: 'warning' },
  { title: '人工复核', desc: '负责人回避、退回意见、多轮留痕', tag: '人工', type: 'danger' },
  { title: '提交归档', desc: '回执号、档号、保存年限与全链路审计', tag: '人工', type: 'info' }
]

const roleRows = [
  { role: '系统管理员', scope: '全部项目与成员', action: '规则配置、成员管理、全部复核' },
  { role: '投标经理', scope: '本部门项目', action: '创建项目、推进流程、提交投标' },
  { role: '复核 / 风控', scope: '全部项目只读', action: '复核待审项目，回避本人项目' },
  { role: '只读观察员', scope: '已通过 / 已提交 / 已归档', action: '只读查看，不可编辑或评论' }
]

const shortcuts = [
  { keys: 'Ctrl + K', desc: '聚焦顶部项目搜索' },
  { keys: 'Enter', desc: '确认搜索或提交当前表单' },
  { keys: 'Esc', desc: '关闭弹窗或抽屉' },
  { keys: '头像菜单', desc: '进入个人中心、消息中心与帮助' }
]

const faqs = [
  {
    q: '为什么资格预审直接被驳回？',
    a: '必填规则（如注册资本、信用等级、同类业绩、失信记录、截止时间）任一不满足，系统会自动驳回并写明原因。修正资格画像后可重新预审，无需管理员改数据。'
  },
  {
    q: 'AI 生成失败会不会影响流程？',
    a: '不会。系统会降级为本地模板生成，并在初稿页标记“降级生成”。配置大模型 Key 后可重新生成，复核人仍须人工把关。'
  },
  {
    q: '修改密码后为什么其他设备退出了？',
    a: '令牌包含版本号，修改密码或管理员重置密码会递增版本，旧令牌立即失效。当前页面会自动续签新令牌，不需要重新登录。'
  },
  {
    q: '项目附件支持哪些格式？',
    a: '支持 PDF、Word、Excel、PPT、TXT、ZIP 与图片，单个文件不超过 20MB。项目归档后附件只读，不能继续上传或删除。'
  },
  {
    q: '关注项目有什么用？',
    a: '关注后，项目状态变更、截止提醒、评论与附件都会进入消息中心。也可以在项目列表用“我关注的”快速筛选。'
  }
]

async function loadSystem() {
  try {
    system.value = await metaApi.system()
  } catch (e) {
    system.value = {}
  }
}

async function copyContact() {
  try {
    await navigator.clipboard.writeText('南京市行至集商业服务有限公司 南京市鼓楼区北祥路67号 025-5806 7506')
    ElMessage.success('联系方式已复制')
  } catch (e) {
    ElMessage.warning('浏览器不支持自动复制')
  }
}

onMounted(loadSystem)
</script>

<style scoped>
.help-hero {
  background:
    radial-gradient(90% 120% at 100% 0%, rgba(180, 35, 42, 0.16), transparent 55%),
    linear-gradient(120deg, #fff, #fdf6f6);
  border: 1px solid #f0dede;
  border-radius: 16px;
  padding: 30px 34px;
  display: flex;
  gap: 26px;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.hero-left {
  max-width: 680px;
}

.eyebrow {
  font-size: 10px;
  letter-spacing: 3px;
  color: var(--brand);
  margin-bottom: 10px;
}

.help-hero h1 {
  font-size: 26px;
  line-height: 1.45;
  letter-spacing: 0.3px;
}

.help-hero p {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--muted);
}

.hero-actions {
  display: flex;
  gap: 8px;
  margin-top: 18px;
}

.hero-meta {
  display: grid;
  grid-template-columns: repeat(2, 150px);
  gap: 12px;
}

.meta-item {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid #f0dede;
  border-radius: 10px;
  padding: 12px;
}

.meta-item span {
  display: block;
  font-size: 10px;
  color: var(--muted);
  margin-bottom: 5px;
}

.meta-item strong {
  font-size: 12px;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.workflow-card {
  display: flex;
  gap: 12px;
  background: #f8f9fb;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 14px;
}

.step-index {
  font-family: Consolas, monospace;
  font-size: 18px;
  font-weight: 700;
  color: var(--brand);
  line-height: 1.2;
}

.step-title {
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 4px;
}

.step-desc {
  font-size: 10px;
  line-height: 1.6;
  color: var(--muted);
  margin-bottom: 8px;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin: 18px 0;
}

.grid-2 .panel {
  margin-top: 0;
}

.shortcut-list {
  display: flex;
  flex-direction: column;
}

.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 2px;
  border-bottom: 1px dashed var(--line);
  font-size: 12px;
}

.shortcut-keys {
  font-family: Consolas, monospace;
  font-size: 11px;
  color: var(--brand);
  background: var(--brand-soft);
  border-radius: 4px;
  padding: 2px 7px;
}

.shortcut-desc {
  color: var(--muted);
  font-size: 11px;
}

.shortcut-tip {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8f9fb;
  font-size: 10px;
  line-height: 1.7;
  color: var(--muted);
}

.faq-title {
  font-size: 12px;
  font-weight: 600;
}

.faq-answer {
  font-size: 12px;
  line-height: 1.85;
  color: var(--muted);
}

.about-panel {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  margin-top: 18px;
}

.about-text {
  margin-top: 8px;
  max-width: 720px;
  font-size: 11px;
  line-height: 1.8;
  color: var(--muted);
}

.about-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}

.about-contact {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 10px;
  color: var(--muted);
  min-width: 260px;
}

.about-contact strong {
  font-size: 12px;
  color: var(--ink);
}

@media (max-width: 1100px) {
  .help-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .workflow-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .grid-2 {
    grid-template-columns: 1fr;
  }

  .about-panel {
    flex-direction: column;
  }
}
</style>
