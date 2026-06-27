import { client } from '../client'
import type { Result, PageResult } from '../../types/result'

/** Tenant status enum — aligned with backend TenantStatus */
export enum TenantStatus {
  ACTIVE = 0,
  SUSPENDED = 1,
  EXPIRED = 2,
  DELETED = 3,
}

/** Tenant — aligned with backend Tenant entity */
export interface TenantInfo {
  id: string
  tenantCode: string
  tenantName: string
  contactName: string
  contactPhone: string
  contactEmail: string
  packageId: string
  status: TenantStatus
  domain: string
  expireAt: string
  remark: string
  createdAt: string
}

/** Tenant package — aligned with backend TenantPackage entity */
export interface TenantPackageInfo {
  id: string
  packageCode: string
  packageName: string
  maxUsers: number
  maxStorage: number
  features: string
  remark: string
}

/** Create / Update command */
export interface TenantCommand {
  id?: string
  tenantCode: string
  tenantName: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  packageId?: string
  domain?: string
  expireAt?: string
  remark?: string
}

/**
 * Tenant API — aligned with TenantController:
 * Path: /admin/tenants
 */
export const tenantApi = {
  /** Paginated list */
  page: (params?: { pageNum?: number; pageSize?: number; keyword?: string }) =>
    client.get<any, Result<PageResult<TenantInfo>>>('/admin/tenants', { params }),

  /** Get by ID */
  getById: (id: string) =>
    client.get<any, Result<TenantInfo>>(`/admin/tenants/${id}`),

  /** Get by code */
  getByCode: (code: string) =>
    client.get<any, Result<TenantInfo>>(`/admin/tenants/by-code/${code}`),

  /** List all packages */
  packages: () =>
    client.get<any, Result<TenantPackageInfo[]>>('/admin/tenants/packages'),

  /** Create a package */
  packageCreate: (data: Partial<TenantPackageInfo>) =>
    client.post<any, Result<string>>('/admin/tenants/packages', data),

  /** Update a package (packageCode immutable) */
  packageUpdate: (id: string, data: Partial<TenantPackageInfo>) =>
    client.put<any, Result<void>>(`/admin/tenants/packages/${id}`, data),

  /** Delete a package */
  packageDelete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/tenants/packages/${id}`),

  /** Create */
  create: (data: TenantCommand) =>
    client.post<any, Result<string>>('/admin/tenants', data),

  /** Update */
  update: (id: string, data: TenantCommand) =>
    client.put<any, Result<void>>(`/admin/tenants/${id}`, data),

  /** Suspend */
  suspend: (id: string) =>
    client.post<any, Result<void>>(`/admin/tenants/${id}/suspend`),

  /** Activate */
  activate: (id: string) =>
    client.post<any, Result<void>>(`/admin/tenants/${id}/activate`),

  /** Renew (extend expiry) */
  renew: (id: string, expireAt: number) =>
    client.post<any, Result<void>>(`/admin/tenants/${id}/renew`, null, {
      params: { expireAt },
    }),

  /** Delete */
  delete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/tenants/${id}`),
}
