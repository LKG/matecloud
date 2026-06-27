import { client } from '../client'
import type { Result, PageQuery, PageResult } from '../../types/result'
import type { UserInfo } from '../../types/user'

/**
 * User API — aligned with mate-system UserController
 */
export const userApi = {
  list: (params?: PageQuery) =>
    client.get<any, Result<PageResult<UserInfo>>>('/users', { params }),

  getById: (id: string) =>
    client.get<any, Result<UserInfo>>(`/users/${id}`),

  create: (data: { mobile: string; nickName: string; password?: string }) =>
    client.post<any, Result<string>>('/users', data),

  update: (id: string, data: Partial<UserInfo>) =>
    client.put<any, Result<void>>(`/users/${id}`, data),

  changeNickName: (id: string, nickName: string) =>
    client.put<any, Result<void>>(`/users/${id}/nickname`, null, { params: { nickName } }),

  freeze: (id: string) =>
    client.put<any, Result<void>>(`/users/${id}/freeze`),

  unfreeze: (id: string) =>
    client.put<any, Result<void>>(`/users/${id}/unfreeze`),

  delete: (id: string) =>
    client.delete<any, Result<void>>(`/users/${id}`),

  /**
   * Change the current user's password. Backend route exposed by
   * mate-auth / mate-system's UserController as {@code PUT /users/password}.
   */
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    client.put<any, Result<void>>('/users/password', data),

  /**
   * Admin-only: reset another user's password. Requires the caller to hold
   * the {@code sys:user:reset} permission. Backend returns {@code void} on
   * success; the new password is supplied by the caller (mate-system does
   * not generate temporary passwords).
   */
  resetPassword: (id: string, newPassword: string) =>
    client.put<any, Result<void>>(`/users/${id}/password/reset`, { newPassword }),

  // ---- Batch ops ----
  // Each returns BatchResult — see useBatch composable for the shape.
  batchFreeze: (ids: string[]) =>
    client.post<any, Result<any>>('/users/batch-freeze', ids),
  batchUnfreeze: (ids: string[]) =>
    client.post<any, Result<any>>('/users/batch-unfreeze', ids),
  batchDelete: (ids: string[]) =>
    client.post<any, Result<any>>('/users/batch-delete', ids),
}
