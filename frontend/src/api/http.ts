import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'
import { toast } from '../composables/useToast'

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

// 响应拦截：401 时登出并跳登录；其余错误弹出全局 toast
http.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && (err.response.status === 401 || err.response.status === 403)) {
      const auth = useAuthStore()
      if (auth.isLoggedIn()) {
        auth.logout()
        router.push(router.currentRoute.value.path.startsWith('/admin') ? '/admin/login' : '/login')
        toast.error(err.response.status === 401 ? '登录已过期，请重新登录' : '登录状态无效，请重新登录')
      }
    } else if (err.code === 'ERR_NETWORK' || err.code === 'ECONNABORTED') {
      toast.error('网络连接失败，请检查后端服务')
    } else if (err.response && err.response.status >= 500) {
      toast.error('服务器开小差了，请稍后再试')
    }
    return Promise.reject(err)
  }
)

export default http
