<template>
  <MatePageCard title="公海规则" description="私海客户超过 N 天无跟进自动回收进公海的规则配置">
    <div v-loading="loading" class="pool-form">
      <el-form label-width="160px" style="max-width: 520px">
        <el-form-item label="启用自动回收">
          <el-switch v-model="enabled" :disabled="!canEdit" />
        </el-form-item>
        <el-form-item label="无跟进回收天数">
          <el-input-number v-model="form.recycleDays" :min="1" :disabled="!canEdit || !enabled" controls-position="right" />
          <span class="hint">私海客户超过该天数无跟进将被回收</span>
        </el-form-item>
        <el-form-item label="单人最大持有数">
          <el-input-number v-model="form.maxHold" :min="0" :disabled="!canEdit" controls-position="right" />
          <span class="hint">0 表示不限制</span>
        </el-form-item>
        <el-form-item label="到期提醒天数">
          <el-input-number v-model="form.remindDays" :min="0" :disabled="!canEdit" controls-position="right" />
          <span class="hint">回收前多少天提醒负责人</span>
        </el-form-item>
        <el-form-item>
          <MateButton v-permission="'crm:pool:config:edit'" variant="primary" :loading="saving" @click="handleSave">保存配置</MateButton>
        </el-form-item>
      </el-form>
    </div>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { poolConfigApi, useAuthStore, type PoolConfigCommand } from '@matecloud/core'
import { MatePageCard, MateButton, MateMessage } from '@matecloud/ui'

defineOptions({ name: 'CrmPoolConfig' })

const auth = useAuthStore()
const canEdit = computed(() => auth.hasPermission?.('crm:pool:config:edit') ?? true)

const loading = ref(false)
const saving = ref(false)
const enabled = ref(false)
const form = reactive<{ recycleDays: number; maxHold: number; remindDays: number }>({ recycleDays: 30, maxHold: 0, remindDays: 3 })

async function load() {
  loading.value = true
  try {
    const res = await poolConfigApi.get()
    const c = res.data
    if (c) {
      enabled.value = c.enabled === 1
      form.recycleDays = c.recycleDays ?? 30
      form.maxHold = c.maxHold ?? 0
      form.remindDays = c.remindDays ?? 3
    }
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}

async function handleSave() {
  saving.value = true
  try {
    const payload: PoolConfigCommand = {
      enabled: enabled.value ? 1 : 0,
      recycleDays: form.recycleDays, maxHold: form.maxHold, remindDays: form.remindDays,
    }
    await poolConfigApi.save(payload)
    MateMessage.success('保存成功')
    load()
  } catch (e: any) { MateMessage.error(e?.msg || '保存失败') }
  finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.pool-form { padding: 8px 4px; }
.hint { margin-left: 12px; color: var(--el-text-color-secondary); font-size: 12px; }
</style>
