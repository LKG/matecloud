import { client } from '../client'
import type { Result, PageResult } from '../../types/result'

export interface ConfigItem {
  id: string
  configKey: string
  configValue: string
  configName: string
  builtIn: boolean
  remark: string
  createdAt: string
}

export interface ConfigQuery {
  pageNum?: number
  pageSize?: number
  /** fuzzy match against configKey / configName */
  keyword?: string
  /** undefined = both, true = built-in only, false = custom only */
  builtIn?: boolean
}

export const configApi = {
  /**
   * Paginated config list. Backend may return either an array (legacy)
   * or a {@link PageResult}; callers should handle both shapes.
   */
  list: (params?: ConfigQuery) =>
    client.get<any, Result<ConfigItem[] | PageResult<ConfigItem>>>('/admin/configs', { params }),

  getById: (id: string) =>
    client.get<any, Result<ConfigItem>>(`/admin/configs/${id}`),

  getByKey: (key: string) =>
    client.get<any, Result<ConfigItem>>(`/admin/configs/key/${key}`),

  create: (data: { configKey: string; configValue: string; configName: string; remark?: string }) =>
    client.post<any, Result<string>>('/admin/configs', data),

  update: (id: string, data: { configValue: string; remark?: string }) =>
    client.put<any, Result<void>>(`/admin/configs/${id}`, data),

  delete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/configs/${id}`),
}
