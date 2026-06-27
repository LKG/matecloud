import { client } from '../client'
import type { Result, PageResult } from '../../types/result'

export type MaterialType = 'image' | 'video' | 'doc'

/** 分片上传初始化结果(后端 MultipartInitVO)。 */
export interface MultipartInit {
  bucket: string
  objectName: string
  uploadId: string
}

export interface MaterialCategory {
  id: string
  name: string
  sort: number
  count: number
}

export interface MaterialCategoryList {
  total: number
  ungrouped: number
  categories: MaterialCategory[]
}

export interface MaterialItem {
  id: string
  type: MaterialType
  categoryId: string | null
  name: string
  objectName: string
  url: string
  size: number
  ext: string
  mime: string
  uploadedBy: string | null
  createdAt: string
}

export interface MaterialQuery {
  type: MaterialType
  categoryId?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

/**
 * 素材管理。素材内容存当前启用的对象存储(渠道配置),仅元数据走该接口。
 * 小文件单次整传(uploadSingle → POST /admin/material/upload,走应用服务器);
 * 大文件走分片直传(init → 逐片预签名 PUT 到对象存储 → complete),其余为 JSON。
 */
export const materialApi = {
  /** 上传地址(供 el-upload 的 action;现已改用 uploadSingle/分片,保留以兼容) */
  uploadAction: `${client.defaults.baseURL || ''}/admin/material/upload`,

  /** 单次整传(multipart/form-data,带上传进度回调)。 */
  uploadSingle: (
    file: File,
    type: MaterialType,
    categoryId: string | undefined,
    onProgress?: (percent: number) => void,
  ) => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('type', type)
    if (categoryId) fd.append('categoryId', categoryId)
    return client.post<any, Result<MaterialItem>>('/admin/material/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100))
      },
    })
  },

  /** 初始化分片上传 → 返回 objectName + uploadId。 */
  initMultipart: (body: { type: MaterialType; filename: string; contentType: string; size: number }) =>
    client.post<any, Result<MultipartInit>>('/admin/material/upload/multipart/init', body),

  /** 取某一分片(1-based)的预签名直传 URL。 */
  partUrl: (params: { objectName: string; uploadId: string; partNumber: number; expirySeconds?: number }) =>
    client.get<any, Result<{ url: string }>>('/admin/material/upload/multipart/part-url', { params }),

  /** 合并分片并落素材元数据。 */
  completeMultipart: (body: {
    type: MaterialType
    categoryId?: string
    objectName: string
    uploadId: string
    filename: string
    size: number
    contentType: string
  }) => client.post<any, Result<MaterialItem>>('/admin/material/upload/multipart/complete', body),

  /** 放弃分片上传,释放已上传分片。 */
  abortMultipart: (objectName: string, uploadId: string) =>
    client.delete<any, Result<void>>('/admin/material/upload/multipart', {
      params: { objectName, uploadId },
    }),

  categories: (type: MaterialType) =>
    client.get<any, Result<MaterialCategoryList>>('/admin/material/categories', { params: { type } }),

  createCategory: (type: MaterialType, name: string) =>
    client.post<any, Result<string>>('/admin/material/categories', { type, name }),

  renameCategory: (id: string, name: string) =>
    client.put<any, Result<void>>(`/admin/material/categories/${id}`, { name }),

  deleteCategory: (id: string) =>
    client.delete<any, Result<void>>(`/admin/material/categories/${id}`),

  page: (params: MaterialQuery) =>
    client.get<any, Result<PageResult<MaterialItem>>>('/admin/material', { params }),

  rename: (id: string, name: string) =>
    client.put<any, Result<void>>(`/admin/material/${id}/rename`, { name }),

  move: (id: string, categoryId: string | null) =>
    client.put<any, Result<void>>(`/admin/material/${id}/move`, { categoryId }),

  delete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/material/${id}`),

  url: (id: string) =>
    client.get<any, Result<{ url: string }>>(`/admin/material/${id}/url`),
}
