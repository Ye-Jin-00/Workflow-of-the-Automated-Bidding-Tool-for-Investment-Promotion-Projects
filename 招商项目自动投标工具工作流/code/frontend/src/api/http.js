import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 90000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('bid_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'code')) {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.message || '请求处理失败')
      return Promise.reject(new Error(body.message || '请求处理失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    const message = body?.message || error.message || '网络异常，请稍后重试'

    if (status === 401) {
      localStorage.removeItem('bid_token')
      localStorage.removeItem('bid_user')
      window.dispatchEvent(new CustomEvent('auth-expired'))
      return Promise.reject(new Error(message))
    }

    if (status === 403) {
      ElMessage.error(message || '无权执行该操作')
    } else if (status === 409) {
      ElMessage.warning(message || '数据状态已变化，请刷新后重试')
    } else if (status >= 500) {
      ElMessage.error(`${message}${body?.traceId ? `（追踪号 ${body.traceId}）` : ''}`)
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(new Error(message))
  }
)

export default http
