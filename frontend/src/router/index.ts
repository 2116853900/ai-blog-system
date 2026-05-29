import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/Home.vue') },
    { path: '/skills', name: 'skills', component: () => import('../views/Skills.vue') },
    { path: '/mcps', name: 'mcps', component: () => import('../views/Mcps.vue') },
    { path: '/tutorials', name: 'tutorials', component: () => import('../views/Tutorials.vue') },
    { path: '/tutorials/:slug', name: 'tutorial-detail', component: () => import('../views/TutorialDetail.vue') },
    { path: '/api-stations', name: 'api-stations', component: () => import('../views/ApiStations.vue') },
    { path: '/submit', name: 'submit', component: () => import('../views/Submit.vue') },

    { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/Login.vue') },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/admin/posts' },
        { path: 'posts', name: 'admin-posts', component: () => import('../views/admin/AdminPosts.vue'), meta: { requiresAuth: true } },
        { path: 'posts/new', name: 'admin-post-new', component: () => import('../views/admin/AdminPostEdit.vue'), meta: { requiresAuth: true } },
        { path: 'posts/:id/edit', name: 'admin-post-edit', component: () => import('../views/admin/AdminPostEdit.vue'), meta: { requiresAuth: true } },
        { path: 'skills', name: 'admin-skills', component: () => import('../views/admin/AdminSkills.vue'), meta: { requiresAuth: true } },
        { path: 'mcps', name: 'admin-mcps', component: () => import('../views/admin/AdminMcps.vue'), meta: { requiresAuth: true } },
        { path: 'api-stations', name: 'admin-api-stations', component: () => import('../views/admin/AdminApiStations.vue'), meta: { requiresAuth: true } },
        { path: 'comments', name: 'admin-comments', component: () => import('../views/admin/AdminComments.vue'), meta: { requiresAuth: true } },
        { path: 'submissions', name: 'admin-submissions', component: () => import('../views/admin/AdminSubmissions.vue'), meta: { requiresAuth: true } }
      ]
    },

    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    // 直接检查 localStorage，避免 Pinia store 初始化问题
    const token = localStorage.getItem('token')
    console.log('[Router Guard] Checking auth for:', to.path, 'token:', token ? 'exists' : 'missing')
    if (!token) {
      console.log('[Router Guard] Redirecting to login')
      return { name: 'admin-login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

export default router
