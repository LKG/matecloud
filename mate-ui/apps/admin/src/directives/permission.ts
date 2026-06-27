/**
 * v-permission directive — hide elements when user lacks the required permission.
 *
 * Usage:
 *   <el-button v-permission="'sys:user:delete'">Delete</el-button>
 *   <el-button v-permission="['sys:user:edit', 'sys:user:create']">Edit</el-button>
 *
 * Logic:
 *   - String value: user must have this exact permission (or wildcard '*')
 *   - Array value: user must have ALL listed permissions
 *   - If check fails, element is removed from DOM (not just hidden)
 */
import type { Directive, DirectiveBinding } from 'vue'
import { useAuthStore } from '@matecloud/core'

function checkPermission(el: HTMLElement, binding: DirectiveBinding) {
  const { value } = binding
  if (!value) return

  const auth = useAuthStore()
  const perms = Array.isArray(value) ? value : [value]
  const hasAll = perms.every((p: string) => auth.hasPermission(p))

  if (!hasAll) {
    el.parentNode?.removeChild(el)
  }
}

export const vPermission: Directive = {
  mounted: checkPermission,
  updated: checkPermission,
}

/**
 * v-role directive — hide elements when user lacks the required role.
 *
 * Usage:
 *   <el-button v-role="'super_admin'">Admin Only</el-button>
 */
export const vRole: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    if (!binding.value) return
    const auth = useAuthStore()
    const roles = Array.isArray(binding.value) ? binding.value : [binding.value]
    const hasAny = roles.some((r: string) => auth.hasRole(r))
    if (!hasAny) {
      el.parentNode?.removeChild(el)
    }
  },
}
