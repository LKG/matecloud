import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * Tenant store — aligned with mate-tenant-starter.
 * X-Tenant-Id header is injected via client.ts interceptor.
 */
export const useTenantStore = defineStore('tenant', () => {
  const tenantId = ref<string>(localStorage.getItem('mate_tenant_id') || '')
  const tenantName = ref<string>('')

  function setTenant(id: string, name: string) {
    tenantId.value = id
    tenantName.value = name
    localStorage.setItem('mate_tenant_id', id)
  }

  function clearTenant() {
    tenantId.value = ''
    tenantName.value = ''
    localStorage.removeItem('mate_tenant_id')
  }

  return { tenantId, tenantName, setTenant, clearTenant }
})
