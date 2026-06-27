<template>
  <MatePageCard :title="t('modelConfig.title')" :description="t('modelConfig.description')">
    <el-tabs v-model="tab" class="mc-tabs" @tab-change="onTabChange">
      <el-tab-pane :label="t('modelConfig.tabProviders')" name="providers" />
      <el-tab-pane :label="t('modelConfig.tabSystem')" name="system" />
      <el-tab-pane :label="t('modelConfig.tabGateway')" name="gateway" />
    </el-tabs>

    <div v-loading="loading" class="mc-body">
      <!-- Providers -->
      <section v-show="tab === 'providers'">
        <div class="sec-hd">
          <div>
            <h3 class="sec-tt">{{ t('modelConfig.providersTitle') }}</h3>
            <p class="sec-sub">{{ t('modelConfig.providersSub') }}</p>
          </div>
          <el-button type="primary" @click="openCreate(null)">
            <Plus :size="15" style="margin-right: 5px" />{{ t('modelConfig.addProvider') }}
          </el-button>
        </div>

        <div class="grp-label">{{ t('modelConfig.configured') }}</div>
        <div v-if="providers.length" class="pgrid">
          <ProviderCard
            v-for="p in providers"
            :key="p.id"
            :provider="p"
            @test="testProvider"
            @edit="openEdit"
            @delete="deleteProvider"
          />
        </div>
        <MateEmpty v-else :description="t('modelConfig.noProviders')" />

        <template v-if="addableDescriptors.length">
          <div class="grp-label" style="margin-top: 16px">{{ t('modelConfig.addable') }}</div>
          <div class="pgrid">
            <button
              v-for="d in addableDescriptors"
              :key="d.vendor"
              class="add-card"
              @click="openCreate(d.vendor)"
            >
              <img v-if="vendorIcon(d.vendor)" class="add-logo add-logo-img" :src="vendorIcon(d.vendor)!" :alt="d.vendor" />
              <span v-else class="add-logo" :style="{ background: vendorColor(d.vendor) }">
                {{ vendorInitials(d.name) }}
              </span>
              <span class="add-meta">
                <span class="add-name">{{ descriptorLabel(d) }}</span>
                <span class="add-mods">{{ d.modalities.map((m) => t(`modelConfig.modality.${m}`)).join(' · ') }}</span>
              </span>
              <Plus :size="15" class="add-plus" />
            </button>
          </div>
        </template>
      </section>

      <!-- System default models -->
      <section v-show="tab === 'system'">
        <div class="sec-hd">
          <div>
            <h3 class="sec-tt">{{ t('modelConfig.systemTitle') }}</h3>
            <p class="sec-sub">{{ t('modelConfig.systemSub') }}</p>
          </div>
        </div>
        <SystemModelList :rows="systemModels" :providers="providers" @save="onSaveSystemModel" />
      </section>

      <!-- External gateway -->
      <section v-show="tab === 'gateway'">
        <div class="sec-hd">
          <div>
            <h3 class="sec-tt">{{ t('modelConfig.gatewayTitle') }}</h3>
            <p class="sec-sub">{{ t('modelConfig.gatewaySub') }}</p>
          </div>
        </div>
        <GatewayPanel
          :gateway="gateway"
          :saving="gwSaving"
          :testing="gwTesting"
          @save="onSaveGateway"
          @test="onTestGateway"
        />
      </section>
    </div>

    <ProviderDrawer
      :visible="drawerVisible"
      :provider="editing"
      :preset-vendor="presetVendor"
      :descriptors="descriptors"
      :saving="saving"
      @close="drawerVisible = false"
      @submit="onSubmitProvider"
    />
  </MatePageCard>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus } from 'lucide-vue-next'
