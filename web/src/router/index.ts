import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// Eager load main views for faster navigation
import HomeView from '@/views/HomeView.vue'
import SiegeWarView from '@/views/SiegeWarView.vue'
import MembersView from '@/views/MembersView.vue'
import PartiesView from '@/views/PartiesView.vue'
import ChannelsView from '@/views/ChannelsView.vue'

// Lazy load less frequent views for code splitting
const LoginView = () => import('@/views/auth/LoginView.vue')
const RegisterView = () => import('@/views/auth/RegisterView.vue')
const ForgotPasswordView = () => import('@/views/auth/ForgotPasswordView.vue')
const ResetPasswordView = () => import('@/views/auth/ResetPasswordView.vue')
const EventsView = () => import('@/views/EventsView.vue')
const ChatView = () => import('@/views/ChatView.vue')
const MemberProfileView = () => import('@/views/MemberProfileView.vue')
const ProfileView = () => import('@/views/ProfileView.vue')
const PendingMembersView = () => import('@/views/admin/PendingMembersView.vue')
const BannedUsersView = () => import('@/views/admin/BannedUsersView.vue')
const PendingApprovalView = () => import('@/views/PendingApprovalView.vue')
const DownloadView = () => import('@/views/DownloadView.vue')

const routes: RouteRecordRaw[] = [
  // Public routes
  {
    path: '/download',
    name: 'download',
    component: DownloadView,
    meta: { requiresAuth: false }
  },

  // Auth routes (public)
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { requiresAuth: false, guestOnly: true }
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: { requiresAuth: false, guestOnly: true }
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: ForgotPasswordView,
    meta: { requiresAuth: false, guestOnly: true }
  },
  {
    path: '/reset-password',
    name: 'reset-password',
    component: ResetPasswordView,
    meta: { requiresAuth: false }
  },

  // Pending approval (requires auth but pending status)
  {
    path: '/pending',
    name: 'pending',
    component: PendingApprovalView,
    meta: { requiresAuth: true, requiresPending: true }
  },

  // Protected routes (requires auth + approved status)
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { requiresAuth: true, requiresApproved: true }
  },
  {
    path: '/events',
    name: 'events',
    component: EventsView,
    meta: { requiresAuth: true, requiresApproved: true }
  },
  {
    path: '/parties',
    name: 'parties',
    component: PartiesView,
    meta: { requiresAuth: true, requiresApproved: true }
  },
  {
    path: '/siege-war',
    name: 'siege-war',
    component: SiegeWarView,
    meta: { requiresAuth: true, requiresApproved: true }
  },
  {
    path: '/channels',
    name: 'channels',
    component: ChannelsView,
    meta: { requiresAuth: true, requiresApproved: true }
  },
  {
    path: '/channels/:id',
    name: 'chat',
    component: ChatView,
    meta: { requiresAuth: true, requiresApproved: true }
  },
  {
    path: '/members',
    name: 'members',
    component: MembersView,
    meta: { requiresAuth: true, requiresApproved: true }
  },
  {
    path: '/members/:id',
    name: 'member-profile',
    component: MemberProfileView,
    meta: { requiresAuth: true, requiresApproved: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: { requiresAuth: true, requiresApproved: true }
  },

  // Admin routes (requires leader role)
  {
    path: '/admin/pending',
    name: 'admin-pending',
    component: PendingMembersView,
    meta: { requiresAuth: true, requiresApproved: true, requiresLeader: true }
  },
  {
    path: '/admin/banned',
    name: 'admin-banned',
    component: BannedUsersView,
    meta: { requiresAuth: true, requiresApproved: true, requiresLeader: true }
  },

  // Catch-all redirect
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory('/web/'),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// Navigation guards
router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  // Wait for auth initialization
  if (!authStore.initialized) {
    await authStore.checkStatus()
  }

  const isAuthenticated = authStore.isAuthenticated
  const isApproved = authStore.isApproved
  const isPending = authStore.isPending
  const isBanned = authStore.isBanned
  const isLeader = authStore.isLeader

  // Guest-only routes (login, register)
  if (to.meta.guestOnly && isAuthenticated) {
    if (isPending) {
      return next({ name: 'pending' })
    }
    if (isBanned) {
      authStore.logout()
      return next({ name: 'login' })
    }
    return next({ name: 'home' })
  }

  // Routes that require authentication
  if (to.meta.requiresAuth && !isAuthenticated) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  // Handle banned users
  if (isAuthenticated && isBanned) {
    authStore.logout()
    return next({ name: 'login' })
  }

  // Routes that require pending status
  if (to.meta.requiresPending) {
    if (!isPending) {
      return next({ name: 'home' })
    }
    return next()
  }

  // Routes that require approved status
  if (to.meta.requiresApproved && !isApproved) {
    if (isPending) {
      return next({ name: 'pending' })
    }
    return next({ name: 'login' })
  }

  // Routes that require leader role
  if (to.meta.requiresLeader && !isLeader) {
    return next({ name: 'home' })
  }

  next()
})

export default router
