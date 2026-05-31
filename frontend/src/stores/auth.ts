import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AuthResponse, AuthRole, UserProfile } from '../api/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const username = ref<string | null>(localStorage.getItem('username'))
  const role = ref<AuthRole | null>((localStorage.getItem('role') as AuthRole | null) || null)
  const userId = ref<number | null>(Number(localStorage.getItem('userId')) || null)
  const nickname = ref<string | null>(localStorage.getItem('nickname'))

  function setAuth(data: AuthResponse) {
    token.value = data.token
    username.value = data.username
    role.value = data.role || 'USER'
    userId.value = data.userId || null
    nickname.value = data.nickname || data.username
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', role.value || 'USER')
    if (userId.value) localStorage.setItem('userId', String(userId.value))
    else localStorage.removeItem('userId')
    if (nickname.value) localStorage.setItem('nickname', nickname.value)
    else localStorage.removeItem('nickname')
  }

  function setProfile(profile: UserProfile) {
    username.value = profile.username
    role.value = profile.role
    userId.value = profile.id || null
    nickname.value = profile.nickname || profile.username
    localStorage.setItem('username', profile.username)
    localStorage.setItem('role', profile.role)
    if (profile.id) localStorage.setItem('userId', String(profile.id))
    else localStorage.removeItem('userId')
    if (nickname.value) localStorage.setItem('nickname', nickname.value)
    else localStorage.removeItem('nickname')
  }

  function logout() {
    token.value = null
    username.value = null
    role.value = null
    userId.value = null
    nickname.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('nickname')
  }

  function isLoggedIn() {
    return !!token.value
  }

  const isAdmin = computed(() => role.value === 'ADMIN')
  const isModerator = computed(() => role.value === 'MODERATOR' || role.value === 'ADMIN')
  const displayName = computed(() => nickname.value || username.value || '')

  return {
    token, username, role, userId, nickname,
    isAdmin, isModerator, displayName,
    setAuth, setProfile, logout, isLoggedIn
  }
})
