<template>
  <div class="gw-wrap">
    <!-- supply mode toggle -->
    <div class="gw-mode">
      <div class="gw-mode-l">{{ t('modelConfig.supplyMode') }}</div>
      <div class="seg">
        <button class="s" :class="{ on: !gateway.enabled }" @click="setMode(false)">
          {{ t('modelConfig.supplyLocal') }}
        </button>
        <button class="s" :class="{ on: gateway.enabled }" @click="setMode(true)">
          {{ t('modelConfig.supplyGateway') }}
        </button>
      </div>
    </div>

    <!-- flow -->
    <div class="flow">
      <template v-if="gateway.enabled">
        <span class="fnode" style="background: var(--el-color-primary)">mate-ai</span>
        <span class="farrow">→</span>
        <span class="fnode" style="background: #7A5AF8">{{ t('modelConfig.flowGateway') }}</span>
        <span class="farrow">→</span>
        <span class="fnode" style="background: #475467">{{ t('modelConfig.flowRouting') }}</span>
        <span class="farrow">→</span>
        <span class="fnode" style="background: var(--el-color-success)">{{ t('modelConfig.flowVendors') }}</span>
      </template>
      <template v-else>
        <span class="fnode" style="background: var(--el-color-primary)">mate-ai · Spring AI</span>
        <span class="farrow">→</span>
        <span class="fnode" style="background: var(--el-color-success)">{{ t('modelConfig.flowDirect') }}</span>
        <span class="farrow muted">→</span>
        <span class="fnode dim">{{ t('modelConfig.flowNoGateway') }}</span>
      </template>
    </div>

    <div class="gw-split">
      <!-- gateway form -->
      <div class="panel" :class="{ dim: !gateway.enabled }">
        <el-form label-position="top">
          <el-form-item :label="t('modelConfig.gatewayType')">
            <el-select v-model="gateway.gatewayType" style="width: 100%">
              <el-option label="New-API" value="NEW_API" />
              <el-option label="LiteLLM Proxy" value="LITELLM" />
              <el-option label="One-API / One-Hub" value="ONE_API" />
              <el-option :label="t('modelConfig.gatewayCustom')" value="CUSTOM" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('modelConfig.gatewayBaseUrl')" required>
            <el-input v-model="gateway.baseUrl" placeholder="http://new-api:3000/v1" clearable />
          </el-form-item>
          <el-form-item :label="t('modelConfig.gatewayToken')" required>
            <el-input
              v-model="gateway.token"
              type="password"
              show-password
              :placeholder="t('modelConfig.secretPlaceholder')"
            />
            <div class="tip">{{ t('modelConfig.secretTipKeep') }}</div>
          </el-form-item>
          <el-form-item :label="t('modelConfig.gatewayGroup')">
            <el-input v-model="gateway.defaultGroup" placeholder="default" clearable />
          </el-form-item>
          <el-form-item :label="t('modelConfig.gatewayMapping')">
            <el-input
              v-model="gateway.modelMapping"
              type="textarea"
              :rows="4"
              placeholder='{"chat":"deepseek-chat","embed":"bge-m3"}'
            />
            <div class="tip">{{ t('modelConfig.gatewayMappingTip') }}</div>
          </el-form-item>
          <div class="gw-foot">
            <el-button :loading="testing" @click="$emit('test')">{{ t('modelConfig.testGateway') }}</el-button>
            <el-button type="primary" :loading="saving" @click="$emit('save')">{{ t('modelConfig.save') }}</el-button>
          </div>
        </el-form>
      </div>

      <!-- responsibility split -->
      <div class="gw-side">
        <h4 class="gw-side-tt">{{ t('modelConfig.respTitle') }}</h4>
        <table class="rtbl">
          <thead>
            <tr><th>{{ t('modelConfig.respCol') }}</th><th>{{ t('modelConfig.respWho') }}</th></tr>
          </thead>
          <tbody>
            <tr v-for="(r, i) in respRows" :key="i">
              <td>{{ t(r.label) }}</td>
              <td>
                <MateBadge :type="r.who === 'mate' ? 'success' : 'info'">
                  {{ t(r.who === 'mate' ? 'modelConfig.respMate' : 'modelConfig.respGateway') }}
                </MateBadge>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="note">
          <b>{{ t('modelConfig.respNoteTitle') }}</b>{{ t('modelConfig.respNote') }}
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useI18n } from 'vue-i18n'
import { MateBadge } from '@matecloud/ui'
import type { ModelGatewayView } from '@matecloud/core'

const props = defineProps<{
  gateway: ModelGatewayView
  saving: boolean
  testing: boolean
}>()
defineEmits<{ (e: 'save'): void; (e: 'test'): void }>()

const { t } = useI18n()

const respRows: Array<{ label: string; who: 'mate' | 'gateway' }> = [
  { label: 'modelConfig.respProviders', who: 'mate' },
  { label: 'modelConfig.respTenant', who: 'mate' },
  { label: 'modelConfig.respRouting', who: 'gateway' },
  { label: 'modelConfig.respBilling', who: 'gateway' },
  { label: 'modelConfig.respAdapt', who: 'gateway' },
]

function setMode(enabled: boolean) {
  props.gateway.enabled = enabled
}
</script>

<style scoped>
.gw-mode { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }
.gw-mode-l { font-size: 13px; font-weight: 600; color: var(--el-text-color-regular); }
.seg { display: flex; gap: 6px; }
.seg .s {
  padding: 6px 13px; border: 1px solid var(--el-border-color);
  border-radius: 8px; background: var(--el-bg-color); cursor: pointer;
  font-weight: 600; font-size: 12px; color: var(--el-text-color-regular); transition: 0.15s;
}
.seg .s.on {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.flow {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 13px; border: 1px dashed var(--el-border-color);
  border-radius: 12px; background: var(--el-fill-color-lighter); margin-bottom: 16px;
}
.fnode { padding: 7px 11px; border-radius: 9px; font-weight: 700; font-size: 12px; color: #fff; }
.fnode.dim { background: var(--el-fill-color-dark); color: var(--el-text-color-secondary); }
.farrow { color: var(--el-text-color-disabled); }
.farrow.muted { opacity: 0.4; }
.gw-split { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
@media (max-width: 980px) { .gw-split { grid-template-columns: 1fr; } }
.panel {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color);
  border-radius: 12px; padding: 14px 16px 4px; transition: opacity 0.2s;
}
.panel.dim { opacity: 0.55; }
.gw-foot { display: flex; justify-content: flex-end; gap: 10px; padding: 4px 0 14px; }
.tip { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.gw-side-tt { font-size: 14px; font-weight: 700; margin: 0 0 10px; }
.rtbl { width: 100%; border-collapse: collapse; font-size: 13px; }
.rtbl th {
  text-align: left; font-size: 12px; color: var(--el-text-color-regular); font-weight: 700;
  padding: 9px 10px; border-bottom: 1px solid var(--el-border-color);
  background: var(--el-fill-color-light); white-space: nowrap;
}
.rtbl td { padding: 10px; border-bottom: 1px solid var(--el-border-color-lighter); vertical-align: middle; }
.note {
  font-size: 12px; color: var(--el-text-color-secondary);
  background: var(--el-bg-color); border: 1px solid var(--el-border-color);
  border-left: 3px solid #7A5AF8; border-radius: 8px;
  padding: 11px 13px; line-height: 1.7; margin-top: 14px;
}
.note b { color: var(--el-text-color-primary); }
</style>
