import { ElMessage } from 'element-plus'

/**
 * 带令牌下载文件。附件与 CSV 导出不能直接用 <a href>，因为需要 Authorization 头。
 * 传入的 url 可以是 /api/... 或 /projects/...，统一去掉 /api 前缀后请求。
 */
export async function downloadWithAuth(url, filename) {
  const path = url.startsWith('/api') ? url.slice(4) : url
  const token = localStorage.getItem('bid_token')
  const response = await fetch(`/api${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!response.ok) {
    let message = '下载失败'
    try {
      const body = await response.json()
      message = body.message || message
    } catch (e) {
      // ignore
    }
    ElMessage.error(message)
    return
  }
  const blob = await response.blob()
  const disposition = response.headers.get('content-disposition') || ''
  let name = filename
  if (!name) {
    const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/i)
    name = match ? decodeURIComponent(match[1].replace(/"/g, '')) : 'download'
  }
  const link = document.createElement('a')
  const objectUrl = URL.createObjectURL(blob)
  link.href = objectUrl
  link.download = name
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}
