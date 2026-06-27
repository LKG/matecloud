import { client } from '../client'
import type { Result, PageResult } from '../../types/result'

/**
 * Server runtime information returned by {@code GET /admin/monitor/server}.
 */
export interface ServerInfoVO {
  cpu: {
    cores: number
    systemLoadAverage?: string
    processUsage?: string
    systemUsage?: string
    totalPhysicalMemory?: string
    freePhysicalMemory?: string
    usedPhysicalMemory?: string
  }
  jvm: {
    maxMemory: string
    totalMemory: string
    freeMemory: string
    usedMemory: string
    usagePercent?: string
    javaVersion: string
    jvmName: string
    javaHome?: string
    startTime?: string
    uptime?: string
    gcCount?: number
    gcTime?: string
    threadCount?: number
  }
  os: {
    name: string
    arch: string
    version?: string
    availableProcessors: number
    userDir?: string
  }
  disk: {
    total: string
    free: string
    used: string
    usagePercent?: string
  }
}

/**
 * Redis cache information returned by {@code GET /admin/monitor/cache}.
 */
export interface CacheInfoVO {
  redis: {
    version: string
    mode?: string
    os?: string
    tcpPort?: string
    usedMemory: string
    usedMemoryRss?: string
    usedMemoryPeak?: string
    maxMemory: string
    maxMemoryPolicy?: string
    connectedClients: string
    blockedClients?: string
    uptimeDays: string
    uptimeSeconds?: string
    dbSize: number
    totalConnectionsReceived?: string
    totalCommandsProcessed?: string
    instantaneousOpsPerSec?: string
    keyspaceHits?: string
    keyspaceMisses?: string
    hitRate?: string
    usedCpuSys?: string
    usedCpuUser?: string
  }
  commandStats: Array<{ name: string; value: string }>
}

/** A single Redis key entry from the key browser. */
export interface CacheKeyItem {
  key: string
  type: string
  ttl: number
  size: string
}

/** Detail of a single Redis key including its value. */
export interface CacheKeyDetail extends CacheKeyItem {
  value: string
}

export const monitorApi = {
  serverInfo: () => client.get<any, Result<ServerInfoVO>>('/admin/monitor/server'),

  cacheInfo: () => client.get<any, Result<CacheInfoVO>>('/admin/monitor/cache'),

  /** Scan Redis keys with optional glob pattern. */
  cacheKeys: (params?: { pattern?: string; pageNum?: number; pageSize?: number }) =>
    client.get<any, Result<PageResult<CacheKeyItem>>>('/admin/monitor/cache/keys', { params }),

  /** Get value + metadata for a specific key. */
  cacheKeyDetail: (key: string) =>
    client.get<any, Result<CacheKeyDetail>>(`/admin/monitor/cache/keys/${encodeURIComponent(key)}`),

  /** Delete a specific Redis key. */
  cacheKeyDelete: (key: string) =>
    client.delete<any, Result<void>>(`/admin/monitor/cache/keys/${encodeURIComponent(key)}`),
}
