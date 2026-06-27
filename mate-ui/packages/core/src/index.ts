// Types — aligned with Java mate-api + mate-auth
export type {
  Result,
  PageQuery,
  PageResult,
} from './types/result'
export type {
  UserInfo,
  PasswordLoginCommand,
  SmsLoginCommand,
  SmsSendCommand,
  RegisterCommand,
  LoginResult,
  CaptchaChallenge,
  CaptchaVerifyResult,
} from './types/user'
export { UserStatus } from './types/user'

// API client
export { client } from './api/client'
export { BizError } from './error/BizError'

// URL registry (platform-agnostic; usable from mobile / desktop / admin)
export { Endpoints } from './api/endpoints'
export type { EndpointKey } from './api/endpoints'

// API modules
export { authApi } from './api/modules/auth'
export { userApi } from './api/modules/user'
export { adminApi } from './api/modules/admin'
export type { MenuItem, MenuType, Role, AdminInfo, DictData, Dept } from './api/modules/admin'
export type {
  DictType,
  DictDataItem,
  CreateDictTypeCommand,
  UpdateDictTypeCommand,
  CreateDictDataCommand,
  UpdateDictDataCommand,
  OperationLogDetail,
  LoginLogDetail,
} from './types/admin'
export { configApi } from './api/modules/config'
export type { ConfigItem, ConfigQuery } from './api/modules/config'
export { gatewayApi } from './api/modules/gateway'
export type {
  RouteMode,
  RateLimitKeyType,
  GrayRule,
  RateLimitRule,
  TimeoutRule,
  VersionStat,
  InstanceView,
  ServiceView,
  RouteTestResult,
  TimeoutView,
} from './api/modules/gateway'
export { channelApi } from './api/modules/channel'
export type {
  ChannelView,
  ChannelProviderView,
  ChannelFieldSpec,
  ChannelFieldType,
  ChannelTestResult,
} from './api/modules/channel'
export { ssoApi } from './api/modules/sso'
export type { SsoSyncResult, SsoSyncMode } from './api/modules/sso'
export { modelApi } from './api/modules/model'
export type {
  ModelFieldType,
  ModelFieldSpec,
  ModelProviderView,
  ModelDescriptorView,
  SystemModelView,
  ModelGatewayView,
  ModelTestResult,
  ModelProviderRequest,
  ModelGatewayRequest,
} from './api/modules/model'
export { materialApi } from './api/modules/material'
export type {
  MaterialType,
  MaterialItem,
  MaterialCategory,
  MaterialCategoryList,
  MaterialQuery,
} from './api/modules/material'
export { logApi } from './api/modules/log'
export type {
  OperationLogItem,
  LoginLogItem,
  OperationLogQuery,
  LoginLogQuery,
} from './api/modules/log'
export { dashboardApi } from './api/modules/dashboard'
export type { DashboardVO, DailyStat, ServiceStatus } from './api/modules/dashboard'
export { monitorApi } from './api/modules/monitor'
export type { ServerInfoVO, CacheInfoVO, CacheKeyItem, CacheKeyDetail } from './api/modules/monitor'
export { tenantApi, TenantStatus } from './api/modules/tenant'
export { noticeApi } from './api/modules/notice'
export type { NoticeRecord, NoticeQuery, SendNoticeCommand, NoticeChannel, NoticeBusinessType } from './api/modules/notice'
export type { TenantInfo, TenantPackageInfo, TenantCommand } from './api/modules/tenant'
export { aiApi } from './api/modules/ai'
export type {
  ConversationView, MessageView, ConversationDetail, ConversationCreate,
  AgentView, McpServerView, ProviderView, ProviderRequest,
} from './api/modules/ai'
// Stores
export { useAuthStore } from './stores/auth'
export { useAppStore } from './stores/app'
export { useTenantStore } from './stores/tenant'

// Composables
export { useExport } from './composables/useExport'
export { useImport, type ImportResult } from './composables/useImport'
export { useBatch, type BatchResult, type BatchFailure } from './composables/useBatch'

// Utilities
export { encryptPassword } from './utils/crypto'

// Components
export { default as SliderCaptcha } from './components/SliderCaptcha.vue'
