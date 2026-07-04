import { client } from '../client'
import type { Result } from '../../types/result'
import type {
  CaptchaChallenge,
  CaptchaVerifyResult,
  LoginResult,
  PasswordLoginCommand,
  RegisterCommand,
  SmsLoginCommand,
  SmsSendCommand,
  SsoLoginCommand,
  SsoProvider,
  UserInfo,
} from '../../types/user'

/**
 * Auth API — aligned with mate-auth DDD controllers:
 * - AuthController     : /auth/login, /auth/logout, /auth/info, /auth/sms/send, /auth/sms/login
 * - RegisterController : /auth/register
 * - CaptchaController  : auto-registered by captcha-plus at /captcha/get and /captcha/check
 *                        (gateway rewrites /api/v1/auth/captcha/** → /captcha/**)
 */
export const authApi = {
  // ---- Password login (account = username / mobile / email) ----
  login: (data: PasswordLoginCommand) =>
    client.post<any, Result<LoginResult>>('/auth/login', data),

  // ---- SMS login ----
  sendSms: (payload: SmsSendCommand) =>
    client.post<any, Result<void>>('/auth/sms/send', payload),

  smsLogin: (payload: SmsLoginCommand) =>
    client.post<any, Result<LoginResult>>('/auth/sms/login', payload),

  // ---- Third-party SSO (企业微信 / 钉钉 / 飞书) ----
  /** Public list of configured providers (non-secret config) for the login page. */
  ssoProviders: () =>
    client.get<any, Result<SsoProvider[]>>('/auth/sso/providers'),

  ssoLogin: (payload: SsoLoginCommand) =>
    client.post<any, Result<LoginResult>>('/auth/sso/login', payload),

  // ---- Register (auto-login after create) ----
  register: (payload: RegisterCommand) =>
    client.post<any, Result<LoginResult>>('/auth/register', payload),

  // ---- Silent session renewal (无感刷新) ----
  /** Exchange a refresh token for a fresh access + refresh token pair. */
  refresh: (refreshToken: string) =>
    client.post<any, Result<LoginResult>>('/auth/refresh', { refreshToken }),

  // ---- Session lifecycle ----
  logout: () => client.post<any, Result<void>>('/auth/logout'),

  getUserInfo: () => client.get<any, Result<UserInfo>>('/auth/info'),

  // ---- Behavioral slider captcha (captcha-plus) ----
  /** Fetch background + puzzle images and AES key for a new slider challenge. */
  getCaptchaChallenge: (captchaType = 'blockPuzzle') =>
    client.get<any, Result<CaptchaChallenge>>('/auth/captcha/get', { params: { captchaType } }),

  /**
   * Submit the AES-ECB encrypted drag position.
   * On success, `repData.captchaVerification` is the one-time token for the login request.
   */
  checkCaptcha: (payload: { captchaType: string; token: string; pointJson: string }) =>
    client.post<any, Result<CaptchaVerifyResult>>('/auth/captcha/check', payload),
}
