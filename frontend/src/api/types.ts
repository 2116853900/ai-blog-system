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

export interface Comment {
  id: number
  refType: RefType
  refId: number
  author: string
  content: string
  approved: boolean
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
