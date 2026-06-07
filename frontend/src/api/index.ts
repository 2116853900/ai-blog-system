import http from './http'
import type {
  Post, Skill, Mcp, ApiStation, ApiStatus, ApiStationStatusCheck, Comment, RefType,
  SubmissionType, Submission, AuthResponse, UserProfile,
  ForumCategory, ForumTagSummary, ForumThread, ForumReply, Page, AdminOperationLog,
  ThreadStatus, ReplyStatus, AdminForumUser, UserStatus, ForumInteraction,
  ResourceFavoriteInteraction, ResourceFavoriteItem, ResourceFavoriteRefType, UserNotification,
  ResourceReview, ResourceReviewSummary,
  ContentReport, ContentReportTarget, ReportReasonType, ReportStatus, ReportTargetType, CommentStatus,
  GlobalSearchResponse, AdminDashboard
} from './types'

// ---------- 公开接口 ----------
export const publicApi = {
  posts: () => http.get<Post[]>('/posts').then(r => r.data),
  post: (slug: string) => http.get<Post>(`/posts/${slug}`).then(r => r.data),
  search: (params?: { q?: string; limit?: number }) =>
    http.get<GlobalSearchResponse>('/search', { params }).then(r => r.data),

  skills: (params?: { q?: string; tag?: string; category?: string }) =>
    http.get<Skill[]>('/skills', { params }).then(r => r.data),
  skill: (id: number) => http.get<Skill>(`/skills/${id}`).then(r => r.data),

  mcps: (params?: { q?: string; tag?: string; category?: string }) =>
    http.get<Mcp[]>('/mcps', { params }).then(r => r.data),
  mcp: (id: number) => http.get<Mcp>(`/mcps/${id}`).then(r => r.data),

  apiStations: (params?: { q?: string; tag?: string; status?: ApiStatus }) =>
    http.get<ApiStation[]>('/api-stations', { params }).then(r => r.data),
  apiStation: (id: number) => http.get<ApiStation>(`/api-stations/${id}`).then(r => r.data),
  apiStationChecks: (id: number, params?: { limit?: number }) =>
    http.get<ApiStationStatusCheck[]>(`/api-stations/${id}/checks`, { params }).then(r => r.data),

  comments: (type: RefType, refId: number) =>
    http.get<Comment[]>('/comments', { params: { type, refId } }).then(r => r.data),

  resourceFavoriteInteraction: (refType: ResourceFavoriteRefType, refId: number) =>
    http.get<ResourceFavoriteInteraction>(`/resource-favorites/${refType}/${refId}`).then(r => r.data),
  resourceReviewSummary: (refType: ResourceFavoriteRefType, refId: number) =>
    http.get<ResourceReviewSummary>(`/resource-reviews/${refType}/${refId}/summary`).then(r => r.data),
  resourceReviews: (refType: ResourceFavoriteRefType, refId: number, params?: { page?: number; size?: number }) =>
    http.get<Page<ResourceReview>>(`/resource-reviews/${refType}/${refId}`, { params }).then(r => r.data),

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
  updateProfile: (body: { nickname?: string; avatarUrl?: string; bio?: string }) =>
    http.put<UserProfile>('/auth/profile', body).then(r => r.data),
  changePassword: (body: { oldPassword: string; newPassword: string }) =>
    http.put('/auth/password', body).then(r => r.data)
}

