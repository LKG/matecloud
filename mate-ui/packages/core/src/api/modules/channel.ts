import { client } from '../client'
import type { Result } from '../../types/result'

/** Field input type — mirrors backend vip.mate.base.channel.FieldType. */
export type ChannelFieldType = 'TEXT' | 'SECRET' | 'TEXTAREA' | 'MAP'

/** Config field metadata for a channel provider (drives the dynamic form). */
export interface ChannelFieldSpec {
  key: string
  label: string
  type: ChannelFieldType
  required: boolean
  placeholder?: string
  tip?: string
}

/** One provider of a channel, with its fields + (masked) current values. */
export interface ChannelProviderView {
  type: string
  name: string
  describe: string
  enabled: boolean
  fields: ChannelFieldSpec[]
  values: Record<string, any>
}

export interface ChannelView {
  channel: string
  active: string | null
  providers: ChannelProviderView[]
}

export interface ChannelTestResult {
  success: boolean
  message: string
}

/**
 * Channel (object storage / SMS) provider configuration. Providers self-describe
 * their fields on the backend; this API returns that metadata so the UI renders a
 * single generic form. Secret fields come back masked ("******") and are preserved
 * on save when left masked/blank.
 */
export const channelApi = {
  /** List all providers of a channel with field metadata + current (masked) values. */
  list: (channel: string) =>
    client.get<any, Result<ChannelView>>(`/admin/channel/${channel}/providers`),

  /** Save a provider's config; {@code enabled=true} also makes it the active provider. */
  save: (channel: string, type: string, data: { enabled: boolean; values: Record<string, any> }) =>
    client.put<any, Result<void>>(`/admin/channel/${channel}/providers/${type}`, data),

  /** Test connectivity (storage) / test-send (sms, to {@code target}). */
  test: (channel: string, type: string, target?: string) =>
    client.post<any, Result<ChannelTestResult>>(`/admin/channel/${channel}/providers/${type}/test`, { target }),
}
