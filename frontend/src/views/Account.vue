<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { accountApi, authApi } from '../api'
import type {
  ForumReply,
  ForumSubscriptionSummary,
  ForumThread,
  ForumThreadSubscriptionItem,
  Page,
  ResourceFavoriteItem,
  UserNotification,
  UserProfile
} from '../api/types'
import { useAuthStore } from '../stores/auth'
import { toast } from '../composables/useToast'

type ActivityTab = 'threads' | 'replies' | 'favorites' | 'subscriptions' | 'resources' | 'notifications'
type ActivityItem = ForumThread | ForumReply | ForumThreadSubscriptionItem | ResourceFavoriteItem | UserNotification

const ACTIVITY_PAGE_SIZE = 6
const resourceTypeLabels: Record<ResourceFavoriteItem['refType'], string> = {
  POST: '教程',
  SKILL: 'Skill',
  MCP: 'MCP',
  API: 'API'
}
const auth = useAuthStore()
const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)
const error = ref('')
const profileError = ref('')
const passwordError = ref('')
const profileForm = reactive({ nickname: '', avatarUrl: '', bio: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const activityTab = ref<ActivityTab>('threads')
const activityPage = ref(0)
const activityLoading = ref(false)
const activityError = ref('')
const unreadNotifications = ref(0)
const subscriptionUnreadOnly = ref(false)
const subscriptionSummary = ref<ForumSubscriptionSummary | null>(null)
const myThreads = ref<Page<ForumThread> | null>(null)
const myReplies = ref<Page<ForumReply> | null>(null)
const myFavorites = ref<Page<ForumThread> | null>(null)
const mySubscriptions = ref<Page<ForumThreadSubscriptionItem> | null>(null)
const myResourceFavorites = ref<Page<ResourceFavoriteItem> | null>(null)
const myNotifications = ref<Page<UserNotification> | null>(null)

const currentActivity = computed<Page<ForumThread> | Page<ForumReply> | Page<ForumThreadSubscriptionItem> | Page<ResourceFavoriteItem> | Page<UserNotification> | null>(() => {
  if (activityTab.value === 'threads') return myThreads.value
  if (activityTab.value === 'replies') return myReplies.value
  if (activityTab.value === 'favorites') return myFavorites.value
  if (activityTab.value === 'subscriptions') return mySubscriptions.value
  if (activityTab.value === 'resources') return myResourceFavorites.value
  return myNotifications.value
})
const activityItems = computed<ActivityItem[]>(() => (currentActivity.value?.content || []) as ActivityItem[])
const activityTotal = computed(() => currentActivity.value?.totalElements || 0)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await authApi.me()
    if ('role' in data && 'username' in data) {
      profile.value = data as UserProfile
      if ('id' in data) {
        const p = data as UserProfile
        auth.setProfile(p)
        profileForm.nickname = p.nickname || p.username
        profileForm.avatarUrl = p.avatarUrl || ''
        profileForm.bio = p.bio || ''
      }
    }
    if (profile.value?.id) {
      await refreshUnreadNotifications()
      await refreshSubscriptionSummary()
      await loadActivity(0)
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  profileError.value = ''
  if (!profile.value?.id) {
    profileError.value = '管理员账号暂无公开资料页'
    return
  }
  if (profileForm.nickname.trim().length > 50) {
    profileError.value = '昵称不能超过 50 个字符'
    return
  }
  if (profileForm.bio.trim().length > 500) {
    profileError.value = '简介不能超过 500 个字符'
    return
  }

  profileSaving.value = true
  try {
    const updated = await authApi.updateProfile({
      nickname: profileForm.nickname.trim() || undefined,
      avatarUrl: profileForm.avatarUrl.trim() || undefined,
      bio: profileForm.bio.trim() || undefined
    })
    profile.value = updated
    profileForm.nickname = updated.nickname || updated.username
    profileForm.avatarUrl = updated.avatarUrl || ''
    profileForm.bio = updated.bio || ''
    auth.setProfile(updated)
    toast.success('资料已保存')
  } catch (e: any) {
    profileError.value = e?.response?.data?.message || '保存失败'
  } finally {
    profileSaving.value = false
  }
}

async function changePassword() {
  passwordError.value = ''
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    passwordError.value = '请填写原密码和新密码'
    return
  }
  if (passwordForm.newPassword.length < 8) {
    passwordError.value = '新密码至少 8 位'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }

  passwordSaving.value = true
  try {
    await authApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    toast.success('密码已更新')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e: any) {
    passwordError.value = e?.response?.data?.message || '修改失败'
  } finally {
    passwordSaving.value = false
  }
}

function fmt(d?: string) {
  return d ? new Date(d).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-'
}

function isReply(item: ActivityItem): item is ForumReply {
  return 'threadId' in item && !('title' in item)
}

function isResourceFavorite(item: ActivityItem): item is ResourceFavoriteItem {
  return 'refType' in item && 'refId' in item && 'url' in item
}

function isSubscriptionItem(item: ActivityItem): item is ForumThreadSubscriptionItem {
  return 'subscribedAt' in item && 'unreadReplyCount' in item
}

function isNotification(item: ActivityItem): item is UserNotification {
  return 'linkUrl' in item && 'read' in item && 'type' in item
}

function previewMarkdown(markdown?: string) {
  const text = (markdown || '')
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/\[[^\]]+]\([^)]*\)/g, (match) => match.slice(1, match.indexOf(']')))
    .replace(/[#>*_\-[\]()]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  if (!text) return '暂无内容摘要。'
  return text.length > 120 ? `${text.slice(0, 120)}…` : text
}

function activityTitle(item: ActivityItem) {
  if (isNotification(item)) return item.title
  if (isResourceFavorite(item)) return item.title
  if (isReply(item)) return `回复 #${item.floorNumber} · 帖子 ${item.threadId}`
  return item.title
}

function activityPreview(item: ActivityItem) {
  if (isNotification(item)) return item.message
  if (isResourceFavorite(item)) return item.description || '暂无描述。'
  return previewMarkdown(item.contentMarkdown)
}

function activityMeta(item: ActivityItem) {
  if (isNotification(item)) {
    return `通知 · ${fmt(item.createdAt)}`
  }
  if (isResourceFavorite(item)) {
    return `${resourceTypeLabels[item.refType]} · 收藏于 ${fmt(item.createdAt)}`
  }
  if (isReply(item)) {
    return `${fmt(item.createdAt)} · ${item.likeCount} 赞`
  }
  if (isSubscriptionItem(item)) {
    const unreadText = item.unreadReplyCount > 0 ? `${item.unreadReplyCount} 条未读` : '无未读'
    return `${unreadText} · ${item.subscriberCount} 人关注 · 关注于 ${fmt(item.subscribedAt)}`
  }
  return `${fmt(item.createdAt)} · ${item.replyCount} 回复 · ${item.viewCount} 浏览`
}

function activityLink(item: ActivityItem) {
  if (isNotification(item)) return item.linkUrl
  if (isResourceFavorite(item)) return item.url
  if (isSubscriptionItem(item)) return item.url
  return isReply(item) ? `/forum/threads/${item.threadId}` : `/forum/threads/${item.id}`
}

async function refreshUnreadNotifications() {
  if (!profile.value?.id) return
  try {
    unreadNotifications.value = (await accountApi.unreadNotificationCount()).count
  } catch { /* ignore */ }
}

async function refreshSubscriptionSummary() {
  if (!profile.value?.id) return
  try {
    subscriptionSummary.value = await accountApi.subscriptionSummary()
  } catch { /* ignore */ }
}

async function handleActivityClick(item: ActivityItem) {
  if (!isNotification(item) || item.read) return
  item.read = true
  item.readAt = new Date().toISOString()
  unreadNotifications.value = Math.max(0, unreadNotifications.value - 1)
  try {
    await accountApi.markNotificationRead(item.id)
  } catch {
    await refreshUnreadNotifications()
  }
}

async function markAllNotificationsRead() {
  try {
    const result = await accountApi.markAllNotificationsRead()
    if (myNotifications.value) {
      myNotifications.value.content.forEach(item => {
        item.read = true
        item.readAt = item.readAt || new Date().toISOString()
      })
    }
    unreadNotifications.value = 0
    toast.success(result.affected > 0 ? '通知已全部标记为已读' : '暂无未读通知')
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '操作失败')
  }
}