export const accountApi = {
  threads: (params?: { page?: number; size?: number }) =>
    http.get<Page<ForumThread>>('/account/threads', { params }).then(r => r.data),
  replies: (params?: { page?: number; size?: number }) =>
    http.get<Page<ForumReply>>('/account/replies', { params }).then(r => r.data),
  favorites: (params?: { page?: number; size?: number }) =>
    http.get<Page<ForumThread>>('/account/favorites', { params }).then(r => r.data),
  resourceFavorites: (params?: { page?: number; size?: number }) =>
    http.get<Page<ResourceFavoriteItem>>('/account/resource-favorites', { params }).then(r => r.data),
  favoriteResource: (refType: ResourceFavoriteRefType, refId: number) =>
    http.post<ResourceFavoriteInteraction>(`/account/resource-favorites/${refType}/${refId}`).then(r => r.data),
  unfavoriteResource: (refType: ResourceFavoriteRefType, refId: number) =>
    http.delete<ResourceFavoriteInteraction>(`/account/resource-favorites/${refType}/${refId}`).then(r => r.data),
  upsertResourceReview: (
    refType: ResourceFavoriteRefType,
    refId: number,
    body: { rating: number; content?: string }
  ) => http.post<ResourceReview>(`/account/resource-reviews/${refType}/${refId}`, body).then(r => r.data),
  deleteResourceReview: (refType: ResourceFavoriteRefType, refId: number) =>
    http.delete(`/account/resource-reviews/${refType}/${refId}`),
  notifications: (params?: { page?: number; size?: number }) =>
    http.get<Page<UserNotification>>('/account/notifications', { params }).then(r => r.data),
  unreadNotificationCount: () =>
    http.get<{ count: number }>('/account/notifications/unread-count').then(r => r.data),
  markNotificationRead: (id: number) =>
    http.post<UserNotification>(`/account/notifications/${id}/read`).then(r => r.data),
  markAllNotificationsRead: () =>
    http.post<{ affected: number }>('/account/notifications/read-all').then(r => r.data)
}

export const forumApi = {
  categories: () => http.get<ForumCategory[]>('/forum/categories').then(r => r.data),
  category: (id: number) => http.get<ForumCategory>(`/forum/categories/${id}`).then(r => r.data),
  threads: (params?: { categoryId?: number; q?: string; tag?: string; unanswered?: boolean; solved?: boolean; sort?: 'latest' | 'newest' | 'popular'; page?: number; size?: number }) =>
    http.get<Page<ForumThread>>('/forum/threads', { params }).then(r => r.data),
  popularThreadTags: (params?: { limit?: number }) =>
    http.get<ForumTagSummary[]>('/forum/threads/tags/popular', { params }).then(r => r.data),
  thread: (id: number) => http.get<ForumThread>(`/forum/threads/${id}`).then(r => r.data),
  linkedThreads: (refType: RefType, refId: number) =>
    http.get<ForumThread[]>('/forum/threads/linked', { params: { refType, refId } }).then(r => r.data),
  createThread: (body: { categoryId: number; title: string; contentMarkdown: string; tags?: string; linkedRefType?: RefType; linkedRefId?: number }) =>
    http.post<ForumThread>('/forum/threads', body).then(r => r.data),
  updateThread: (id: number, body: { categoryId: number; title: string; contentMarkdown: string; tags?: string; linkedRefType?: RefType; linkedRefId?: number }) =>
    http.put<ForumThread>(`/forum/threads/${id}`, body).then(r => r.data),
  deleteThread: (id: number) => http.delete(`/forum/threads/${id}`),
  acceptReply: (threadId: number, replyId: number) =>
    http.post<ForumThread>(`/forum/threads/${threadId}/solution`, { replyId }).then(r => r.data),
  clearAcceptedReply: (threadId: number) =>
    http.delete<ForumThread>(`/forum/threads/${threadId}/solution`).then(r => r.data),
  replies: (threadId: number, params?: { page?: number; size?: number }) =>
    http.get<Page<ForumReply>>(`/forum/threads/${threadId}/replies`, { params }).then(r => r.data),
  createReply: (threadId: number, body: { contentMarkdown: string; replyToId?: number }) =>
    http.post<ForumReply>(`/forum/threads/${threadId}/replies`, body).then(r => r.data),
  updateReply: (id: number, body: { contentMarkdown: string; replyToId?: number }) =>
    http.put<ForumReply>(`/forum/replies/${id}`, body).then(r => r.data),
  deleteReply: (id: number) => http.delete(`/forum/replies/${id}`),
  interaction: (threadId: number) =>
    http.get<ForumInteraction>(`/forum/threads/${threadId}/interaction`).then(r => r.data),
  likeThread: (threadId: number) =>
    http.post<ForumInteraction>(`/forum/threads/${threadId}/like`).then(r => r.data),
  unlikeThread: (threadId: number) =>
    http.delete<ForumInteraction>(`/forum/threads/${threadId}/like`).then(r => r.data),
  favoriteThread: (threadId: number) =>
    http.post<ForumInteraction>(`/forum/threads/${threadId}/favorite`).then(r => r.data),
  unfavoriteThread: (threadId: number) =>
    http.delete<ForumInteraction>(`/forum/threads/${threadId}/favorite`).then(r => r.data),
  report: (body: { targetType: ReportTargetType; targetId: number; reasonType: ReportReasonType; reasonText?: string }) =>
    http.post<ContentReport>('/reports', body).then(r => r.data)
}

