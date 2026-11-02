export function money(value) {
  if (value === null || value === undefined || value === '') return '—'
  const number = Number(value)
  if (Number.isNaN(number)) return String(value)
  return number.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

export function moneyWan(value) {
  const text = money(value)
  return text === '—' ? text : `${text} 万元`
}

export function percent(value) {
  if (value === null || value === undefined) return '—'
  return `${Number(value).toFixed(2)}%`
}

export function daysLabel(days) {
  if (days === null || days === undefined) return '未设置'
  if (days < 0) return `已逾期 ${Math.abs(days)} 天`
  if (days === 0) return '今天截止'
  return `${days} 天后截止`
}

export function riskColor(level) {
  if (level === 'HIGH') return '#C0392B'
  if (level === 'MEDIUM') return '#D68910'
  return '#2E8B57'
}

export function shortText(value, max = 40) {
  if (!value) return ''
  return value.length > max ? `${value.slice(0, max)}…` : value
}

export function greeting() {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

export function todayText() {
  const now = new Date()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日 · 星期${weekdays[now.getDay()]}`
}

export function roleName(role) {
  const map = {
    ADMIN: '系统管理员',
    BID_MANAGER: '投标经理',
    REVIEWER: '复核 / 风控',
    VIEWER: '只读观察员',
    SYSTEM: '系统'
  }
  return map[role] || role || '—'
}

export function actionLabel(action) {
  const map = {
    EDIT: '编辑信息',
    QUALIFICATION_CHECK: '资格预审',
    EXTRACT: '要素抽取',
    MATCH_PRICE: '匹配与报价',
    AI_DRAFT: '生成初稿',
    UPDATE_DRAFT: '编辑初稿',
    SUBMIT_REVIEW: '提交复核',
    REVIEW: '人工复核',
    SUBMIT: '提交投标',
    ARCHIVE: '项目归档'
  }
  return map[action] || action
}

export function tabOfAction(action) {
  const map = {
    QUALIFICATION_CHECK: 'qualification',
    EXTRACT: 'extraction',
    MATCH_PRICE: 'pricing',
    AI_DRAFT: 'draft',
    UPDATE_DRAFT: 'draft',
    SUBMIT_REVIEW: 'review',
    REVIEW: 'review',
    SUBMIT: 'submit',
    ARCHIVE: 'submit'
  }
  return map[action] || 'overview'
}
