import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

declare module 'vue-router' { interface RouteMeta { requiresAuth?: boolean; title?: string } }

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
  {
    path: '/', component: MainLayout,
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
      { path: 'shops', name: 'shops', component: () => import('@/views/ShopsView.vue'), meta: { title: '店铺中心' } },
      { path: 'shops/new', name: 'shop-new', component: () => import('@/views/ShopEditorView.vue'), meta: { requiresAuth: true, title: '新增店铺' } },
      { path: 'shops/:id/edit', name: 'shop-edit', component: () => import('@/views/ShopEditorView.vue'), meta: { requiresAuth: true, title: '编辑店铺' } },
      { path: 'shops/:id', name: 'shop-detail', component: () => import('@/views/ShopDetailView.vue'), meta: { title: '店铺详情' } },
      { path: 'blogs', name: 'blogs', component: () => import('@/views/BlogsView.vue'), meta: { title: '热门笔记' } },
      { path: 'blogs/new', name: 'blog-new', component: () => import('@/views/BlogEditorView.vue'), meta: { requiresAuth: true, title: '发布笔记' } },
      { path: 'blogs/mine', name: 'blog-mine', component: () => import('@/views/MyBlogsView.vue'), meta: { requiresAuth: true, title: '我的笔记' } },
      { path: 'blogs/:id', name: 'blog-detail', component: () => import('@/views/BlogDetailView.vue'), meta: { requiresAuth: true, title: '笔记详情' } },
      { path: 'feed', name: 'feed', component: () => import('@/views/FeedView.vue'), meta: { requiresAuth: true, title: '关注动态' } },
      { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { requiresAuth: true, title: '个人中心' } },
      { path: 'users/:id', name: 'user-detail', component: () => import('@/views/UserDetailView.vue'), meta: { requiresAuth: true, title: '用户资料' } },
      { path: 'vouchers', name: 'vouchers', component: () => import('@/views/VouchersView.vue'), meta: { title: '优惠券与秒杀' } },
      { path: 'orders/:id', name: 'order-status', component: () => import('@/views/OrderStatusView.vue'), meta: { requiresAuth: true, title: '订单状态' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })
router.beforeEach((to) => {
  const token = localStorage.getItem('hmdp-token')
  if (to.meta.requiresAuth && !token) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && token) return { name: 'home' }
  return true
})
router.afterEach((to) => { document.title = `${to.meta.title || '联调台'} · 黑马点评` })
export default router
