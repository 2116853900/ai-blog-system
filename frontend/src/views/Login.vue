<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { authApi } from '../api'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const error = ref('')

const form = reactive({
  username: '',
  email: '',
  nickname: '',
  password: ''
})

const title = computed(() => mode.value === 'login' ? '用户登录' : '注册账号')

function targetPath() {
  const redirect = (route.query.redirect as string) || '/forum'
  return redirect.startsWith('/admin') ? '/forum' : redirect
}

async function submit() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  if (mode.value === 'register' && !form.email) {
    error.value = '请输入邮箱'
    return
  }

  loading.value = true
  try {
    const res = mode.value === 'login'
      ? await authApi.login(form.username, form.password)
      : await authApi.register({
          username: form.username,
          email: form.email,
          password: form.password,
          nickname: form.nickname || undefined
        })
    auth.setAuth(res)
    router.push(targetPath())
  } catch (e: any) {
    error.value = e?.response?.data?.message || (mode.value === 'login' ? '登录失败' : '注册失败')
  } finally {
    loading.value = false
  }
}

function switchMode(next: 'login' | 'register') {
  mode.value = next
  error.value = ''
}
</script>

<template>
  <div class="container page auth-page">
    <section class="card auth-card">
      <div class="auth-copy">
        <p class="mono dim">// community access</p>
        <h1 class="section-title prompt">{{ title }}</h1>
        <p class="muted">登录后可以在论坛发帖、回帖，并维护自己的账号信息。</p>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <div class="tabs" role="tablist">
          <button type="button" class="btn" :class="{ 'btn-primary': mode === 'login' }" @click="switchMode('login')">登录</button>
          <button type="button" class="btn" :class="{ 'btn-primary': mode === 'register' }" @click="switchMode('register')">注册</button>
        </div>

        <label class="label" for="login-username">用户名</label>
        <input id="login-username" class="input" v-model.trim="form.username" autocomplete="username" />

        <template v-if="mode === 'register'">
          <label class="label" for="login-email">邮箱</label>
          <input id="login-email" class="input" v-model.trim="form.email" type="email" autocomplete="email" />

          <label class="label" for="login-nickname">昵称（可选）</label>
          <input id="login-nickname" class="input" v-model.trim="form.nickname" />
        </template>

        <label class="label" for="login-password">密码</label>
        <input id="login-password" class="input" v-model="form.password" type="password" autocomplete="current-password" />

        <p v-if="error" class="err">{{ error }}</p>
        <button class="btn btn-primary submit" :disabled="loading" type="submit">
          {{ loading ? '处理中…' : title }}
        </button>
        <RouterLink to="/admin/login" class="admin-link">管理员登录 →</RouterLink>
      </form>
    </section>
  </div>
</template>

<style scoped>
.page { padding: 42px 0 70px; }
.auth-page { max-width: 880px; }
.auth-card {
  display: grid;
  grid-template-columns: 1fr minmax(300px, 380px);
  gap: 28px;
  padding: 28px;
  overflow: hidden;
}
.auth-copy {
  min-height: 390px;
  padding: 16px;
  border-radius: var(--radius);
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--primary) 18%, transparent), transparent 34%),
    linear-gradient(135deg, var(--bg-soft), transparent);
}
.auth-copy p { max-width: 430px; }
.auth-form { display: flex; flex-direction: column; gap: 9px; }
.tabs { display: flex; gap: 8px; margin-bottom: 8px; }
.label { font-size: 13px; font-weight: 700; color: var(--text-soft); font-family: var(--font-mono); }
.err { margin: 0; color: var(--danger); font-size: 13px; }
.submit { justify-content: center; margin-top: 8px; }
.admin-link { align-self: center; font-size: 13px; color: var(--text-dim); }
@media (max-width: 760px) {
  .auth-card { grid-template-columns: 1fr; }
  .auth-copy { min-height: auto; }
}
</style>