import { MateEmpty, MatePageCard } from '@matecloud/ui'
import type { ModelDescriptorView, ModelProviderRequest, ModelProviderView } from '@matecloud/core'
import { useModelConfig } from '@/composables/useModelConfig'
import ProviderCard from './modelconfig/ProviderCard.vue'
import ProviderDrawer from './modelconfig/ProviderDrawer.vue'
import SystemModelList from './modelconfig/SystemModelList.vue'
import GatewayPanel from './modelconfig/GatewayPanel.vue'
import { vendorColor, vendorIcon, vendorInitials } from './modelconfig/theme'

defineOptions({ name: 'ModelConfigView' })

const { t, te } = useI18n()
const {
  loading, providers, descriptors, systemModels, gateway,
  refresh, saveProvider, deleteProvider, testProvider,
  saveSystemModel, saveGateway, testGateway,
} = useModelConfig()

const tab = ref<'providers' | 'system' | 'gateway'>('providers')
const drawerVisible = ref(false)
const editing = ref<ModelProviderView | null>(null)
const presetVendor = ref<string | null>(null)
const saving = ref(false)
const gwSaving = ref(false)
const gwTesting = ref(false)

// Descriptors whose vendor has no configured provider yet → "addable" cards.
const addableDescriptors = computed(() => {
  const used = new Set(providers.value.map((p) => p.vendor))
  return descriptors.value.filter((d) => !used.has(d.vendor))
})

function descriptorLabel(d: ModelDescriptorView): string {
  const k = `modelConfig.vendor.${d.vendor}`
  return te(k) ? t(k) : d.name
}

function onTabChange(_name: string | number) { /* sections are v-show; nothing to load */ }

function openCreate(vendor: string | null) {
  editing.value = null
  presetVendor.value = vendor
  drawerVisible.value = true
}
function openEdit(p: ModelProviderView) {
  editing.value = p
  presetVendor.value = null
  drawerVisible.value = true
}

async function onSubmitProvider(payload: { id: string | null; req: ModelProviderRequest }) {
  saving.value = true
  try {
    await saveProvider(payload.id, payload.req)
    drawerVisible.value = false
  } finally {
    saving.value = false
  }
}

async function onSaveSystemModel(payload: { type: string; providerId: string; model: string }) {
  await saveSystemModel(payload.type, payload.providerId, payload.model)
}

async function onSaveGateway() {
  gwSaving.value = true
  try {
    await saveGateway()
  } finally {
    gwSaving.value = false
  }
}
async function onTestGateway() {
  gwTesting.value = true
  try {
    await testGateway()
  } finally {
    gwTesting.value = false
  }
}

onMounted(refresh)
</script>

<style scoped>
.mc-tabs { margin-top: 4px; }
.mc-body { min-height: 280px; }
.sec-hd { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.sec-tt { font-size: 15px; font-weight: 700; margin: 0; }
.sec-sub { font-size: 12.5px; color: var(--el-text-color-secondary); margin: 4px 0 0; line-height: 1.6; }
.grp-label { font-size: 11px; font-weight: 700; color: var(--el-text-color-disabled); text-transform: uppercase; margin: 8px 0 8px; letter-spacing: 0.3px; }
.pgrid { display: grid; grid-template-columns: repeat(auto-fill, minmax(284px, 1fr)); gap: 14px; }
.add-card {
  display: flex; align-items: center; gap: 10px; text-align: left;
  border: 1px dashed var(--el-border-color); border-radius: 14px;
  background: var(--el-bg-color); padding: 13px 14px; cursor: pointer;
  color: var(--el-text-color-regular); transition: 0.15s; font: inherit;
}
.add-card:hover { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.add-logo {
  width: 36px; height: 36px; border-radius: 10px; flex: none;
  display: grid; place-items: center; color: #fff; font-weight: 800; font-size: 13px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
}
.add-logo-img { background: var(--mc-logo-plate); object-fit: contain; padding: 5px; box-shadow: inset 0 0 0 1px var(--el-border-color-lighter); }
.add-meta { display: flex; flex-direction: column; min-width: 0; }
.add-name { font-weight: 600; font-size: 13.5px; }
.add-mods { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 1px; }
.add-plus { margin-left: auto; color: var(--el-text-color-disabled); flex: none; }
</style>
