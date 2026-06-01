<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { authApi } from '../api'
import type { UserProfile } from '../api/types'
import { useAuthStore } from '../stores/auth'
import { toast } from '../composables/useToast'

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
@media (max-width: 900px) {
  .account-grid { grid-template-columns: 1fr; }
}
</style>
