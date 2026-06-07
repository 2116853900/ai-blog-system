import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/Home.vue') },
    { path: '/search', name: 'search', component: () => import('../views/Search.vue') },
    { path: '/stats', name: 'stats', component: () => import('../views/Stats.vue') },
    { path: '/skills', name: 'skills', component: () => import('../views/Skills.vue') },
    { path: '/skills/:id', name: 'skill-detail', component: () => import('../views/SkillDetail.vue') },
    { path: '/mcps', name: 'mcps', component: () => import('../views/Mcps.vue') },
    { path: '/mcps/:id', name: 'mcp-detail', component: () => import('../views/McpDetail.vue') },
    { path: '/tutorials', name: 'tutorials', component: () => import('../views/Tutorials.vue') },
    { path: '/tutorials/:slug', name: 'tutorial-detail', component: () => import('../views/TutorialDetail.vue') },
    { path: '/api-stations', name: 'api-stations', component: () => import('../views/ApiStations.vue') },
    { path: '/api-stations/:id', name: 'api-station-detail', component: () => import('../views/ApiStationDetail.vue') },
    { path: '/forum', name: 'forum', component: () => import('../views/Forum.vue') },
    { path: '/forum/new', name: 'forum-new', component: () => import('../views/ForumNew.vue'), meta: { requiresAuth: true } },
    { path: '/forum/threads/:id/edit', name: 'forum-thread-edit', component: () => import('../views/ForumNew.vue'), meta: { requiresAuth: true } },
    { path: '/forum/threads/:id', name: 'forum-thread', component: () => import('../views/ForumThreadDetail.vue') },
    { path: '/users/:id', name: 'user-profile', component: () => import('../views/UserProfile.vue') },
    { path: '/submit', name: 'submit', component: () => import('../views/Submit.vue') },
    { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
    { path: '/account', name: 'account', component: () => import('../views/Account.vue'), meta: { requiresAuth: true } },

    { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/Login.vue') },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        { path: '', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboard.vue'), meta: { requiresAdmin: true } },
        { path: 'operation-logs', name: 'admin-operation-logs', component: () => import('../views/admin/AdminOperationLogs.vue'), meta: { requiresAdmin: true } },
        { path: 'posts', name: 'admin-posts', component: () => import('../views/admin/AdminPosts.vue'), meta: { requiresAdmin: true } },
        { path: 'posts/new', name: 'admin-post-new', component: () => import('../views/admin/AdminPostEdit.vue'), meta: { requiresAdmin: true } },
        { path: 'posts/:id/edit', name: 'admin-post-edit', component: () => import('../views/admin/AdminPostEdit.vue'), meta: { requiresAdmin: true } },
        { path: 'skills', name: 'admin-skills', component: () => import('../views/admin/AdminSkills.vue'), meta: { requiresAdmin: true } },
        { path: 'mcps', name: 'admin-mcps', component: () => import('../views/admin/AdminMcps.vue'), meta: { requiresAdmin: true } },
        { path: 'api-stations', name: 'admin-api-stations', component: () => import('../views/admin/AdminApiStations.vue'), meta: { requiresAdmin: true } },
        { path: 'comments', name: 'admin-comments', component: () => import('../views/admin/AdminComments.vue'), meta: { requiresAdmin: true } },
        { path: 'submissions', name: 'admin-submissions', component: () => import('../views/admin/AdminSubmissions.vue'), meta: { requiresAdmin: true } },
        { path: 'users', name: 'admin-users', component: () => import('../views/admin/AdminUsers.vue'), meta: { requiresAdmin: true } },
        { path: 'reports', name: 'admin-reports', component: () => import('../views/admin/AdminReports.vue'), meta: { requiresAdmin: true } },
        { path: 'forum-posts', name: 'admin-forum-posts', component: () => import('../views/admin/AdminForumPosts.vue'), meta: { requiresAdmin: true } },
        { path: 'forum-replies', name: 'admin-forum-replies', component: () => import('../views/admin/AdminForumReplies.vue'), meta: { requiresAdmin: true } },
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
