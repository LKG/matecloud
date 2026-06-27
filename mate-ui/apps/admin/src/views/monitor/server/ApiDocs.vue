<template>
  <MatePageCard
    :title="t('apidocs.title')"
    :description="t('apidocs.description')"
  >
    <template #actions>
      <el-button @click="openFullPage">
        <ExternalLink :size="14" class="mr-1" />{{ t('apidocs.openFull') }}
      </el-button>
    </template>

    <!-- Service tabs -->
    <div class="doc-layout">
      <div class="service-pane">
        <div class="pane-title">{{ t('apidocs.services') }}</div>
        <div
          v-for="svc in services"
          :key="svc.id"
          class="service-row"
          :class="{ 'service-row--active': svc.id === currentService.id }"
          @click="currentService = svc"
        >
          <span class="service-name">{{ svc.name }}</span>
          <span class="service-port mc-mono">:{{ svc.port }}</span>
          <MateBadge :type="svc.up ? 'success' : 'danger'" :dot="false">
            {{ svc.up ? 'UP' : 'DOWN' }}
          </MateBadge>
        </div>
      </div>

      <div class="detail-pane">
        <div class="pane-title">
          {{ currentService.name }}
          <span class="pane-subtitle">— {{ currentService.desc }}</span>
        </div>

        <div class="endpoint-filter">
          <el-input
            v-model="keyword"
            size="small"
            :placeholder="t('apidocs.searchPlaceholder')"
            clearable
            style="width: 260px"
          >
            <template #prefix><Search :size="13" /></template>
          </el-input>
          <el-select v-model="methodFilter" size="small" clearable style="width: 120px" :placeholder="t('apidocs.method')">
            <el-option v-for="m in methods" :key="m" :label="m" :value="m" />
          </el-select>
        </div>

        <div class="endpoint-list">
          <div v-for="ep in filteredEndpoints" :key="ep.id" class="endpoint-row">
            <span class="endpoint-method" :class="`method--${ep.method.toLowerCase()}`">
              {{ ep.method }}
            </span>
            <span class="endpoint-path mc-mono">{{ ep.path }}</span>
            <span class="endpoint-desc">{{ ep.desc }}</span>
            <el-tag v-if="ep.auth" size="small" type="warning">auth</el-tag>
          </div>
          <div v-if="!filteredEndpoints.length" class="empty">{{ t('common.noData') }}</div>
        </div>
      </div>
    </div>

    <p class="demo-hint">
      <Info :size="13" class="mr-1" />
      {{ t('apidocs.hint') }}
    </p>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, ExternalLink, Info } from 'lucide-vue-next'
import { MatePageCard, MateBadge, MateMessage } from '@matecloud/ui'

defineOptions({ name: 'ApiDocsView' })

const { t } = useI18n()

interface Service {
  id: string
  name: string
  port: number
  desc: string
  up: boolean
  docUrl: string
}

interface Endpoint {
  id: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  path: string
  desc: string
  auth?: boolean
}

const services = ref<Service[]>([
  { id: 'gateway', name: 'mate-gateway', port: 9010, desc: 'API Gateway', up: true, docUrl: 'http://localhost:9010/doc.html' },
  { id: 'auth', name: 'mate-auth', port: 9020, desc: 'Authentication / SMS / Captcha', up: true, docUrl: 'http://localhost:9020/doc.html' },
  { id: 'system', name: 'mate-system', port: 9030, desc: 'User domain (DDD example)', up: true, docUrl: 'http://localhost:9030/doc.html' },
  { id: 'admin', name: 'mate-admin', port: 9040, desc: 'RBAC / Dict / Config / Logs', up: true, docUrl: 'http://localhost:9040/doc.html' },
  { id: 'notice', name: 'mate-notice', port: 9050, desc: 'SMS / Email / WeChat', up: false, docUrl: 'http://localhost:9050/doc.html' },
])

