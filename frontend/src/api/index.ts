import http from './http'
import type {
  Post, Skill, Mcp, ApiStation, Comment, RefType,
  SubmissionType, Submission, AuthResponse, UserProfile,
  ForumCategory, ForumThread, ForumReply, Page
} from './types'

// ---------- 公开接口 ----------
export const publicApi = {
  posts: () => http.get<Post[]>('/posts').then(r => r.data),
  post: (slug: string) => http.get<Post>(`/posts/${slug}`).then(r => r.data),

  skills: (params?: { q?: string; tag?: string; category?: string }) =>
    http.get<Skill[]>('/skills', { params }).then(r => r.data),

  mcps: (params?: { q?: string; tag?: string; category?: string }) =>
    http.get<Mcp[]>('/mcps', { params }).then(r => r.data),

  apiStations: (params?: { q?: string; tag?: string }) =>
    http.get<ApiStation[]>('/api-stations', { params }).then(r => r.data),

  comments: (type: RefType, refId: number) =>
    http.get<Comment[]>('/comments', { params: { type, refId } }).then(r => r.data),

  addComment: (body: { refType: RefType; refId: number; author: string; content: string }) =>
    http.post('/comments', body).then(r => r.data),

  submit: (body: { type: SubmissionType; payloadJson: string; contactInfo?: string }) =>
    http.post('/submissions', body).then(r => r.data)
}

// ---------- 鉴权 ----------
export const authApi = {
  login: (username: string, password: string) =>
    http.post<AuthResponse>('/auth/login', { username, password })
      .then(r => r.data),
  register: (body: { username: string; email: string; password: string; nickname?: string }) =>
    http.post<AuthResponse>('/auth/register', body).then(r => r.data),
  me: () => http.get<UserProfile | { username: string; role: string }>('/auth/me').then(r => r.data),
  changePassword: (body: { oldPassword: string; newPassword: string }) =>
    http.put('/auth/password', body).then(r => r.data)
}

export const forumApi = {
  categories: () => http.get<ForumCategory[]>('/forum/categories').then(r => r.data),
  category: (id: number) => http.get<ForumCategory>(`/forum/categories/${id}`).then(r => r.data),
  threads: (params?: { categoryId?: number; page?: number; size?: number }) =>
    http.get<Page<ForumThread>>('/forum/threads', { params }).then(r => r.data),
  thread: (id: number) => http.get<ForumThread>(`/forum/threads/${id}`).then(r => r.data),
  linkedThreads: (refType: RefType, refId: number) =>
    http.get<ForumThread[]>('/forum/threads/linked', { params: { refType, refId } }).then(r => r.data),
  createThread: (body: { categoryId: number; title: string; contentMarkdown: string; tags?: string; linkedRefType?: RefType; linkedRefId?: number }) =>
    http.post<ForumThread>('/forum/threads', body).then(r => r.data),
  updateThread: (id: number, body: { categoryId: number; title: string; contentMarkdown: string; tags?: string; linkedRefType?: RefType; linkedRefId?: number }) =>
    http.put<ForumThread>(`/forum/threads/${id}`, body).then(r => r.data),
  deleteThread: (id: number) => http.delete(`/forum/threads/${id}`),
  replies: (threadId: number, params?: { page?: number; size?: number }) =>
    http.get<Page<ForumReply>>(`/forum/threads/${threadId}/replies`, { params }).then(r => r.data),
  createReply: (threadId: number, body: { contentMarkdown: string; replyToId?: number }) =>
    http.post<ForumReply>(`/forum/threads/${threadId}/replies`, body).then(r => r.data),
  updateReply: (id: number, body: { contentMarkdown: string; replyToId?: number }) =>
    http.put<ForumReply>(`/forum/replies/${id}`, body).then(r => r.data),
  deleteReply: (id: number) => http.delete(`/forum/replies/${id}`)
}

export const userApi = {
  profile: (id: number) => http.get<UserProfile>(`/users/${id}`).then(r => r.data)
}

// ---------- 后台接口 ----------
export const adminApi = {
  // posts
  posts: () => http.get<Post[]>('/admin/posts').then(r => r.data),
  post: (id: number) => http.get<Post>(`/admin/posts/${id}`).then(r => r.data),
  createPost: (body: Partial<Post>) => http.post<Post>('/admin/posts', body).then(r => r.data),
  updatePost: (id: number, body: Partial<Post>) => http.put<Post>(`/admin/posts/${id}`, body).then(r => r.data),
  publishPost: (id: number, published: boolean) =>
    http.post(`/admin/posts/${id}/publish`, null, { params: { published } }).then(r => r.data),
  deletePost: (id: number) => http.delete(`/admin/posts/${id}`),

  // skills
  skills: () => http.get<Skill[]>('/admin/skills').then(r => r.data),
  createSkill: (body: Partial<Skill>) => http.post<Skill>('/admin/skills', body).then(r => r.data),
  updateSkill: (id: number, body: Partial<Skill>) => http.put<Skill>(`/admin/skills/${id}`, body).then(r => r.data),
  deleteSkill: (id: number) => http.delete(`/admin/skills/${id}`),

  // mcps
  mcps: () => http.get<Mcp[]>('/admin/mcps').then(r => r.data),
  createMcp: (body: Partial<Mcp>) => http.post<Mcp>('/admin/mcps', body).then(r => r.data),
  updateMcp: (id: number, body: Partial<Mcp>) => http.put<Mcp>(`/admin/mcps/${id}`, body).then(r => r.data),
  deleteMcp: (id: number) => http.delete(`/admin/mcps/${id}`),

  // api stations
  apiStations: () => http.get<ApiStation[]>('/admin/api-stations').then(r => r.data),
  createApiStation: (body: Partial<ApiStation>) => http.post<ApiStation>('/admin/api-stations', body).then(r => r.data),
  updateApiStation: (id: number, body: Partial<ApiStation>) => http.put<ApiStation>(`/admin/api-stations/${id}`, body).then(r => r.data),
  deleteApiStation: (id: number) => http.delete(`/admin/api-stations/${id}`),
  checkApiStation: (id: number) => http.post<ApiStation>(`/admin/api-stations/${id}/check`).then(r => r.data),
  checkAllApiStations: () => http.post('/admin/api-stations/check-all'),

  // comments
  comments: (pending?: boolean) =>
    http.get<Comment[]>('/admin/comments', { params: pending ? { pending: true } : {} }).then(r => r.data),
  approveComment: (id: number) => http.post(`/admin/comments/${id}/approve`).then(r => r.data),
  deleteComment: (id: number) => http.delete(`/admin/comments/${id}`),

  // submissions
  submissions: (status?: string) =>
    http.get<Submission[]>('/admin/submissions', { params: status ? { status } : {} }).then(r => r.data),
  approveSubmission: (id: number) => http.post(`/admin/submissions/${id}/approve`).then(r => r.data),
  rejectSubmission: (id: number) => http.post(`/admin/submissions/${id}/reject`).then(r => r.data),
  deleteSubmission: (id: number) => http.delete(`/admin/submissions/${id}`),

  // forum categories
  forumCategories: () => http.get<ForumCategory[]>('/admin/forum-categories').then(r => r.data),
  createForumCategory: (body: Partial<ForumCategory>) => http.post<ForumCategory>('/admin/forum-categories', body).then(r => r.data),
  updateForumCategory: (id: number, body: Partial<ForumCategory>) => http.put<ForumCategory>(`/admin/forum-categories/${id}`, body).then(r => r.data),
  deleteForumCategory: (id: number) => http.delete(`/admin/forum-categories/${id}`)
}
