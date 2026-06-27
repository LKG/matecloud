# RFC-033: Frontend Core Package (API Client, Types, Stores)

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: FE-2 (parallel with RFC-034, RFC-035)

## Background

`mate-ui/packages/core/` is the shared foundation package for the MateCloud frontend monorepo. It provides a typed API client (Axios), TypeScript types aligned with the Java `mate-api` module, Pinia stores for auth/app/tenant/dict state, and a BizError class for structured error handling. All other packages and apps depend on `@matecloud/core`.

## Design

---

## Change 1: package.json

Create `mate-ui/packages/core/package.json`

```json
{
  "name": "@matecloud/core",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "main": "./src/index.ts",
  "types": "./src/index.ts",
  "exports": {
    ".": {
      "import": "./src/index.ts",
      "types": "./src/index.ts"
    },
    "./api": {
      "import": "./src/api/index.ts",
      "types": "./src/api/index.ts"
    },
    "./stores": {
      "import": "./src/stores/index.ts",
      "types": "./src/stores/index.ts"
    },
    "./types": {
      "import": "./src/types/index.ts",
      "types": "./src/types/index.ts"
    }
  },
  "scripts": {
    "lint": "eslint src/",
    "typecheck": "vue-tsc --noEmit"
  },
  "dependencies": {
    "axios": "^1.7.9",
    "pinia": "^3.0.2",
    "vue": "^3.5.13"
  },
  "devDependencies": {
    "typescript": "^5.7.3",
    "vue-tsc": "^2.2.8"
  }
}
```

---

## Change 2: tsconfig.json

Create `mate-ui/packages/core/tsconfig.json`

```json
{
  "extends": "../../tsconfig.base.json",
  "compilerOptions": {
    "composite": true,
    "rootDir": "src",
    "outDir": "dist",
    "declaration": true,
    "declarationMap": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts"],
  "exclude": ["node_modules", "dist"]
}
```

---

## Change 3: Error — BizError

Create `mate-ui/packages/core/src/error/BizError.ts`

```typescript
/**
 * Structured business error aligned with Java BizException.
 * Thrown by the API response interceptor when Result.success === false.
 */
export class BizError extends Error {
  /** Error code from backend ErrorCode enum, e.g. "USRB001" */
  readonly code: string;

  /** Human-readable message */
  readonly msg: string;

  constructor(code: string, msg: string) {
    super(msg);
    this.name = 'BizError';
    this.code = code;
    this.msg = msg;

    // Fix prototype chain for instanceof checks
    Object.setPrototypeOf(this, BizError.prototype);
  }

  /**
   * Check if error matches a specific error code.
   */
  is(code: string): boolean {
    return this.code === code;
  }
}
```

Create `mate-ui/packages/core/src/error/index.ts`

```typescript
export { BizError } from './BizError';
```

---

## Change 4: Types — Result

Create `mate-ui/packages/core/src/types/result.ts`

```typescript
/**
 * Unified API response wrapper, aligned with Java Result<T>.
 */
export interface Result<T = unknown> {
  /** Business status code, "200" for success */
  code: string;
  /** Message */
  msg: string;
  /** Whether the request was successful */
  success: boolean;
  /** Response payload */
  data: T;
}
```

---

## Change 5: Types — Page

Create `mate-ui/packages/core/src/types/page.ts`

```typescript
/**
 * Pagination query params, aligned with Java PageQuery.
 */
export interface PageQuery {
  /** Current page number (1-based) */
  pageNum: number;
  /** Page size */
  pageSize: number;
}

/**
 * Paginated result, aligned with Java PageResult<T>.
 */
export interface PageResult<T> {
  /** Data list for current page */
  list: T[];
  /** Total record count */
  total: number;
}
```

---

## Change 6: Types — User

Create `mate-ui/packages/core/src/types/user.ts`

