import { defineStore } from 'pinia'
import { notificationApi } from '../api'

export const useNotificationStore = defineStore('notifications', {
  state: () => ({
    unread: 0,
    items: [],
    total: 0,
    loading: false,
    loaded: false
  }),
  actions: {
    async refreshUnread() {
      try {
        const data = await notificationApi.unreadCount()
        this.unread = data.count || 0
      } catch (e) {
        this.unread = 0
      }
    },
    async load(unreadOnly = false) {
      this.loading = true
      try {
        const data = await notificationApi.list({ page: 1, size: 20, unreadOnly })
        this.items = data.items
        this.total = data.total
        this.loaded = true
      } finally {
        this.loading = false
      }
    },
    async markRead(id) {
      await notificationApi.read(id)
      const item = this.items.find((n) => n.id === id)
      if (item && !item.read) {
        item.read = true
        this.unread = Math.max(0, this.unread - 1)
      }
    },
    async markAll() {
      const data = await notificationApi.readAll()
      this.items.forEach((item) => { item.read = true })
      this.unread = 0
      return data.count
    },
    async remove(id) {
      const item = this.items.find((n) => n.id === id)
      await notificationApi.remove(id)
      this.items = this.items.filter((n) => n.id !== id)
      this.total = Math.max(0, this.total - 1)
      if (item && !item.read) {
        this.unread = Math.max(0, this.unread - 1)
      }
    }
  }
})
