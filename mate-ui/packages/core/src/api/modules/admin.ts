import { client } from '../client'
import type { Result, PageResult } from '../../types/result'
import type {
  DictType,
  DictDataItem,
  CreateDictTypeCommand,
  CreateDictDataCommand,
} from '../../types/admin'

/**
 * Menu type — backend may emit either the full word form
 * ({@code DIRECTORY}/{@code MENU}/{@code BUTTON}) or the single-letter code
 * ({@code M}/{@code C}/{@code F}). Both are accepted.
 */
export type MenuType = 'DIRECTORY' | 'MENU' | 'BUTTON' | 'M' | 'C' | 'F'

/** Menu item — aligned with mate-admin Menu entity */
export interface MenuItem {
  id: string
  parentId: string
  /** Chinese menu name (always present) */
  name: string
  /** English menu name (nullable — falls back to {@link name}) */
  nameEn?: string
  /** Short name used by narrow rail layouts (e.g. two-column sidebar).
   *  Nullable — falls back to {@link name}. */
  shortName?: string
  path: string
  component: string
  perms: string
  type: MenuType
  icon: string
  sort: number
  children?: MenuItem[]
}

/** Status enum name — project-wide convention for status exposed to the UI. */
export type StatusName = 'ACTIVE' | 'DISABLED'

/** Role — aligned with mate-admin RoleAggregate */
export interface Role {
  id: string
  roleKey: string
  roleName: string
  sort: number
  status: StatusName
  /** Data scope: 1=ALL 2=DEPT 3=DEPT_AND_CHILD 4=SELF 5=CUSTOM. */
  dataScope?: number
  customDeptIds?: string
  menuIds?: string[]
}

/** Department — aligned with mate-admin Dept (self-referencing tree). */
export interface Dept {
  id: string
  parentId: string
  deptName: string
  ancestors?: string
  sort: number
  leader?: string
  phone?: string
  email?: string
  status: number
  children?: Dept[]
}

/** Admin — aligned with mate-admin AdminAggregate */
export interface AdminInfo {
  id: string
  username: string
  nickName: string
  avatar: string
  status: StatusName
  deptId?: string
  roleIds: string[]
  createdAt: string
}

/** Dict data (read-by-type shape) — kept for compatibility with old callers */
export interface DictData {
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
}

/**
 * Admin API — aligned with mate-admin controllers:
 * - AdminController:  /admin/admins
 * - RoleController:   /admin/roles
 * - MenuController:   /admin/menus
 * - DictController:   /admin/dict
 */
