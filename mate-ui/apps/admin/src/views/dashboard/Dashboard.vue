<template>
  <div v-loading="loading" class="dashboard">
    <div class="dashboard-inner">
      <!-- ===== Welcome Card ===== -->
      <div class="glass-card welcome-card">
        <div class="welcome-content">
          <p class="welcome-kicker">{{ greeting }}</p>
          <h2 class="welcome-title">{{ displayName }}</h2>
          <p class="welcome-sub">{{ t('login.subtitle') }}</p>
          <div v-if="roleCodes.length" class="welcome-roles">
            <span v-for="r in roleCodes" :key="r" class="role-chip">{{ r }}</span>
          </div>
        </div>
        <div class="welcome-actions">
          <el-button @click="loadData">
            <RefreshCw :size="14" class="mr-1" />{{ t('common.refresh') }}
          </el-button>
        </div>
      </div>

      <!-- ===== Stat Cards (real data) ===== -->
      <div class="stat-grid">
        <div v-for="stat in statCards" :key="stat.label" class="glass-card stat-card">
          <div class="stat-icon" :style="{ background: stat.color }">
            <component :is="stat.icon" :size="22" :stroke-width="2.2" />
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>
      </div>

      <!-- ===== Charts Row ===== -->
      <div class="chart-grid">
        <div class="glass-card chart-card">
          <div class="chart-header">
            <h3 class="chart-title">{{ t('dashboard.trendTitle') }}</h3>
            <span class="chart-badge">7d</span>
          </div>
          <Suspense>
            <DashboardTrendChart
              v-if="data?.last7Days?.length"
              :days="data.last7Days"
              :api-label="t('dashboard.legendApiCalls')"
              :login-label="t('dashboard.legendLogins')"
            />
            <template #fallback>
              <div class="chart-loading">…</div>
            </template>
          </Suspense>
        </div>

        <div class="glass-card chart-card">
          <div class="chart-header">
            <h3 class="chart-title">{{ t('dashboard.servicesTitle') }}</h3>
            <span class="chart-badge">Live</span>
          </div>
          <div class="services-list">
            <div v-for="svc in (data?.services ?? [])" :key="svc.name" class="service-row">
              <div class="service-left">
                <span class="status-dot" :class="svc.state.toLowerCase()" />
                <span class="service-name">{{ svc.name }}</span>
              </div>
              <span class="service-meta">
                <span v-if="svc.latencyMs != null" class="service-latency">{{ svc.latencyMs }}ms</span>
                <span class="service-state" :class="svc.state.toLowerCase()">{{ svc.state }}</span>
              </span>
            </div>
            <div v-if="!data?.services?.length" class="services-empty">
              {{ t('dashboard.noServices') }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { MateMessage } from '@matecloud/ui'
import { computed, defineAsyncComponent, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Users as IconUsers,
  Activity as IconActivity,
  LogIn as IconLogin,
  Wifi as IconOnline,
  RefreshCw,
} from 'lucide-vue-next'
import {
  useAuthStore,
  dashboardApi,
  type DashboardVO,
} from '@matecloud/core'

defineOptions({ name: 'DashboardView' })

// Lazy-load the ECharts wrapper. This is the trick that keeps echarts (~870KB)
// out of the main bundle and lets the production bundler split it into its
// own chunk on first render. Without this, Vite 8's rolldown (Rust) OOMs on
// Windows when minifying the combined echarts + app bundle in one pass.
const DashboardTrendChart = defineAsyncComponent(() => import('./DashboardTrendChart.vue'))

const { t } = useI18n()
const auth = useAuthStore()

const displayName = computed(() => auth.user?.realName || auth.user?.username || 'Guest')
const roleCodes = computed(() => auth.roleCodes ?? [])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return t('dashboard.greetingMorning')
  if (h < 18) return t('dashboard.greetingAfternoon')
  return t('dashboard.greetingEvening')
})

