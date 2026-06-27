<template>
  <MatePageCard :title="t('channelConfig.title')" :description="t('channelConfig.description')">
    <el-tabs v-model="channel" @tab-change="loadChannel">
      <el-tab-pane :label="t('channelConfig.tabStorage')" name="storage" />
      <el-tab-pane :label="t('channelConfig.tabSms')" name="sms" />
    </el-tabs>

    <div class="ch-layout" v-loading="loading">
      <!-- provider list -->
      <div class="ch-list">
        <div
          v-for="p in providers"
          :key="p.type"
          class="ch-item"
          :class="{ sel: current && p.type === current.type }"
          @click="select(p)"
        >
          <div class="ch-meta">
            <div class="ch-name">{{ providerName(p) }}</div>
            <div class="ch-desc">{{ providerDesc(p) }}</div>
          </div>
          <el-tag v-if="p.enabled" type="success" size="small" effect="light">{{ t('channelConfig.enabledTag') }}</el-tag>
        </div>
      </div>

      <!-- config form -->
      <div class="ch-panel" v-if="current">
        <div class="ch-panel-hd">
          <div>
            <span class="ch-title">{{ providerName(current) }} {{ t('channelConfig.configTitle') }}</span>
            <span class="ch-desc"> · type={{ current.type }}</span>
          </div>
          <el-switch v-model="form.enabled" :active-text="t('channelConfig.setActive')" />
        </div>

        <el-form label-width="150px" class="ch-form">
          <template v-for="f in current.fields" :key="f.key">
            <el-form-item :label="fieldLabel(f)" :required="f.required">
              <template v-if="f.type === 'SECRET'">
                <el-input
                  v-model="form.values[f.key]"
                  type="password"
                  show-password
                  :placeholder="secretConfigured[f.key] ? t('channelConfig.secretConfigured') : t('channelConfig.secretPlaceholder')"
                />
                <div class="tip">{{ t('channelConfig.secretTipKeep') }}</div>
              </template>
              <template v-else-if="f.type === 'MAP'">
                <div class="kv-row" v-for="(row, i) in mapRows[f.key]" :key="i">
                  <el-input v-model="row.k" :placeholder="t('channelConfig.businessType')" style="flex:1" />
                  <span class="kv-arrow">→</span>
                  <el-input v-model="row.v" :placeholder="t('channelConfig.templateValue')" style="flex:1" />
                  <el-button text type="danger" @click="mapRows[f.key].splice(i, 1)">×</el-button>
                </div>
                <el-button link type="primary" @click="mapRows[f.key].push({ k: '', v: '' })">{{ t('channelConfig.addRow') }}</el-button>
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

          <el-form-item v-if="!current.fields.length">
            <span class="ch-desc">{{ t('channelConfig.noFields') }}</span>
          </el-form-item>
        </el-form>

        <div class="ch-foot">
          <el-button @click="doTest" :loading="testing">
            {{ channel === 'sms' ? t('channelConfig.testSms') : t('channelConfig.testStorage') }}
          </el-button>
          <el-button type="primary" :loading="saving" @click="doSave">{{ t('channelConfig.save') }}</el-button>
        </div>
      </div>
    </div>
  </MatePageCard>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { channelApi, type ChannelProviderView } from '@matecloud/core'
import { MatePageCard, MateMessage, MateMessageBox } from '@matecloud/ui'

const { t, te } = useI18n()

// Backend descriptors carry Chinese labels; overlay an i18n label by stable
// key/type when one exists, else fall back to the backend-provided text.
function providerName(p: ChannelProviderView): string {
  const k = `channelConfig.provider.${p.type}`
  return te(k) ? t(k) : p.name
}
function fieldLabel(f: { key: string; label: string }): string {
  const k = `channelConfig.field.${f.key}`
  return te(k) ? t(k) : f.label
}
function providerDesc(p: ChannelProviderView): string {
  const k = `channelConfig.providerDesc.${p.type}`
  return te(k) ? t(k) : p.describe
}

const channel = ref<'storage' | 'sms'>('storage')
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)

