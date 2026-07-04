<template>
  <MatePageCard :title="t('log.loginTitle')" :description="t('log.loginDesc')">
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="search.username"
        :placeholder="t('log.username')"
        style="width: 160px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-select v-model="search.status" :placeholder="t('log.status')" clearable style="width: 130px">
        <el-option :label="t('log.filterSuccess')" :value="0" />
        <el-option :label="t('log.filterFailed')" :value="1" />
      </el-select>
      <el-select v-model="search.loginType" :placeholder="t('log.loginType')" clearable style="width: 140px">
        <el-option
          v-for="opt in loginTypeOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-date-picker
        v-model="search.range"
        type="datetimerange"
        :range-separator="'~'"
        :start-placeholder="t('log.startTime')"
        :end-placeholder="t('log.endTime')"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: min(100%, 360px)"
      />
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="logs"
      :loading="loading"
      row-key="id"
      :action-width="80"
      :action-label="t('common.action')"
      :empty-text="t('common.noData')"
    >
      <template #col-clientIp="{ row }">
        <span class="mc-mono">{{ row.clientIp }}</span>
      </template>
      <template #col-loginType="{ row }">
        <el-tag :type="loginTypeTagType(row.loginType)" size="small" disable-transitions>
          {{ loginTypeLabel(row.loginType) }}
        </el-tag>
      </template>
      <template #col-status="{ row }">
        <MateBadge :status="row.status" domain="log">
          {{ row.status === 0 ? t('log.statusOk') : t('log.statusFail') }}
        </MateBadge>
      </template>
      <template #col-userAgent="{ row }">
        <span class="mc-mono">{{ row.userAgent }}</span>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openDetail(row)">{{ t('log.detail') }}</button>
      </template>
    </MateTable>

    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadData"
    />

    <!-- Detail dialog -->
    <MateDialog
      v-model="detailVisible"
      :title="t('log.detail')"
      width="600px"
      :show-footer="false"
    >
      <template #footer>
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
      </template>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-grid">
            <div class="kv"><span>{{ t('log.username') }}</span><b>{{ detail.username }}</b></div>
            <div class="kv"><span>{{ t('log.status') }}</span>
              <MateBadge :status="detail.status" domain="log">
                {{ detail.status === 0 ? t('log.statusOk') : t('log.statusFail') }}
              </MateBadge>
            </div>
            <div class="kv"><span>{{ t('log.loginType') }}</span>
              <el-tag :type="loginTypeTagType(detail.loginType)" size="small" disable-transitions>
                {{ loginTypeLabel(detail.loginType) }}
              </el-tag>
            </div>
            <div class="kv"><span>{{ t('log.time') }}</span><b>{{ detail.createdAt }}</b></div>
            <div class="kv"><span>{{ t('log.ip') }}</span><b class="mc-mono">{{ detail.clientIp }}</b></div>
            <div v-if="detail.location" class="kv"><span>{{ t('log.location') }}</span><b>{{ detail.location }}</b></div>
            <div v-if="detail.browser" class="kv"><span>{{ t('log.browser') }}</span><b>{{ detail.browser }}</b></div>
            <div v-if="detail.os" class="kv"><span>{{ t('log.os') }}</span><b>{{ detail.os }}</b></div>
            <div v-if="detail.userAgent" class="kv kv--full">
              <span>{{ t('log.userAgent') }}</span><b class="mc-mono wrap">{{ detail.userAgent }}</b>
            </div>
            <div v-if="detail.failMsg" class="kv kv--full">
              <span>{{ t('log.failMsg') }}</span><b class="error">{{ detail.failMsg }}</b>
            </div>
          </div>
        </template>
      </div>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'LoginLogView' })
import { MatePageCard, MateTable, MateBadge, MateSearchBar, MatePagination, MateDialog, type MateColumn, MateMessage } from '@matecloud/ui'
import { logApi, type LoginLogItem } from '@matecloud/core'

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'username', label: t('log.username'), width: 150 },
  { prop: 'clientIp', label: t('log.ip'), width: 130 },
  { prop: 'loginType', label: t('log.loginType'), width: 110 },
  { prop: 'status', label: t('log.status'), width: 90, align: 'center' },
  { prop: 'failMsg', label: t('log.failMsg') },
  { prop: 'userAgent', label: t('log.userAgent'), width: 220 },
  { prop: 'createdAt', label: t('log.time'), width: 160 },
]

// Login-type options / labels. Values are the UPPERCASE enum names the backend
// stores in mate_login_log.login_type (PASSWORD / SMS / SSO / LDAP / REFRESH).
const loginTypeOptions = computed(() => [
  { value: 'PASSWORD', label: t('log.loginTypePassword') },
  { value: 'SMS', label: t('log.loginTypeSms') },
  { value: 'SSO', label: t('log.loginTypeSso') },
  { value: 'LDAP', label: t('log.loginTypeLdap') },
  { value: 'REFRESH', label: t('log.loginTypeRefresh') },
])
function loginTypeLabel(code?: string) {
  if (!code) return '-'
  const hit = loginTypeOptions.value.find((x) => x.value === code.toUpperCase())
  return hit ? hit.label : code
}
// 令牌续期(REFRESH)用 warning 橙色醒目区分, 其余登录用中性 info。
function loginTypeTagType(code?: string): 'warning' | 'info' {
  return code?.toUpperCase() === 'REFRESH' ? 'warning' : 'info'
}

const loading = ref(false)
const logs = ref<LoginLogItem[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const search = reactive({
  username: '',
  status: undefined as number | undefined,
  loginType: undefined as string | undefined,
  range: [] as string[],
})

async function loadData() {
  loading.value = true
  try {
    const res = await logApi.loginLogs({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: search.username || undefined,
      status: search.status,
      loginType: search.loginType || undefined,
      startTime: search.range?.[0],
      endTime: search.range?.[1],
    })
    const data: any = res.data ?? res
    if (Array.isArray(data)) {
      logs.value = data
      total.value = data.length
    } else {
      logs.value = data.list ?? []
      total.value = data.total ?? 0
    }
  } catch {
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  search.username = ''
  search.status = undefined
  search.loginType = undefined
  search.range = []
  handleSearch()
}

// ---- detail dialog ----
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

async function openDetail(row: LoginLogItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await logApi.loginLogDetail(row.id)
    detail.value = res.data ?? row
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
    detail.value = row
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.detail-body { min-height: 180px; }
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}
.kv {
  display: flex; align-items: center; gap: 10px;
  font-size: 13px;
  min-width: 0;
}
.kv--full { grid-column: span 2; }
.kv > span {
  color: var(--mc-text-muted);
  flex-shrink: 0;
  min-width: 90px;
}
.kv > b {
  color: var(--mc-text-primary);
  font-weight: 500;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  min-width: 0;
}
.kv > b.wrap { white-space: normal; word-break: break-all; }
.kv > b.error { color: var(--mc-danger); }
</style>