```typescript
/**
 * User status enum, aligned with Java UserStatus.
 */
export enum UserStatus {
  /** Active */
  ACTIVE = 1,
  /** Disabled */
  DISABLED = 0,
}

/**
 * User info DTO, aligned with Java UserInfoResponse.
 */
export interface UserInfo {
  id: string;
  username: string;
  nickName: string;
  avatar: string;
  mobile: string;
  email: string;
  deptId: string;
  deptName: string;
  status: UserStatus;
  roles: string[];
  permissions: string[];
  createTime: string;
}

/**
 * Login command (password mode), aligned with Java LoginCommand.
 */
export interface LoginCommand {
  username: string;
  password: string;
  captchaKey: string;
  captchaCode: string;
}

/**
 * SMS login command.
 */
export interface SmsLoginCommand {
  mobile: string;
  smsCode: string;
}

/**
 * Login response containing token.
 */
export interface LoginResponse {
  token: string;
  /** Token expiry in seconds */
  expireIn: number;
}

/**
 * Captcha response.
 */
export interface CaptchaResponse {
  /** Unique key to send back with login */
  captchaKey: string;
  /** Base64-encoded captcha image */
  captchaImage: string;
}
```

---

## Change 7: Types — Admin

Create `mate-ui/packages/core/src/types/admin.ts`

```typescript
/**
 * Menu item from backend, aligned with Java MenuItem.
 */
export interface MenuItem {
  id: string;
  parentId: string;
  name: string;
  path: string;
  component: string;
  redirect: string;
  icon: string;
  title: string;
  /** 0=directory, 1=menu, 2=button */
  type: number;
  permission: string;
  sort: number;
  visible: boolean;
  keepAlive: boolean;
  children?: MenuItem[];
}

/**
 * Role DTO.
 */
export interface Role {
  id: string;
  roleName: string;
  roleCode: string;
  sort: number;
  status: number;
  remark: string;
  createTime: string;
}

/**
 * Role create/update command.
 */
export interface RoleCommand {
  id?: string;
  roleName: string;
  roleCode: string;
  sort: number;
  status: number;
  remark: string;
  menuIds: string[];
}

/**
 * Dict type.
 */
export interface DictType {
  id: string;
  dictName: string;
  dictType: string;
  status: number;
  remark: string;
}

/**
 * Dict data item.
 */
export interface DictData {
  id: string;
  dictType: string;
  label: string;
  value: string;
  cssClass: string;
  sort: number;
  status: number;
}
```

---

## Change 8: Types — index

Create `mate-ui/packages/core/src/types/index.ts`

```typescript
export type { Result } from './result';
export type { PageQuery, PageResult } from './page';
export {
  UserStatus,
} from './user';
export type {
  UserInfo,
  LoginCommand,
  SmsLoginCommand,
  LoginResponse,
  CaptchaResponse,
} from './user';
export type {
  MenuItem,
  Role,
  RoleCommand,
  DictType,
  DictData,
} from './admin';
```

---

## Change 9: API Client

Create `mate-ui/packages/core/src/api/client.ts`

```typescript
import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { BizError } from '../error/BizError';
import type { Result } from '../types/result';

/**
 * Create the shared Axios instance.
 * Token and tenant injection happen via interceptors that read from Pinia stores.
 * We lazily import stores to avoid circular dependency issues during module init.
 */
const client: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ---------------------------------------------------------------------------
// Request interceptor
// ---------------------------------------------------------------------------
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Lazy import to break circular dependency between client <-> stores
    // Pinia stores are only available after app.use(pinia), which is fine
    // because no API call is made before the app is mounted.
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    const tenantId = getTenantId();
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId;
    }

    return config;
  },
  (error) => Promise.reject(error),
);

// ---------------------------------------------------------------------------
// Response interceptor
// ---------------------------------------------------------------------------
client.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const result = response.data;

    // If the backend wraps everything in Result<T>, unwrap it
    if (result && typeof result === 'object' && 'success' in result) {
      if (!result.success) {
        throw new BizError(result.code, result.msg);
      }
      // Return unwrapped data — callers get T directly
      return result.data as any;
    }

    // Non-Result responses (e.g. file downloads) pass through
    return response.data as any;
  },
  (error) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;

      if (status === 401) {
        // Clear auth state and redirect to login
        clearAuth();
        const currentPath = window.location.pathname;
        if (currentPath !== '/login') {
          window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
        }
        return Promise.reject(new BizError('AUTH_EXPIRED', 'Login expired, please log in again'));
      }

      if (status === 403) {
        return Promise.reject(new BizError('FORBIDDEN', 'No permission to access this resource'));
      }

      // Try to extract backend Result from error response
      const data = error.response?.data as Result | undefined;
      if (data?.code) {
        return Promise.reject(new BizError(data.code, data.msg));
      }
    }

    return Promise.reject(error);
  },
);

// ---------------------------------------------------------------------------
// Helper functions — read from localStorage as a simple bridge to stores.
// Stores call setToken/setTenantId to keep these in sync.
// ---------------------------------------------------------------------------

const TOKEN_KEY = 'mate_token';
const TENANT_KEY = 'mate_tenant_id';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function getTenantId(): string | null {
  return localStorage.getItem(TENANT_KEY);
}

export function setTenantId(tenantId: string | null): void {
  if (tenantId) {
    localStorage.setItem(TENANT_KEY, tenantId);
  } else {
    localStorage.removeItem(TENANT_KEY);
  }
}

function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export { client };
export default client;
```

