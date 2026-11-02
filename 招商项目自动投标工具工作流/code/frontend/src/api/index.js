import http from './http'

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  logout: () => http.post('/auth/logout')
}

export const profileApi = {
  view: () => http.get('/profile'),
  update: (data) => http.put('/profile', data),
  changePassword: (data) => http.put('/profile/password', data),
  uploadAvatar: (formData) => http.post('/profile/avatar', formData),
  removeAvatar: () => http.delete('/profile/avatar'),
  updatePreferences: (preferences) => http.put('/profile/preferences', { preferences }),
  summary: () => http.get('/profile/summary'),
  activity: () => http.get('/profile/activity')
}

export const notificationApi = {
  list: (params) => http.get('/notifications', { params }),
  unreadCount: () => http.get('/notifications/unread-count'),
  read: (id) => http.post(`/notifications/${id}/read`),
  readAll: () => http.post('/notifications/read-all'),
  remove: (id) => http.delete(`/notifications/${id}`)
}

export const dashboardApi = {
  overview: () => http.get('/dashboard')
}

export const projectApi = {
  list: (params) => http.get('/projects', { params }),
  detail: (id) => http.get(`/projects/${id}`),
  create: (data) => http.post('/projects', data),
  update: (id, data) => http.put(`/projects/${id}`, data),
  deleteProject: (id, reason) => http.delete(`/projects/${id}`, { params: { reason } }),
  timeline: (id) => http.get(`/projects/${id}/timeline`),
  importText: (text) => http.post('/projects/import/parse', { text }),
  importFile: (formData) => http.post('/projects/import/file', formData),
  importBatch: (formData) => http.post('/projects/import/batch', formData),
  batchDelete: (ids, reason) => http.post('/projects/batch/delete', { ids, reason })
}

export const recycleApi = {
  list: () => http.get('/admin/recycle-bin'),
  restore: (id) => http.post(`/admin/recycle-bin/${id}/restore`),
  purge: (id) => http.delete(`/admin/recycle-bin/${id}`),
  batchRestore: (ids) => http.post('/admin/recycle-bin/batch/restore', { ids }),
  batchPurge: (ids) => http.post('/admin/recycle-bin/batch/purge', { ids })
}

export const collaborationApi = {
  uploadAttachment: (id, formData) => http.post(`/projects/${id}/attachments`, formData),
  deleteAttachment: (id, attachmentId) => http.delete(`/projects/${id}/attachments/${attachmentId}`),
  addComment: (id, content) => http.post(`/projects/${id}/comments`, { content }),
  deleteComment: (id, commentId) => http.delete(`/projects/${id}/comments/${commentId}`),
  toggleFollow: (id) => http.post(`/projects/${id}/follow`),
  duplicate: (id) => http.post(`/projects/${id}/duplicate`)
}

export const workflowApi = {
  qualificationCheck: (id, data) => http.post(`/projects/${id}/qualification-check`, data),
  extract: (id, data) => http.post(`/projects/${id}/extract`, data),
  matchPrice: (id, data) => http.post(`/projects/${id}/match-price`, data),
  aiDraft: (id, data) => http.post(`/projects/${id}/ai-draft`, data),
  updateDraft: (id, data) => http.put(`/projects/${id}/draft`, data),
  submitReview: (id, version) => http.post(`/projects/${id}/submit-review`, null, { params: { version } }),
  review: (id, data) => http.post(`/projects/${id}/review`, data),
  submit: (id, data) => http.post(`/projects/${id}/submit`, data),
  archive: (id, data) => http.post(`/projects/${id}/archive`, data)
}

export const ruleApi = {
  list: () => http.get('/rules'),
  update: (code, data) => http.put(`/rules/${code}`, data),
  reset: () => http.post('/rules/reset')
}

export const auditApi = {
  list: (params) => http.get('/audits', { params })
}

export const adminApi = {
  users: () => http.get('/admin/users'),
  create: (data) => http.post('/admin/users', data),
  update: (id, data) => http.put(`/admin/users/${id}`, data),
  toggle: (id) => http.post(`/admin/users/${id}/toggle`),
  resetPassword: (id) => http.post(`/admin/users/${id}/reset-password`)
}

export const metaApi = {
  options: () => http.get('/meta/options'),
  users: () => http.get('/meta/users'),
  profileTemplate: () => http.get('/meta/profile-template'),
  system: () => http.get('/meta/system')
}
