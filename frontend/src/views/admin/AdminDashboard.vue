<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { adminApi } from '../../api'
import type { AdminDashboard } from '../../api/types'

const data = ref<AdminDashboard | null>(null)
const loading = ref(false)
const error = ref('')

const workload = computed(() => {
  const moderation = data.value?.moderation
  return [
    {
      label: '待审核评论',
      value: moderation?.pendingComments ?? 0,
      to: '/admin/comments',
      hint: '普通内容评论'
    },
    {
      label: '待审核投稿',
      value: moderation?.pendingSubmissions ?? 0,
      to: '/admin/submissions',
      hint: 'Skill、MCP、API 投稿'
    },
    {
      label: '待处理举报',
      value: moderation?.pendingReports ?? 0,
      to: '/admin/reports',
      hint: '帖子、回复、评论举报'
    }
  ]
})

const contentStats = computed(() => {
  const content = data.value?.content
  return [
    { label: '教程', value: content?.posts ?? 0, to: '/admin/posts' },
    { label: 'Skill', value: content?.skills ?? 0, to: '/admin/skills' },
    { label: 'MCP', value: content?.mcps ?? 0, to: '/admin/mcps' },
    { label: 'API 站点', value: content?.apiStations ?? 0, to: '/admin/api-stations' }
  ]
})

const communityStats = computed(() => {
  const community = data.value?.community
  return [
    { label: '用户', value: community?.users ?? 0, to: '/admin/users' },
    { label: '活跃用户', value: community?.activeUsers ?? 0, to: '/admin/users' },
    { label: '封禁用户', value: community?.bannedUsers ?? 0, to: '/admin/users' },
    { label: '帖子', value: community?.threads ?? 0, to: '/admin/forum-posts' },
    { label: '回复', value: community?.replies ?? 0, to: '/admin/forum-replies' }
  ]
})

const apiStatusStats = computed(() => {
  const api = data.value?.apiStations
  return [
    { label: '在线', value: api?.up ?? 0, className: 'badge-up' },
    { label: '不可用', value: api?.down ?? 0, className: 'badge-down' },
    { label: '未知', value: api?.unknown ?? 0, className: 'badge-unknown' }
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await adminApi.dashboard()
  } catch (e: any) {
    error.value = e?.response?.data?.message || '总览数据加载失败'
  } finally {
    loading.value = false
  }
}

function fmt(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value)
}

onMounted(load)
</script>

<template>
  <div>
    <div class="head">
      <div>
        <h1 class="section-title">后台总览</h1>
        <p class="muted">集中查看审核工作量、内容规模和社区状态。</p>
      </div>
      <button class="btn btn-primary" :disabled="loading" @click="load">刷新</button>
    </div>

    <p v-if="loading && !data" class="muted">加载中…</p>
    <div v-else-if="error" class="card error-box">
      <strong>加载失败</strong>
      <p class="muted">{{ error }}</p>
      <button class="btn btn-sm" @click="load">重试</button>
    </div>

    <template v-else-if="data">
      <section class="workload-grid" aria-label="待处理事项">
        <RouterLink v-for="item in workload" :key="item.to" :to="item.to" class="card workload-card">
          <span class="muted">{{ item.label }}</span>
          <strong class="mono">{{ fmt(item.value) }}</strong>
          <small class="muted">{{ item.hint }}</small>
        </RouterLink>
      </section>

      <section class="panel-section">
        <div>
          <h2 class="panel-title">内容库</h2>
          <div class="stat-grid">
            <RouterLink v-for="item in contentStats" :key="item.label" :to="item.to" class="stat-row">
              <span>{{ item.label }}</span>
              <strong class="mono">{{ fmt(item.value) }}</strong>
            </RouterLink>
          </div>
        </div>

        <div>
          <h2 class="panel-title">社区</h2>
          <div class="stat-grid">
            <RouterLink v-for="item in communityStats" :key="item.label" :to="item.to" class="stat-row">
              <span>{{ item.label }}</span>
              <strong class="mono">{{ fmt(item.value) }}</strong>
            </RouterLink>
          </div>
        </div>
      </section>

      <section class="api-section">
        <div class="api-head">
          <h2 class="panel-title">API 站点状态</h2>
          <RouterLink to="/admin/api-stations" class="btn btn-sm">管理站点</RouterLink>
        </div>
        <div class="api-status-grid">
          <div v-for="item in apiStatusStats" :key="item.label" class="status-cell">
            <span class="badge" :class="item.className">{{ item.label }}</span>
            <strong class="mono">{{ fmt(item.value) }}</strong>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.head p { margin: 0; }
.error-box { padding: 18px; max-width: 560px; }
.error-box p { margin: 6px 0 14px; }
.workload-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  margin-bottom: 24px;
}
.workload-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px;
  color: var(--text);
}
.workload-card:hover { text-decoration: none; }
.workload-card strong {
  font-size: 34px;
  line-height: 1;
  color: var(--primary);
}
.workload-card small { font-size: 12px; }
.panel-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}
.panel-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-family: var(--font-mono);
}
.stat-grid {
  display: grid;
  gap: 8px;
}
.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-elevated);
  color: var(--text);
}
.stat-row:hover {
  border-color: var(--primary-dim);
  color: var(--primary);
  text-decoration: none;
}
.api-section {
  border-top: 1px solid var(--border);
  padding-top: 20px;
}
.api-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.api-status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
}
.status-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
}
.status-cell strong { font-size: 22px; }
@media (max-width: 760px) {
  .panel-section { grid-template-columns: 1fr; }
  .head { align-items: stretch; }
  .head .btn { width: 100%; justify-content: center; }
}
</style>
