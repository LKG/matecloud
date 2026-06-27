<template>
  <MatePageCard :title="t('identityConfig.title')" :description="t('identityConfig.description')">
    <div class="id-layout" v-loading="loading">
      <!-- provider list -->
      <div class="id-list">
        <div
          v-for="p in providers"
          :key="p.type"
          class="id-item"
          :class="{ sel: current && p.type === current.type }"
          @click="select(p)"
        >
          <div class="id-left">
            <img v-if="providerIcon(p.type)" :src="providerIcon(p.type)!" class="id-logo" :alt="p.type" />
            <span v-else class="id-logo id-logo-fb"><Network :size="18" /></span>
            <div class="id-meta">
              <div class="id-name">{{ providerName(p) }}</div>
              <div class="id-desc">{{ p.describe }}</div>
            </div>
          </div>
          <el-tag v-if="p.enabled" type="success" size="small" effect="light">{{ t('identityConfig.enabledTag') }}</el-tag>
        </div>
        <el-empty v-if="!loading && !providers.length" :description="errorMsg || t('identityConfig.empty')" :image-size="80" />
      </div>

      <!-- config panel -->
      <div class="id-panel" v-if="current">
        <div class="id-panel-hd">
          <div class="id-hd-title">
            <img v-if="providerIcon(current.type)" :src="providerIcon(current.type)!" class="id-logo id-logo-sm" :alt="current.type" />
            <span v-else class="id-logo id-logo-sm id-logo-fb"><Network :size="16" /></span>
            <span class="id-title">{{ providerName(current) }}</span>
            <span class="id-desc"> · type={{ current.type }}</span>
          </div>
          <el-switch v-model="form.enabled" :active-text="t('identityConfig.setActive')" />
        </div>

        <el-form label-width="170px" class="id-form">
          <template v-for="f in current.fields" :key="f.key">
            <el-form-item :label="f.label" :required="f.required">
              <template v-if="f.type === 'SECRET'">
                <el-input
                  v-model="form.values[f.key]"
                  type="password"
                  show-password
                  :placeholder="secretConfigured[f.key] ? t('identityConfig.secretConfigured') : t('identityConfig.secretPlaceholder')"
                />
                <div v-if="f.tip" class="tip">{{ f.tip }}</div>
              </template>
              <template v-else-if="f.type === 'TEXTAREA'">
                <el-input v-model="form.values[f.key]" type="textarea" :rows="3" :placeholder="f.placeholder" />
                <div v-if="f.tip" class="tip">{{ f.tip }}</div>
              </template>
              <template v-else>
                <el-input v-model="form.values[f.key]" :placeholder="f.placeholder" clearable />
                <div v-if="f.tip" class="tip">{{ f.tip }}</div>
              </template>
            </el-form-item>
          </template>

          <!-- callback URL hint (providers that expose a callback token) -->
          <el-form-item v-if="supportsCallback" :label="t('identityConfig.callbackUrl')">
            <div class="cb-row">
              <el-input :model-value="callbackUrl" readonly />
              <el-button @click="copyCallback">{{ t('identityConfig.copy') }}</el-button>
            </div>
            <div class="tip">{{ t('identityConfig.callbackTip') }}</div>
          </el-form-item>
        </el-form>

        <!-- last sync result -->
        <div class="id-sync" v-if="lastSync">
          <el-alert
            :type="lastSync.success ? 'success' : 'error'"
            :closable="false"
            show-icon
          >
            <template v-if="lastSync.success">
              {{ t('identityConfig.syncOk', {
                dept: lastSync.deptCount, add: lastSync.userAdded,
                upd: lastSync.userUpdated, rm: lastSync.userRemoved,
              }) }}
            </template>
            <template v-else>{{ t('identityConfig.syncFail') }}: {{ lastSync.error }}</template>
          </el-alert>
        </div>

        <div class="id-foot">
          <el-button @click="doTest" :loading="testing">{{ t('identityConfig.test') }}</el-button>
          <el-button @click="doSync" :loading="syncing">{{ t('identityConfig.sync') }}</el-button>
          <el-button type="primary" :loading="saving" @click="doSave">{{ t('identityConfig.save') }}</el-button>
        </div>
      </div>
    </div>
  </MatePageCard>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { channelApi, ssoApi, type ChannelProviderView, type SsoSyncResult } from '@matecloud/core'
import { MatePageCard, MateMessage, MateMessageBox } from '@matecloud/ui'
import { Network } from 'lucide-vue-next'
import wecomUrl from '@/assets/channel-icons/wecom.svg?url'
import dingtalkUrl from '@/assets/channel-icons/dingtalk.svg?url'
import feishuUrl from '@/assets/channel-icons/feishu.svg?url'

const { t, te } = useI18n()

const CHANNEL = 'identity'

// Provider brand logo by type; LDAP has no brand icon → lucide fallback in template.
const PROVIDER_LOGO: Record<string, string> = {
  wechat_work: wecomUrl,
  dingtalk: dingtalkUrl,
  feishu: feishuUrl,
}
function providerIcon(type: string): string | null {
  return PROVIDER_LOGO[type] || null
}

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const syncing = ref(false)

