<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="brand-top">
        <div class="brand-mark"><img :src="logoMark" alt="SINCIT" /></div>
        <div class="brand-name">
          <strong>SINCIT</strong>
          <span>行至集</span>
        </div>
        <div class="brand-divider"></div>
        <div class="brand-product">招商智投</div>
      </div>

      <div class="brand-copy">
        <div class="eyebrow">招商项目自动投标工具工作流</div>
        <h1>让每一个招商标的，<br />都有章法地赢。</h1>
        <p>
          面向产业招商场景，把资格预审、招标要素抽取、资质匹配与报价测算、
          标书生成、人工复核与归档沉淀为一条可追溯的流水线。
        </p>

        <div class="feature-list">
          <div class="feature">
            <div class="feature-icon"><el-icon :size="18"><CircleCheck /></el-icon></div>
            <div>
              <strong>规则先行，自动驳回</strong>
              <span>资格不满足即刻拦截，避免无效投入</span>
            </div>
          </div>
          <div class="feature">
            <div class="feature-icon"><el-icon :size="18"><MagicStick /></el-icon></div>
            <div>
              <strong>AI 初稿，人工把关</strong>
              <span>本地模板兜底，大模型可选接入</span>
            </div>
          </div>
          <div class="feature">
            <div class="feature-icon"><el-icon :size="18"><Lock /></el-icon></div>
            <div>
              <strong>职责分离，全程留痕</strong>
              <span>数据权限、复核回避、审计日志可追溯</span>
            </div>
          </div>
        </div>
      </div>

      <div class="brand-foot">
        <div class="credentials">
          <span>鼓楼国资参股企业</span>
          <span>AAA 级信用企业</span>
          <span>ISO 三体系认证</span>
        </div>
        <div class="contact-line">南京市鼓楼区北祥路 67 号 · 025-5806 7506 · www.svcgval.com</div>
      </div>

      <div class="skyline" aria-hidden="true">
        <i v-for="n in 14" :key="n" :style="buildingStyle(n)"></i>
      </div>
    </section>

    <section class="form-panel">
      <div class="form-wrap">
        <div class="mobile-brand">
          <div class="brand-mark"><img :src="logoMark" alt="SINCIT" /></div>
          <span>SINCIT · 招商智投</span>
        </div>

        <h2>登录工作台</h2>
        <p class="form-sub">使用您的投标或复核账号继续</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              @keyup.enter="submit"
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <div class="login-options">
            <el-checkbox v-model="form.rememberMe">记住账号</el-checkbox>
            <el-tooltip content="请联系系统管理员在“成员管理”中重置密码" placement="top">
              <span class="forgot-link">忘记密码？</span>
            </el-tooltip>
          </div>
          <el-button type="primary" size="large" class="login-btn" :loading="auth.loading" @click="submit">
            进入工作台
          </el-button>
        </el-form>

        <div class="demo-block">
          <div class="demo-head">
            <span>演示账号</span>
            <small>点击自动填充</small>
          </div>
          <div class="demo-grid">
            <button
              v-for="account in accounts"
              :key="account.username"
              type="button"
              class="demo-card"
              :class="{ active: form.username === account.username }"
              @click="fill(account)"
            >
              <span class="demo-role">{{ account.role }}</span>
              <strong>{{ account.name }}</strong>
              <small>{{ account.username }}</small>
            </button>
          </div>
        </div>

        <div class="form-foot">
          <el-icon :size="13"><InfoFilled /></el-icon>
          <span>本地演示环境 · v1.3 · 连续登录失败 5 次将锁定 5 分钟。</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const formRef = ref()

const rememberedUser = localStorage.getItem('bid_remember_user')
const form = reactive({
  username: rememberedUser || 'manager',
  password: rememberedUser ? '' : 'Bid@123',
  rememberMe: Boolean(rememberedUser)
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const accounts = [
  { role: '投标经理', name: 'manager', username: 'manager', password: 'Bid@123' },
  { role: '复核 / 风控', name: 'reviewer', username: 'reviewer', password: 'Review@123' },
  { role: '系统管理员', name: 'admin', username: 'admin', password: 'Admin@123' },
  { role: '只读观察员', name: 'viewer', username: 'viewer', password: 'View@123' }
]

function fill(account) {
  form.username = account.username
  form.password = account.password
}

function buildingStyle(n) {
  const heights = [26, 42, 34, 58, 46, 72, 52, 88, 64, 40, 54, 34, 28, 20]
  const delays = [0.2, 0.8, 0.4, 1.2, 0.6, 1.6, 0.3, 2.0, 1.0, 0.5, 1.4, 0.9, 1.8, 0.1]
  return {
    height: `${heights[n - 1]}%`,
    animationDelay: `${delays[n - 1]}s`
  }
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await auth.login({ username: form.username.trim(), password: form.password, rememberMe: form.rememberMe })
      if (form.rememberMe) {
        localStorage.setItem('bid_remember_user', form.username.trim())
      } else {
        localStorage.removeItem('bid_remember_user')
      }
      ElMessage.success(`欢迎回来，${auth.displayName}`)
      router.replace(route.query.redirect || '/dashboard')
    } catch (e) {
      // 错误提示由 HTTP 拦截器统一处理
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  background: #fff;
}

.brand-panel {
  position: relative;
  overflow: hidden;
  padding: 38px 48px 34px;
  color: #fff;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(120% 90% at 12% 8%, rgba(255, 255, 255, 0.14), transparent 46%),
    radial-gradient(80% 70% at 95% 20%, rgba(255, 174, 150, 0.22), transparent 55%),
    linear-gradient(145deg, #a41b23 0%, #b4232a 42%, #7f141a 100%);
}

.brand-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 52px 52px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.7), transparent 72%);
  pointer-events: none;
}

