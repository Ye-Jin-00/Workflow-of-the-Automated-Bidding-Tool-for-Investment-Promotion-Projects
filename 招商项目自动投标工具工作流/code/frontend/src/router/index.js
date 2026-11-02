import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'projects',
        name: 'projects',
        component: () => import('../views/ProjectListView.vue'),
        meta: { title: '投标项目', icon: 'Files' }
      },
      {
        path: 'drafts',
        name: 'drafts',
        component: () => import('../views/DraftListView.vue'),
        meta: { title: '草稿箱', icon: 'EditPen' }
      },
      {
        path: 'projects/new',
        name: 'project-create',
        component: () => import('../views/ProjectCreateView.vue'),
        meta: { title: '新建投标项目' }
      },
      {
        path: 'projects/:id/edit',
        name: 'project-edit',
        component: () => import('../views/ProjectCreateView.vue'),
        meta: { title: '编辑投标项目' }
      },
      {
        path: 'projects/:id',
        name: 'project-detail',
        component: () => import('../views/ProjectDetailView.vue'),
        meta: { title: '项目详情' }
      },
      {
        path: 'reviews',
        name: 'reviews',
        component: () => import('../views/ReviewCenterView.vue'),
        meta: { title: '复核中心', icon: 'Stamp' }
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: () => import('../views/NotificationCenterView.vue'),
        meta: { title: '消息中心', icon: 'Bell' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/ProfileView.vue'),
        meta: { title: '个人中心', icon: 'User' }
      },
      {
        path: 'admin/users',
        name: 'admin-users',
        component: () => import('../views/UserManageView.vue'),
        meta: { title: '成员管理', icon: 'UserFilled', adminOnly: true }
      },
      {
        path: 'admin/recycle-bin',
        name: 'recycle-bin',
        component: () => import('../views/RecycleBinView.vue'),
        meta: { title: '回收站', icon: 'Delete', adminOnly: true }
      },
      {
        path: 'help',
        name: 'help',
        component: () => import('../views/HelpView.vue'),
        meta: { title: '帮助中心', icon: 'QuestionFilled' }
      },
      {
        path: 'rules',
        name: 'rules',
        component: () => import('../views/RuleConfigView.vue'),
        meta: { title: '资格规则', icon: 'SetUp', adminOnly: true }
      },
      {
        path: 'audits',
        name: 'audits',
        component: () => import('../views/AuditLogView.vue'),
        meta: { title: '审计日志', icon: 'Tickets' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  document.title = `${to.meta.title || '工作台'} · 招商智投`

  if (to.meta.public) {
    if (to.name === 'login' && auth.isLoggedIn) {
      return { path: '/dashboard' }
    }
    return true
  }

  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch (e) {
      auth.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.adminOnly && !auth.isAdmin) {
    return { path: '/dashboard' }
  }

  return true
})

export default router
