function normalize(value) {
  if (Array.isArray(value)) {
    return value.filter(item => typeof item === 'string' && item)
  }
  if (typeof value === 'string' && value) {
    return [value]
  }
  return []
}

export function checkPermission(userPermissions, requiredPermissions) {
  const permissions = normalize(userPermissions)
  const required = normalize(requiredPermissions)

  if (!required.length) {
    return true
  }
  if (permissions.includes('*') || permissions.includes('*:*:*')) {
    return true
  }
  return required.some(code => permissions.includes(code))
}

export function checkRole(userRoles, requiredRoles) {
  const roles = normalize(userRoles)
  const required = normalize(requiredRoles)

  if (!required.length) {
    return true
  }
  if (roles.includes('admin')) {
    return true
  }
  return required.some(role => roles.includes(role))
}

export function checkRouteAccess(route, roles, permissions, menuPaths) {
  const meta = (route && route.meta) || {}

  // 登录页、401/404、redirect 等特殊路由不参与权限判断，避免跳转死循环
  if (meta.notNeedAuth || meta.hidden) {
    return true
  }

  const menus = normalize(menuPaths)
  if (menus.length) {
    if (menus.includes('*')) {
      return true
    }
    if (!route.name || route.name === 'DashboardView') {
      return true
    }
    return menus.includes(route.name)
  }

  // 没有菜单配置时回退到路由 meta 上的角色/权限标识
  if (meta.roles && !checkRole(roles, meta.roles)) {
    return false
  }
  if (
    meta.permissions &&
    !checkRole(roles, 'admin') &&
    !checkPermission(permissions, meta.permissions)
  ) {
    return false
  }
  return true
}
