/**
 * Tenant management composable — single source of truth for list + CRUD state.
 * All child components share this via provide/inject or direct import.
 */
import { MateMessage } from '@matecloud/ui'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { tenantApi, TenantStatus, type TenantInfo, type TenantPackageInfo, type TenantCommand } from '@matecloud/core'

export function useTenant() {
  const { t } = useI18n()

  // ========== List state ==========
  const loading = ref(false)
  const tableData = ref<TenantInfo[]>([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const keyword = ref('')
  const packages = ref<TenantPackageInfo[]>([])

  async function loadData() {
    loading.value = true
    try {
      const res = await tenantApi.page({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: keyword.value,
      })
      const data: any = res.data ?? res
      let list: TenantInfo[]
      if (Array.isArray(data)) {
        list = data
        total.value = data.length
      } else {
        list = data.list ?? data
        total.value = data.total ?? 0
      }
      // Backend serializes the TenantStatus enum by NAME (e.g. "ACTIVE"), not its
      // numeric code. Normalize to the 0/1/2/3 codes the table/badge logic expects.
      tableData.value = (list ?? []).map(t => ({ ...t, status: normalizeStatus((t as any).status) }))
    } catch (e: any) {
      MateMessage.error(e?.msg || t('common.loadFailed'))
    } finally {
      loading.value = false
    }
  }

  async function loadPackages() {
    try {
      const res = await tenantApi.packages()
      const data: any = res.data ?? res
      packages.value = Array.isArray(data) ? data : []
    } catch {
      packages.value = []
    }
  }

  function handleSearch() {
    pageNum.value = 1
    loadData()
  }

  function handleReset() {
    keyword.value = ''
    handleSearch()
  }

  // ========== Create / Edit ==========
  const formDialogVisible = ref(false)
  const editing = ref<TenantInfo | null>(null)
  const submitting = ref(false)
  const isEdit = computed(() => !!editing.value)

  const form = reactive<Record<string, any>>({
    tenantCode: '',
    tenantName: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    packageId: '',
    domain: '',
    expireAt: null,
    remark: '',
  })

  function openFormDialog(row?: TenantInfo) {
    editing.value = row ?? null
    form.tenantCode = row?.tenantCode ?? ''
    form.tenantName = row?.tenantName ?? ''
    form.contactName = row?.contactName ?? ''
    form.contactPhone = row?.contactPhone ?? ''
    form.contactEmail = row?.contactEmail ?? ''
    form.packageId = row?.packageId ?? ''
    form.domain = row?.domain ?? ''
    form.expireAt = row?.expireAt ? new Date(row.expireAt) : null
    form.remark = row?.remark ?? ''
    formDialogVisible.value = true
  }

  async function submitForm(): Promise<boolean> {
    submitting.value = true
    try {
      const cmd: TenantCommand = {
        tenantCode: form.tenantCode,
        tenantName: form.tenantName,
        contactName: form.contactName,
        contactPhone: form.contactPhone,
        contactEmail: form.contactEmail,
        packageId: form.packageId,
        domain: form.domain,
        // Backend LocalDateTime deserializer expects "yyyy-MM-dd HH:mm:ss"
        // (platform Jackson pattern), NOT ISO-8601 with a 'T'/'Z'.
        expireAt: form.expireAt ? toBackendDateTime(form.expireAt) : undefined,
        remark: form.remark,
      }
      if (isEdit.value) {
        await tenantApi.update(editing.value!.id, cmd)
      } else {
        await tenantApi.create(cmd)
      }
      MateMessage.success(t('common.success'))
      formDialogVisible.value = false
      loadData()
      return true
    } catch (e: any) {
      MateMessage.error(e?.msg || t('common.failed'))
      return false
    } finally {
      submitting.value = false
    }
  }

  // ========== Suspend / Activate ==========
  async function handleSuspend(row: TenantInfo) {
    try {
      await tenantApi.suspend(row.id)
      MateMessage.success(t('tenant.suspendSuccess'))
      loadData()
    } catch (e: any) {
      MateMessage.error(e?.msg || t('common.failed'))
    }
  }

  async function handleActivate(row: TenantInfo) {
    try {
      await tenantApi.activate(row.id)
      MateMessage.success(t('tenant.activateSuccess'))
      loadData()
    } catch (e: any) {
      MateMessage.error(e?.msg || t('common.failed'))
    }
  }

  // ========== Renew ==========
  const renewDialogVisible = ref(false)
  const renewingRow = ref<TenantInfo | null>(null)
  const renewExpireAt = ref<Date | null>(null)

  function openRenewDialog(row: TenantInfo) {
    renewingRow.value = row
    renewExpireAt.value = row.expireAt ? new Date(row.expireAt) : null
    renewDialogVisible.value = true
  }

  async function submitRenew(): Promise<boolean> {
    if (!renewExpireAt.value) {
      MateMessage.warning(t('tenant.selectExpiry'))
      return false
    }
    submitting.value = true
    try {
      await tenantApi.renew(renewingRow.value!.id, new Date(renewExpireAt.value).getTime())
      MateMessage.success(t('tenant.renewSuccess'))
      renewDialogVisible.value = false
      loadData()
      return true
    } catch (e: any) {
      MateMessage.error(e?.msg || t('common.failed'))
      return false
    } finally {
      submitting.value = false
    }
  }

  // ========== Delete ==========
  async function handleDelete(row: TenantInfo) {
    try {
      await tenantApi.delete(row.id)
      MateMessage.success(t('common.success'))
      loadData()
    } catch (e: any) {
      MateMessage.error(e?.msg || t('common.failed'))
    }
  }

  // ========== Helpers ==========
  /**
   * Coerce a status value into the numeric code (0/1/2/3) the UI maps on.
   * The backend may return either the numeric code or — by Jackson's default
   * enum serialization — the enum NAME ("ACTIVE"/"SUSPENDED"/"EXPIRED"/"DELETED").
   */
  function normalizeStatus(status: any): TenantStatus {
    if (typeof status === 'number') return status as TenantStatus
    const byName: Record<string, TenantStatus> = {
      ACTIVE: TenantStatus.ACTIVE,
      SUSPENDED: TenantStatus.SUSPENDED,
      EXPIRED: TenantStatus.EXPIRED,
      DELETED: TenantStatus.DELETED,
    }
    if (typeof status === 'string') {
      if (status in byName) return byName[status]
      const n = Number(status)
      if (!Number.isNaN(n)) return n as TenantStatus
    }
    return TenantStatus.ACTIVE
  }

  function getPackageName(packageId: string): string {
    if (!packageId) return '-'
    const pkg = packages.value.find(p => p.id === packageId)
    return pkg?.packageName ?? packageId
  }

  function statusBadge(status: number): string {
    const map: Record<number, string> = { 0: 'ACTIVE', 1: 'DISABLED', 2: 'WARNING', 3: 'DISABLED' }
    return map[status] ?? 'DISABLED'
  }

  function statusLabel(status: number): string {
    const map: Record<number, string> = {
      0: t('tenant.statusActive'),
      1: t('tenant.statusSuspended'),
      2: t('tenant.statusExpired'),
      3: t('tenant.statusDeleted'),
    }
    return map[status] ?? '-'
  }

  function isExpired(expireAt: string): boolean {
    if (!expireAt) return false
    return new Date(expireAt).getTime() < Date.now()
  }

  function formatDate(dateStr: string): string {
    if (!dateStr) return '-'
    const d = new Date(dateStr)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }

  /** Format a Date/value into the backend's "yyyy-MM-dd HH:mm:ss" LocalDateTime pattern. */
  function toBackendDateTime(value: Date | string | number): string {
    const d = new Date(value)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
      `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }

  // ========== Init ==========
  onMounted(() => {
    loadData()
    loadPackages()
  })

  return {
    // list
    loading, tableData, total, pageNum, pageSize, keyword, packages,
    loadData, handleSearch, handleReset,
    // form dialog
    formDialogVisible, editing, isEdit, form, submitting,
    openFormDialog, submitForm,
    // suspend / activate
    handleSuspend, handleActivate,
    // renew
    renewDialogVisible, renewingRow, renewExpireAt,
    openRenewDialog, submitRenew,
    // delete
    handleDelete,
    // helpers
    getPackageName, statusBadge, statusLabel, isExpired, formatDate,
  }
}