const providers = ref<ChannelProviderView[]>([])
const errorMsg = ref('')
const current = ref<ChannelProviderView | null>(null)
const form = reactive<{ enabled: boolean; values: Record<string, any> }>({ enabled: false, values: {} })
const secretConfigured = reactive<Record<string, boolean>>({})
const lastSync = ref<SsoSyncResult | null>(null)

// Friendly provider name via i18n when present, else backend-provided.
function providerName(p: ChannelProviderView): string {
  const k = `identityConfig.provider.${p.type}`
  return te(k) ? t(k) : p.name
}

// A provider supports the contacts-change callback when it exposes a callback token field.
const supportsCallback = computed(() =>
  !!current.value?.fields.some((f) => f.key === 'callbackToken'))

const callbackUrl = computed(() =>
  current.value ? `${window.location.origin}/api/v1/sso/callback/${current.value.type}` : '')

async function load() {
  loading.value = true
  current.value = null
  lastSync.value = null
  errorMsg.value = ''
  try {
    const { data } = await channelApi.list(CHANNEL)
    providers.value = data.providers || []
    if (providers.value.length) {
      select(providers.value.find((p) => p.enabled) || providers.value[0])
    }
  } catch (e: any) {
    // Surface the real reason (e.g. "SSO 未启用") in the empty state, not a generic message.
    errorMsg.value = e?.message || t('identityConfig.loadFail')
    MateMessage.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}

function select(p: ChannelProviderView) {
  current.value = p
  lastSync.value = null
  form.enabled = p.enabled
  const vals: Record<string, any> = {}
  Object.keys(secretConfigured).forEach((k) => delete secretConfigured[k])
  for (const f of p.fields) {
    if (f.type === 'SECRET') {
      vals[f.key] = ''
      secretConfigured[f.key] = p.values?.[f.key] === '******'
    } else {
      vals[f.key] = p.values?.[f.key] ?? ''
    }
  }
  form.values = vals
}

async function doSave() {
  if (!current.value) return
  saving.value = true
  try {
    await channelApi.save(CHANNEL, current.value.type, { enabled: form.enabled, values: { ...form.values } })
    MateMessage.success(t('identityConfig.saveSuccess'))
    await load()
  } finally {
    saving.value = false
  }
}

async function doTest() {
  if (!current.value) return
  testing.value = true
  try {
    const { data } = await channelApi.test(CHANNEL, current.value.type)
    if (data.success) MateMessage.success(data.message || t('identityConfig.testPass'))
    else MateMessage.error(data.message || t('identityConfig.testFail'))
  } finally {
    testing.value = false
  }
}

async function doSync() {
  if (!current.value) return
  const type = current.value.type
  try {
    await MateMessageBox.confirm(t('identityConfig.syncConfirm', { name: providerName(current.value) }),
      t('identityConfig.sync'), { type: 'warning' })
  } catch {
    return // cancelled
  }
  syncing.value = true
  lastSync.value = null
  try {
    const { data } = await ssoApi.sync(type, 'FULL')
    lastSync.value = data
    if (data.success) MateMessage.success(t('identityConfig.syncDone'))
    else MateMessage.error(t('identityConfig.syncFail'))
  } catch (e: any) {
    MateMessage.error(e?.message || t('identityConfig.syncFail'))
  } finally {
    syncing.value = false
  }
}

async function copyCallback() {
  try {
    await navigator.clipboard.writeText(callbackUrl.value)
    MateMessage.success(t('identityConfig.copied'))
  } catch {
    window.prompt(t('identityConfig.copy'), callbackUrl.value)
  }
}

load()
</script>

<style scoped>
.id-layout { display: grid; grid-template-columns: 300px 1fr; gap: 20px; margin-top: 12px; }
@media (max-width: 768px) { .id-layout { grid-template-columns: 1fr; gap: 12px; } }
.id-list { display: flex; flex-direction: column; gap: 8px; }
.id-item {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 12px 14px; border: 1px solid var(--el-border-color); border-radius: 8px;
  cursor: pointer; transition: 0.15s;
}
.id-item:hover { background: var(--el-fill-color-light); }
.id-item.sel { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px rgba(64, 128, 255, 0.08); }
.id-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.id-logo { width: 32px; height: 32px; flex: none; border-radius: 8px; object-fit: contain; }
.id-logo-fb {
  display: flex; align-items: center; justify-content: center;
  background: var(--el-fill-color); color: var(--el-text-color-secondary);
}
.id-meta { min-width: 0; }
.id-name { font-weight: 600; }
.id-desc { font-size: 12px; color: var(--el-text-color-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.id-panel { border: 1px solid var(--el-border-color); border-radius: 12px; overflow: hidden; }
.id-panel-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; background: var(--el-fill-color-lighter); border-bottom: 1px solid var(--el-border-color);
}
.id-title { font-weight: 600; font-size: 15px; }
.id-hd-title { display: flex; align-items: center; gap: 8px; }
.id-logo-sm { width: 22px; height: 22px; border-radius: 6px; }
.id-hd-actions { display: flex; align-items: center; gap: 14px; }
.id-form { padding: 18px 20px 4px; }
.tip { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.cb-row { display: flex; gap: 8px; width: 100%; }
.id-sync { padding: 0 20px; }
.id-foot {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 14px 20px 18px; border-top: 1px solid var(--el-border-color-lighter); margin-top: 12px;
}
</style>
