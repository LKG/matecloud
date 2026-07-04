import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '../api/modules/auth'
import { encryptPassword } from '../utils/crypto'
import type {
  LoginResult,
  PasswordLoginCommand,
  RegisterCommand,
  SmsLoginCommand,
  SsoLoginCommand,
  UserInfo,
} from '../types/user'
import type { MenuItem } from '../api/modules/admin'
import { adminApi } from '../api/modules/admin'

/**
 * Strip BUTTON-type nodes from the menu tree.
 * Buttons (type='F' / 'BUTTON') are permission codes consumed by v-permission,
 * not navigable entries — they must not appear in the sidebar or as routes.
 * The menu-management / role-assignment pages fetch the raw tree separately.
 */
function stripButtons(nodes: MenuItem[]): MenuItem[] {
  return nodes
    .filter((n) => n.type !== 'BUTTON' && n.type !== 'F')
    .map((n) => ({ ...n, children: n.children ? stripButtons(n.children) : undefined }))
}

/**
 * Auth store — manages the Sa-Token session on the frontend.
 * <p>
 * The token value is persisted in {@code localStorage} under {@code mate_token}
 * and the header name Sa-Token expects is persisted under {@code mate_token_name}
 * so the frontend doesn't need to hard-code "Authorization" vs "satoken".
 * <p>
 * The resolved identity (user / permissions / roles / menu tree) is ALSO cached
 * in {@code localStorage} under {@code mate_auth_state}. This lets a page refresh
 * restore the session from cache instead of blocking on a {@code /auth/info}
 * round-trip — so a transient backend hiccup on reload can no longer bounce the
 * user back to the login screen (only a genuine 401 ends the session).
 */
const AUTH_STATE_KEY = 'mate_auth_state'

interface PersistedAuthState {
  user: UserInfo | null
  permissions: string[]
  roleCodes: string[]
  menuTree: MenuItem[]
}

function readAuthState(): PersistedAuthState | null {
  try {
    const raw = localStorage.getItem(AUTH_STATE_KEY)
    return raw ? (JSON.parse(raw) as PersistedAuthState) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('mate_token') || '')
  const tokenName = ref<string>(localStorage.getItem('mate_token_name') || 'Authorization')

  // Restore the cached identity on store creation so a refresh keeps the session
  // alive without a mandatory network call (aligned with how mature admin
  // frontends persist userInfo across reloads).
  const cached = token.value ? readAuthState() : null
  const user = ref<UserInfo | null>(cached?.user ?? null)
  const permissions = ref<string[]>(cached?.permissions ?? [])
  const roleCodes = ref<string[]>(cached?.roleCodes ?? [])
  const menuTree = ref<MenuItem[]>(cached?.menuTree ?? [])

  /** Persist the resolved identity so the next page load can rehydrate it. */
  function persistState() {
    try {
      const state: PersistedAuthState = {
        user: user.value,
        permissions: permissions.value,
        roleCodes: roleCodes.value,
        menuTree: menuTree.value,
      }
      localStorage.setItem(AUTH_STATE_KEY, JSON.stringify(state))
    } catch {
      /* quota / serialization issues are non-fatal — session still works in-memory */
    }
  }

  const isLoggedIn = computed(() => !!token.value)

  // The API client renews the access token silently on 401 and writes the new
  // value straight to localStorage; mirror it into the store so token / isLoggedIn
  // (and the next request header) stay consistent without a page reload.
  if (typeof window !== 'undefined') {
    window.addEventListener('mate:token-refreshed', (e) => {
      const detail = (e as CustomEvent).detail
      if (detail?.token) token.value = detail.token
      if (detail?.tokenName) tokenName.value = detail.tokenName
    })
  }

  function applyLoginResult(result: LoginResult) {
    token.value = result.tokenValue
    tokenName.value = result.tokenName
    localStorage.setItem('mate_token', result.tokenValue)
    localStorage.setItem('mate_token_name', result.tokenName)
    // Persist the refresh token so the API client can silently renew the access
    // token on 401 (see client.ts). Only overwrite when present — a refresh
    // response always carries a rotated one; a legacy backend may omit it.
    if (result.refreshToken) {
      localStorage.setItem('mate_refresh_token', result.refreshToken)
    }
    user.value = {
      userId: result.userId,
      username: result.username,
      realName: result.realName,
      mobile: result.mobile,
      roleCodes: result.roleCodes,
      permissions: result.permissions,
    }
    roleCodes.value = result.roleCodes ?? []
    permissions.value = result.permissions ?? []
    persistState()
  }

  async function loginWithPassword(command: PasswordLoginCommand) {
    // Encrypt password before transport (AES-CFB, backend decrypts)
    const encrypted = { ...command, password: encryptPassword(command.password) }
    const res = await authApi.login(encrypted)
    applyLoginResult(res.data)
    await fetchUserInfo()
  }

  async function loginWithSms(command: SmsLoginCommand) {
    const res = await authApi.smsLogin(command)
    applyLoginResult(res.data)
    await fetchUserInfo()
  }

  async function loginWithSso(command: SsoLoginCommand) {
    const res = await authApi.ssoLogin(command)
    applyLoginResult(res.data)
    await fetchUserInfo()
  }

  async function register(command: RegisterCommand) {
    // Encrypt password before transport (AES-CFB, backend decrypts)
    const encrypted = { ...command, password: encryptPassword(command.password) }
    const res = await authApi.register(encrypted)
    applyLoginResult(res.data)
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    const res = await authApi.getUserInfo()
    user.value = res.data
    permissions.value = res.data.permissions || []
    roleCodes.value = res.data.roleCodes || []

    // Load menu tree best-effort (RBAC may be empty on dev profile).
    // On failure, KEEP any previously cached menu rather than wiping it — a
    // transient menu-endpoint error must not strip the user's navigation on
    // refresh.
    try {
      const menuRes = await adminApi.menuTree()
      menuTree.value = stripButtons(menuRes.data ?? [])
    } catch {
      if (!menuTree.value.length) menuTree.value = []
    }
    persistState()
  }

  /**
   * Wipe all local session state (token + cached identity) WITHOUT calling the
   * backend. Used by the router guard when a genuine 401 proves the token is
   * dead — clearing here keeps the single source of truth in the store.
   */
  function clearSession() {
    token.value = ''
    tokenName.value = 'Authorization'
    user.value = null
    permissions.value = []
    roleCodes.value = []
    menuTree.value = []
    localStorage.removeItem('mate_token')
    localStorage.removeItem('mate_token_name')
    localStorage.removeItem('mate_refresh_token')
    localStorage.removeItem(AUTH_STATE_KEY)
  }

  async function logout() {
    try {
      if (token.value) await authApi.logout()
    } catch {
      /* ignore — always clear local state */
    }
    clearSession()
  }

  /** Check permission — aligned with Sa-Token @SaCheckPermission. */
  function hasPermission(perm: string): boolean {
    if (permissions.value.includes('*')) return true
    return permissions.value.includes(perm)
  }

  /** Check role — aligned with Sa-Token @SaCheckRole. */
  function hasRole(role: string): boolean {
    return roleCodes.value.includes(role)
  }

  return {
    token,
    tokenName,
    user,
    permissions,
    roleCodes,
    menuTree,
    isLoggedIn,
    loginWithPassword,
    loginWithSms,
    loginWithSso,
    register,
    fetchUserInfo,
    logout,
    clearSession,
    hasPermission,
    hasRole,
  }
})