async function selectActivity(tab: ActivityTab) {
  if (activityTab.value === tab && currentActivity.value) return
  activityTab.value = tab
  activityPage.value = 0
  await loadActivity(0)
}

async function setSubscriptionUnreadOnly(value: boolean) {
  if (subscriptionUnreadOnly.value === value && mySubscriptions.value) return
  subscriptionUnreadOnly.value = value
  activityPage.value = 0
  if (activityTab.value === 'subscriptions') {
    await loadActivity(0)
  }
}

async function loadActivity(nextPage = activityPage.value) {
  if (!profile.value?.id) return
  activityPage.value = nextPage
  activityLoading.value = true
  activityError.value = ''
  try {
    const params = { page: activityPage.value, size: ACTIVITY_PAGE_SIZE }
    if (activityTab.value === 'threads') {
      myThreads.value = await accountApi.threads(params)
    } else if (activityTab.value === 'replies') {
      myReplies.value = await accountApi.replies(params)
    } else if (activityTab.value === 'favorites') {
      myFavorites.value = await accountApi.favorites(params)
    } else if (activityTab.value === 'subscriptions') {
      mySubscriptions.value = await accountApi.subscriptions({ ...params, unreadOnly: subscriptionUnreadOnly.value })
      await refreshSubscriptionSummary()
    } else if (activityTab.value === 'resources') {
      myResourceFavorites.value = await accountApi.resourceFavorites(params)
    } else {
      myNotifications.value = await accountApi.notifications(params)
      await refreshUnreadNotifications()
    }
  } catch (e: any) {
    activityError.value = e?.response?.data?.message || '动态加载失败'
  } finally {
    activityLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <h1 class="section-title prompt">账号中心</h1>
      <p class="muted">查看当前登录信息，并修改论坛账号密码。</p>
      <p v-if="error" class="err">{{ error }}</p>
    </header>

    <div class="grid account-grid">
      <section class="card panel">
        <p class="mono dim">// profile</p>
        <div class="profile-head">
          <img v-if="profileForm.avatarUrl" class="avatar" :src="profileForm.avatarUrl" alt="" />
          <div v-else class="avatar placeholder" aria-hidden="true">{{ (auth.displayName || auth.username || '?').slice(0, 1).toUpperCase() }}</div>
          <div>
            <h2>{{ auth.displayName }}</h2>
            <p class="muted">{{ profile?.bio || '暂无个人简介。' }}</p>
          </div>
        </div>
        <dl class="meta">
          <div><dt>用户名</dt><dd>{{ auth.username }}</dd></div>
          <div><dt>角色</dt><dd>{{ auth.role || 'USER' }}</dd></div>
          <div><dt>等级</dt><dd>{{ profile?.level || 1 }}</dd></div>
          <div><dt>经验</dt><dd>{{ profile?.experiencePoints || 0 }}</dd></div>
        </dl>

        <div class="profile-form">
          <div class="field">
            <label class="label">昵称</label>
            <input class="input" v-model="profileForm.nickname" maxlength="50" :disabled="!profile?.id" />
          </div>
          <div class="field">
            <label class="label">头像链接</label>
            <input class="input" v-model="profileForm.avatarUrl" maxlength="500" placeholder="https://..." :disabled="!profile?.id" />
          </div>
          <div class="field">
            <label class="label">个人简介</label>
            <textarea class="textarea" v-model="profileForm.bio" maxlength="500" :disabled="!profile?.id"></textarea>
          </div>
          <p v-if="profileError" class="err">{{ profileError }}</p>
          <button class="btn btn-primary" :disabled="profileSaving || !profile?.id" @click="saveProfile">
            {{ profileSaving ? '保存中…' : '保存资料' }}
          </button>
        </div>
      </section>

      <section class="card panel">
        <p class="mono dim">// security</p>
        <h2>修改密码</h2>
        <div class="field">
          <label class="label">原密码</label>
          <input class="input" v-model="passwordForm.oldPassword" type="password" />
        </div>
        <div class="field">
          <label class="label">新密码</label>
          <input class="input" v-model="passwordForm.newPassword" type="password" />
        </div>
        <div class="field">
          <label class="label">确认新密码</label>
          <input class="input" v-model="passwordForm.confirmPassword" type="password" />
        </div>
        <p v-if="passwordError" class="err">{{ passwordError }}</p>
        <button class="btn btn-primary" :disabled="passwordSaving" @click="changePassword">
          {{ passwordSaving ? '保存中…' : '保存密码' }}
        </button>
      </section>
    </div>

    <section v-if="profile?.id" class="card panel activity-panel">
      <div class="activity-head">
        <div>
          <p class="mono dim">// activity</p>
          <h2>我的动态</h2>
        </div>
        <div class="activity-head-actions">
          <button
            v-if="activityTab === 'notifications' && unreadNotifications > 0"
            class="btn btn-sm"
            type="button"
            @click="markAllNotificationsRead"
          >
            全部已读
          </button>
          <span class="muted mono">{{ activityTotal }} items</span>
        </div>
      </div>

      <div v-if="subscriptionSummary" class="subscription-stats" aria-label="关注统计">
        <div>
          <span>关注帖子</span>
          <strong>{{ subscriptionSummary.subscribedThreadCount }}</strong>
        </div>
        <div>
          <span>我的帖子被关注</span>
          <strong>{{ subscriptionSummary.receivedSubscriberCount }}</strong>
        </div>
        <div>
          <span>有更新</span>
          <strong>{{ subscriptionSummary.unreadSubscribedThreadCount }}</strong>
        </div>
      </div>

      <div class="activity-tabs" role="tablist" aria-label="账号动态">
        <button
          class="tab-btn"
          :class="{ active: activityTab === 'threads' }"
          type="button"
          role="tab"
          :aria-selected="activityTab === 'threads'"
          @click="selectActivity('threads')"
        >
          我的帖子
        </button>
        <button
          class="tab-btn"
          :class="{ active: activityTab === 'replies' }"
          type="button"
          role="tab"
          :aria-selected="activityTab === 'replies'"
          @click="selectActivity('replies')"
        >
          我的回复
        </button>
        <button
          class="tab-btn"
          :class="{ active: activityTab === 'favorites' }"
          type="button"
          role="tab"
          :aria-selected="activityTab === 'favorites'"
          @click="selectActivity('favorites')"
        >
          帖子收藏
        </button>
        <button
          class="tab-btn"
          :class="{ active: activityTab === 'subscriptions' }"
          type="button"
          role="tab"
          :aria-selected="activityTab === 'subscriptions'"
          @click="selectActivity('subscriptions')"
        >
          帖子关注
        </button>
        <button
          class="tab-btn"
          :class="{ active: activityTab === 'resources' }"
          type="button"
          role="tab"
          :aria-selected="activityTab === 'resources'"
          @click="selectActivity('resources')"
        >
          资源收藏
        </button>
        <button
          class="tab-btn"
          :class="{ active: activityTab === 'notifications' }"
          type="button"
          role="tab"
          :aria-selected="activityTab === 'notifications'"
          @click="selectActivity('notifications')"
        >
          通知
          <span v-if="unreadNotifications" class="tab-count mono">{{ unreadNotifications }}</span>
        </button>
      </div>

      <div v-if="activityTab === 'subscriptions'" class="subscription-tools" aria-label="关注筛选">
        <button
          class="tab-btn"
          :class="{ active: !subscriptionUnreadOnly }"
          type="button"
          @click="setSubscriptionUnreadOnly(false)"
        >
          全部关注
        </button>
        <button
          class="tab-btn"
          :class="{ active: subscriptionUnreadOnly }"
          type="button"
          @click="setSubscriptionUnreadOnly(true)"
        >
          仅看未读
        </button>
      </div>

      <p v-if="activityError" class="err">{{ activityError }}</p>
      <div v-if="activityLoading" class="activity-state muted mono">加载中...</div>
      <div v-else-if="activityItems.length === 0" class="activity-state muted">暂无记录。</div>
      <div v-else class="activity-list">
        <RouterLink
          v-for="item in activityItems"
          :key="`${activityTab}-${item.id}`"
          class="activity-item"
          :class="{ unread: isNotification(item) && !item.read }"
          :to="activityLink(item)"
          @click="handleActivityClick(item)"
        >
          <div class="activity-title-row">
            <strong>{{ activityTitle(item) }}</strong>
            <span v-if="isNotification(item) && !item.read" class="chip chip-active">未读</span>
            <span v-else-if="isResourceFavorite(item)" class="chip chip-active">{{ resourceTypeLabels[item.refType] }}</span>
            <span v-else-if="!isReply(item) && activityTab === 'favorites'" class="chip chip-active">收藏</span>
            <span v-else-if="isSubscriptionItem(item) && item.unread" class="chip chip-active">{{ item.unreadReplyCount }} 未读</span>
            <span v-else-if="isSubscriptionItem(item)" class="chip chip-active">已读</span>
          </div>
          <span class="activity-preview muted">{{ activityPreview(item) }}</span>
          <span class="activity-meta mono dim">{{ activityMeta(item) }}</span>
        </RouterLink>
      </div>

      <div v-if="currentActivity && currentActivity.totalPages > 1" class="activity-pager">
        <button class="btn btn-sm" :disabled="currentActivity.first || activityLoading" @click="loadActivity(activityPage - 1)">上一页</button>
        <span class="muted mono">{{ currentActivity.number + 1 }} / {{ currentActivity.totalPages }}</span>
        <button class="btn btn-sm" :disabled="currentActivity.last || activityLoading" @click="loadActivity(activityPage + 1)">下一页</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; }
.account-grid { grid-template-columns: 1fr 1fr; gap: 18px; }
.panel { padding: 24px; }
.panel h2 { margin: 0 0 12px; }
.profile-head { display: flex; gap: 14px; align-items: center; }
.avatar { width: 58px; height: 58px; border-radius: 50%; object-fit: cover; border: 1px solid var(--border-strong); background: var(--bg-soft); flex-shrink: 0; }
.avatar.placeholder { display: grid; place-items: center; color: var(--primary); font-family: var(--font-mono); font-weight: 800; }
.meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  margin: 20px 0 0;
}
.meta div { padding: 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-soft); }
.meta dt { font-size: 12px; color: var(--text-dim); margin-bottom: 4px; }
.meta dd { margin: 0; font-family: var(--font-mono); }
.profile-form { margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--border); }
.field { margin-bottom: 12px; }
.label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 700; color: var(--text-soft); font-family: var(--font-mono); }
.err { color: var(--danger); margin: 0 0 10px; font-size: 13px; }
.activity-panel { margin-top: 18px; }
.activity-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 14px;
}
.activity-head h2 { margin: 0; }
.activity-head-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}
.activity-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.subscription-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 0 0 14px;
}
.subscription-stats div {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
}
.subscription-stats span {
  display: block;
  color: var(--text-dim);
  font-size: 12px;
  margin-bottom: 4px;
}
.subscription-stats strong {
  font-family: var(--font-mono);
  font-size: 22px;
}
.subscription-tools {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: -4px 0 14px;
}
.tab-btn {
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text-soft);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 7px 12px;
  transition: border-color var(--dur) var(--ease), color var(--dur) var(--ease), background var(--dur) var(--ease);
}
.tab-btn:hover,
.tab-btn.active {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-soft);
}
.tab-count {
  min-width: 18px;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--primary);
  color: var(--bg);
  font-size: 11px;
  font-weight: 800;
}
.activity-state {
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-sm);
  padding: 18px;
  background: var(--bg-inset);
}
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.activity-item {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-soft);
  color: var(--text);
  transition: border-color var(--dur) var(--ease), background var(--dur) var(--ease);
}
.activity-item:hover {
  border-color: var(--primary-dim);
  background: var(--bg-inset);
  text-decoration: none;
}
.activity-item.unread {
  border-color: var(--primary-dim);
  background: color-mix(in srgb, var(--primary-soft) 58%, var(--bg-soft));
}
.activity-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}
.activity-title-row strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.activity-preview {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.55;
}
.activity-meta { font-size: 12px; }
.activity-pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}
@media (max-width: 900px) {
  .account-grid { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .subscription-stats {
    grid-template-columns: 1fr;
  }
  .activity-head,
  .activity-pager {
    flex-direction: column;
    align-items: stretch;
  }
  .activity-head-actions {
    justify-content: flex-start;
  }
  .activity-title-row {
    align-items: flex-start;
    flex-direction: column;
  }
  .activity-title-row strong {
    white-space: normal;
  }
}
</style>
