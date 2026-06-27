<template>
  <MateDialog
    :model-value="visible"
    :title="isEdit ? t('tenant.editTenant') : t('tenant.createTenant')"
    :submitting="submitting"
    @update:model-value="$emit('update:visible', $event)"
    @submit="handleSubmit"
  >
    <MateForm
      ref="formRef"
      :schema="formSchema"
      :model-value="form"
      label-position="top"
      @update:model-value="(v: any) => $emit('update:form', v)"
    />
  </MateDialog>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { MateDialog, MateForm, type FormSchema } from '@matecloud/ui'
import type { TenantPackageInfo } from '@matecloud/core'

const props = defineProps<{
  visible: boolean
  isEdit: boolean
  form: Record<string, any>
  submitting: boolean
  packages: TenantPackageInfo[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'update:form', val: Record<string, any>): void
  (e: 'submit'): void
}>()

const { t } = useI18n()
const formRef = ref<InstanceType<typeof MateForm>>()

// MateForm only reseeds its internal model when the modelValue object REFERENCE
// changes; the parent mutates the same `form` object in place, so we explicitly
// repopulate via setModel each time the dialog opens (the documented API for
// "open with edit data"). Without this the edit form shows stale/empty fields.
watch(() => props.visible, (open) => {
  if (open) {
    nextTick(() => formRef.value?.setModel(props.form))
  }
})

const packageOptions = computed(() =>
  props.packages.map(p => ({ label: p.packageName, value: p.id }))
)

const formSchema = computed<FormSchema[]>(() => [
  {
    field: 'tenantCode',
    label: t('tenant.tenantCode'),
    type: 'input',
    disabled: () => props.isEdit,
    rules: [{ required: true, message: t('tenant.tenantCodeRequired'), trigger: 'blur' }],
  },
  {
    field: 'tenantName',
    label: t('tenant.tenantName'),
    type: 'input',
    rules: [{ required: true, message: t('tenant.tenantNameRequired'), trigger: 'blur' }],
  },
  {
    field: 'contactName',
    label: t('tenant.contactName'),
    type: 'input',
  },
  {
    field: 'contactPhone',
    label: t('tenant.contactPhone'),
    type: 'input',
  },
  {
    field: 'contactEmail',
    label: t('tenant.contactEmail'),
    type: 'input',
  },
  {
    field: 'packageId',
    label: t('tenant.package'),
    type: 'select',
    options: packageOptions.value,
  },
  {
    field: 'domain',
    label: t('tenant.domain'),
    type: 'input',
    placeholder: t('tenant.domainPlaceholder'),
  },
  {
    field: 'expireAt',
    label: t('tenant.expireAt'),
    type: 'date',
    placeholder: t('tenant.selectExpiry'),
  },
  {
    field: 'remark',
    label: t('tenant.remark'),
    type: 'textarea',
  },
])

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  emit('submit')
}

defineExpose({ formRef })
</script>
