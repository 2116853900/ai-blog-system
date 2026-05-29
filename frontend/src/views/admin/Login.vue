<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../../api'
import { useAuthStore } from '../../stores/auth'

const username = ref('')
const password = ref('')
const err = ref('')
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function login() {
  if (!username.value || !password.value) {
    err.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  err.value = ''
  try {
    const res = await authApi.login(username.value, password.value)
    auth.setAuth(res.token, res.username)
    const redirect = (route.query.redirect as string) || '/admin/posts'
    router.push(redirect)
  } catch (e: any) {
    err.value = e?.response?.data?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="card login">
      <h1>🤖 后台登录</h1>
      <p class="muted">AI 信息站管理后台</p>
      <input class="input" v-model="username" placeholder="用户名" @keyup.enter="login" />
      <input class="input" type="password" v-model="password" placeholder="密码" @keyup.enter="login" />
      <p v-if="err" class="err">{{ err }}</p>
      <button class="btn btn-primary block" :disabled="loading" @click="login">
        {{ loading ? '登录中…' : '登录' }}
      </button>
      <RouterLink to="/" class="muted back">← 返回首页</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.login-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }
.login { padding: 32px; width: 100%; max-width: 360px; display: flex; flex-direction: column; gap: 14px; }
.login h1 { margin: 0; }
.login p { margin: 0; }
.block { width: 100%; justify-content: center; }
.err { color: var(--danger); margin: 0; }
.back { text-align: center; font-size: 13px; }
</style>