.brand-top {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #fff;
  color: var(--brand);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.brand-mark img {
  width: 31px;
  height: 31px;
  object-fit: contain;
  display: block;
}

.brand-name {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.brand-name strong {
  font-size: 18px;
  letter-spacing: 2px;
}

.brand-name span {
  font-size: 12px;
  opacity: 0.82;
  letter-spacing: 1px;
}

.brand-divider {
  width: 1px;
  height: 18px;
  background: rgba(255, 255, 255, 0.35);
  margin: 0 4px;
}

.brand-product {
  font-size: 12px;
  letter-spacing: 3px;
  opacity: 0.85;
}

.brand-copy {
  position: relative;
  z-index: 2;
  margin-top: auto;
  margin-bottom: auto;
  max-width: 560px;
  padding: 48px 0 24px;
}

.eyebrow {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 3px;
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.06);
  margin-bottom: 24px;
}

.brand-copy h1 {
  font-size: clamp(34px, 3.1vw, 48px);
  line-height: 1.28;
  letter-spacing: 1px;
  font-weight: 800;
  margin-bottom: 20px;
}

.brand-copy p {
  max-width: 480px;
  font-size: 14px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.78);
}

.feature-list {
  margin-top: 38px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 430px;
}

.feature {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.feature-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.13);
  border: 1px solid rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 34px;
}

.feature strong {
  display: block;
  font-size: 13px;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.feature span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.68);
}

.brand-foot {
  position: relative;
  z-index: 2;
  margin-top: 36px;
}

.credentials {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.credentials span {
  font-size: 10px;
  padding: 5px 9px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.8);
}

.contact-line {
  margin-top: 14px;
  font-size: 10px;
  letter-spacing: 0.4px;
  color: rgba(255, 255, 255, 0.45);
}

.skyline {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 150px;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 0 30px;
  opacity: 0.17;
  pointer-events: none;
}

.skyline i {
  flex: 1;
  min-width: 8px;
  background: linear-gradient(to top, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.05));
  border-radius: 2px 2px 0 0;
  animation: rise 1.1s ease both;
}

@keyframes rise {
  from {
    transform: scaleY(0.2);
    opacity: 0;
  }
  to {
    transform: scaleY(1);
    opacity: 1;
  }
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
  position: relative;
}

.form-wrap {
  width: 100%;
  max-width: 400px;
}

.mobile-brand {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  color: var(--brand);
  font-weight: 700;
  letter-spacing: 1px;
}

.form-wrap h2 {
  font-size: 28px;
  letter-spacing: 0.5px;
}

.form-sub {
  margin: 8px 0 28px;
  color: var(--muted);
  font-size: 13px;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -6px 0 12px;
}

.forgot-link {
  font-size: 11px;
  color: var(--muted);
  cursor: help;
}

.forgot-link:hover {
  color: var(--brand);
}

.login-btn {
  width: 100%;
  margin-top: 6px;
  letter-spacing: 2px;
}

.demo-block {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px dashed var(--line);
}

.demo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.demo-head span {
  font-size: 12px;
  color: var(--ink-2);
  font-weight: 600;
}

.demo-head small {
  font-size: 11px;
  color: var(--muted);
}

.demo-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.demo-card {
  text-align: left;
  border: 1px solid var(--line);
  background: #fbfcfd;
  border-radius: 8px;
  padding: 9px 11px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 2px;
  transition: all 0.16s ease;
  font-family: inherit;
}

.demo-card:hover {
  border-color: #e2b6b8;
  background: #fff8f8;
}

.demo-card.active {
  border-color: var(--brand);
  background: var(--brand-soft);
  box-shadow: 0 0 0 2px rgba(180, 35, 42, 0.06);
}

.demo-role {
  font-size: 10px;
  color: var(--brand);
  font-weight: 600;
}

.demo-card strong {
  font-size: 12px;
  color: var(--ink);
}

.demo-card small {
  font-size: 10px;
  color: var(--muted);
  font-family: Consolas, monospace;
}

.form-foot {
  display: flex;
  gap: 6px;
  align-items: flex-start;
  margin-top: 22px;
  font-size: 11px;
  line-height: 1.6;
  color: var(--muted);
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .mobile-brand {
    display: flex;
  }

  .form-panel {
    padding: 24px;
  }
}
</style>
