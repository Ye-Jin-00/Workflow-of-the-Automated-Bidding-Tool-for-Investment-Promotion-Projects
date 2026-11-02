import { defineStore } from 'pinia'
import { authApi } from '../api'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem('bid_user') || 'null')
  } catch (e) {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('bid_token') || '',
    user: readUser(),
    loading: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isReviewer: (state) => state.user?.role === 'REVIEWER',
    isViewer: (state) => state.user?.role === 'VIEWER',
    displayName: (state) => state.user?.displayName || '未登录',
    roleLabel: (state) => state.user?.roleLabel || '',
    department: (state) => state.user?.department || '',
    avatarUrl: (state) => state.user?.avatarUrl || ''
  },
  actions: {
    applySession(data) {
      this.token = data.token
      this.user = data.user
      localStorage.setItem('bid_token', data.token)
      localStorage.setItem('bid_user', JSON.stringify(data.user))
    },
    async login(payload) {
      this.loading = true
      try {
        const data = await authApi.login(payload)
        this.applySession(data)
        return data
      } finally {
        this.loading = false
      }
    },
    async fetchMe() {
      if (!this.token) return null
      const user = await authApi.me()
      this.setUser(user)
      return user
    },
    setUser(user) {
      this.user = user
      localStorage.setItem('bid_user', JSON.stringify(user))
    },
    updateToken(token, expireAt) {
      this.token = token
      localStorage.setItem('bid_token', token)
      if (expireAt) {
        localStorage.setItem('bid_expire_at', expireAt)
      }
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('bid_token')
      localStorage.removeItem('bid_user')
      localStorage.removeItem('bid_expire_at')
    }
  }
})
