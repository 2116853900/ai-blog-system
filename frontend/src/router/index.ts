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
    { path: '/forum', name: 'forum', component: () => import('../views/Forum.vue') },
    { path: '/forum/new', name: 'forum-new', component: () => import('../views/ForumNew.vue'), meta: { requiresAuth: true } },
    { path: '/forum/threads/:id', name: 'forum-thread', component: () => import('../views/ForumThreadDetail.vue') },
    { path: '/submit', name: 'submit', component: () => import('../views/Submit.vue') },
    { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
    { path: '/account', name: 'account', component: () => import('../views/Account.vue'), meta: { requiresAuth: true } },

    { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/Login.vue') },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        { path: '', redirect: '/admin/posts' },
        { path: 'posts', name: 'admin-posts', component: () => import('../views/admin/AdminPosts.vue'), meta: { requiresAdmin: true } },
        { path: 'posts/new', name: 'admin-post-new', component: () => import('../views/admin/AdminPostEdit.vue'), meta: { requiresAdmin: true } },
        { path: 'posts/:id/edit', name: 'admin-post-edit', component: () => import('../views/admin/AdminPostEdit.vue'), meta: { requiresAdmin: true } },
        { path: 'skills', name: 'admin-skills', component: () => import('../views/admin/AdminSkills.vue'), meta: { requiresAdmin: true } },
        { path: 'mcps', name: 'admin-mcps', component: () => import('../views/admin/AdminMcps.vue'), meta: { requiresAdmin: true } },
        { path: 'api-stations', name: 'admin-api-stations', component: () => import('../views/admin/AdminApiStations.vue'), meta: { requiresAdmin: true } },
        { path: 'comments', name: 'admin-comments', component: () => import('../views/admin/AdminComments.vue'), meta: { requiresAdmin: true } },
        { path: 'submissions', name: 'admin-submissions', component: () => import('../views/admin/AdminSubmissions.vue'), meta: { requiresAdmin: true } },
        { path: 'forum-categories', name: 'admin-forum-categories', component: () => import('../views/admin/AdminForumCategories.vue'), meta: { requiresAdmin: true } }
      ]
    },

    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.requiresAdmin) {
    if (!token || role !== 'ADMIN') {
      return { name: 'admin-login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.requiresAuth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
