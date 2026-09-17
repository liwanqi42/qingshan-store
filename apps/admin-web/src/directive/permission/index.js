import store from '@/store'
import { checkPermission, checkRole } from '@/utils/permission'

function hasPermission(value) {
  const roles = store.getters['user/roles']
  const permissions = store.getters['user/permissions']
  return checkRole(roles, 'admin') || checkPermission(permissions, value)
}

function removeElement(el) {
  if (el.parentNode) {
    el.parentNode.removeChild(el)
  }
}

export const permission = {
  inserted(el, binding) {
    if (!hasPermission(binding.value)) {
      removeElement(el)
    }
  }
}

export const role = {
  inserted(el, binding) {
    if (!checkRole(store.getters['user/roles'], binding.value)) {
      removeElement(el)
    }
  }
}