---

## Change 10: API Module — Auth

Create `mate-ui/packages/core/src/api/modules/auth.ts`

```typescript
import client from '../client';
import type { LoginCommand, SmsLoginCommand, LoginResponse, CaptchaResponse, UserInfo } from '../../types/user';

/**
 * Password login.
 */
export function login(data: LoginCommand): Promise<LoginResponse> {
  return client.post('/auth/login', data);
}

/**
 * SMS login.
 */
export function smsLogin(data: SmsLoginCommand): Promise<LoginResponse> {
  return client.post('/auth/sms-login', data);
}

/**
 * Logout current session.
 */
export function logout(): Promise<void> {
  return client.post('/auth/logout');
}

/**
 * Get current user info (profile + permissions + menus).
 */
export function getUserInfo(): Promise<UserInfo> {
  return client.get('/auth/info');
}

/**
 * Send SMS verification code.
 */
export function sendSms(mobile: string): Promise<void> {
  return client.post('/auth/sms/send', { mobile });
}

/**
 * Get captcha image for login.
 */
export function getCaptcha(): Promise<CaptchaResponse> {
  return client.get('/auth/captcha');
}
```

---

## Change 11: API Module — User

Create `mate-ui/packages/core/src/api/modules/user.ts`

```typescript
import client from '../client';
import type { PageQuery, PageResult } from '../../types/page';
import type { UserInfo } from '../../types/user';

export interface UserCreateCommand {
  username: string;
  nickName: string;
  password: string;
  mobile: string;
  email: string;
  deptId: string;
  roleIds: string[];
  status: number;
}

export interface UserUpdateCommand extends Partial<UserCreateCommand> {
  id: string;
}

/**
 * Paginated user list.
 */
export function listUsers(params: PageQuery & Record<string, unknown>): Promise<PageResult<UserInfo>> {
  return client.get('/users', { params });
}

/**
 * Get user by ID.
 */
export function getUserById(id: string): Promise<UserInfo> {
  return client.get(`/users/${id}`);
}

/**
 * Create user.
 */
export function createUser(data: UserCreateCommand): Promise<void> {
  return client.post('/users', data);
}

/**
 * Update user.
 */
export function updateUser(data: UserUpdateCommand): Promise<void> {
  return client.put(`/users/${data.id}`, data);
}

/**
 * Delete user by ID.
 */
export function deleteUser(id: string): Promise<void> {
  return client.delete(`/users/${id}`);
}

/**
 * Change current user's nickname.
 */
export function changeNickName(nickName: string): Promise<void> {
  return client.put('/users/nick-name', { nickName });
}
```

---

## Change 12: API Module — Admin

Create `mate-ui/packages/core/src/api/modules/admin.ts`

```typescript
import client from '../client';
import type { MenuItem, Role, RoleCommand, DictData } from '../../types/admin';
import type { PageQuery, PageResult } from '../../types/page';

/**
 * Get menu tree for current user.
 */
export function getMenuTree(): Promise<MenuItem[]> {
  return client.get('/admin/menus/tree');
}

/**
 * Paginated role list.
 */
export function getRoleList(params: PageQuery & Record<string, unknown>): Promise<PageResult<Role>> {
  return client.get('/admin/roles', { params });
}

/**
 * Create role.
 */
export function createRole(data: RoleCommand): Promise<void> {
  return client.post('/admin/roles', data);
}

/**
 * Get dict data by dict type code.
 */
export function getDictByType(dictType: string): Promise<DictData[]> {
  return client.get(`/admin/dicts/type/${dictType}`);
}

/**
 * Get dict data list (paginated).
 */
export function getDictDataList(params: PageQuery & { dictType?: string }): Promise<PageResult<DictData>> {
  return client.get('/admin/dicts/data', { params });
}
```

