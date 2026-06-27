import { client } from '../client'
import type { Result, PageResult } from '../../types/result'

/** Conversation (chat session) header. */
export interface ConversationView {
  id: string
  title: string
  agentId?: string
  agentCode?: string
  providerId?: string
  model?: string
  status: number
  statusLabel: string
  messageCount: number
  lastActiveAt?: string
  createdAt: string
}

/** Single message inside a conversation. */
export interface MessageView {
  id: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL'
  content: string
  toolName?: string
  latencyMs?: number
  finishReason?: string
  createdAt: string
}

export interface ConversationDetail {
  header: ConversationView
  messages: MessageView[]
}

export interface ConversationCreate {
  title?: string
  agentId?: string
  agentCode?: string
  providerId?: string
  model?: string
  systemPrompt?: string
}

export interface AgentView {
  id: string
  code: string
  name: string
  nameEn?: string
  category: string
  provider?: string
  description?: string
  icon?: string
  installCmd?: string
  launchCmd?: string
  defaultModel?: string
  systemPrompt?: string
  enabled: number
  builtIn: number
  sort: number
}

export interface McpServerView {
  id: string
  code: string
  name: string
  transport: 'STDIO' | 'SSE' | 'HTTP'
  command?: string
  args?: string
  endpoint?: string
  envJson?: string
  description?: string
  toolCount: number
  status: number
  lastCheckAt?: string
  lastError?: string
  sort: number
}

export interface ProviderView {
  id: string
  code: string
  name: string
  vendor: string
  baseUrl?: string
  apiKeyMasked?: string
  defaultModel?: string
  availableModels?: string
  temperature?: number
  maxTokens?: number
  enabled: number
  isDefault: number
  lastTestAt?: string
  lastTestOk?: number
  sort: number
}

export interface ProviderRequest {
  code?: string
  name?: string
  vendor?: string
  baseUrl?: string
  apiKey?: string
  defaultModel?: string
  availableModels?: string
  temperature?: number
  maxTokens?: number
  enabled?: number
  isDefault?: number
  sort?: number
}

/**
 * AI suite API — aligned with mate-ai service (gateway path /api/v1/ai/**).
 */
export const aiApi = {
  // ---- Conversations (persisted chat) ----
  conversationPage: (params?: { pageNum?: number; pageSize?: number; status?: number; keyword?: string }) =>
    client.get<any, Result<PageResult<ConversationView>>>('/ai/conversations', { params }),
  conversationCreate: (data: ConversationCreate) =>
    client.post<any, Result<ConversationView>>('/ai/conversations', data),
  conversationDetail: (id: string) =>
    client.get<any, Result<ConversationDetail>>(`/ai/conversations/${id}`),
  conversationUpdate: (id: string, data: { title?: string; archive?: boolean }) =>
    client.put<any, Result<void>>(`/ai/conversations/${id}`, data),
  conversationDelete: (id: string) =>
    client.delete<any, Result<void>>(`/ai/conversations/${id}`),
  /** Blocking send — returns the assistant reply. */
  sendMessage: (id: string, message: string) =>
    client.post<any, Result<MessageView>>(`/ai/conversations/${id}/messages`, { message }),
  /** SSE streaming endpoint path — call via {@code fetch} or {@code EventSource}. */
  streamMessageUrl: (id: string) => `/api/v1/ai/conversations/${id}/messages/stream`,

  // ---- Agents ----
  agentPage: (params?: { pageNum?: number; pageSize?: number; keyword?: string; enabled?: number }) =>
    client.get<any, Result<PageResult<AgentView>>>('/ai/agents', { params }),
  agentListEnabled: () =>
    client.get<any, Result<AgentView[]>>('/ai/agents/enabled'),
  agentDetail: (id: string) =>
    client.get<any, Result<AgentView>>(`/ai/agents/${id}`),
  agentCreate: (data: Partial<AgentView>) =>
    client.post<any, Result<string>>('/ai/agents', data),
  agentUpdate: (id: string, data: Partial<AgentView>) =>
    client.put<any, Result<void>>(`/ai/agents/${id}`, data),
  agentDelete: (id: string) =>
    client.delete<any, Result<void>>(`/ai/agents/${id}`),
  agentToggle: (id: string, enabled: boolean) =>
    client.put<any, Result<void>>(`/ai/agents/${id}/enabled`, null, { params: { enabled } }),

  // ---- MCP servers ----
  mcpPage: (params?: { pageNum?: number; pageSize?: number; keyword?: string; status?: number }) =>
    client.get<any, Result<PageResult<McpServerView>>>('/ai/mcp-servers', { params }),
  mcpListEnabled: () =>
    client.get<any, Result<McpServerView[]>>('/ai/mcp-servers/enabled'),
  mcpDetail: (id: string) =>
    client.get<any, Result<McpServerView>>(`/ai/mcp-servers/${id}`),
  mcpCreate: (data: Partial<McpServerView>) =>
    client.post<any, Result<string>>('/ai/mcp-servers', data),
  mcpUpdate: (id: string, data: Partial<McpServerView>) =>
    client.put<any, Result<void>>(`/ai/mcp-servers/${id}`, data),
  mcpDelete: (id: string) =>
    client.delete<any, Result<void>>(`/ai/mcp-servers/${id}`),
  mcpToggle: (id: string, enabled: boolean) =>
    client.put<any, Result<void>>(`/ai/mcp-servers/${id}/enabled`, null, { params: { enabled } }),

  // ---- Providers ----
  providerPage: (params?: { pageNum?: number; pageSize?: number; keyword?: string; vendor?: string; enabled?: number }) =>
    client.get<any, Result<PageResult<ProviderView>>>('/ai/providers', { params }),
  providerListEnabled: () =>
    client.get<any, Result<ProviderView[]>>('/ai/providers/enabled'),
  providerDetail: (id: string) =>
    client.get<any, Result<ProviderView>>(`/ai/providers/${id}`),
  providerCreate: (data: ProviderRequest) =>
    client.post<any, Result<string>>('/ai/providers', data),
  providerUpdate: (id: string, data: ProviderRequest) =>
    client.put<any, Result<void>>(`/ai/providers/${id}`, data),
  providerDelete: (id: string) =>
    client.delete<any, Result<void>>(`/ai/providers/${id}`),
  providerToggle: (id: string, enabled: boolean) =>
    client.put<any, Result<void>>(`/ai/providers/${id}/enabled`, null, { params: { enabled } }),
  providerTest: (id: string) =>
    client.post<any, Result<boolean>>(`/ai/providers/${id}/test`),

  // ---- Tools (exposed by mate-ai-starter — registry of @Tool beans) ----
  toolList: () =>
    client.get<any, Result<any[]>>('/ai/tools'),
}
