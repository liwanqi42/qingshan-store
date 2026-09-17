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