// ----- Data load -----
const loading = ref(false)
const data = ref<DashboardVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await dashboardApi.get()
    data.value = res.data
  } catch (e: any) {
    MateMessage.error(e?.msg || e?.message || t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ----- Stat cards driven off data -----
const statCards = computed(() => {
  const d = data.value
  return [
    {
      label: t('dashboard.statAdmins'),
      value: d ? d.userCount.toLocaleString() : '—',
      icon: IconUsers,
      color: 'linear-gradient(135deg, var(--mc-primary), #4f5fe9)',
    },
    {
      label: t('dashboard.statTodayLogins'),
      value: d ? d.todayLoginCount.toLocaleString() : '—',
      icon: IconLogin,
      color: 'linear-gradient(135deg, #00b42a, #34d058)',
    },
    {
      label: t('dashboard.statTodayOps'),
      value: d ? d.todayOpCount.toLocaleString() : '—',
      icon: IconActivity,
      color: 'linear-gradient(135deg, #f76b6b, #ff9a44)',
    },
    {
      label: t('dashboard.statOnline'),
      value: d ? d.onlineCount.toLocaleString() : '—',
      icon: IconOnline,
      color: 'linear-gradient(135deg, #0099ff, #00c6ff)',
    },
  ]
})

onMounted(loadData)
</script>

<style scoped>
.dashboard {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.dashboard-inner {
  /* 外部间距对齐列表页紧凑风格:layout-content(8) + dashboard-inner(8) = 16px */
  padding: 8px;
}

.glass-card {
  position: relative;
  border-radius: var(--mc-radius-lg);
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border);
  box-shadow: var(--mc-shadow);
  transition: box-shadow 0.3s ease, transform 0.3s, border-color 0.3s ease;
}
/* Border brighten is the hover cue that survives dark mode — the shadow
   step (--mc-shadow-md) is near-invisible on a dark canvas. */
.glass-card:hover {
  box-shadow: var(--mc-shadow-md);
  border-color: var(--mc-border-hover);
}

/* ---- Welcome ---- */
.welcome-card {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  overflow: hidden;
}
.welcome-content { flex: 1; }
.welcome-kicker {
  font-size: 12px; font-weight: 600;
  letter-spacing: 1px; text-transform: uppercase;
  color: var(--mc-text-muted); margin: 0 0 6px;
}
.welcome-title {
  font-size: 24px; font-weight: 800; line-height: 1.2;
  margin: 0; letter-spacing: -0.02em;
  color: var(--mc-text-primary);
}
.welcome-sub {
  margin: 8px 0 0;
  font-size: 13px; color: var(--mc-text-muted);
}
.welcome-roles {
  margin-top: 16px; display: flex; flex-wrap: wrap; gap: 8px;
}
.role-chip {
  display: inline-flex; align-items: center;
  padding: 4px 12px; border-radius: 999px;
  font-size: 11px; font-weight: 600;
  color: var(--mc-primary);
  background: var(--mc-primary-light-5);
  border: 1px solid var(--mc-primary-light-3);
}
.welcome-actions { flex-shrink: 0; }

/* ---- Stat grid ---- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px; margin-top: 12px;
}
@media (max-width: 1024px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px)  { .stat-grid { grid-template-columns: 1fr; } }

.stat-card {
  padding: 16px 18px;
  display: flex; align-items: center; gap: 14px;
}
.stat-card:hover { transform: translateY(-2px); }
.stat-icon {
  width: 48px; height: 48px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.35), 0 4px 12px -3px rgba(0,0,0,0.15);
}
.stat-info { display: flex; flex-direction: column; }
.stat-value {
  font-size: 22px; font-weight: 800;
  line-height: 1.1; letter-spacing: -0.5px;
  color: var(--mc-text-primary);
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 11px; text-transform: uppercase;
  letter-spacing: 0.5px; font-weight: 500;
  color: var(--mc-text-muted); margin-top: 4px;
}

/* ---- Charts ---- */
.chart-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 12px; margin-top: 12px;
}
@media (max-width: 1024px) { .chart-grid { grid-template-columns: 1fr; } }
.chart-card { padding: 16px 20px; }

.chart-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.chart-title {
  font-size: 15px; font-weight: 700;
  color: var(--mc-text-primary); margin: 0;
}
.chart-badge {
  font-size: 11px; font-weight: 600;
  padding: 3px 10px; border-radius: 999px;
  background: var(--mc-primary-light-5);
  color: var(--mc-primary);
  border: 1px solid var(--mc-primary-light-3);
}

.chart-loading {
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mc-text-muted);
  font-size: 24px;
}

/* ---- Services ---- */
.services-list { display: flex; flex-direction: column; gap: 6px; }
.service-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; border-radius: var(--mc-radius);
  background: var(--mc-fill);
  transition: background 0.2s, transform 0.2s;
}
.service-row:hover {
  background: var(--mc-fill-hover);
  transform: translateX(3px);
}
.service-left { display: flex; align-items: center; gap: 10px; }
.service-name { font-size: 13px; font-weight: 600; color: var(--mc-text-primary); }
.service-meta { display: flex; align-items: center; gap: 8px; }
.service-latency {
  font-size: 11px; font-family: 'JetBrains Mono', monospace;
  color: var(--mc-text-muted);
}
.service-state {
  font-size: 11px; font-weight: 700;
  padding: 2px 6px; border-radius: 4px;
}
.service-state.up   { background: rgb(23 178 106 / 0.12); color: var(--mc-success); }
.service-state.down { background: rgb(240 68 56 / 0.12); color: var(--mc-danger); }
.service-state.unknown { background: var(--mc-fill); color: var(--mc-text-muted); }

.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.up   { background: var(--mc-success); box-shadow: 0 0 8px rgba(23, 178, 106, 0.6); }
.status-dot.down { background: var(--mc-danger);  box-shadow: 0 0 8px rgba(240, 68, 56, 0.6); }
.status-dot.unknown { background: var(--mc-text-disabled); }

.services-empty {
  padding: 32px 0;
  text-align: center;
  color: var(--mc-text-muted);
  font-size: 13px;
}
</style>