export const userApi = {
  profile: (id: number) => http.get<UserProfile>(`/users/${id}`).then(r => r.data),
  threads: (id: number, params?: { page?: number; size?: number }) =>
    http.get<Page<ForumThread>>(`/users/${id}/threads`, { params }).then(r => r.data),
  replies: (id: number, params?: { page?: number; size?: number }) =>
    http.get<Page<ForumReply>>(`/users/${id}/replies`, { params }).then(r => r.data)
}

// ---------- 后台接口 ----------
export const adminApi = {
  dashboard: () => http.get<AdminDashboard>('/admin/dashboard').then(r => r.data),
  operationLogs: (params?: {
    operatorUsername?: string
    action?: string
    targetType?: string
    targetId?: number
    createdFrom?: string
    createdTo?: string
    page?: number
    size?: number
  }) => http.get<Page<AdminOperationLog>>('/admin/operation-logs', { params }).then(r => r.data),

  // posts
  posts: (params?: { page?: number; size?: number }) =>
    http.get<Page<Post>>('/admin/posts', { params }).then(r => r.data),
  post: (id: number) => http.get<Post>(`/admin/posts/${id}`).then(r => r.data),
  createPost: (body: Partial<Post>) => http.post<Post>('/admin/posts', body).then(r => r.data),
  updatePost: (id: number, body: Partial<Post>) => http.put<Post>(`/admin/posts/${id}`, body).then(r => r.data),
  publishPost: (id: number, published: boolean) =>
    http.post(`/admin/posts/${id}/publish`, null, { params: { published } }).then(r => r.data),
  deletePost: (id: number) => http.delete(`/admin/posts/${id}`),

  // skills
  skills: (params?: { page?: number; size?: number }) =>
    http.get<Page<Skill>>('/admin/skills', { params }).then(r => r.data),
  createSkill: (body: Partial<Skill>) => http.post<Skill>('/admin/skills', body).then(r => r.data),
  updateSkill: (id: number, body: Partial<Skill>) => http.put<Skill>(`/admin/skills/${id}`, body).then(r => r.data),
  deleteSkill: (id: number) => http.delete(`/admin/skills/${id}`),

  // mcps
  mcps: (params?: { page?: number; size?: number }) =>
    http.get<Page<Mcp>>('/admin/mcps', { params }).then(r => r.data),
  createMcp: (body: Partial<Mcp>) => http.post<Mcp>('/admin/mcps', body).then(r => r.data),
  updateMcp: (id: number, body: Partial<Mcp>) => http.put<Mcp>(`/admin/mcps/${id}`, body).then(r => r.data),
  deleteMcp: (id: number) => http.delete(`/admin/mcps/${id}`),

  // api stations
  apiStations: (params?: { page?: number; size?: number }) =>
    http.get<Page<ApiStation>>('/admin/api-stations', { params }).then(r => r.data),
  createApiStation: (body: Partial<ApiStation>) => http.post<ApiStation>('/admin/api-stations', body).then(r => r.data),
  updateApiStation: (id: number, body: Partial<ApiStation>) => http.put<ApiStation>(`/admin/api-stations/${id}`, body).then(r => r.data),
  deleteApiStation: (id: number) => http.delete(`/admin/api-stations/${id}`),
  checkApiStation: (id: number) => http.post<ApiStation>(`/admin/api-stations/${id}/check`).then(r => r.data),
  checkAllApiStations: () => http.post('/admin/api-stations/check-all'),

  // comments
  comments: (params?: { pending?: boolean; status?: CommentStatus; page?: number; size?: number }) =>
    http.get<Page<Comment>>('/admin/comments', { params }).then(r => r.data),
  approveComment: (id: number) => http.post(`/admin/comments/${id}/approve`).then(r => r.data),
  hideComment: (id: number) => http.post<Comment>(`/admin/comments/${id}/hide`).then(r => r.data),
  restoreComment: (id: number) => http.post<Comment>(`/admin/comments/${id}/restore`).then(r => r.data),
  deleteComment: (id: number) => http.delete(`/admin/comments/${id}`),

  // submissions
  submissions: (params?: { status?: string; page?: number; size?: number }) =>
    http.get<Page<Submission>>('/admin/submissions', { params }).then(r => r.data),
  approveSubmission: (id: number) => http.post(`/admin/submissions/${id}/approve`).then(r => r.data),
  rejectSubmission: (id: number) => http.post(`/admin/submissions/${id}/reject`).then(r => r.data),
  deleteSubmission: (id: number) => http.delete(`/admin/submissions/${id}`),

  // forum categories
  forumCategories: () => http.get<ForumCategory[]>('/admin/forum-categories').then(r => r.data),
  createForumCategory: (body: Partial<ForumCategory>) => http.post<ForumCategory>('/admin/forum-categories', body).then(r => r.data),
  updateForumCategory: (id: number, body: Partial<ForumCategory>) => http.put<ForumCategory>(`/admin/forum-categories/${id}`, body).then(r => r.data),
  deleteForumCategory: (id: number) => http.delete(`/admin/forum-categories/${id}`),

  // forum posts governance
  forumPosts: (params?: {
    q?: string
    author?: string
    authorId?: number
    status?: ThreadStatus
    reported?: boolean
    createdFrom?: string
    createdTo?: string
    page?: number
    size?: number
  }) => http.get<Page<ForumThread>>('/admin/forum/posts', { params }).then(r => r.data),
  forumPost: (id: number) => http.get<ForumThread>(`/admin/forum/posts/${id}`).then(r => r.data),
  forumPostLogs: (id: number) => http.get<AdminOperationLog[]>(`/admin/forum/posts/${id}/operation-logs`).then(r => r.data),
  hideForumPost: (id: number, reason?: string) =>
    http.post<ForumThread>(`/admin/forum/posts/${id}/hide`, { reason }).then(r => r.data),
  restoreForumPost: (id: number, reason?: string) =>
    http.post<ForumThread>(`/admin/forum/posts/${id}/restore`, { reason }).then(r => r.data),
  deleteForumPost: (id: number, reason?: string) =>
    http.delete(`/admin/forum/posts/${id}`, { data: { reason } }),
  batchHideForumPosts: (ids: number[], reason?: string) =>
    http.post<{ affected: number }>('/admin/forum/posts/batch-hide', { ids, reason }).then(r => r.data),
  batchDeleteForumPosts: (ids: number[], reason?: string) =>
    http.post<{ affected: number }>('/admin/forum/posts/batch-delete', { ids, reason }).then(r => r.data),

  // forum replies governance
  forumReplies: (params?: {
    threadId?: number
    author?: string
    authorId?: number
    status?: ReplyStatus
    reported?: boolean
    createdFrom?: string
    createdTo?: string
    page?: number
    size?: number
  }) => http.get<Page<ForumReply>>('/admin/forum/replies', { params }).then(r => r.data),
  forumReply: (id: number) => http.get<ForumReply>(`/admin/forum/replies/${id}`).then(r => r.data),
  forumReplyLogs: (id: number) => http.get<AdminOperationLog[]>(`/admin/forum/replies/${id}/operation-logs`).then(r => r.data),
  hideForumReply: (id: number, reason?: string) =>
    http.post<ForumReply>(`/admin/forum/replies/${id}/hide`, { reason }).then(r => r.data),
  restoreForumReply: (id: number, reason?: string) =>
    http.post<ForumReply>(`/admin/forum/replies/${id}/restore`, { reason }).then(r => r.data),
  deleteForumReply: (id: number, reason?: string) =>
    http.delete(`/admin/forum/replies/${id}`, { data: { reason } }),
  batchHideForumReplies: (ids: number[], reason?: string) =>
    http.post<{ affected: number }>('/admin/forum/replies/batch-hide', { ids, reason }).then(r => r.data),
  batchDeleteForumReplies: (ids: number[], reason?: string) =>
    http.post<{ affected: number }>('/admin/forum/replies/batch-delete', { ids, reason }).then(r => r.data),

  // forum users governance
  forumUsers: (params?: {
    q?: string
    status?: UserStatus
    createdFrom?: string
    createdTo?: string
    page?: number
    size?: number
  }) => http.get<Page<AdminForumUser>>('/admin/users', { params }).then(r => r.data),
  forumUser: (id: number) => http.get<AdminForumUser>(`/admin/users/${id}`).then(r => r.data),
  forumUserLogs: (id: number) => http.get<AdminOperationLog[]>(`/admin/users/${id}/operation-logs`).then(r => r.data),
  forumUserThreads: (id: number, params?: { page?: number; size?: number }) =>
    http.get<Page<ForumThread>>(`/admin/users/${id}/threads`, { params }).then(r => r.data),
  forumUserReplies: (id: number, params?: { page?: number; size?: number }) =>
    http.get<Page<ForumReply>>(`/admin/users/${id}/replies`, { params }).then(r => r.data),
  forumUserReports: (id: number, params?: { page?: number; size?: number }) =>
    http.get<Page<ContentReport>>(`/admin/users/${id}/reports`, { params }).then(r => r.data),
  forumUserReported: (id: number, params?: { page?: number; size?: number }) =>
    http.get<Page<ContentReport>>(`/admin/users/${id}/reported`, { params }).then(r => r.data),
  banForumUser: (id: number, body: { reason?: string; banEndTime?: string }) =>
    http.post<AdminForumUser>(`/admin/users/${id}/ban`, body).then(r => r.data),
  unbanForumUser: (id: number) =>
    http.post<AdminForumUser>(`/admin/users/${id}/unban`).then(r => r.data),

  // content reports
  reports: (params?: {
    targetType?: ReportTargetType
    reasonType?: ReportReasonType
    status?: ReportStatus
    createdFrom?: string
    createdTo?: string
    page?: number
    size?: number
  }) => http.get<Page<ContentReport>>('/admin/reports', { params }).then(r => r.data),
  report: (id: number) => http.get<ContentReport>(`/admin/reports/${id}`).then(r => r.data),
  reportLogs: (id: number) =>
    http.get<AdminOperationLog[]>(`/admin/reports/${id}/operation-logs`).then(r => r.data),
  reportTarget: (id: number) =>
    http.get<ContentReportTarget>(`/admin/reports/${id}/target`).then(r => r.data),
  approveReport: (id: number, body: { reviewNote?: string; hideContent?: boolean; banTargetAuthor?: boolean; banReason?: string; banEndTime?: string }) =>
    http.post<ContentReport>(`/admin/reports/${id}/approve`, body).then(r => r.data),
  rejectReport: (id: number, body: { reviewNote?: string }) =>
    http.post<ContentReport>(`/admin/reports/${id}/reject`, body).then(r => r.data),
  closeReport: (id: number, body: { reviewNote?: string }) =>
    http.post<ContentReport>(`/admin/reports/${id}/close`, body).then(r => r.data)
}
