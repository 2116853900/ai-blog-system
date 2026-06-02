export interface Post {
  id: number
  title: string
  slug: string
  summary?: string
  bodyMarkdown?: string
  tags?: string
  category?: string
  published: boolean
  createdAt: string
  updatedAt: string
}

export interface Skill {
  id: number
  name: string
  description?: string
  link?: string
  tags?: string
  category?: string
  recommendLevel?: number
  createdAt?: string
}

export interface Mcp {
  id: number
  name: string
  description?: string
  repoUrl?: string
  installCmd?: string
  tags?: string
  category?: string
  recommendLevel?: number
  createdAt?: string
}

export type ApiStatus = 'UP' | 'DOWN' | 'UNKNOWN'

export interface ApiStation {
  id: number
  name: string
  baseUrl: string
  description?: string
  supportedModels?: string
  tags?: string
  status: ApiStatus
  latencyMs?: number
  lastCheckedAt?: string
  createdAt?: string
}

export type RefType = 'POST' | 'SKILL' | 'MCP' | 'API'
export type CommentStatus = 'NORMAL' | 'HIDDEN' | 'DELETED'

export interface Comment {
  id: number
  refType: RefType
  refId: number
  author: string
  content: string
  approved: boolean
  status?: CommentStatus
  createdAt: string
}

export type SubmissionType = 'SKILL' | 'MCP' | 'API'
export type SubmissionStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface Submission {
  id: number
  type: SubmissionType
  payloadJson: string
  contactInfo?: string
  status: SubmissionStatus
  createdAt: string
}

export type AuthRole = 'USER' | 'MODERATOR' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'BANNED' | 'INACTIVE'

export interface AuthResponse {
  token: string
  username: string
  role: AuthRole
  userId?: number
  nickname?: string
  message?: string
}

export interface UserProfile {
  id?: number
  username: string
  nickname?: string
  avatarUrl?: string
  bio?: string
  role: AuthRole
  level?: number
  experiencePoints?: number
  createdAt?: string
}

export interface AdminForumUser {
  id: number
  username: string
  email: string
  nickname?: string
  avatarUrl?: string
  bio?: string
  role: AuthRole
  status: UserStatus
  banReason?: string
  banStartTime?: string
  banEndTime?: string
  banOperatorUsername?: string
  level: number
  experiencePoints: number
  createdAt: string
  lastLoginAt?: string
}

export interface ForumCategory {
  id: number
  name: string
  slug: string
  description?: string
  icon?: string
  sortOrder: number
  parentId?: number | null
  active: boolean
  threadCount: number
  createdAt?: string
}

export type ThreadStatus = 'NORMAL' | 'PINNED' | 'FEATURED' | 'LOCKED' | 'HIDDEN' | 'DELETED'
export type ReplyStatus = 'NORMAL' | 'HIDDEN' | 'DELETED'

export interface ForumThread {
  id: number
  categoryId: number
  authorId: number
  title: string
  contentMarkdown: string
  tags?: string
  status: ThreadStatus
  viewCount: number
  replyCount: number
  likeCount: number
  favoriteCount: number
  reportCount: number
  lastReplyUserId?: number
  lastReplyAt?: string
  linkedRefType?: RefType
  linkedRefId?: number
  createdAt: string
  updatedAt: string
}

export interface ForumReply {
  id: number
  threadId: number
  authorId: number
  floorNumber: number
  contentMarkdown: string
  replyToId?: number
  replyToUserId?: number
  likeCount: number
  reportCount: number
  status: ReplyStatus
  createdAt: string
  updatedAt: string
}

export interface ForumInteraction {
  liked: boolean
  favorited: boolean
  likeCount: number
  favoriteCount: number
}

export type ReportTargetType = 'POST' | 'REPLY' | 'COMMENT'
export type ReportReasonType = 'SPAM' | 'ABUSE' | 'PORN' | 'POLITICS' | 'ILLEGAL' | 'COPYRIGHT' | 'OTHER'
export type ReportStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CLOSED'

export interface ContentReport {
  id: number
  targetType: ReportTargetType
  targetId: number
  targetAuthorId?: number
  reporterId: number
  reasonType: ReportReasonType
  reasonText?: string
  contentSnapshot: string
  status: ReportStatus
  reviewerId?: number
  reviewerUsername?: string
  reviewResult?: string
  reviewNote?: string
  reviewedAt?: string
  createdAt: string
  updatedAt: string
}

export interface ContentReportTarget {
  targetType: ReportTargetType
  targetId: number
  exists: boolean
  status?: string
  authorId?: number
  authorName?: string
  title?: string
  content?: string
  refType?: string
  refId?: number
  createdAt?: string
  updatedAt?: string
}

export interface Page<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface AdminOperationLog {
  id: number
  operatorUsername: string
  action: string
  targetType: string
  targetId: number
  detail?: string
  createdAt: string
}

export interface AdminDashboard {
  moderation: {
    pendingComments: number
    pendingSubmissions: number
    pendingReports: number
  }
  content: {
    posts: number
    skills: number
    mcps: number
    apiStations: number
  }
  community: {
    users: number
    activeUsers: number
    bannedUsers: number
    threads: number
    replies: number
  }
  apiStations: {
    up: number
    down: number
    unknown: number
  }
}

export type GlobalSearchType = 'POST' | 'SKILL' | 'MCP' | 'API' | 'FORUM_THREAD'

export interface GlobalSearchItem {
  type: GlobalSearchType
  id: number
  title: string
  description?: string
  url: string
  category?: string
  tags?: string
  meta?: string
  createdAt?: string
}

export interface GlobalSearchGroup {
  type: GlobalSearchType
  label: string
  items: GlobalSearchItem[]
}

export interface GlobalSearchResponse {
  query: string
  totalCount: number
  groups: GlobalSearchGroup[]
}