---

## Change 13: API — index

Create `mate-ui/packages/core/src/api/index.ts`

```typescript
export { client, default as apiClient, getToken, setToken, getTenantId, setTenantId } from './client';
export * as authApi from './modules/auth';
export * as userApi from './modules/user';
export * as adminApi from './modules/admin';
```

---

## Change 14: Store — Auth

Create `mate-ui/packages/core/src/stores/auth.ts`

```typescript
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { setToken as persistToken } from '../api/client';
import { authApi } from '../api';
import type { UserInfo, LoginCommand, SmsLoginCommand, LoginResponse } from '../types/user';
import type { MenuItem } from '../types/admin';

export const useAuthStore = defineStore('auth', () => {
  // -----------------------------------------------------------------------
  // State
  // -----------------------------------------------------------------------
  const token = ref<string | null>(localStorage.getItem('mate_token'));
  const user = ref<UserInfo | null>(null);
  const permissions = ref<string[]>([]);
  const menuTree = ref<MenuItem[]>([]);

  // -----------------------------------------------------------------------
  // Getters
  // -----------------------------------------------------------------------
  const isLoggedIn = computed(() => !!token.value);

  // -----------------------------------------------------------------------
  // Actions
  // -----------------------------------------------------------------------

  /**
   * Password login.
   */
  async function login(cmd: LoginCommand): Promise<LoginResponse> {
    const res = await authApi.login(cmd);
    token.value = res.token;
    persistToken(res.token);
    return res;
  }

  /**
   * SMS login.
   */
  async function smsLogin(cmd: SmsLoginCommand): Promise<LoginResponse> {
    const res = await authApi.smsLogin(cmd);
    token.value = res.token;
    persistToken(res.token);
    return res;
  }

  /**
   * Logout: call backend then clear local state.
   */
  async function logout(): Promise<void> {
    try {
      await authApi.logout();
    } finally {
      token.value = null;
      user.value = null;
      permissions.value = [];
      menuTree.value = [];
      persistToken(null);
    }
  }

  /**
   * Fetch current user info + permissions + menus.
   * Called once after login or on first router navigation.
   */
  async function fetchUserInfo(): Promise<UserInfo> {
    const info = await authApi.getUserInfo();
    user.value = info;
    permissions.value = info.permissions ?? [];
    return info;
  }

  /**
   * Set menu tree (called after fetching from admin API).
   */
  function setMenuTree(menus: MenuItem[]): void {
    menuTree.value = menus;
  }

  /**
   * Check if current user has a specific permission code.
   * Supports wildcard '*' for super admin.
   */
  function hasPermission(perm: string): boolean {
    if (permissions.value.includes('*')) return true;
    return permissions.value.includes(perm);
  }

  return {
    // state
    token,
    user,
    permissions,
    menuTree,
    // getters
    isLoggedIn,
    // actions
    login,
    smsLogin,
    logout,
    fetchUserInfo,
    setMenuTree,
    hasPermission,
  };
});
```

---

## Change 15: Store — App

Create `mate-ui/packages/core/src/stores/app.ts`

```typescript
import { defineStore } from 'pinia';
import { ref } from 'vue';

export type Locale = 'zh-CN' | 'en';

export const useAppStore = defineStore('app', () => {
  // -----------------------------------------------------------------------
  // State
  // -----------------------------------------------------------------------
  const sidebarCollapsed = ref<boolean>(false);
  const darkMode = ref<boolean>(
    localStorage.getItem('mate_dark_mode') === 'true'
    || window.matchMedia('(prefers-color-scheme: dark)').matches,
  );
  const locale = ref<Locale>(
    (localStorage.getItem('mate_locale') as Locale) || 'zh-CN',
  );

  // -----------------------------------------------------------------------
  // Actions
  // -----------------------------------------------------------------------

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  function toggleDarkMode(): void {
    darkMode.value = !darkMode.value;
    localStorage.setItem('mate_dark_mode', String(darkMode.value));
    applyDarkMode(darkMode.value);
  }

  function setLocale(loc: Locale): void {
    locale.value = loc;
    localStorage.setItem('mate_locale', loc);
  }

  function applyDarkMode(dark: boolean): void {
    if (dark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  // Apply on init
  applyDarkMode(darkMode.value);

  return {
    sidebarCollapsed,
    darkMode,
    locale,
    toggleSidebar,
    toggleDarkMode,
    setLocale,
  };
});
```