const providers = ref<ChannelProviderView[]>([])
const current = ref<ChannelProviderView | null>(null)
const form = reactive<{ enabled: boolean; values: Record<string, any> }>({ enabled: false, values: {} })
const mapRows = reactive<Record<string, Array<{ k: string; v: string }>>>({})
const secretConfigured = reactive<Record<string, boolean>>({})

async function loadChannel() {
  loading.value = true
  current.value = null
  try {
    const { data } = await channelApi.list(channel.value)
    providers.value = data.providers || []
    if (providers.value.length) {
      select(providers.value.find((p) => p.enabled) || providers.value[0])
    }
  } finally {
    loading.value = false
  }
}

function select(p: ChannelProviderView) {
  current.value = p
  form.enabled = p.enabled
  const vals: Record<string, any> = {}
  Object.keys(mapRows).forEach((k) => delete mapRows[k])
  Object.keys(secretConfigured).forEach((k) => delete secretConfigured[k])
  for (const f of p.fields) {
    if (f.type === 'SECRET') {
      vals[f.key] = ''
      secretConfigured[f.key] = p.values?.[f.key] === '******'
    } else if (f.type === 'MAP') {
      const obj = (p.values?.[f.key] as Record<string, any>) || {}
      mapRows[f.key] = Object.entries(obj).map(([k, v]) => ({ k, v: String(v) }))
    } else {
      vals[f.key] = p.values?.[f.key] ?? ''
    }
  }
  form.values = vals
}

function assembleValues(): Record<string, any> {
  const values: Record<string, any> = { ...form.values }
  if (!current.value) return values
  for (const f of current.value.fields) {
    if (f.type === 'MAP') {
      const obj: Record<string, string> = {}
      for (const row of mapRows[f.key] || []) {
        if (row.k && row.k.trim()) obj[row.k.trim()] = row.v
      }
      values[f.key] = obj
    }
  }
  return values
}

async function doSave() {
  if (!current.value) return
  saving.value = true
  try {
    await channelApi.save(channel.value, current.value.type, { enabled: form.enabled, values: assembleValues() })
    MateMessage.success(t('channelConfig.saveSuccess'))
    await loadChannel()
  } finally {
    saving.value = false
  }
}

async function doTest() {
  if (!current.value) return
  const type = current.value.type
  try {
    let target: string | undefined
    if (channel.value === 'sms') {
      const { value } = await MateMessageBox.prompt(t('channelConfig.smsTargetPrompt'), t('channelConfig.testSms'), {
        confirmButtonText: t('channelConfig.sendBtn'),
        cancelButtonText: t('channelConfig.cancel'),
        inputPattern: /^\d{6,15}$/,
        inputErrorMessage: t('channelConfig.mobileInvalid'),
      })
      target = value
    }
    testing.value = true
    const { data } = await channelApi.test(channel.value, type, target)
    if (data.success) MateMessage.success(data.message || t('channelConfig.testPass'))
    else MateMessage.error(data.message || t('channelConfig.testFail'))
  } catch (e) {
    // prompt cancelled — ignore
  } finally {
    testing.value = false
  }
}

loadChannel()
</script>

<style scoped>
.ch-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
  margin-top: 12px;
}
/* Phone: 300px + detail don't fit — stack to a single column. */
@media (max-width: 768px) {
  .ch-layout {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
.ch-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: 0.15s;
}
.ch-item:hover { background: var(--el-fill-color-light); }
.ch-item.sel { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px rgba(64, 128, 255, 0.08); }
.ch-name { font-weight: 600; }
.ch-desc { font-size: 12px; color: var(--el-text-color-secondary); }
.ch-panel { border: 1px solid var(--el-border-color); border-radius: 12px; overflow: hidden; }
.ch-panel-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: var(--el-fill-color-lighter);
  border-bottom: 1px solid var(--el-border-color);
}
.ch-title { font-weight: 600; font-size: 15px; }
.ch-form { padding: 18px 20px 4px; }
.tip { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.kv-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.kv-arrow { color: var(--el-text-color-secondary); }
.ch-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px 18px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
