import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：自动附带 Bearer token
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// 响应拦截：401 时登出并跳登录
http.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      const auth = useAuthStore()
      if (auth.isLoggedIn()) {
        auth.logout()
        router.push('/admin/login')
      }
    }
    return Promise.reject(err)
  }
)

export default http