---

## Change 16: Store — Tenant

Create `mate-ui/packages/core/src/stores/tenant.ts`

```typescript
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { setTenantId as persistTenantId } from '../api/client';

export const useTenantStore = defineStore('tenant', () => {
  const tenantId = ref<string | null>(localStorage.getItem('mate_tenant_id'));
  const tenantName = ref<string>('');

  function setTenant(id: string, name: string): void {
    tenantId.value = id;
    tenantName.value = name;
    persistTenantId(id);
  }

  function clearTenant(): void {
    tenantId.value = null;
    tenantName.value = '';
    persistTenantId(null);
  }

  return {
    tenantId,
    tenantName,
    setTenant,
    clearTenant,
  };
});
```

---

## Change 17: Store — Dict

Create `mate-ui/packages/core/src/stores/dict.ts`

```typescript
import { defineStore } from 'pinia';
import { reactive } from 'vue';
import { adminApi } from '../api';
import type { DictData } from '../types/admin';

export const useDictStore = defineStore('dict', () => {
  /**
   * Cache: dictType -> DictData[]
   * Using a reactive Map so Vue can track changes.
   */
  const dictCache = reactive<Map<string, DictData[]>>(new Map());

  /**
   * In-flight promise cache to prevent duplicate requests for the same type.
   */
  const pendingRequests = new Map<string, Promise<DictData[]>>();

  /**
   * Get dict data by type code. Cache-first strategy:
   * 1. Return from cache if available.
   * 2. Otherwise fetch from backend, cache, and return.
   * 3. Concurrent calls for the same type share one request.
   */
  async function getDict(dictType: string): Promise<DictData[]> {
    // 1. Cache hit
    if (dictCache.has(dictType)) {
      return dictCache.get(dictType)!;
    }

    // 2. Deduplicate in-flight requests
    if (pendingRequests.has(dictType)) {
      return pendingRequests.get(dictType)!;
    }

    // 3. Fetch
    const request = adminApi.getDictByType(dictType).then((data) => {
      dictCache.set(dictType, data);
      pendingRequests.delete(dictType);
      return data;
    }).catch((err) => {
      pendingRequests.delete(dictType);
      throw err;
    });

    pendingRequests.set(dictType, request);
    return request;
  }

  /**
   * Translate a dict value to its label.
   * Returns the raw value if not found.
   */
  function getDictLabel(dictType: string, value: string): string {
    const items = dictCache.get(dictType);
    if (!items) return value;
    const item = items.find((d) => d.value === value);
    return item?.label ?? value;
  }

  /**
   * Clear a specific dict type from cache, or clear all.
   */
  function invalidate(dictType?: string): void {
    if (dictType) {
      dictCache.delete(dictType);
    } else {
      dictCache.clear();
    }
  }

  return {
    dictCache,
    getDict,
    getDictLabel,
    invalidate,
  };
});
```

---

## Change 18: Stores — index

Create `mate-ui/packages/core/src/stores/index.ts`

```typescript
export { useAuthStore } from './auth';
export { useAppStore } from './app';
export type { Locale } from './app';
export { useTenantStore } from './tenant';
export { useDictStore } from './dict';
```

---

## Change 19: Root index

Create `mate-ui/packages/core/src/index.ts`

```typescript
// --- Error ---
export { BizError } from './error/BizError';

// --- Types ---
export type {
  Result,
} from './types/result';
export type {
  PageQuery,
  PageResult,
} from './types/page';
export {
  UserStatus,
} from './types/user';
export type {
  UserInfo,
  LoginCommand,
  SmsLoginCommand,
  LoginResponse,
  CaptchaResponse,
} from './types/user';
export type {
  MenuItem,
  Role,
  RoleCommand,
  DictType,
  DictData,
} from './types/admin';

// --- API ---
export { client, apiClient, getToken, setToken, getTenantId, setTenantId } from './api/client';
export { authApi, userApi, adminApi } from './api';

// --- Stores ---
export { useAuthStore } from './stores/auth';
export { useAppStore } from './stores/app';
export type { Locale } from './stores/app';
export { useTenantStore } from './stores/tenant';
export { useDictStore } from './stores/dict';
```