export const adminApi = {
  // ---- Admins ----
  adminList: (params?: { pageNum?: number; pageSize?: number; keyword?: string }) =>
    client.get<any, Result<AdminInfo[] | PageResult<AdminInfo>>>('/admin/admins', { params }),
  adminGetById: (id: string) =>
    client.get<any, Result<AdminInfo>>(`/admin/admins/${id}`),
  adminCreate: (data: { username: string; password: string; nickName: string; realName?: string; mobile?: string; email?: string }) =>
    client.post<any, Result<string>>('/admin/admins', data),
  adminUpdate: (id: string, data: { nickName: string; avatar?: string; deptId?: string; realName?: string; mobile?: string; email?: string }) =>
    client.put<any, Result<void>>(`/admin/admins/${id}`, data),

  // ---- Self-service (个人中心 — targets the logged-in mate_admin, NOT mate_user) ----
  /** Update current admin's own profile. Does NOT touch deptId. */
  adminUpdateMyProfile: (data: { nickName?: string; avatar?: string; mobile?: string; email?: string; realName?: string }) =>
    client.put<any, Result<void>>('/admin/admins/profile', data),
  /** Change current admin's own password (verifies oldPassword). */
  adminChangePassword: (data: { oldPassword: string; newPassword: string }) =>
    client.put<any, Result<void>>('/admin/admins/password', data),
  adminAssignRoles: (id: string, roleIds: string[]) =>
    client.put<any, Result<void>>(`/admin/admins/${id}/roles`, roleIds),
  adminDisable: (id: string) =>
    client.put<any, Result<void>>(`/admin/admins/${id}/disable`),
  adminEnable: (id: string) =>
    client.put<any, Result<void>>(`/admin/admins/${id}/enable`),
  adminDelete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/admins/${id}`),
  adminResetPassword: (id: string, newPassword: string) =>
    client.put<any, Result<void>>(`/admin/admins/${id}/password/reset`, { newPassword }),

  // ---- Batch ops ----
  adminBatchEnable: (ids: string[]) =>
    client.post<any, Result<any>>('/admin/admins/batch-enable', ids),
  adminBatchDisable: (ids: string[]) =>
    client.post<any, Result<any>>('/admin/admins/batch-disable', ids),
  adminBatchDelete: (ids: string[]) =>
    client.post<any, Result<any>>('/admin/admins/batch-delete', ids),

  // ---- Roles ----
  roleList: (params?: { pageNum?: number; pageSize?: number; keyword?: string }) =>
    client.get<any, Result<Role[] | PageResult<Role>>>('/admin/roles', { params }),
  roleGetById: (id: string) =>
    client.get<any, Result<Role>>(`/admin/roles/${id}`),
  roleCreate: (data: { roleKey: string; roleName: string; sort: number }) =>
    client.post<any, Result<string>>('/admin/roles', data),
  roleUpdate: (id: string, data: { roleName: string; sort: number }) =>
    client.put<any, Result<void>>(`/admin/roles/${id}`, data),
  roleAssignMenus: (id: string, menuIds: string[]) =>
    client.put<any, Result<void>>(`/admin/roles/${id}/menus`, menuIds),
  roleUpdateDataScope: (id: string, data: { dataScope: number; customDeptIds?: string }) =>
    client.put<any, Result<void>>(`/admin/roles/${id}/data-scope`, data),
  roleDelete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/roles/${id}`),

  // ---- Menus ----
  menuTree: () =>
    client.get<any, Result<MenuItem[]>>('/admin/menus/tree'),
  menuCreate: (data: Partial<MenuItem>) =>
    client.post<any, Result<string>>('/admin/menus', data),
  menuUpdate: (id: string, data: Partial<MenuItem>) =>
    client.put<any, Result<void>>(`/admin/menus/${id}`, data),
  menuDelete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/menus/${id}`),

  // ---- Departments ----
  deptTree: () =>
    client.get<any, Result<Dept[]>>('/admin/depts/tree'),
  deptList: () =>
    client.get<any, Result<Dept[]>>('/admin/depts'),
  deptCreate: (data: Partial<Dept>) =>
    client.post<any, Result<string>>('/admin/depts', data),
  deptUpdate: (id: string, data: Partial<Dept>) =>
    client.put<any, Result<void>>(`/admin/depts/${id}`, data),
  deptDelete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/depts/${id}`),

  // ---- Dict: legacy read-by-type (still used by old DictManager callers) ----
  dictByType: (type: string) =>
    client.get<any, Result<DictData[]>>(`/admin/dict/type/${type}`),

  // ---- Dict Types CRUD (new) ----
  dictTypeList: (params?: { pageNum?: number; pageSize?: number; keyword?: string }) =>
    client.get<any, Result<PageResult<DictType>>>('/admin/dict/types', { params }),
  dictTypeCreate: (data: CreateDictTypeCommand) =>
    client.post<any, Result<string>>('/admin/dict/types', data),
  dictTypeUpdate: (id: string, data: Partial<CreateDictTypeCommand>) =>
    client.put<any, Result<void>>(`/admin/dict/types/${id}`, data),
  dictTypeDelete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/dict/types/${id}`),

  // ---- Dict Data CRUD (new) ----
  dictDataList: (params: { pageNum?: number; pageSize?: number; dictType: string; keyword?: string }) =>
    client.get<any, Result<PageResult<DictDataItem>>>('/admin/dict/data', { params }),
  dictDataCreate: (data: CreateDictDataCommand) =>
    client.post<any, Result<string>>('/admin/dict/data', data),
  dictDataUpdate: (id: string, data: Partial<CreateDictDataCommand>) =>
    client.put<any, Result<void>>(`/admin/dict/data/${id}`, data),
  dictDataDelete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/dict/data/${id}`),

  // ---- Online Users ----
  onlineUsers: (params?: any) =>
    client.get<any, Result<PageResult<any>>>('/admin/online-users', { params }),
  kickOnlineUser: (userId: string) =>
    client.post<any, Result<void>>(`/admin/online-users/${userId}/kick`),
}
