<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { authApi } from '../api'
import type { UserProfile } from '../api/types'
import { useAuthStore } from '../stores/auth'
import { toast } from '../composables/useToast'

const auth = useAuthStore()
const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await authApi.me()
    if ('role' in data && 'username' in data) {
      profile.value = data as UserProfile
      if ('id' in data) auth.setProfile(data as UserProfile)
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function changePassword() {
  error.value = ''
  if (!form.oldPassword || !form.newPassword) {
    error.value = '请填写原密码和新密码'
    return
  }
  if (form.newPassword.length < 8) {
    error.value = '新密码至少 8 位'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }

  saving.value = true
  try {
    await authApi.changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    toast.success('密码已更新')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
  } catch (e: any) {
    error.value = e?.response?.data?.message || '修改失败'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <h1 class="section-title prompt">账号中心</h1>
      <p class="muted">查看当前登录信息，并修改论坛账号密码。</p>
    </header>

    <div class="grid account-grid">
      <section class="card panel">
        <p class="mono dim">// profile</p>
        <h2>{{ auth.displayName }}</h2>
        <p class="muted">{{ profile?.bio || '暂无个人简介。' }}</p>
        <dl class="meta">
          <div><dt>用户名</dt><dd>{{ auth.username }}</dd></div>
          <div><dt>角色</dt><dd>{{ auth.role || 'USER' }}</dd></div>
          <div><dt>等级</dt><dd>{{ profile?.level || 1 }}</dd></div>
          <div><dt>经验</dt><dd>{{ profile?.experiencePoints || 0 }}</dd></div>
        </dl>
      </section>

      <section class="card panel">
        <p class="mono dim">// security</p>
        <h2>修改密码</h2>
        <div class="field">
          <label class="label">原密码</label>
          <input class="input" v-model="form.oldPassword" type="password" />
        </div>
        <div class="field">
          <label class="label">新密码</label>
          <input class="input" v-model="form.newPassword" type="password" />
        </div>
        <div class="field">
          <label class="label">确认新密码</label>
          <input class="input" v-model="form.confirmPassword" type="password" />
        </div>
        <p v-if="error" class="err">{{ error }}</p>
        <button class="btn btn-primary" :disabled="saving" @click="changePassword">
          {{ saving ? '保存中…' : '保存密码' }}
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
.meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  margin: 20px 0 0;
}
.meta div { padding: 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-soft); }
.meta dt { font-size: 12px; color: var(--text-dim); margin-bottom: 4px; }
.meta dd { margin: 0; font-family: var(--font-mono); }
.field { margin-bottom: 12px; }
.label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 700; color: var(--text-soft); font-family: var(--font-mono); }
.err { color: var(--danger); margin: 0 0 10px; font-size: 13px; }
@media (max-width: 900px) {
  .account-grid { grid-template-columns: 1fr; }
}
</style>
