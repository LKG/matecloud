/**
 * Aligned with Java: vip.mate.api.system.response.UserInfoResponse
 * and vip.mate.auth.domain.model.aggregate.AuthUser
 */
export interface UserInfo {
  userId: string
  username: string
  realName?: string
  mobile?: string
  email?: string
  avatar?: string
  gender?: number
  status?: UserStatus
  roleCodes?: string[]
  permissions?: string[]
}

/**
 * Aligned with Java: vip.mate.api.system.enums.UserStatus
 */
export enum UserStatus {
  ACTIVE = 'ACTIVE',
  DISABLED = 'DISABLED',
  DELETED = 'DELETED',
}

/**
 * vip.mate.auth.application.command.PasswordLoginCommand
 * {@code account} is a union of username / mobile / email — the backend's
 * {@code Account.of(String)} classifies it automatically.
 */
export interface PasswordLoginCommand {
  account: string
  password: string
  /** One-time token returned by captcha-plus /captcha/check after slider verification. */
  captchaVerification?: string
}

/** vip.mate.auth.application.command.SmsLoginCommand */
export interface SmsLoginCommand {
  mobile: string
  code: string
}

/** vip.mate.auth.application.command.SmsSendCommand */
export interface SmsSendCommand {
  mobile: string
  /** One-time token returned by captcha-plus /captcha/check after slider verification. */
  captchaVerification?: string
}

/** vip.mate.auth.application.command.SsoLoginCommand (企业微信/钉钉/飞书) */
export interface SsoLoginCommand {
  providerCode: string
  code: string
  state?: string
  redirectUri?: string
}

/**
 * Public, pre-login view of a configured SSO provider (returned by
 * GET /auth/sso/providers). Only non-secret config (corpId, agentId, …).
 */
export interface SsoProvider {
  code: string
  name: string
  authKind: 'OAUTH' | 'DIRECTORY_BIND'
  config: Record<string, string>
}

/** vip.mate.auth.application.command.RegisterCommand */
export interface RegisterCommand {
  username: string
  password: string
  mobile?: string
  smsCode?: string
  email?: string
  realName?: string
  /** One-time token returned by captcha-plus /captcha/check after slider verification. */
  captchaVerification: string
}

/**
 * vip.mate.auth.domain.model.valobj.LoginResult — returned by every login
 * endpoint (and by {@code /auth/register} for auto-login-after-register).
 */
export interface LoginResult {
  userId: string
  username: string
  realName?: string
  mobile?: string
  tokenName: string
  tokenValue: string
  expiresInSeconds: number
  roleCodes?: string[]
  permissions?: string[]
}

/**
 * captcha-plus GET /captcha/get response data.
 * Background image + puzzle piece + AES key for encrypting the drag position.
 */
export interface CaptchaChallenge {
  /** Base64-encoded background image with the puzzle hole cut out. */
  originalImageBase64: string
  /** Base64-encoded jigsaw puzzle piece. */
  jigsawImageBase64: string
  /** Server-side token — pass back on /captcha/check. */
  token: string
  /** 16-char AES-ECB key used to encrypt the drag offset before submission. */
  secretKey: string
}

/**
 * captcha-plus POST /captcha/check response data.
 * The {@code captchaVerification} string is a one-time token valid for ~2 minutes.
 */
export interface CaptchaVerifyResult {
  captchaVerification: string
}