const endpointsByService: Record<string, Endpoint[]> = {
  auth: [
    { id: '1', method: 'POST', path: '/auth/login', desc: '密码登录' },
    { id: '2', method: 'POST', path: '/auth/sms/send', desc: '发送短信验证码' },
    { id: '3', method: 'POST', path: '/auth/sms/login', desc: '短信登录' },
    { id: '4', method: 'POST', path: '/auth/register', desc: '注册' },
    { id: '5', method: 'POST', path: '/auth/logout', desc: '登出', auth: true },
    { id: '6', method: 'GET', path: '/auth/info', desc: '获取当前用户', auth: true },
    { id: '7', method: 'GET', path: '/auth/captcha', desc: '获取图形验证码' },
  ],
  system: [
    { id: '1', method: 'GET', path: '/users', desc: '用户分页', auth: true },
    { id: '2', method: 'GET', path: '/users/{id}', desc: '用户详情', auth: true },
    { id: '3', method: 'POST', path: '/users', desc: '创建用户', auth: true },
    { id: '4', method: 'PUT', path: '/users/{id}', desc: '更新用户', auth: true },
    { id: '5', method: 'PUT', path: '/users/{id}/freeze', desc: '冻结', auth: true },
    { id: '6', method: 'PUT', path: '/users/{id}/unfreeze', desc: '解冻', auth: true },
    { id: '7', method: 'DELETE', path: '/users/{id}', desc: '删除', auth: true },
    { id: '8', method: 'PUT', path: '/users/password', desc: '修改自己密码', auth: true },
  ],
  admin: [
    { id: '1', method: 'GET', path: '/admin/admins', desc: '管理员列表', auth: true },
    { id: '2', method: 'POST', path: '/admin/admins', desc: '创建管理员', auth: true },
    { id: '3', method: 'PUT', path: '/admin/admins/{id}/roles', desc: '分配角色', auth: true },
    { id: '4', method: 'GET', path: '/admin/roles', desc: '角色列表', auth: true },
    { id: '5', method: 'POST', path: '/admin/roles', desc: '创建角色', auth: true },
    { id: '6', method: 'PUT', path: '/admin/roles/{id}/menus', desc: '分配菜单', auth: true },
    { id: '7', method: 'GET', path: '/admin/menus/tree', desc: '菜单树', auth: true },
    { id: '8', method: 'GET', path: '/admin/dict/types', desc: '字典类型列表', auth: true },
    { id: '9', method: 'GET', path: '/admin/dict/data', desc: '字典数据列表', auth: true },
    { id: '10', method: 'GET', path: '/admin/configs', desc: '配置列表', auth: true },
    { id: '11', method: 'GET', path: '/admin/operation-logs', desc: '操作日志', auth: true },
    { id: '12', method: 'GET', path: '/admin/login-logs', desc: '登录日志', auth: true },
  ],
  gateway: [
    { id: '1', method: 'GET', path: '/actuator/health', desc: '健康检查' },
    { id: '2', method: 'GET', path: '/actuator/routes', desc: '已注册路由', auth: true },
  ],
  notice: [
    { id: '1', method: 'POST', path: '/notice/sms', desc: '发送短信' },
    { id: '2', method: 'POST', path: '/notice/email', desc: '发送邮件' },
    { id: '3', method: 'POST', path: '/notice/wechat', desc: '发送企业微信' },
  ],
}

const currentService = ref<Service>(services.value[1])
const keyword = ref('')
const methodFilter = ref<string>('')
const methods: Endpoint['method'][] = ['GET', 'POST', 'PUT', 'DELETE']

const filteredEndpoints = computed<Endpoint[]>(() => {
  let list = endpointsByService[currentService.value.id] ?? []
  if (methodFilter.value) list = list.filter(e => e.method === methodFilter.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(e => e.path.toLowerCase().includes(kw) || e.desc.toLowerCase().includes(kw))
  }
  return list
})

function openFullPage() {
  if (!currentService.value.up) {
    MateMessage.warning(t('apidocs.serviceDown'))
    return
  }
  window.open(currentService.value.docUrl, '_blank')
}
</script>

<style scoped>
.doc-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 280px);
  min-height: 420px;
}

.service-pane {
  width: 280px;
  flex-shrink: 0;
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  overflow: hidden;
  background: var(--mc-bg-elevated);
}
.detail-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  padding: 14px 16px;
  background: var(--mc-bg-elevated);
  overflow: hidden;
}

.pane-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--mc-text-primary);
  padding: 10px 14px;
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
}
.detail-pane .pane-title {
  padding: 0 0 6px;
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  margin-bottom: 4px;
}
.pane-subtitle {
  font-weight: 400;
  color: var(--mc-text-muted);
  margin-left: 4px;
  font-size: 12px;
}

.service-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  border-left: 2px solid transparent;
}
.service-row:hover { background: var(--mc-state-hover, rgb(200 206 218 / 0.2)); }
.service-row--active {
  background: var(--mc-menu-bg-active, rgb(21 90 239 / 0.08));
  border-left-color: var(--mc-primary);
}
.service-name { font-size: 13px; font-weight: 500; flex: 1; color: var(--mc-text-primary); }
.service-port { font-size: 11px; color: var(--mc-text-muted); }

.endpoint-filter { display: flex; gap: 8px; flex-shrink: 0; }
.endpoint-list { flex: 1; overflow: auto; display: flex; flex-direction: column; gap: 4px; }

.endpoint-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  transition: background 0.1s;
  font-size: 13px;
}
.endpoint-row:hover { background: var(--mc-state-hover, rgb(200 206 218 / 0.2)); }

.endpoint-method {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 11px; font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  min-width: 54px;
  text-align: center;
  color: #fff;
  flex-shrink: 0;
}
.method--get { background: #17B26A; }
.method--post { background: #155AEF; }
.method--put { background: #F79009; }
.method--delete { background: #F04438; }

.endpoint-path {
  font-size: 12px;
  color: var(--mc-text-primary);
  flex-shrink: 0;
}
.endpoint-desc {
  flex: 1;
  min-width: 0;
  color: var(--mc-text-muted);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty {
  padding: 48px;
  text-align: center;
  color: var(--mc-text-muted);
  font-size: 13px;
}

.demo-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--mc-text-muted);
  background: rgb(54 191 250 / 0.06);
  border-radius: 6px;
}
</style>
